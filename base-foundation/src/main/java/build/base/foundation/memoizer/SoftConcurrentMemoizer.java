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

import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A thread-safe {@link Memoizer} that holds memoized results with {@link SoftReference}s,
 * allowing the JVM to reclaim them under memory pressure. Evicted entries are transparently
 * recomputed on the next {@link #compute(Object)} call.
 * <p>
 * The fast-path read ({@link #readCache}) does not evict stale references to avoid a race between
 * concurrent reads and writes. Stale wrappers are overwritten when the value is recomputed under
 * the per-key lock.
 * <p>
 * {@link #size()} is approximate: it counts all entries including those whose referents have
 * already been collected.
 * <p>
 * For a non-concurrent variant see {@link SoftMemoizer}.
 *
 * @param <T> the input type
 * @param <R> the result type
 * @author reed.vonredwitz
 * @since Jun-2026
 */
final class SoftConcurrentMemoizer<T, R> extends AbstractConcurrentMemoizer<T, R> {

    SoftConcurrentMemoizer(
        final Function<T, R> function,
        final Supplier<Map<Object, Object>> mapSupplier,
        final Supplier<ConcurrentMap<Object, ReentrantLock>> locksSupplier) {
        super(function, mapSupplier, locksSupplier);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Object readCache(final Object key) {
        final var ref = (SoftReference<Object>) this.cache.get(key);
        if (ref == null) {
            return null;
        }
        // Returns null if the referent was collected; caller falls through to locked recomputation.
        // We intentionally do not evict the stale wrapper here to avoid a race with a concurrent write.
        return ref.get();
    }

    @Override
    protected void writeCache(final Object key, final Object value) {
        this.cache.put(key, new SoftReference<>(value));
    }
}
