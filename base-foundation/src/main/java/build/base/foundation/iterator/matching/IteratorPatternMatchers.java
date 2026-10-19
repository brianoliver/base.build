package build.base.foundation.iterator.matching;

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

import build.base.foundation.predicate.Predicates;

import java.util.Iterator;
import java.util.function.Predicate;

/**
 * Helper methods for {@link IteratorPatternMatcher}s.
 *
 * @author brian.oliver
 * @since Jun-2019
 */
public class IteratorPatternMatchers {

    /**
     * Private constructor to prevent instantiation.
     */
    private IteratorPatternMatchers() {
        // empty constructor
    }

    /**
     * Creates an {@link IteratorPatternMatcher} matching the starting position of an {@link Iterator},
     * followed by another pattern.
     *
     * @param <T> the type of {@link Iterator} elements
     * @return a new {@link IteratorPatternMatcher} pattern
     */
    public static <T> Sequence<T> starts() {
        return new Starts<>();
    }

    /**
     * Creates an {@link IteratorPatternMatcher} matching an element in an {@link Iterator}
     * using {@link Predicate#isEqual(Object)}.
     *
     * @param <T>   the type of {@link Iterator} elements
     * @param value the value to match
     * @return a new {@link IteratorPatternMatcher} pattern
     */
    public static <T> Matched<T, T> matches(final T value) {
        return matches(Predicates.isEqual(value, value));
    }

    /**
     * Creates an {@link IteratorPatternMatcher} matching an element in an {@link Iterator}
     * using to a {@link Class} of value.
     *
     * @param <T>          the type of {@link Iterator} elements
     * @param classOfValue the {@link Class} of the value to match
     * @return a new {@link IteratorPatternMatcher} pattern
     */
    @SuppressWarnings("unchecked")
    public static <T> Matched<T, T> matches(final Class<? extends T> classOfValue) {
        return (Matched<T, T>) matches(classOfValue::isInstance);
    }

    /**
     * Creates an {@link IteratorPatternMatcher} matching any value in an {@link Iterator},
     * using the {@link Predicates#always()} for matching.
     *
     * @param <T> the type of {@link Iterator} elements
     * @return a new {@link IteratorPatternMatcher} pattern
     */
    public static <T> Matched<T, T> matchesAny() {
        return matches(Predicates.always());
    }

    /**
     * Creates an {@link IteratorPatternMatcher} matching an element in an {@link Iterator}
     * using a {@link Predicate}.
     *
     * @param <T>       the type of {@link Iterator} elements
     * @param predicate the {@link Predicate} to match
     * @return a new {@link IteratorPatternMatcher} pattern
     */
    public static <T> Matched<T, T> matches(final Predicate<? super T> predicate) {
        return new Element<>(predicate);
    }

    /**
     * Creates an {@link IteratorPatternMatcher} matching either one or both of the
     * specified values in an {@link Iterator}.
     *
     * @param <T>    the type of {@link Iterator} elements
     * @param first  the first of the possible values
     * @param second the second of the possible values
     * @return a new {@link IteratorPatternMatcher} pattern
     */
    public static <T> Matched<T, T> matchesEither(final T first, final T second) {
        return matches(Predicates.anyOf(Predicates.isEqual(first, first), Predicates.isEqual(second, second)));
    }

    /**
     * Creates an {@link IteratorPatternMatcher} matching a nested
     * {@link IteratorPatternMatcher} in an {@link Iterator}.
     *
     * @param <T>     the type of {@link Iterator} elements
     * @param pattern the {@link IteratorPatternMatcher}
     * @return a new {@link IteratorPatternMatcher} pattern
     */
    public static <T> Composition<T, T> satisfies(final IteratorPatternMatcher<? super T> pattern) {
        return new NestedIteratorPatternMatcher<>(pattern);
    }
}
