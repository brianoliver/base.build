package build.base.telemetry;

/*-
 * #%L
 * base.build Telemetry
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
import java.util.Objects;

/**
 * A {@link TextualLocation}, consisting of a {@link #line()} and character {@link #position()} in the said
 * {@link #line()}, within a text document.
 *
 * @author brian.oliver
 * @since Jan-2023
 */
public interface TextualPosition
    extends TextualLocation {

    /**
     * Obtains the line number.
     *
     * @return line number
     */
    int line();

    /**
     * Obtains the character position.
     *
     * @return the character position
     */
    int position();

    /**
     * Creates a {@link TextualPosition} in the text document at the specified {@link URI}.
     *
     * @param uri the {@link URI} of the text document
     * @param line the line
     * @param position the position
     * @return a {@link TextualPosition}
     */
    static TextualPosition create(final URI uri,
                                  final int line,
                                  final int position) {

        Objects.requireNonNull(uri, "The document URI must not be null");

        return new TextualPosition() {

            @Override
            public URI uri() {
                return uri;
            }

            @Override
            public int line() {
                return line;
            }

            @Override
            public int position() {
                return position;
            }

            @Override
            public String toString() {
                return uri + String.format(" (%d:%d)", line, position);
            }
        };
    }
}
