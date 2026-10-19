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

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * An immutable mapping from a key to a value.
 *
 * @param <K> the key type
 * @param <V> the value type
 *
 * @author brian.oliver
 * @since Feb-2025
 */
public class Mapping<K, V> {

    /**
     * The key.
     */
    private final K key;

    /**
     * The value.
     */
    private final V value;

    /**
     * Constructs a {@link Mapping} given a key and value.
     *
     * @param key the key
     * @param value the value
     */
    private Mapping(final K key, final V value) {
        this.key = Objects.requireNonNull(key, "The key must not be null");
        this.value = value;
    }

    /**
     * Obtains the key of the {@link Mapping}.
     *
     * @return the key
     */
    public K key() {
        return this.key;
    }

    /**
     * Obtains the value of the {@link Mapping}.
     *
     * @return the value
     */
    public V value() {
        return this.value;
    }

    /**
     * Constructs a {@link Mapping} given a key and value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param key the key
     * @param value the value
     *
     * @return a {@link Mapping}
     */
    public static <K, V> Mapping<K, V> of(final K key, final V value) {
        return new Mapping<>(key, value);
    }

    /**
     * Constructs a {@link Mapping} given a {@link java.util.Map.Entry}.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param entry the {@link Map.Entry}
     *
     * @return a {@link Mapping}
     */
    public static <K, V> Mapping<K, V> of(final Map.Entry<K, V> entry) {
        return new Mapping<>(entry.getKey(), entry.getValue());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
            .add("key = " + this.key)
            .add("value = " + this.value)
            .toString();
    }

    /**
     * Obtains a {@link Collector} that can collect {@link Mapping}s into a {@link Map}.
     *
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @return a new {@link Map} containing the collected {@link Mapping}s
     */
    public static <K, V> Collector<Mapping<K, V>, ?, Map<K, V>> collector() {
        return new Collector<Mapping<K, V>, LinkedHashMap<K, V>, Map<K, V>>() {
            @Override
            public Supplier<LinkedHashMap<K, V>> supplier() {
                return LinkedHashMap::new;
            }

            @Override
            public BiConsumer<LinkedHashMap<K, V>, Mapping<K, V>> accumulator() {
                return (map, mapping) -> map.put(mapping.key(), mapping.value());
            }

            @Override
            public BinaryOperator<LinkedHashMap<K, V>> combiner() {
                return (first, second) -> {
                    first.putAll(second);
                    return first;
                };
            }

            @Override
            public Function<LinkedHashMap<K, V>, Map<K, V>> finisher() {
                return map -> map;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return EnumSet.noneOf(Characteristics.class);
            }
        };
    }
}
