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
import build.base.table.Cell;

/**
 * A {@link TableOption} to specify the maximum width (number of characters) when formatting a {@link Cell}.
 *
 * @author brian.oliver
 * @since Dec-2018
 */
public class CellWidth
    implements TableOption {

    /**
     * The default {@link CellWidth} is to autodetect.
     */
    private static final CellWidth AUTODETECT = new CellWidth(-1);

    /**
     * The width as a number of characters, -1 meaning autodetect.
     */
    private final int size;

    /**
     * Privately constructs a {@link CellWidth} using the provided size (number of characters).
     *
     * @param size the number of characters
     */
    private CellWidth(final int size) {
        this.size = size;
    }

    /**
     * Constructs a {@link CellWidth} using the provided number of characters.
     *
     * @param size the number of characters
     * @return a new {@link CellWidth}
     */
    public static CellWidth of(final int size) {
        return new CellWidth(size);
    }

    /**
     * The standard default {@link CellWidth}.
     *
     * @return the {@link CellWidth}
     */
    @Default
    public static CellWidth autodetect() {
        return AUTODETECT;
    }

    /**
     * Obtains the {@link CellWidth} as a number of characters, -1 meaning autodetect.
     *
     * @return the number of characters
     */
    public int get() {
        return this.size;
    }

    /**
     * Determines if the {@link CellWidth} of a {@link Cell} should be autodetected.
     *
     * @return <code>true</code> if the width should be autodetected, <code>false</code> otherwise
     */
    public boolean isAutoDetect() {
        return this.size == -1;
    }
}
