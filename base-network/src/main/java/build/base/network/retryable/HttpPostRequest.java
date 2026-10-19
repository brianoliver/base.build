package build.base.network.retryable;

/*-
 * #%L
 * base.build Network
 * %%
 * Copyright (C) 2026 The base.build Authors and Contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import build.base.configuration.AbstractValueOption;
import build.base.configuration.Configuration;
import build.base.configuration.ConfigurationBuilder;
import build.base.configuration.Option;
import build.base.foundation.Strings;
import build.base.option.Attribute;
import build.base.retryable.EphemeralFailureException;
import build.base.retryable.PermanentFailureException;
import build.base.retryable.Retryable;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Objects;

/**
 * A {@link Retryable} to perform a http-based {@code POST} request.
 *
 * @author brian.oliver
 * @since Mar-2021
 */
public class HttpPostRequest
    implements Retryable<String> {

    /**
     * The {@link URL} of the request to be retried.
     */
    private final URL url;

    /**
     * The {@link Configuration} for the {@link HttpPostRequest}.
     */
    private final Configuration configuration;

    /**
     * The {@code POST} parameters.
     */
    private final String parameters;

    /**
     * Constructs a {@link HttpPostRequest}.
     *
     * @param url     the {@link URL} for the request
     * @param options the {@link Option}s for the {@link HttpPostRequest}
     */
    public HttpPostRequest(final URL url,
                           final Option... options) {

        this.url = Objects.requireNonNull(url);
        this.configuration = ConfigurationBuilder.create(options)
            .build();

        // build the parameter string from the provided parameters
        final var builder = new StringBuilder();

        this.configuration.stream(Parameter.class)
            .forEach(parameter -> {
                if (!builder.isEmpty()) {
                    builder.append("&");
                }

                try {
                    builder.append(URLEncoder.encode(parameter.key(), "UTF-8"));
                    builder.append("=");
                    builder.append(URLEncoder.encode(parameter.value(), "UTF-8"));
                }
                catch (final UnsupportedEncodingException e) {
                    throw new IllegalArgumentException("Unsupported Encoding for " + parameter, e);
                }
            });

        this.parameters = builder.toString();
    }

    @Override
    public String get()
        throws EphemeralFailureException, PermanentFailureException {

        try {
            final var connection = (HttpURLConnection) this.url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            this.configuration.stream(Property.class)
                .forEach(property -> connection.setRequestProperty(property.key(), property.value()));

            final var out = new DataOutputStream(connection.getOutputStream());
            if (!Strings.isEmpty(this.parameters)) {
                out.writeBytes(this.parameters);
            }

            final var content = this.configuration.getOptionalValue(Content.class).orElse(null);
            if (!Strings.isEmpty(content)) {
                out.writeBytes(content);
            }

            out.flush();
            out.close();

            // attempt to read the response
            try (var in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                final var builder = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    builder.append(inputLine);
                }
                connection.disconnect();

                return builder.toString();
            }
        }
        catch (final IOException e) {
            throw new EphemeralFailureException(e);
        }
    }

    /**
     * The optional {@link Content} for a {@link HttpPostRequest}.
     */
    public static class Content
        extends AbstractValueOption<String> {

        /**
         * Constructs the {@link Content}.
         *
         * @param string the {@link Content} string
         */
        private Content(final String string) {
            super(string);
        }

        /**
         * Creates a new {@link Content} with the specified {@link String}.
         *
         * @param string the {@link Content}
         * @return a new {@link Content}
         */
        public static Content of(final String string) {
            return new Content(string);
        }

        /**
         * Creates a new {@link Content} with the specified {@link Object}.
         *
         * @param object the {@link Object}
         * @return a new {@link Content}
         */
        public static Content of(final Object object) {
            return new Content(object == null ? null : object.toString());
        }
    }

    /**
     * A {@link HttpPostRequest} parameter.
     */
    public static class Parameter
        extends Attribute {

        /**
         * Constructs a {@link Parameter} with a specific non-null key and non-null value.
         *
         * @param key   the key
         * @param value the value
         */
        private Parameter(final String key, final String value) {
            super(key, value);
        }

        /**
         * Create an {@link Parameter} with a specific non-null key and non-null value.
         *
         * @param name  the key for the {@link Parameter}
         * @param value the value for the {@link Parameter}
         * @return the new {@link Attribute}
         */
        public static Parameter of(final String name, final String value) {
            return new Parameter(name, value);
        }
    }

    /**
     * A {@link HttpPostRequest} Property.
     */
    public static class Property
        extends Attribute {

        /**
         * Constructs a {@link Property} with a specific non-null key and non-null value.
         *
         * @param key   the key
         * @param value the value
         */
        private Property(final String key, final String value) {
            super(key, value);
        }

        /**
         * Create an {@link Property} with a specific non-null key and non-null value.
         *
         * @param name  the key for the {@link Parameter}
         * @param value the value for the {@link Property}
         * @return the new {@link Attribute}
         */
        public static Property of(final String name, final String value) {
            return new Property(name, value);
        }
    }
}
