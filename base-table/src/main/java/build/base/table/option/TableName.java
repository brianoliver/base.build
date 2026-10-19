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

import build.base.table.Table;

import java.util.Objects;

/**
 * An {@link TableOption} to define the name of a {@link Table}.
 *
 * @author brian.oliver
 * @since Dec-2018
 */
public class TableName
    implements TableOption {

    /**
     * The name.
     */
    private final String name;

    /**
     * Constructs a {@link TableName}.
     *
     * @param name the name
     */
    private TableName(final String name) {
        this.name = name;
    }

    /**
     * Obtains the name.
     *
     * @return the name
     */
    public String get() {
        return this.name;
    }

    /**
     * Creates a {@link TableName}.
     *
     * @param name the name
     * @return a {@link TableName}
     */
    public static TableName of(final String name) {
        return new TableName(name);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final TableName that = (TableName) other;
        return Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name);
    }

    @Override
    public String toString() {
        return "Name{" + this.name + '}';
    }
}
