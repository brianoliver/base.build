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
import build.base.table.Table;

/**
 * A {@link TableOption} to define the separator {@link String} output between {@link Cell}s in a {@link Table}.
 *
 * @author brian.oliver
 * @since Dec-2018
 */
public class CellSeparator
    implements TableOption {

    /**
     * The default {@link CellSeparator}.
     */
    private static final CellSeparator STANDARD = new CellSeparator(" : ");

    /**
     * The separator.
     */
    private final String separator;

    /**
     * Privately constructs a {@link CellSeparator} using the provided separator {@link String}.
     *
     * @param separator the separator
     */
    private CellSeparator(final String separator) {
        this.separator = separator;
    }

    /**
     * Constructs a {@link CellSeparator} using the provided separator {@link String}.
     *
     * @param separator the separator
     * @return a new {@link CellSeparator}
     */
    public static CellSeparator of(final String separator) {
        return new CellSeparator(separator);
    }

    /**
     * The default {@link CellSeparator} {@link TableOption}.
     *
     * @return the {@link CellSeparator}
     */
    @Default
    public static CellSeparator standard() {
        return STANDARD;
    }

    /**
     * Obtains the {@link CellSeparator} {@link String}.
     *
     * @return the {@link CellSeparator} {@link String}
     */
    public String get() {
        return this.separator;
    }
}
