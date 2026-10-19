package build.base.configuration;

/*-
 * #%L
 * base.build Configuration
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

import java.util.Objects;

/**
 * An abstract {@link ValueOption} representing an immutable non-{@code null} value of a specific type.
 *
 * @param <T> the type of value
 * @author brian.oliver
 * @since Jan-2019
 */
public abstract class AbstractValueOption<T>
    implements ValueOption<T> {

    /**
     * The non-{@code null} value.
     */
    private final T value;

    /**
     * Constructs an {@link AbstractValueOption}.
     *
     * @param value the non-{@code null} value
     */
    protected AbstractValueOption(final T value) {
        this.value = Objects.requireNonNull(value, "The value must not be null");
    }

    public T get() {
        return this.value;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final AbstractValueOption<?> that = (AbstractValueOption<?>) other;
        return Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.value);
    }

    @Override
    public String toString() {
        return getClass().getName() + "{" + this.value + '}';
    }
}
