package build.base.foundation.tuple;

/*-
 * #%L
 * base.build Foundation
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

import java.util.Arrays;
import java.util.Objects;

/**
 * An abstract un-typed immutable implementation of an n-<a href="https://en.wikipedia.org/wiki/Tuple">Tuple</a>.
 *
 * @author brian.oliver
 * @since Jun-2018
 */
public abstract class AbstractTuple
    implements Tuple {

    /**
     * The values in the n-tuple.
     */
    private final Object[] values;

    /**
     * Constructs an {@link AbstractTuple} with a specified array of values (non-null and non-zero).
     *
     * @param values the values
     */
    public AbstractTuple(final Object... values) {
        Objects.requireNonNull(values, "Values must be provided for the n-tuple");

        if (values.length == 0) {
            throw new IllegalArgumentException("One or more values must be provided for the n-tuple");
        }

        this.values = values;
    }

    @Override
    public int size() {
        return this.values.length;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(final int i)
        throws IndexOutOfBoundsException {
        return (T) this.values[i];
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final AbstractTuple that = (AbstractTuple) object;
        return Arrays.equals(this.values, that.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.values);
    }

    @Override
    public String toString() {
        return "Tuple<" + Arrays.toString(this.values) + '>';
    }
}
