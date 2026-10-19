package build.base.table.option;

/*-
 * #%L
 * base.build Table
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

import build.base.configuration.Default;
import build.base.foundation.Strings;
import build.base.table.Cell;

/**
 * A {@link TableOption} specifying how single line content will be aligned in a {@link Cell}.
 *
 * @author brian.oliver
 * @since Dec-2018
 */
public enum CellAlignment
    implements TableOption {

    /**
     * Align {@link Cell} content to the left of the {@link Cell}.
     */
    @Default
    LEFT,

    /**
     * Align {@link Cell} content to the center of the {@link Cell}.
     */
    CENTERED,

    /**
     * Align {@link Cell} content to the right of the {@link Cell}.
     */
    RIGHT;

    /**
     * Aligns the specified single line of {@link String} content in an area of the specified width.
     *
     * @param string the {@link String} content to align
     * @param width  the width of the area in which to align the {@link String}
     * @return an aligned {@link String}
     */
    public String align(final String string, final int width) {

        if (string == null) {
            return Strings.repeat(" ", width);
        }
        else if (width <= 0) {
            return string;
        }
        else {
            // ensure we have at most width number of characters in the string
            final String trimmedString = string.length() > width ? string.substring(0, width) : string;

            return switch (this) {
                case CENTERED -> {
                    final int leftPadding = Math.max(0, width / 2 - trimmedString.length() / 2);

                    yield Strings.repeat(" ", leftPadding) + trimmedString
                        + Strings.repeat(" ", width - leftPadding - trimmedString.length());
                }
                case RIGHT -> String.format("%" + width + "s", trimmedString);
                default -> String.format("%-" + width + "s", trimmedString);
            };
        }
    }
}
