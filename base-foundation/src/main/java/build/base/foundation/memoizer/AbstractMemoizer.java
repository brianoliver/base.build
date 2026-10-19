package build.base.foundation.memoizer;

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

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Abstract base for non-thread-safe {@link Memoizer} implementations.
 * <p>
 * Subclasses may customize cache access by overriding {@link #readCache(Object)} and
 * {@link #writeCache(Object, Object)} — for example, to wrap results in
 * {@link java.lang.ref.SoftReference}s as {@link SoftMemoizer} does.
 * <p>
 * This class is <em>not</em> thread-safe. For concurrent use see {@link AbstractConcurrentMemoizer}.
 *
 * @param <T> the input type
 * @param <R> the result type
 * @author reed.vonredwitz
 * @since Jun-2026
 */
public abstract class AbstractMemoizer<T, R> extends BaseMemoizer<T, R> {

    protected AbstractMemoizer(final Function<T, R> function, final Supplier<Map<Object, Object>> mapSupplier) {
        super(function, mapSupplier);
    }

    @Override
    public final R compute(final T input) {
        return computeWith(input, () -> this.function.apply(input));
    }

    @Override
    public final R computeWith(final T input, final Supplier<R> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null");
        final var key = input == null ? NULL_KEY : input;
        final var cached = readCache(key);
        if (cached != null) {
            return unwrap(cached);
        }
        final R result = supplier.get();
        writeCache(key, wrap(result));
        return result;
    }
}
