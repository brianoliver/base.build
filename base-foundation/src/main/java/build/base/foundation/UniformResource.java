package build.base.foundation;

/*-
 * #%L
 * base.build Foundation
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

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Helper methods for {@link URI}s.
 *
 * @author brian.oliver
 * @since May-2024
 */
public class UniformResource {

    /**
     * The {@link URI} scheme to use for an {@link Object} reference.
     */
    public final static String OBJECT_SCHEME = "object";

    /**
     * The {@link URI} scheme to use for an {@link Class} reference.
     */
    public final static String CLASS_SCHEME = "class";

    /**
     * Prevent instantiation.
     */
    private UniformResource() {
        // prevent instantiation
    }

    /**
     * Sanitizes the specified {@link String} for use as part of a {@link URI}.
     *
     * @param string the {@link String}
     * @return a sanitized {@link String}
     */
    public static String sanitize(final String string) {
        return string == null
            ? ""
            : string.trim().replace('"', '\'').replace('$', '.');
    }

    /**
     * Attempts to create a {@link URI} for the specified path using the provided scheme.
     *
     * @param scheme the scheme
     * @param path   the path
     * @return a {@link URI}
     */
    public static URI createURI(final String scheme, final String path) {
        try {
            String sanitizedPath = sanitize(path);
            if (!sanitizedPath.startsWith("//")) {
                if (sanitizedPath.startsWith("/")) {
                    sanitizedPath = "/" + sanitizedPath;
                }
                else {
                    sanitizedPath = "//" + sanitizedPath;
                }
            }

            return new URI(scheme, null, sanitizedPath, null, null);
        }
        catch (final URISyntaxException uriSyntaxException) {
            try {
                return new URI("unknown", "", "resource", null, null);
            }
            catch (final URISyntaxException e) {
                throw new RuntimeException(
                    "Failed to produce a URI for scheme [" + scheme + "] with path [" + path + "]", e);
            }
        }
    }

    /**
     * Creates a {@link URI} for the specified {@link Object} using the {@link #OBJECT_SCHEME}.
     *
     * @param object the {@link Object}
     * @return the {@link URI}
     */
    public static URI createURI(final Object object) {
        return createURI(OBJECT_SCHEME, object);
    }

    /**
     * Creates a {@link URI} for the specified {@link Object} with the provided scheme.
     *
     * @param scheme the scheme
     * @param object the {@link Object}
     * @return the {@link URI}
     */
    public static URI createURI(final String scheme, final Object object) {
        return object == null
            ? createURI(scheme, "null")
            : createURI(scheme, Introspection.describe(object.getClass()) + ":" + System.identityHashCode(object));
    }
}
