package build.base.table;

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

import build.base.configuration.Configurable;
import build.base.configuration.ConfigurationBuilder;
import build.base.foundation.Strings;
import build.base.table.option.CellAlignment;
import build.base.table.option.CellSeparator;
import build.base.table.option.CellWidth;
import build.base.table.option.DisplayNull;
import build.base.table.option.RowComparator;
import build.base.table.option.TableOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * A sequence of zero or more vertically arranged {@link Row}s, each {@link Row} consisting
 * or zero or more horizontally arranged {@link Cell}s.   The formatting and arrangement of {@link Row}s may be
 * specified using {@link TableOption}s.
 *
 * @author brian.oliver
 * @since Dec-2018
 */
public class Table
    implements Configurable {

    /**
     * The {@link Row}s in the {@link Table}.
     */
    private final List<Row> rows;

    /**
     * The {@link ConfigurationBuilder} of {@link TableOption}s for the {@link Table}s.
     */
    private final ConfigurationBuilder options;

    /**
     * Privately constructs a {@link Table} based on a sequence of {@link Row}s.
     *
     * @param rows the {@link Row}s
     */
    private Table(final Row... rows) {
        this.rows = new ArrayList<>();
        this.options = ConfigurationBuilder.create();

        if (rows != null) {
            for (final var row : rows) {
                this.rows.add(row == null ? Row.create() : row);
            }
        }
    }

    @Override
    public ConfigurationBuilder options() {
        return this.options;
    }

    /**
     * Constructs a {@link Table} based on a sequence of {@link Row}s.
     *
     * @param rows the {@link Row}s
     * @return a new {@link Table} consisting of the specified {@link Row}s
     */
    public static Table of(final Row... rows) {
        return new Table(rows);
    }

    /**
     * Constructs a new empty {@link Table}.
     *
     * @return a new empty {@link Table}
     */
    public static Table create() {
        return new Table();
    }

    /**
     * Constructs a {@link Table} based on the specified {@link Properties}, ordered by the
     * key of the properties.
     *
     * @param properties the {@link Properties}
     * @return a new {@link Table} containing the entries in the {@link Properties}
     */
    public static Table of(final Properties properties) {
        final var table = Table.create();

        // order the table rows (ie: property entries) by the key column
        table.options().add(RowComparator.orderByColumn(0));

        // we don't use a cell separator
        table.options().add(CellSeparator.of(" "));

        // we output nulls as "(null)"
        table.options().add(DisplayNull.as("(null)"));

        if (properties != null) {
            for (final var key : properties.stringPropertyNames()) {
                table.addRow(key, properties.get(key).toString());
            }
        }

        return table;
    }

    /**
     * Adds the specified {@link Row} to the {@link Table}.
     *
     * @param row the {@link Row} to add to the {@link Table}
     * @return the {@link Table} to permit fluent-style method calls
     */
    public Table addRow(final Row row) {
        this.rows.add(row == null ? Row.create() : row);

        return this;
    }

    /**
     * Adds the specified a {@link Row} consisting of the specified {@link Cell}s to the {@link Table}.
     *
     * @param cells the {@link Cell}s of the {@link Row} to add to the {@link Table}
     * @return the {@link Table} to permit fluent-style method calls
     */
    public Table addRow(final Cell... cells) {
        this.rows.add(Row.of(cells));

        return this;
    }

    /**
     * Adds the specified a {@link Row} consisting of the specified {@link Cell} content to the {@link Table}.
     *
     * @param cells the content of each {@link Cell} of the {@link Row} to add to the {@link Table}
     * @return the {@link Table} to permit fluent-style method calls
     */
    public Table addRow(final String... cells) {
        this.rows.add(Row.of(cells));

        return this;
    }

    /**
     * Obtains the {@link Row} at the specified index, the first {@link Row} being zero.
     * <p>
     * Should there be no {@link Row} at the specified index, for example due to the index being out-of-bounds,
     * <code>null</code> is returned.
     *
     * @param index the {@link Row} index
     * @return the {@link Row} at the specified index, or <code>null</code>
     */
    public Row getRow(final int index) {
        return index < 0 || index >= this.rows.size() ? null : this.rows.get(index);
    }

    @Override
    public String toString() {

        // determine the number of columns and their necessary widths based on the cells in each of the rows in the table
        final var columnWidths = new ArrayList<Integer>();

        // establish the Configuration to use for rendering the Table
        final var configuration = this.options.build();

        final var tableCellWidth = configuration.get(CellWidth.class);
        final var tableDisplayNull = configuration.get(DisplayNull.class);
        final var tableCellAlignment = configuration.get(CellAlignment.class);

        for (final var row : this.rows) {

            // determine the number of columns for the current row
            final var columnCount = row.getWidth();

            // ensure the width of each column we're tracking is at least the same as the width of the cells in the current row
            for (var column = 0; column < columnCount; column++) {

                // ensure there's a columnWidth location for the current column
                if (columnWidths.size() < column + 1) {
                    columnWidths.add(0);
                }

                // acquire the cell for the column to determine the cell and column widths
                final var cell = row.getCell(column);

                // determine how the cell width is calculated by looking for a Cell.Width option
                // (we first consider the Cell, then the Row, then lastly the Table)
                final var cellWidth = cell.hasOptions()
                    ? cell.options().getOrDefault(CellWidth.class,
                    () -> row.hasOptions()
                        ? row.options().getOrDefault(CellWidth.class, () -> tableCellWidth)
                        : tableCellWidth)
                    : tableCellWidth;

                int columnWidth;

                if (cellWidth.isAutoDetect()) {

                    // assume the column width is the cell width
                    columnWidth = cell.getWidth();

                    // ensure there's room for null values (if there are any)
                    if (cell.isNullPresent()) {

                        // determine how to the cell wants to display nulls by looking for a Cell.DisplayNulls option
                        final var displayNull = cell.hasOptions()
                            ? cell.options().getOrDefault(DisplayNull.class,
                            () -> row.hasOptions()
                                ? row.options().getOrDefault(DisplayNull.class,
                                () -> tableDisplayNull)
                                : tableDisplayNull)
                            : tableDisplayNull;

                        // ensure the column width is at least as wide as null content
                        columnWidth = Math.max(columnWidth, displayNull.get().length());
                    }

                }
                else {
                    // use the specified width
                    columnWidth = cellWidth.get();
                }

                // use the maximum of the current column and column width of the cell
                columnWidths.set(column, Math.max(columnWidth, columnWidths.get(column)));
            }
        }

        // sort the rows according to the Row.Comparator option
        final var sortedRows = new Row[this.rows.size()];
        this.rows.toArray(sortedRows);

        final var rowComparator = configuration.get(RowComparator.class);

        if (rowComparator != null) {
            Arrays.sort(sortedRows, rowComparator);
        }

        // build the table row at a time
        final var builder = new StringBuilder();

        // we use the same Cell.Separator for the entire table
        final var cellSeparator = configuration.get(CellSeparator.class);

        for (final var row : sortedRows) {

            int rowHeight = 0;
            int line = 0;

            // add each line from each cell in the row to the table
            do {
                for (var columnIndex = 0; columnIndex < columnWidths.size(); columnIndex++) {

                    // determine if this is the first and last column
                    final var isFirstColumn = columnIndex == 0;
                    final var isLastColumn = columnIndex == columnWidths.size() - 1;

                    // add the cell separator for after the first cell
                    if (!isFirstColumn) {
                        builder.append(cellSeparator.get());
                    }

                    // determine the cell for the row
                    final var cell = Optional.ofNullable(row.getCell(columnIndex))
                        .orElseGet(Cell::create);

                    // ensure the row height is at least as high as the cell
                    rowHeight = Math.max(rowHeight, cell.getHeight());

                    // determine the alignment of the cell
                    final var cellAlignment = cell.hasOptions()
                        ? cell.options().getOrDefault(CellAlignment.class,
                        () -> row.hasOptions()
                            ? row.options().getOrDefault(CellAlignment.class, () -> tableCellAlignment)
                            : tableCellAlignment)
                        : tableCellAlignment;

                    // determine the Cell content
                    final var content = cell.getLine(line)
                        .orElseGet(() ->
                            // determine how to display the null content
                            (cell.hasOptions()
                                ? cell.options().getOrDefault(DisplayNull.class,
                                () -> row.hasOptions()
                                    ? row.options().getOrDefault(DisplayNull.class, () -> tableDisplayNull)
                                    : tableDisplayNull)
                                : tableDisplayNull).get());

                    // justify the content and add to the table
                    final var aligned = cellAlignment.align(content, columnWidths.get(columnIndex));
                    builder.append(isLastColumn ? Strings.trimTrailingWhiteSpace(aligned) : aligned);
                }

                // next line
                builder.append("\n");
                line++;
            } while (line < rowHeight);
        }

        return builder.toString();
    }
}
