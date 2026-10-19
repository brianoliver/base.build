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

/**
 * Helper methods for working with {@link AutoCloseable}.
 *
 * @author patrick.peralta
 * @since Mar-2019
 */
public class Closeables {

    /**
     * Private constructor to prevent instantiation.
     */
    private Closeables() {
    }

    /**
     * Invoke {@link AutoCloseable#close()} on each of the provided {@link AutoCloseable}s. Any checked exceptions thrown
     * as a result of invoking {@code close} are ignored. Null parameters are supported; they do not need to be
     * excluded.
     *
     * @param closeables the {@link AutoCloseable}s to invoke {@code close} on; may be {@code null} or include
     *                   {@code null} values
     */
    public static void close(final AutoCloseable... closeables) {
        if (closeables != null) {
            for (final AutoCloseable closeable : closeables) {
                close(closeable);
            }
        }
    }

    /**
     * Invoke {@link AutoCloseable#close()} on the provided {@link AutoCloseable}. Any checked exceptions thrown
     * as a result of invoking {@code close} are ignored.
     *
     * @param closeable the {@link AutoCloseable} to invoke {@code close} on; may be {@code null}
     */
    public static void close(final AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            }
            catch (final Exception e) {
                Exceptions.consumeChecked(e);
            }
        }
    }
}
