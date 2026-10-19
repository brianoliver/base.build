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

import build.base.foundation.ConcurrentWeakHashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A thread-safety-agnostic contract for <a href="https://en.wikipedia.org/wiki/Memoization">memoization</a>
 * of {@link Function} invocations.
 * <p>
 * Instances are obtained via the builder:
 * <pre>{@code
 * Memoizer.of(fn).build()                              // non-concurrent, strong references
 * Memoizer.of(fn).soft().build()                       // non-concurrent, soft references (GC-evictable values)
 * Memoizer.of(fn).weak().build()                       // non-concurrent, weak keys (entries evicted when key is unreachable)
 * Memoizer.of(fn).concurrent().build()                 // concurrent, strong references
 * Memoizer.of(fn).soft().concurrent().build()          // concurrent, soft references
 * Memoizer.of(fn).weak().concurrent().build()          // concurrent, weak keys
 * Memoizer.of(fn).concurrent().withLocks(supplier)...  // concurrent with custom lock map
 * }</pre>
 * Custom implementations can extend {@link AbstractMemoizer} or {@link AbstractConcurrentMemoizer}.
 *
 * @param <T> the input type
 * @param <R> the result type
 * @author reed.vonredwitz
 * @since Jun-2026
 */
public interface Memoizer<T, R> {

    /**
     * Returns the memoized result for {@code input}, computing it via the underlying {@link Function}
     * on the first call for a given input.
     *
     * @param input the input; may be {@code null}
     * @return the memoized result; may be {@code null}
     */
    R compute(T input);

    /**
     * Returns the memoized result for {@code input}, computing it via {@code supplier} on the
     * first call for a given input. Unlike {@link #compute(Object)}, the computation is not fixed
     * to the memoizer's underlying {@link Function} — this allows callers who don't have (or don't
     * want to construct) a {@code Function<T, R>} up front to still share the same cache, keyed by
     * {@code input}.
     * <p>
     * {@code supplier} is only invoked on a cache miss; on a hit, the cached result is returned and
     * {@code supplier} is never called. If two callers race on the same {@code input} with different
     * suppliers, only one supplier's result is memoized — the other is discarded.
     *
     * @param input    the input; may be {@code null}
     * @param supplier supplies the result on a cache miss; must not be {@code null}
     * @return the memoized result; may be {@code null}
     */
    R computeWith(T input, Supplier<R> supplier);

    /**
     * Clears all memoized results, forcing subsequent calls to recompute.
     */
    void clear();

    /**
     * Returns the number of memoized results. For soft-reference and weak-key implementations
     * this count is approximate, as entries may have been collected by the GC.
     *
     * @return the number of memoized results
     */
    int size();

    /**
     * Returns {@code true} if a result is currently memoized for {@code input}.
     *
     * @param input the input to check; may be {@code null}
     * @return {@code true} if a memoized result is present
     */
    boolean contains(T input);

    /**
     * Returns a {@link Builder} for the given function.
     *
     * @param function the function to memoize; must not be {@code null}
     */
    static <T, R> Builder<T, R> of(final Function<T, R> function) {
        return new Builder<>(function);
    }

    /**
     * Builder for non-concurrent {@link Memoizer} instances. Defaults to strong references;
     * call {@link #soft()} or {@link #weak()} to change the eviction strategy. Call
     * {@link #concurrent()} to obtain a {@link ConcurrentBuilder} that adds thread-safety options.
     *
     * @param <T> the input type
     * @param <R> the result type
     */
    final class Builder<T, R> {

        final Function<T, R> function;
        Refs refs = Refs.STRONG;
        Supplier<Map<Object, Object>> mapSupplier;

        Builder(final Function<T, R> function) {
            this.function = Objects.requireNonNull(function, "function must not be null");
        }

        /** Uses soft references; entries may be evicted under memory pressure. */
        public Builder<T, R> soft() {
            this.refs = Refs.SOFT;
            return this;
        }

        /**
         * Uses weak keys; an entry is evicted once its key is no longer strongly reachable.
         * Suitable when the memoizer's lifetime should be governed by the key's lifetime rather
         * than by memory pressure.
         */
        public Builder<T, R> weak() {
            this.refs = Refs.WEAK;
            return this;
        }

        /**
         * Overrides the default backing cache map.
         */
        public Builder<T, R> withMap(final Supplier<Map<Object, Object>> mapSupplier) {
            this.mapSupplier = Objects.requireNonNull(mapSupplier, "mapSupplier must not be null");
            return this;
        }

        /**
         * Returns a {@link ConcurrentBuilder} that will produce a thread-safe memoizer using a
         * per-key lock. Carries over any {@link #soft()}, {@link #weak()}, and {@link #withMap}
         * configuration.
         */
        public ConcurrentBuilder<T, R> concurrent() {
            return new ConcurrentBuilder<>(this);
        }

        /** Builds the configured non-concurrent {@link Memoizer}. */
        public Memoizer<T, R> build() {
            return switch (refs) {
                case SOFT -> new SoftMemoizer<>(function, mapSupplier != null ? mapSupplier : HashMap::new);
                case WEAK -> new WeakMemoizer<>(function, mapSupplier != null ? mapSupplier : WeakHashMap::new);
                default   -> new StrongMemoizer<>(function, mapSupplier != null ? mapSupplier : HashMap::new);
            };
        }
    }

    /**
     * Builder for concurrent {@link Memoizer} instances, obtained by calling
     * {@link Builder#concurrent()}. Reference mode and backing map are inherited from the
     * {@link Builder}; call {@link #withLocks(Supplier)} to supply a custom per-key lock map.
     *
     * @param <T> the input type
     * @param <R> the result type
     */
    final class ConcurrentBuilder<T, R> {

        private final Builder<T, R> base;
        private Supplier<ConcurrentMap<Object, ReentrantLock>> locksSupplier;

        ConcurrentBuilder(final Builder<T, R> base) {
            this.base = base;
        }

        /**
         * Overrides the default per-key lock map.
         */
        public ConcurrentBuilder<T, R> withLocks(final Supplier<ConcurrentMap<Object, ReentrantLock>> locksSupplier) {
            this.locksSupplier = Objects.requireNonNull(locksSupplier, "locksSupplier must not be null");
            return this;
        }

        /** Builds the configured concurrent {@link Memoizer}. */
        public Memoizer<T, R> build() {
            final Supplier<ConcurrentMap<Object, ReentrantLock>> ls = locksSupplier != null ? locksSupplier : ConcurrentHashMap::new;
            return switch (base.refs) {
                case SOFT -> new SoftConcurrentMemoizer<>(base.function, base.mapSupplier != null ? base.mapSupplier : ConcurrentHashMap::new, ls);
                case WEAK -> new WeakConcurrentMemoizer<>(base.function, base.mapSupplier != null ? base.mapSupplier : ConcurrentWeakHashMap::new, ls);
                default   -> new StrongConcurrentMemoizer<>(base.function, base.mapSupplier != null ? base.mapSupplier : ConcurrentHashMap::new, ls);
            };
        }
    }
}
