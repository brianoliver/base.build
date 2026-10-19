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
import java.util.function.Supplier;

/**
 * A {@link Lazy} non-{@code null} value that is supplied directly or by consulting a {@link Supplier}
 * once-and-only once.
 *
 * @param <T> the type of value
 * @author brian.oliver
 * @since Feb-2026
 */
final class LazilySupplied<T>
    implements Lazy<T> {

    /**
     * The value, {@code null} meaning non-yet-set
     */
    private volatile T value;

    /**
     * The {@code null}able {@link Supplier} of a value when one has not been provided and one is requested.
     */
    private final Supplier<? extends T> supplier;

    /**
     * Constructs an empty {@link LazilySupplied}.
     */
    private LazilySupplied() {
        this.value = null;
        this.supplier = null;
    }

    /**
     * Constructs a {@link LazilySupplied} given the specified lazy {@link Supplier}.
     *
     * @param supplier the {@link Supplier}
     */
    private LazilySupplied(final Supplier<? extends T> supplier) {
        this.value = null;
        this.supplier = supplier;
    }

    @Override
    public T getOrNull() {
        var value = this.value;

        if (value == null && this.supplier != null) {
            synchronized (this) {
                value = this.value;

                if (value == null) {
                    value = this.supplier.get();

                    this.value = value;
                }
            }
        }

        return value;
    }

    @Override
    public boolean isEmpty() {
        synchronized (this) {
            return this.value == null && this.supplier == null;
        }
    }

    @Override
    public boolean isPresent() {
        synchronized (this) {
            return this.value != null || this.supplier != null;
        }
    }

    @Override
    public Lazy<T> set(final T value)
        throws IllegalStateException {

        Objects.requireNonNull(value, "The value must not be null");

        synchronized (this) {
            if (this.value != null) {
                throw new IllegalStateException(
                    "Attempted to set Lazy<T> with " + value + " when it's already set to " + this.value);
            }

            this.value = value;
            return this;
        }
    }

    @Override
    public Lazy<T> computeIfAbsent(final Supplier<? extends T> supplier) {
        synchronized (this) {
            if (supplier == null) {
                return this;
            }

            if (isEmpty()) {
                this.value = supplier.get();
            }

            return this;
        }
    }

    /**
     * Indicates whether some other object is "equal to" this {@link LazilySupplied}. The other object is
     * considered equal if:
     * <ul>
     * <li>it is also an {@link LazilySupplied} and;
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

        // TODO: this could be more optimal by comparing internal structures?

        return object instanceof LazilySupplied<?> other
            && Objects.equals(getOrNull(), other.getOrNull());
    }

    @Override
    public String toString() {
        // TODO: this should be lazy when there's a supplier and no value!
        return getOrNull() == null
            ? "Lazy.empty"
            : "Lazy[" + this.value + "]";
    }

    /**
     * Obtains an empty {@link Lazy} value.
     *
     * @param <T> the type of {@link Lazy} value
     * @return an empty {@link Lazy} value
     */
    public static <T> Lazy<T> empty() {
        return new LazilySupplied<>();
    }

    /**
     * Obtains a {@link Lazy} representation of the {@code null}able value.
     *
     * @param <T>   the type of {@link Lazy} value
     * @param value the type
     * @return a new {@link Lazy} value
     */
    public static <T> Lazy<T> ofNullable(final T value) {
        return value == null
            ? Lazy.empty()
            : Lazy.of(value);
    }

    /**
     * Obtains a {@link Lazy} that will be lazily initialized with a non-{@code null} value provided by the
     * {@link Supplier}.
     *
     * @param <T>      the type of {@link Lazy} value
     * @param supplier the {@link Supplier} of a non-{@code null} value
     * @return a new {@link Lazy} value
     */
    public static <T> Lazy<T> of(final Supplier<? extends T> supplier) {
        return supplier == null
            ? Lazy.empty()
            : new LazilySupplied<>(supplier);
    }
}
