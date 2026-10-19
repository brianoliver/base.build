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

import java.util.Optional;
import java.util.function.Supplier;

/**
 * An {@link Object} that can be tabularized, into a tabular formate, using a {@link Table}.
 *
 * @author brian.oliver
 * @since Dec-2018
 *
 * @see Table
 */
@FunctionalInterface
public interface Tabular {

    /**
     * Obtains an {@link Optional} {@link Supplier} of {@link Table}s for the type of {@link Tabular} {@link Object}, 
     * allowing it to customize the type and appearance of {@link Table} used for tabularization with
     * {@link #tabularize(Table)}.
     * <p>
     * Should no special type of {@link Table} be required, {@link Optional#empty()} will be returned, in which case
     * a provided {@link Table} will be used for tabularization.
     *
     * @return a {@link Supplier} of {@link Table}s
     */
    default Optional<Supplier<Table>> getTableSupplier() {
        return Optional.empty();
    }

    /**
     * Determines when a {@link Tabular} {@link Object} has content to contribute to a {@link Table}.
     * <p>
     * When a {@link Tabular} {@link Object} does not have content to contribute this method should return
     * {@code false}, indicating it will not be tabularized, meaning the {@link #getTableSupplier()} and
     * {@link #tabularize(Table)} methods <strong>will not be invoked</strong> to output table content.
     *
     * @return {@code true} when the {@link Tabular} {@link Object} has content to contribute, {@code false} otherwise
     */
    default boolean hasTabularContent() {
        return true;
    }

    /**
     * Tabulates the state of a {@link Tabular} {@link Object} into the specified {@link Table}.
     *
     * @param table the {@link Table}
     */
    void tabularize(Table table);
}
