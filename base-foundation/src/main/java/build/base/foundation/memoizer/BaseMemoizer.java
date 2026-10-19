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
 * Package-private base for {@link AbstractMemoizer} and {@link AbstractConcurrentMemoizer}, providing
 * shared infrastructure: null sentinels, the backing cache, the memoized function, and default
 * {@link #readCache}/{@link #writeCache} hooks.
 *
 * @param <T> the input type
 * @param <R> the result type
 */
abstract class BaseMemoizer<T, R> implements Memoizer<T, R> {

    /**
     * Sentinel used as the map key for {@code null} inputs.
     */
    protected static final Object NULL_KEY = new Object();

    /**
     * Sentinel stored in the cache as the map value for {@code null} results, since the backing
     * maps do not permit {@code null} values.
     */
    protected static final Object NULL_RESULT = new Object();

    /**
     * The backing cache. Values are either direct results (strong implementations) or
     * {@link java.lang.ref.SoftReference} wrappers (soft implementations).
     */
    protected final Map<Object, Object> cache;

    /**
     * The function whose results are memoized.
     */
    protected final Function<T, R> function;

    protected BaseMemoizer(final Function<T, R> function, final Supplier<Map<Object, Object>> mapSupplier) {
        this.function = Objects.requireNonNull(function, "function must not be null");
        Objects.requireNonNull(mapSupplier, "mapSupplier must not be null");
        this.cache = Objects.requireNonNull(mapSupplier.get(), "the supplied Map must not be null");
    }

    /**
     * Reads a value from the cache for the given key. Returns {@code null} if absent (or if a
     * soft reference has been collected). Subclasses override this to unwrap soft references.
     *
     * @param key the cache key (never {@code null}; callers substitute {@link #NULL_KEY})
     * @return the cached value, or {@code null} if not present
     */
    protected Object readCache(final Object key) {
        return this.cache.get(key);
    }

    /**
     * Writes a value to the cache. Subclasses override this to wrap results in soft references.
     *
     * @param key   the cache key
     * @param value the value to cache (never {@code null}; {@link #NULL_RESULT} is used for null results)
     */
    protected void writeCache(final Object key, final Object value) {
        this.cache.put(key, value);
    }

    @Override
    public void clear() {
        this.cache.clear();
    }

    @Override
    public int size() {
        return this.cache.size();
    }

    @Override
    public boolean contains(final T input) {
        return readCache(input == null ? NULL_KEY : input) != null;
    }

    @SuppressWarnings("unchecked")
    protected static <R> R unwrap(final Object value) {
        return value == NULL_RESULT ? null : (R) value;
    }

    protected static Object wrap(final Object value) {
        return value == null ? NULL_RESULT : value;
    }
}
