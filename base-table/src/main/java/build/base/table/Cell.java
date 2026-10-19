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
import build.base.table.option.TableOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * A grouping of zero or more lines of content (including <code>null</code>s) as a single unit in a {@link Table},
 * each arranged horizontally in a {@link Row}.   The formatting and dimensions of {@link Cell}s may be specified
 * as {@link TableOption}s.
 *
 * @author brian.oliver
 * @since Dec-2018
 */
public class Cell
    implements Configurable {

    /**
     * The lines of content (zero or more) in a {@link Cell}.
     */
    private final List<String> lines;

    /**
     * Indicates that one or more <code>null</code> lines of content are present in the {@link Cell}.
     */
    private boolean nullPresent;

    /**
     * The {@link Lazy}ily initialized {@link ConfigurationBuilder} of {@link TableOption}s for the {@link Cell}s.
     */
    private final Lazy<ConfigurationBuilder> options;

    /**
     * Privately constructs a {@link Cell} given an array of separate lines of content.
     * <p>
     * Should any of the lines of content contain new-line characters, the said content is split into separate lines
     * add added as individual lines.
     *
     * @param linesOfContent the lines of content
     */
    private Cell(final String... linesOfContent) {

        this.lines = new ArrayList<>();
        this.nullPresent = false;
        this.options = Lazy.of(ConfigurationBuilder::create);

        if (linesOfContent != null) {
            for (final var lineOfContent : linesOfContent) {
                if (lineOfContent == null) {
                    this.lines.add(null);
                    this.nullPresent = true;
                }
                else {
                    this.lines.addAll(Arrays.asList(lineOfContent.split("\\n\\r|\\n")));
                }
            }
        }
    }

    /**
     * Constructs a {@link Cell} given an array of separate lines of content.
     * <p>
     * Should any of the lines of content contain new-line characters, the said content is split into separate lines
     * add added as individual lines.
     *
     * @param linesOfContent the lines of content
     * @return a new {@link Cell} representing the specified lines of content
     */
    public static Cell of(final String... linesOfContent) {
        return new Cell(linesOfContent);
    }

    /**
     * Constructs a new empty {@link Cell}.
     *
     * @return a new empty {@link Cell}
     */
    public static Cell create() {
        return Cell.of();
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
     * Determines the ideal height of the {@link Cell}, that being the number of lines of content
     * in the {@link Cell}.
     *
     * @return the number of lines of content in the {@link Cell}
     */
    public int getHeight() {
        return this.lines.size();
    }

    /**
     * Determines the ideal width of the {@link Cell} in characters, that being the
     * maximum of the number of characters in each of the lines of content in the {@link Cell}.
     *
     * @return the ideal width of the {@link Cell}
     */
    public int getWidth() {
        int maximumWidth = 0;

        for (final String line : this.lines) {
            if (line != null && line.length() > maximumWidth) {
                maximumWidth = line.length();
            }
        }

        return maximumWidth;
    }

    /**
     * Determines if one or more lines of <code>null</code> content is present in the {@link Cell}.
     *
     * @return <code>true</code> if one or more lines of <code>null</code> is present, <code>false</code> otherwise
     */
    public boolean isNullPresent() {
        return this.nullPresent;
    }

    /**
     * Obtains the content at the specified line number index, the first line being zero.
     * <p>
     * Should there be no content at the specified line, for example due to the line being out-of-bounds,
     * {@link Optional#empty()} is returned.
     *
     * @param index the line number index
     * @return the {@link Optional} content at the specified line number
     */
    public Optional<String> getLine(final int index) {
        return index < 0 || index >= this.lines.size() ? Optional.empty() : Optional.ofNullable(this.lines.get(index));
    }

}
