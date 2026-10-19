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

import build.base.configuration.OptionDiscriminator;
import build.base.table.Row;

/**
 * A {@link TableOption} specifying that a {@link Row} is to be used a header and thus should not be sorted in the
 * presence of a {@link RowComparator}.
 *
 * @author brian.oliver
 * @since Dec-2018
 */
@OptionDiscriminator
public class HeaderRow
    implements TableOption {

    /**
     * An instance of the {@link HeaderRow}.
     */
    public static HeaderRow MARKER = new HeaderRow();

    /**
     * Constructs a {@link HeaderRow}.
     */
    private HeaderRow() {
        // HeaderRow has no state
    }
}
