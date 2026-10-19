package build.base.foundation;

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

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A {@link Lazy} non-{@code null} value that can not be changed.
 *
 * @param <T> the type of value
 * @author brian.oliver
 * @since Feb-2026
 */
final class LazyValue<T>
    implements Lazy<T> {

    /**
     * The value, {@code null} meaning non-yet-set
     */
    private final T value;

    /**
     * Constructs a {@link LazyValue} with the specified non-{@code null} value.
     *
     * @param value the value
     */
    private LazyValue(final T value) {
        this.value = Objects.requireNonNull(value, "The value must not be null");
    }

    @Override
    public T getOrNull() {
        return this.value;
    }

    @Override
    public boolean isPresent() {
        return true;
    }

    @Override
    public Lazy<T> set(final T value) throws IllegalStateException {
        throw new IllegalStateException(
            "Attempted to set Lazy<T> with " + value + " when it's already set to " + this.value);
    }

    @Override
    public Lazy<T> computeIfAbsent(final Supplier<? extends T> supplier) {
        return this;
    }

    @Override
    public <R> Lazy<R> map(final Function<? super T, R> function) {
        return function == null
            ? Lazy.empty()
            : Lazy.of(function.apply(this.value));
    }

    @Override
    public Lazy<T> filter(final Predicate<? super T> predicate) {
        return predicate == null || predicate.test(this.value)
            ? this
            : Lazy.empty();
    }

    /**
     * Indicates whether some other object is "equal to" this {@link LazyValue}. The other object is
     * considered equal if:
     * <ul>
     * <li>it is also an {@link LazyValue} and;
     * <li>both instances have no value present or;
     * <li>the present values are "equal to" each other via {@code equals()}.
     * </ul>
     *
     * @param object an object to be tested for equality
     * @return {@code true} if the other object is "equal to" this object otherwise {@code false}
     */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        return object instanceof Lazy<?> other
            && Objects.equals(getOrNull(), other.getOrNull());
    }

    /**
     * Returns a non-empty string representation of this {@code Optional} suitable for debugging.  The
     * exact presentation format is unspecified and may vary between implementations and versions.
     *
     * @return the string representation of this instance
     * result.  Empty and present {@code Optional}s must be unambiguously differentiable.
     */
    @Override
    public String toString() {
        return "Lazy[" + this.value + "]";
    }

    /**
     * Obtains a {@link LazyValue} representation of the non-{@code null} value.
     *
     * @param <T>   the type of {@link LazyValue} value
     * @param value the type
     * @return a new {@link LazyValue} value
     */
    public static <T> Lazy<T> of(final T value) {
        return new LazyValue<>(value);
    }
}
