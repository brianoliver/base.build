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
import java.util.function.Supplier;

/**
 * A {@link Lazy} value that is lazily mapped to another value.
 *
 * @param <T> the type of the underlying {@link Lazy} value
 * @param <R> the type of the mapped value
 * @author brian.oliver
 * @since Feb-2026
 */
final class LazilyMapped<T, R> implements Lazy<R> {

    /**
     * The underlying {@link Lazy} value to be lazily mapped.
     */
    private final Lazy<T> underlying;

    /**
     * The {@link Function} to map a value to another value.
     */
    private final Function<? super T, ? extends R> mapper;

    /**
     * Constructs a {@link LazilyMapped} given the specified {@link Lazy} value and {@link Function}.
     *
     * @param lazy   the {@link Lazy} value
     * @param mapper the {@link Function} to map the {@link Lazy} value to another value
     */
    LazilyMapped(final Lazy<T> lazy,
                 final Function<? super T, R> mapper) {

        this.underlying = Objects.requireNonNull(lazy, "The underlying lazy value must not be null");
        this.mapper = Objects.requireNonNull(mapper, "The value mapper must not be null");
    }

    @Override
    public R getOrNull() {
        final var value = this.underlying.getOrNull();

        return value == null ? null : this.mapper.apply(value);
    }

    @Override
    public boolean isPresent() {
        return this.underlying.isPresent();
    }

    @Override
    public Lazy<R> set(final R value) throws IllegalStateException {
        throw new IllegalStateException("Unable to set a Lazily Mapped value");
    }

    @Override
    public Lazy<R> computeIfAbsent(final Supplier<? extends R> supplier) {
        throw new IllegalStateException("Unable to set a Lazily Mapped value");
    }
}
