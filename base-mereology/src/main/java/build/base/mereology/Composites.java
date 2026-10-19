package build.base.mereology;

/*-
 * #%L
 * base.build Mereology
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

import build.base.foundation.Arrays;
import build.base.foundation.iterator.Iterators;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Provides mechanisms to work with {@link Composite}s.
 *
 * @author brian.oliver
 * @since Sep-2025
 */
public class Composites {

    /**
     * Prevent public instantiation.
     */
    private Composites() {
        // prevent public instantiation
    }

    /**
     * Creates a {@link Composite} based on the specified <i>parts</i>.
     *
     * @param <P>   the type of <i>parts</i>
     * @param parts the <i>parts</i>
     * @return a {@link Composite} consisting of the specified <i>parts</i>
     */

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <P> Composite of(final P... parts) {
        return new Composite() {
            @Override
            public <T> Iterator<T> iterator(final Class<T> type) {
                return type == null
                    ? Iterators.empty()
                    : (type == Object.class
                    ? (Iterator<T>) Arrays.iterator(parts)
                    : Iterators.isInstanceOf(Arrays.iterator(parts), type));
            }
        };
    }

    /**
     * Creates a {@link Composite} that uses the underlying {@link Iterable} as its <i>parts</i>.
     *
     * @param <P>   the type of <i>parts</i>
     * @param parts the {@link Iterable} of <i>parts</i>
     * @return a {@link Composite}
     */
    @SuppressWarnings("unchecked")
    public static <P> Composite of(final Iterable<P> parts) {

        return new Composite() {
            @Override
            public <T> Iterator<T> iterator(final Class<T> type) {
                return type == null
                    ? Iterators.empty()
                    : (type == Object.class
                    ? (Iterator<T>) parts.iterator()
                    : Iterators.isInstanceOf(parts.iterator(), type));
            }
        };
    }

    /**
     * Creates a {@link Composite} based on the keys of a {@link Map}.
     *
     * @param <K> the type of key
     * @param map the {@link Map}
     * @return a {@link Composite} of the keys of the {@link Map}
     */
    public static <K> Composite keysOf(final Map<K, ?> map) {
        return of(map.keySet());
    }

    /**
     * Creates a {@link Composite} based on the values of a {@link Map}.
     *
     * @param <V> the type of value
     * @param map the {@link Map}
     * @return a {@link Composite} of the values of the {@link Map}
     */
    public static <V> Composite valuesOf(final Map<?, V> map) {
        return of(map.values());
    }

    /**
     * Walks the transitive composition of {@code root} and returns every reachable {@link Composite}
     * that matches {@code suspect} and exposes no parts — a signal that
     * {@link Composite#iterator(Class)} is not correctly implemented for that node.
     *
     * @param root    the root {@link Composite} to walk, included reflexively
     * @param suspect identifies which {@link Composite}s are expected to have parts
     * @return the list of suspect empty {@link Composite}s; empty when none are found
     */
    public static List<Composite> verify(final Composite root, final Predicate<? super Composite> suspect) {
        return root.traverse(Composite.class)
            .reflexive(true)
            .stream()
            .filter(suspect)
            .filter(c -> !c.iterator(Object.class).hasNext())
            .toList();
    }
}
