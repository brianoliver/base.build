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
import build.base.foundation.Lazy;
import build.base.table.option.HeaderRow;
import build.base.table.option.TableOption;

import java.util.ArrayList;

/**
 * A sequence of zero or more horizontally arranged {@link Cell}s in a {@link Table}, each {@link Row} being
 * arranged vertically with other {@link Row}s with in said {@link Table}.   The formatting and arrangement of
 * {@link Cell}s in a {@link Row} may be specified using {@link TableOption}s.
 *
 * @author brian.oliver
 * @since Dec-2018
 */
public class Row
    implements Configurable {

    /**
     * The {@link Cell}s in the {@link Row}.
     */
    private final ArrayList<Cell> cells;

    /**
     * The {@link Lazy} {@link ConfigurationBuilder} of {@link TableOption}s for the {@link Row}.
     */
    private final Lazy<ConfigurationBuilder> options;

    /**
     * Privately constructs a {@link Row} based on a sequence of {@link Cell}s.
     *
     * @param cells the {@link Cell}s
     */
    private Row(final Cell... cells) {
        this.cells = new ArrayList<>();
        this.options = Lazy.of(ConfigurationBuilder::create);

        if (cells != null) {
            for (final var cell : cells) {
                this.cells.add(cell == null ? Cell.create() : cell);
            }
        }
    }

    @Override
    public ConfigurationBuilder options() {
        return this.options.get();
    }

    @Override
    public boolean hasOptions() {
        return this.options.isPresent() && !this.options.get().isEmpty();
    }

    /**
     * Constructs a {@link Row} based on a sequence of {@link Cell}s.
     *
     * @param cells the {@link Cell}s
     * @return a new {@link Row} consisting of the specified {@link Cell}s
     */
    public static Row of(final Cell... cells) {
        return new Row(cells);
    }

    /**
     * Constructs a {@link Row} based on a sequence of {@link Cell} content.
     *
     * @param cells the content of each {@link Cell} for the {@link Row}
     * @return a new {@link Row} consisting of @link Cell}s with the specified content
     */
    public static Row of(final String... cells) {
        final Row row = Row.create();

        if (cells != null) {
            for (final var cell : cells) {
                row.cells.add(Cell.of(cell));
            }
        }

        return row;
    }

    /**
     * Constructs a header {@link Row} based on a sequence of {@link Cell}s,
     * where the {@link Row} has the {@link HeaderRow} {@link TableOption} added.
     *
     * @param cells the {@link Cell}s
     * @return a new {@link Row} consisting of the specified {@link Cell}s,
     * with the {@link HeaderRow} {@link TableOption}
     */
    public static Row header(final Cell... cells) {
        final Row row = Row.of(cells);

        row.options().add(HeaderRow.MARKER);

        return row;
    }

    /**
     * Constructs a {@link Row} based on a sequence of {@link Cell} content, where the {@link Row} has the
     * {@link HeaderRow} {@link TableOption} added.
     *
     * @param cells the content of each {@link Cell} for the {@link Row}
     * @return a new {@link Row} consisting of @link Cell}s with the specified content,
     * with the {@link HeaderRow} {@link TableOption}
     */
    public static Row header(final String... cells) {
        final var row = Row.create();

        if (cells != null) {
            for (final var cell : cells) {
                row.cells.add(Cell.of(cell));
            }
        }

        row.options().add(HeaderRow.MARKER);

        return row;
    }

    /**
     * Constructs a new empty {@link Row}.
     *
     * @return a new empty {@link Row}
     */
    public static Row create() {
        return new Row();
    }

    /**
     * Determines the width of the {@link Row}, that being the number of {@link Cell}s
     * defined by the {@link Row}.
     *
     * @return the number of {@link Cell}s in the {@link Row}
     */
    public int getWidth() {
        return this.cells.size();
    }

    /**
     * Obtains the {@link Cell} at the specified index, the first {@link Cell} being zero.
     * <p>
     * Should there be no {@link Cell} at the specified index, for example due to the index being out-of-bounds,
     * <code>null</code> is returned.
     *
     * @param index the {@link Cell} index
     * @return the {@link Cell} at the specified index, or <code>null</code>
     */
    public Cell getCell(final int index) {
        return index < 0 || index >= this.cells.size() ? null : this.cells.get(index);
    }
}
