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
import build.base.table.Table;

import java.util.Optional;

/**
 * A {@link TableOption} to compare {@link Row}s within a {@link Table}
 *
 * @author brian.oliver
 * @since Dec-2018
 */
@OptionDiscriminator
public interface RowComparator
    extends TableOption, java.util.Comparator<Row> {

    /**
     * Constructs a {@link RowComparator} to order {@link Row}s in a {@link Table} by a specific column.
     *
     * @param column the column of the {@link Table} {@link Row} to order by
     * @return a new {@link RowComparator}
     */
    static RowComparator orderByColumn(final int column) {

        return (final Row x, final Row y) -> {
            if (x.getCell(column) == null || (x.hasOptions() && x.options().get(HeaderRow.class) != null)) {
                return -1;
            }
            else if (y.getCell(column) == null || (y.hasOptions() && y.options().get(HeaderRow.class) != null)) {
                return +1;
            }
            else {
                final Optional<String> xContent = x.getCell(column).getLine(0);
                final Optional<String> yContent = y.getCell(column).getLine(0);

                return (xContent.isPresent() && yContent.isPresent())
                    ? xContent.get().compareTo(yContent.get())
                    : (xContent.isPresent() ? +1 : (yContent.isPresent() ? -1 : 0));
            }
        };
    }
}
