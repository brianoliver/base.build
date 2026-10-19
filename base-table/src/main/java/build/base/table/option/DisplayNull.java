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
 * A {@link TableOption} to define how {@code null} {@link Cell} content is displayed in a {@link Table}.
 *
 * @author brian.oliver
 * @since Dec-2018
 */
public class DisplayNull
    implements TableOption {

    /**
     * The default {@link DisplayNull}.
     */
    private static final DisplayNull STANDARD = new DisplayNull("");

    /**
     * The {@link String} used to display <code>null</code> values.
     */
    private final String string;

    /**
     * Privately constructs a {@link DisplayNull} using the provided {@link String}.
     *
     * @param string the string
     */
    private DisplayNull(final String string) {
        this.string = string;
    }

    /**
     * Constructs a {@link DisplayNull} using the provided non-null {@link String}.
     *
     * @param string the non-null string
     * @return a new {@link DisplayNull}
     */
    public static DisplayNull as(final String string) {
        return new DisplayNull(string);
    }

    /**
     * The default {@link DisplayNull} {@link TableOption}.
     *
     * @return the {@link DisplayNull}
     */
    @Default
    public static DisplayNull standard() {
        return STANDARD;
    }

    /**
     * Obtains the {@link DisplayNull} {@link String}.
     *
     * @return the {@link DisplayNull} {@link String}
     */
    public String get() {
        return this.string;
    }
}
