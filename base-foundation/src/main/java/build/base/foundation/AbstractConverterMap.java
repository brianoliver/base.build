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

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * An {@code abstract} {@link Map} which stores its entries in another {@link Map} with keys and/or values in a
 * converted form.
 *
 * @param <K>  the key type
 * @param <V>  the value type
 * @param <UK> the underlying key type
 * @param <UV> the underlying value type
 * @author mark.falco
 * @author brian.oliver
 * @since March-2022
 */
public abstract class AbstractConverterMap<K, V, UK, UV>
    extends AbstractMap<K, V> {

    /**
     * The underlying {@link Map}.
     */
    protected final Map<UK, UV> underlying;

    /**
     * A cached {@link #entrySet()}.
     */
    protected Set<Entry<K, V>> entries;

    /**
     * A cached {@link #keySet()}.
     */
    protected Set<K> keys;

    /**
     * A cached {@link #values()}.
     */
    protected Collection<V> values;

    /**
     * Constructs an {@link AbstractConverterMap}.
     *
     * @param underlying the underlying {@link Map}
     */
    protected AbstractConverterMap(final Map<UK, UV> underlying) {
        this.underlying = Objects.requireNonNull(underlying, "The underlying Map must not be null");
    }

    /**
     * Invoked prior to mutating operations against the underlying {@link Map}.
     */
    protected void preWrite() {
    }

    /**
     * Invoked prior to non-mutating operations against the underlying {@link Map}.
     */
    protected void preRead() {
    }

    @Override
    public int size() {
        preRead();
        return this.underlying.size();
    }

    @Override
    public boolean isEmpty() {
        preRead();
        return this.underlying.isEmpty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean containsValue(final Object value) {
        preRead();
        return isOuterValue(value) && this.underlying.containsValue(downValue((V) value));
    }

    @Override
    public boolean containsKey(final Object key) {
        preRead();
        return isOuterKey(key) && this.underlying.containsKey(downReadKey(key));
    }

    @Override
    public V get(final Object key) {
        preRead();
        return isOuterKey(key) ? upValue(this.underlying.get(downReadKey(key))) : null;
    }

    @Override
    public V put(final K key, final V value) {
        preWrite();
        return upValue(this.underlying.put(downKey(key), downValue(value)));
    }

    @Override
    public V remove(final Object key) {
        preWrite();
        return isOuterKey(key) ? upValue(this.underlying.remove(downReadKey(key))) : null;
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> that) {
        preWrite();
        that.forEach((key, value) -> this.underlying.put(downKey(key), downValue(value)));
    }

    @Override
    public void clear() {
        preWrite();
        this.underlying.clear();
    }

    @Override
    public Set<K> keySet() {
        Set<K> keys = this.keys;
        if (keys == null) {
            this.keys = keys = new ConverterKeySet();
        }

        return keys;
    }

    @Override
    public Collection<V> values() {
        preRead();
        Collection<V> values = this.values;
        if (values == null) {
            this.values = values = new ConverterValues();
        }

        return values;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> entries = this.entries;
        if (entries == null) {
            this.entries = entries = new ConverterEntrySet();
        }

        return entries;
    }

    @Override
    public void replaceAll(final BiFunction<? super K, ? super V, ? extends V> function) {
        preWrite();
        this.underlying.replaceAll((sk, sv) -> downValue(function.apply(upKey(sk), upValue(sv))));
    }

    @Override
    public V putIfAbsent(final K key, final V value) {
        preWrite();
        return upValue(this.underlying.putIfAbsent(downKey(key), downValue(value)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean remove(final Object key, final Object value) {
        preWrite();
        return isOuterKey(key) && isOuterValue(value) && this.underlying.remove(downReadKey(key), downValue((V) value));
    }

    @Override
    public boolean replace(final K key, final V oldValue, final V newValue) {
        preWrite();
        return this.underlying.replace(downKey(key), downValue(oldValue), downValue(newValue));
    }

    @Override
    public V replace(final K key, final V value) {
        preWrite();
        return upValue(this.underlying.replace(downKey(key), downValue(value)));
    }

    @Override
    public V computeIfAbsent(final K key, final Function<? super K, ? extends V> mappingFunction) {
        final V value = get(key); // because ConcurrentHashMap doesn't do this useful optimization
        if (value == null) {
            preWrite();
            return upValue(this.underlying.computeIfAbsent(downKey(key), sk -> downValue(mappingFunction.apply(key))));
        }

        preRead();
        return value;
    }

    @Override
    public V computeIfPresent(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        preWrite();
        return upValue(this.underlying.computeIfPresent(downKey(key),
            (sk, sv) -> downValue(remappingFunction.apply(key, upValue(sv)))));
    }

    @Override
    public V compute(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        preWrite();
        return upValue(this.underlying.compute(downKey(key),
            (sk, sv) -> downValue(remappingFunction.apply(key, upValue(sv)))));
    }

    @Override
    public V merge(final K key, final V value, final BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        preWrite();
        return upValue(this.underlying.merge(downKey(key), downValue(value),
            (sv1, sv2) -> downValue(remappingFunction.apply(upValue(sv1), upValue(sv2)))));
    }

    @Override
    public V getOrDefault(final Object key, final V defaultValue) {
        return isOuterKey(key)
            ? upValue(this.underlying.getOrDefault(downReadKey(key), downValue(defaultValue)))
            : defaultValue;
    }

    @Override
    public void forEach(final BiConsumer<? super K, ? super V> action) {
        this.underlying.forEach((sk, sv) -> action.accept(upKey(sk), upValue(sv)));
    }

    /**
     * Return {@code true} iff the supplied key is compatible with the map.
     *
     * @param key the key to test
     * @return {@code true} iff compatible
     */
    protected boolean isOuterKey(final Object key) {
        return true;
    }

    /**
     * Return {@code true} iff the supplied value is compatible with the map.
     *
     * @param value the value to test
     * @return {@code true} iff compatible
     */
    protected boolean isOuterValue(final Object value) {
        return true;
    }

    /**
     * Convert the key into its storage type.
     *
     * @param key the key to convert
     * @return the converted key
     */
    protected abstract UK downKey(K key);

    /**
     * A special variant of {@link #downKey} which produces a key which is only meant to be used in operations which
     * will *not* store the key in the map
     *
     * @param key the key
     * @return the non-storable storage key
     */
    @SuppressWarnings("unchecked")
    protected Object downReadKey(final Object key) {
        return downKey((K) key);
    }

    /**
     * Convert a key from its storage type.
     *
     * @param key the key to convert
     * @return the converted key
     */
    protected abstract K upKey(UK key);

    /**
     * Convert the value into its storage type.
     *
     * @param value the value to convert
     * @return the converted value
     */
    protected abstract UV downValue(V value);

    /**
     * Convert a value from its storage type.
     *
     * @param value the value to convert
     * @return the converted value
     */
    protected abstract V upValue(UV value);

    /**
     * The {@link #keySet()} implementation.
     */
    protected class ConverterKeySet
        extends AbstractConverterSet<K, UK> {

        /**
         * Construct a {@link ConverterKeySet}.
         */
        protected ConverterKeySet() {
            super(AbstractConverterMap.this.underlying.keySet());
        }

        @Override
        protected void preWrite() {
            AbstractConverterMap.this.preWrite();
        }

        @Override
        protected void preRead() {
            AbstractConverterMap.this.preRead();
        }

        @Override
        protected boolean isOuter(final Object key) {
            return isOuterKey(key);
        }

        @Override
        protected UK down(final K value) {
            return AbstractConverterMap.this.downKey(value);
        }

        @Override
        protected Object downRead(final Object key) {
            return AbstractConverterMap.this.downReadKey(key);
        }

        @Override
        protected K up(final UK value) {
            return AbstractConverterMap.this.upKey(value);
        }
    }

    /**
     * The {@link #values} implementation.
     */
    protected class ConverterValues
        extends AbstractConverterCollection<V, UV> {

        /**
         * Construct a {@link ConverterValues}
         */
        protected ConverterValues() {
            super(AbstractConverterMap.this.underlying.values());
        }

        @Override
        protected void preWrite() {
            AbstractConverterMap.this.preWrite();
        }

        @Override
        protected void preRead() {
            AbstractConverterMap.this.preRead();
        }

        @Override
        protected boolean isOuter(final Object value) {
            return isOuterValue(value);
        }

        @Override
        protected UV down(final V value) {
            return AbstractConverterMap.this.downValue(value);
        }

        @Override
        protected V up(final UV value) {
            return AbstractConverterMap.this.upValue(value);
        }
    }

    /**
     * The {@link #entrySet()} implementation.
     */
    protected class ConverterEntrySet
        extends AbstractConverterSet<Entry<K, V>, Entry<UK, UV>> {

        /**
         * Constract a {@link ConverterEntrySet}.
         */
        protected ConverterEntrySet() {
            super(AbstractConverterMap.this.underlying.entrySet());
        }

        @Override
        protected void preWrite() {
            AbstractConverterMap.this.preWrite();
        }

        @Override
        protected void preRead() {
            AbstractConverterMap.this.preRead();
        }

        @Override
        protected boolean isOuter(final Object o) {
            if (o instanceof Entry) {
                final Entry<?, ?> entry = (Entry<?, ?>) o;
                return isOuterKey(entry.getKey()) && isOuter(entry.getValue());
            }

            return false;
        }

        @Override
        protected Entry<UK, UV> down(final Entry<K, V> outer) {
            return new Entry<UK, UV>() {
                final UK key = downKey(outer.getKey());

                @Override
                public UK getKey() {
                    return this.key;
                }

                @Override
                public UV getValue() {
                    return downValue(outer.getValue());
                }

                @Override
                public UV setValue(final UV value) {
                    return downValue(outer.setValue(upValue(value)));
                }
            };
        }

        @Override
        protected Object downRead(final Object entry) {
            @SuppressWarnings("unchecked") final Entry<Object, V> outer = (Entry<Object, V>) entry;
            return new Entry<Object, UV>() {
                final Object key = downReadKey(outer.getKey());

                @Override
                public Object getKey() {
                    return this.key;
                }

                @Override
                public UV getValue() {
                    return downValue(outer.getValue());
                }

                @Override
                public UV setValue(final UV value) {
                    return downValue(outer.setValue(upValue(value)));
                }
            };
        }

        @Override
        protected Entry<K, V> up(final Entry<UK, UV> inner) {
            return new Entry<K, V>() {
                final K key = upKey(inner.getKey());

                @Override
                public K getKey() {
                    return this.key;
                }

                @Override
                public V getValue() {
                    return upValue(inner.getValue());
                }

                @Override
                public V setValue(final V value) {
                    return upValue(inner.setValue(downValue(value)));
                }
            };
        }
    }
}
