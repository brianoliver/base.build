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
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A {@link Lazy} value that is lazily filtered.
 *
 * @param <T> the type of the underlying {@link Lazy} value
 * @author brian.oliver
 * @since Feb-2026
 */
final class LazilyFiltered<T> implements Lazy<T> {

    /**
     * The underlying {@link Lazy} value to be lazily filtered.
     */
    private final Lazy<T> underlying;

    /**
     * The {@link Predicate} to filter the underlying {@link Lazy} value.
     */
    private final Predicate<? super T> predicate;

    /**
     * Constructs a {@link LazilyFiltered} given the specified {@link Lazy} value and {@link Predicate}.
     *
     * @param underlying the {@link Lazy} value
     * @param predicate  the {@link Predicate} to filter the {@link Lazy} value
     */
    LazilyFiltered(final Lazy<T> underlying,
                   final Predicate<? super T> predicate) {

        this.underlying = Objects.requireNonNull(underlying, "The underlying Lazy value must not be null");
        this.predicate = Objects.requireNonNull(predicate, "The Predicate must not be null");
    }

    @Override
    public T getOrNull() {
        final var value = this.underlying.getOrNull();

        return value != null && this.predicate.test(value)
            ? value
            : null;
    }

    @Override
    public boolean isEmpty() {
        return this.underlying.isEmpty() || getOrNull() == null;
    }

    @Override
    public boolean isPresent() {
        return this.underlying.isPresent() && getOrNull() != null;
    }

    @Override
    public Lazy<T> set(final T value) throws IllegalStateException {
        throw new IllegalStateException("Unable to set a Lazily Filtered value");
    }

    @Override
    public Lazy<T> computeIfAbsent(final Supplier<? extends T> supplier) {
        throw new IllegalStateException("Unable to set a Lazily Filtered value");
    }
}
