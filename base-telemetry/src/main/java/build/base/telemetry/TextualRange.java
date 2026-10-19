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
 * A {@link TextualLocation} consisting of a continuous inclusive range of characters, {@link #start()}ing from a
 * {@link TextualPosition}, ending with a {@link TextualPosition}, with in the same text document.
 *
 * @author brian.oliver
 * @since Jan-2023
 */
public interface TextualRange
    extends TextualLocation {

    /**
     * The starting {@link TextualPosition} for the range of text.
     *
     * @return the start {@link TextualPosition}
     */
    TextualPosition start();

    /**
     * The ending {@link TextualPosition} for the range of text.
     *
     * @return the end {@link TextualPosition}
     */
    TextualPosition end();

    /**
     * Creates a {@link TextualRange} given start and end {@link TextualPosition}s for a text document.
     *
     * @param start the starting {@link TextualPosition}
     * @param end the ending {@link TextualPosition}
     *
     * @return a new {@link TextualRange}
     */
    static TextualRange create(final TextualPosition start,
                               final TextualPosition end) {

        Objects.requireNonNull(start, "The start TextualPosition must not be null");
        Objects.requireNonNull(end, "The end TextualPosition must not be null");

        return new TextualRange() {

            @Override
            public URI uri() {
                return start.uri();
            }

            @Override
            public TextualPosition start() {
                return start;
            }

            @Override
            public TextualPosition end() {
                return end;
            }

            @Override
            public String toString() {
                return "in " + uri() + " starting " + String.format(" (%d:%d)", start().line(), start().position())
                    + " ending " + String.format(" (%d:%d)", end().line(), end().position());
            }
        };
    }
}
