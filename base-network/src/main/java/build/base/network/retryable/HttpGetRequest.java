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

import build.base.retryable.EphemeralFailureException;
import build.base.retryable.PermanentFailureException;
import build.base.retryable.Retryable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Objects;

/**
 * A {@link Retryable} allowing a http-based {@code GET} request to be retried.
 *
 * @author brian.oliver
 * @since Mar-2021
 */
public class HttpGetRequest
    implements Retryable<String> {

    /**
     * The {@link URL} of the request to be retried.
     */
    private final URL url;

    /**
     * Constructs a {@link HttpGetRequest}.
     *
     * @param url the {@link String} representation of the {@link URL}
     * @throws MalformedURLException should the {@link String} representation be an invalid {@link URL}
     */
    public HttpGetRequest(final String url)
        throws MalformedURLException {
        this(URI.create(url).toURL());
    }

    /**
     * Constructs a {@link HttpGetRequest}.
     *
     * @param url the {@link URL}
     */
    public HttpGetRequest(final URL url) {
        this.url = Objects.requireNonNull(url);
    }

    @Override
    public String get()
        throws EphemeralFailureException, PermanentFailureException {

        try {
            final var connection = (HttpURLConnection) this.url.openConnection();
            connection.setRequestMethod("GET");

            // attempt to read the response
            try (var in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                final var content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                connection.disconnect();

                return content.toString();
            }
        }
        catch (final IOException e) {
            throw new EphemeralFailureException(e);
        }
    }
}
