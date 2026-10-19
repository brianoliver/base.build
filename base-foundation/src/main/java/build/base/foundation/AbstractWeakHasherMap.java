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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * A hash based {@link Map} which weakly references its keys and which compares them based on a {@link Hasher}.
 * <p>
 * As with {@link WeakHashMap} this map will eventually remove an entry once its key is no longer reachable.
 * <p>
 * This map does not support {@code null} keys.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @author mark.falco
 * @since March-2022
 */
public abstract class AbstractWeakHasherMap<K, V>
    extends AbstractConverterMap<K, V, AbstractWeakHasherMap.WeakHasherReference<K>, V> {

    /**
     * The {@link Hasher}.
     */
    protected final Hasher<? super K> hasher;

    /**
     * The queue of {@link WeakHasherReference}s which can be removed from the map.
     */
    protected final ReferenceQueue<K> garbageQueue = new ReferenceQueue<>();

    /**
     * Constructs an {@link AbstractWeakHasherMap}.
     *
     * @param storage the storage map
     * @param hasher  the hasher.
     */
    protected AbstractWeakHasherMap(final Map<WeakHasherReference<K>, V> storage,
                                    final Hasher<? super K> hasher) {
        super(storage);
        this.hasher = Objects.requireNonNull(hasher, "The Hasher must not be null");
    }

    @Override
    public int size() {
        gc(); // in case we had an iter.remove calls
        return super.size();
    }

    @Override
    public boolean isEmpty() {
        gc(); // in case we had an iter.remove calls
        return super.isEmpty();
    }

    @Override
    public Set<K> keySet() {
        var keys = this.keys;
        if (keys == null) {
            this.keys = keys = new ConverterKeySet() {
                @Override
                public int size() {
                    return AbstractWeakHasherMap.this.size();
                }

                @Override
                public boolean isEmpty() {
                    return AbstractWeakHasherMap.this.isEmpty();
                }

                @Override
                public Iterator<K> iterator() {
                    // iterate via the entrySet to get an iterator which is stable in the presence of GC
                    return new AbstractConverterIterator<K, Entry<K, V>>(entrySet().iterator()) {
                        @Override
                        protected K up(final Entry<K, V> entry) {
                            return entry.getKey();
                        }
                    };
                }
            };
        }

        return keys;
    }

    @Override
    public Collection<V> values() {
        var values = this.values;
        if (values == null) {
            this.values = values = new ConverterValues() {

                @Override
                public int size() {
                    return AbstractWeakHasherMap.this.size();
                }

                @Override
                public boolean isEmpty() {
                    return AbstractWeakHasherMap.this.isEmpty();
                }

                @Override
                public Iterator<V> iterator() {
                    // iterate via the entrySet to get an iterator which is stable in the presence of GC
                    return new AbstractConverterIterator<V, Entry<K, V>>(entrySet().iterator()) {
                        @Override
                        protected V up(final Entry<K, V> entry) {
                            return entry.getValue();
                        }
                    };
                }
            };
        }

        return values;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        var entries = this.entries;
        if (entries == null) {
            this.entries = entries = new ConverterEntrySet() {
                @Override
                public int size() {
                    return AbstractWeakHasherMap.this.size();
                }

                @Override
                public boolean isEmpty() {
                    return AbstractWeakHasherMap.this.isEmpty();
                }

                @Override
                public Iterator<Entry<K, V>> iterator() {
                    preRead();
                    final Function<Entry<WeakHasherReference<K>, V>, Entry<K, V>> up = this::up;
                    return new AbstractConverterIterator<Entry<K, V>, Entry<WeakHasherReference<K>, V>>(this.underlying.iterator()) {

                        // We look ahead and hold the next converted entry thereby ensuring that it can't be concurrently
                        // removed via GC and invalidate our hasNext() result. We hold the prior key and value so that we can
                        // honor iter.remove() which we can't simply delegate to our storageIterator as it is one ahead
                        // of us and would remove the next rather than prior entry. We hold onto the key/value rather
                        // than entry as the entry itself to avoid needless garbage retention
                        Entry<K, V> next = preloadNext();
                        WeakHasherReference<K> nextStorageKey;
                        WeakHasherReference<K> priorStorageKey;

                        @Override
                        public boolean hasNext() {
                            return this.next != null;
                        }

                        /**
                         * Preload the next entry with a non-GC'd key
                         *
                         * @return the next entry
                         */
                        Entry<K, V> preloadNext() {
                            Entry<K, V> next = null;
                            WeakHasherReference<K> nextStorageKey = null;
                            while (next == null && this.underlying.hasNext()) {
                                final Entry<WeakHasherReference<K>, V> storageNext = this.underlying.next();
                                nextStorageKey = storageNext.getKey();
                                next = up(storageNext);

                                if (nextStorageKey.get() == null) {
                                    // we don't allow null keys, thus key has been GC'd
                                    next = null;
                                    nextStorageKey = null;
                                }
                            }

                            this.nextStorageKey = nextStorageKey;
                            return next;
                        }

                        @Override
                        public Entry<K, V> next() {
                            final Entry<K, V> result = this.next;
                            if (result == null) {
                                throw new NoSuchElementException();
                            }

                            this.priorStorageKey = this.nextStorageKey;
                            this.next = preloadNext();

                            return result;
                        }

                        @Override
                        public void remove() {
                            // We can't delegate to storageIterator.remove as it always point one ahead of where we are
                            // and we can't use map.remove since that would invalidate the iterator on most maps. But we
                            // can invalidate the key via its weak reference which results in the same effect.
                            final WeakHasherReference<K> priorKey = this.priorStorageKey;
                            if (priorKey == null) {
                                throw new NoSuchElementException();
                            }

                            this.priorStorageKey = null;
                            priorKey.enqueue();
                            priorKey.clear();
                        }

                        @Override
                        protected Entry<K, V> up(final Entry<WeakHasherReference<K>, V> entry) {
                            return up.apply(entry);
                        }
                    };
                }
            };
        }

        return entries;
    }

    @Override
    protected void preWrite() {
        gc(true);
    }

    @Override
    protected void preRead() {
        gc(false);
    }

    @Override
    protected WeakHasherReference<K> downKey(final K key) {
        Objects.requireNonNull(key, "The key must not be null");
        return new WeakHasherReference<>(key, this.hasher, this.garbageQueue);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected StrongHasherReference<K> downReadKey(final Object key) {
        return new StrongHasherReference<>((K) key, this.hasher);
    }

    @Override
    protected final K upKey(final WeakHasherReference<K> key) {
        return key.get();
    }

    @Override
    protected final V downValue(final V value) {
        return value;
    }

    @Override
    protected final V upValue(final V value) {
        return value;
    }

    /**
     * Remove any garbage keys.
     *
     * @param preWrite {@code true} if called as part of a write operation
     */
    protected void gc(final boolean preWrite) {
        gc();
    }

    /**
     * Remove any garbage keys.
     */
    @SuppressWarnings("unchecked")
    protected void gc() {
        WeakHasherReference<K> key = (WeakHasherReference<K>) this.garbageQueue.poll();
        if (key != null) {
            // just like WeakHashMap we take a lock while doing "gc" as multiple reader threads could get here
            // concurrently in a map which is being accessing in a read-only fashion
            synchronized (this.garbageQueue) {
                do {
                    this.underlying.remove(key);
                } while ((key = (WeakHasherReference<K>) this.garbageQueue.poll()) != null);
            }
        }
    }

    /**
     * Wrapper around the map's keys to allow them to be GC'able.
     *
     * @param <K> the key type
     */
    protected static class WeakHasherReference<K>
        extends WeakReference<K> {

        /**
         * The map's hasher.
         */
        final Hasher<? super K> hasher;

        /**
         * The wrapped key's hash.
         */
        final int hash;

        /**
         * Construct a {@link WeakHasherReference}.
         *
         * @param key    the key to wrap
         * @param hasher the {@link Hasher} for the storage map
         * @param queue  the {@link ReferenceQueue} or {@code null} to enqueue the reference once cleared
         */
        protected WeakHasherReference(final K key, final Hasher<? super K> hasher, final ReferenceQueue<K> queue) {
            super(key, queue);
            this.hasher = hasher;
            this.hash = hasher.hashCode(key);
        }

        @Override
        public int hashCode() {
            return this.hash;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(final Object obj) {
            return obj instanceof WeakHasherReference<?>
                ? this.hasher.equals(get(), ((WeakHasherReference<K>) obj).get())
                : obj instanceof StrongHasherReference<?>
                && this.hasher.equals(get(), ((StrongHasherReference<K>) obj).get());
        }
    }

    /**
     * A key wrapper which can be compared with a proper {@link WeakHasherReference}, but which is not suitable for
     * being stored in the storage map. Specifically the key is strongly referenced.
     * <p>
     * This unsafe variant is far cheaper to construct then a {@link WeakHasherReference} and can be used to optimize
     * operations which will not add the key to the storage map.
     *
     * @param <K> the key type
     */
    protected static class StrongHasherReference<K>
        extends AbstractHasherReference<K> {

        /**
         * The map's hasher.
         */
        final Hasher<? super K> hasher;

        /**
         * Construct a {@link StrongHasherReference}.
         *
         * @param key    the key to wrap
         * @param hasher the {@link Hasher} for the storage map
         */
        protected StrongHasherReference(final K key, final Hasher<? super K> hasher) {
            super(key, hasher);
            this.hasher = hasher;
        }

        @Override
        protected Hasher<? super K> getHasher() {
            return this.hasher;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(final Object obj) {
            return obj instanceof WeakHasherReference<?>
                ? this.hasher.equals(this.key, ((WeakHasherReference<K>) obj).get())
                : obj instanceof StrongHasherReference<?>
                && this.hasher.equals(this.key, ((StrongHasherReference<K>) obj).get());
        }
    }
}
