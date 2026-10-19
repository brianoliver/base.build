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
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Abstract base for thread-safe {@link Memoizer} implementations.
 * <p>
 * Uses a per-key {@link ReentrantLock} to synchronize cache misses. Locks are allocated on first
 * miss and removed after the result is stored, keeping the lock map lean. Cache hits follow a
 * lock-free fast path.
 * <p>
 * {@link ReentrantLock} is used rather than {@code ConcurrentHashMap.computeIfAbsent} because the
 * memoized function may re-enter this memoizer for a key that hashes to the same map bin.
 * {@code computeIfAbsent} holds the bin lock during computation, so a re-entrant call on the same
 * bin would deadlock or throw {@link IllegalStateException}. The per-key lock approach holds no bin
 * lock during computation, so re-entrant calls always succeed.
 * <p>
 * The supplied cache {@link Map} must implement {@link ConcurrentMap} to ensure the fast-path read
 * is visible across threads without holding a lock.
 * <p>
 * Subclasses may customize cache access by overriding {@link #readCache(Object)} and
 * {@link #writeCache(Object, Object)} — for example, to wrap results in
 * {@link java.lang.ref.SoftReference}s as {@link SoftConcurrentMemoizer} does.
 *
 * @param <T> the input type
 * @param <R> the result type
 * @author reed.vonredwitz
 * @since Jun-2026
 */
public abstract class AbstractConcurrentMemoizer<T, R> extends BaseMemoizer<T, R> {

    private final ConcurrentMap<Object, ReentrantLock> computeLocks;

    /**
     * Constructs an {@link AbstractConcurrentMemoizer}.
     *
     * @param function      the function to memoize; must not be {@code null}
     * @param mapSupplier   supplies the backing cache {@link ConcurrentMap}; must not be {@code null}
     * @param locksSupplier supplies the per-key lock {@link ConcurrentMap}; must not be {@code null}
     * @throws IllegalArgumentException if the map returned by {@code mapSupplier} does not implement
     *                                  {@link ConcurrentMap}
     */
    protected AbstractConcurrentMemoizer(
        final Function<T, R> function,
        final Supplier<Map<Object, Object>> mapSupplier,
        final Supplier<ConcurrentMap<Object, ReentrantLock>> locksSupplier) {
        super(function, mapSupplier);
        if (!(this.cache instanceof ConcurrentMap)) {
            throw new IllegalArgumentException(
                "The supplied Map must implement ConcurrentMap to support lock-free reads");
        }
        this.computeLocks = Objects.requireNonNull(
            Objects.requireNonNull(locksSupplier, "locksSupplier must not be null").get(),
            "the supplied ConcurrentMap must not be null");
    }

    @Override
    public final R compute(final T input) {
        return computeWith(input, () -> this.function.apply(input));
    }

    @Override
    public final R computeWith(final T input, final Supplier<R> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null");
        final var key = input == null ? NULL_KEY : input;

        // fast path — no lock needed on a cache hit
        var cached = readCache(key);
        if (cached != null) {
            return unwrap(cached);
        }

        final var lock = this.computeLocks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            cached = readCache(key);
            if (cached != null) {
                return unwrap(cached);
            }
            final R result = supplier.get();
            writeCache(key, wrap(result));
            return result;
        } finally {
            lock.unlock();
            this.computeLocks.remove(key);
        }
    }
}
