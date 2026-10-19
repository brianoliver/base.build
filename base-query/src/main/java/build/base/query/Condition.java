package build.base.query;

/*-
 * #%L
 * base.build Query
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

import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Provides a mechanism to specify a condition for filtering {@link Object}s of a specific {@link Class} being queried
 * based on values extracted from the {@link Object}s.
 *
 * @param <Q> the type of {@link Class} of {@link Object} being queried
 * @param <V> the type of value
 */
public interface Condition<Q, V> {

    /**
     * Specifies a value that must successfully compare with the extracted value using
     * {@link Objects#equals(Object, Object)}.
     *
     * @param value the value that must successfully compare
     * @return a {@link Terminal} that can be used to obtain the results
     */
    Terminal<Q, ?> isEqualTo(V value);

    /**
     * Specifies a value that must fail to compare with the extracted value using
     * {@link Objects#equals(Object, Object)}.
     *
     * @param value the value that must fail to compare
     * @return a {@link Terminal} that can be used to obtain the results
     */
    Terminal<Q, ?> isNotEqualTo(V value);

    /**
     * Specifies that the extracted value must be {@code null}.
     * <p>
     * The index normalizes {@code null} to an internal sentinel, so this is a thin wrapper around
     * {@link #isEqualTo(Object)} and benefits from the same indexed lookup.
     *
     * @return a {@link Terminal} that can be used to obtain the results
     */
    default Terminal<Q, ?> isNull() {
        return isEqualTo(null);
    }

    /**
     * Specifies that the extracted value must not be {@code null}.
     * <p>
     * A thin wrapper around {@link #isNotEqualTo(Object)}.
     *
     * @return a {@link Terminal} that can be used to obtain the results
     */
    default Terminal<Q, ?> isNotNull() {
        return isNotEqualTo(null);
    }

    /**
     * Specifies a {@link Collection} of values, one of which must successfully compare with the extracted value
     * using {@link Objects#equals(Object, Object)}.
     * <p>
     * When the underlying {@link Indexable} {@link java.util.function.Function} has been indexed, this performs one
     * reverse-map lookup per value and unions the results; otherwise it falls back to a linear scan.
     *
     * @param values the values, one of which must successfully compare
     * @return a {@link Terminal} that can be used to obtain the results
     */
    Terminal<Q, ?> isIn(Collection<? extends V> values);

    /**
     * Specifies a {@link Collection} of values, none of which may successfully compare with the extracted value
     * using {@link Objects#equals(Object, Object)}.
     * <p>
     * This always performs a linear scan because the reverse index only supports positive membership lookups;
     * negation cannot be answered from the index alone without enumerating all objects.
     *
     * @param values the values, none of which may successfully compare
     * @return a {@link Terminal} that can be used to obtain the results
     */
    default Terminal<Q, ?> isNotIn(final Collection<? extends V> values) {
        return doesNotMatch(values::contains);
    }

    /**
     * Specifies a {@link Predicate} that must successfully match the extracted value.
     *
     * @param predicate the {@link Predicate} that must match
     * @return a {@link Terminal} that can be used to obtain the results
     */
    Terminal<Q, ?> matches(Predicate<? super V> predicate);

    /**
     * Specifies a {@link Predicate} that must fail to match the extracted value.
     *
     * @param predicate the {@link Predicate} that must fail to match
     * @return a {@link Terminal} that can be used to obtain the results
     */
    default Terminal<Q, ?> doesNotMatch(final Predicate<? super V> predicate) {
        return matches(predicate.negate());
    }

    /**
     * Specifies an element that must be contained within the extracted value, which is expected to be a
     * {@link Collection}, {@link Stream}, or {@link Iterable}.
     * <p>
     * When the underlying {@link Indexable} {@link java.util.function.Function} has been indexed, this performs an O(1)
     * lookup; otherwise it falls back to a linear scan.
     *
     * @param element the element that must be present
     * @return a {@link Terminal} that can be used to obtain the results
     */
    Terminal<Q, ?> contains(Object element);

    /**
     * Specifies an element that must not be contained within the extracted value, which is expected to be a
     * {@link java.util.Collection}, {@link java.util.stream.Stream}, or {@link Iterable}.
     * <p>
     * This always performs a linear scan because the reverse index only supports positive membership lookups; negation
     * cannot be answered from the index alone without enumerating all objects.
     *
     * @param element the element that must be absent
     * @return a {@link Terminal} that can be used to obtain the results
     */
    Terminal<Q, ?> doesNotContain(Object element);
}
