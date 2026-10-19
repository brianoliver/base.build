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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static build.base.foundation.predicate.Predicates.descriptive;

/**
 * Represents a {@link Condition} for evaluating zero or more values provided by {@link Iterator}, and/or the
 * current state of an {@link Iterator}.
 *
 * @param <T> the type of elements to be evaluated
 * @author brian.oliver
 * @since Jun-2019
 */
public interface Term<T>
    extends Condition<T> {

    /**
     * Specifies a value to be matched, at the current position in an {@link Iterator},
     * using {@link Objects#equals(Object, Object)} for matching.
     *
     * @param value the value
     * @return a {@link Matched}, allowing capture of the match and composition of additional {@link Condition}s.
     */
    default Matched<T, T> matches(final Object value) {
        return matches(Predicates.isEqual(value, value));
    }

    /**
     * Specifies a {@link Class} of value to be matched, at the current position in an {@link Iterator},
     * using {@link Class#isInstance(Object)} for matching.
     *
     * @param <C>          the {@link Class} of value to be matched
     * @param classOfValue the {@link Class} of value
     * @return a {@link Matched}, allowing capture of the match and composition of additional {@link Condition}s.
     */
    @SuppressWarnings("unchecked")
    default <C extends T> Matched<T, C> matches(final Class<C> classOfValue) {
        return (Matched<T, C>) matches((Predicate<T>) Predicates.isInstance(classOfValue));
    }

    /**
     * Specifies two possible values to be matched, at the current position in an {@link Iterator},
     * using {@link Objects#equals(Object, Object)} for matching.
     *
     * @param first  the first value
     * @param second the second value
     * @return a {@link Matched}, allowing capture of the match and composition of additional {@link Condition}s.
     */
    default Matched<T, T> matchesEither(final Object first, final Object second) {
        return matches(Predicates.anyOf(Predicates.isEqual(first, first), Predicates.isEqual(second, second)));
    }

    /**
     * Specifies any value is to be matched, at the current position in the {@link Iterator},
     * using the {@link Predicates#always()} for matching.
     *
     * @return a {@link Matched}, allowing capture of the match and composition of additional {@link Condition}s.
     */
    default Matched<T, T> matchesAny() {
        return matches(Predicates.always());
    }

    /**
     * Specifies a {@link Predicate} to be evaluated using the value at the current position in an {@link Iterator}.
     *
     * @param predicate the {@link Predicate}
     * @return a {@link Matched}, allowing capture of the match and composition of additional {@link Condition}s.
     */
    Matched<T, T> matches(Predicate<? super T> predicate);

    /**
     * Specifies one of many possible values to be matched, at the current position in an {@link Iterator},
     * using {@link Objects#equals(Object, Object)} for matching.
     *
     * @param value  one value
     * @param values the other possible values
     * @return a {@link Matched}, allowing capture of the match and composition of additional {@link Condition}s.
     */
    default Matched<T, T> matchesAnyOf(final Object value, final Object... values) {

        final Predicate<?> predicate = values == null || values.length == 0
            ? Predicates.isEqual(value, value)
            : Predicates.anyOf(
                Stream.concat(
                    Stream.of(Predicates.isEqual(value, value)),
                    Arrays.stream(values).map(v -> Predicates.isEqual(v, v))));

        return matches(predicate);
    }

    /**
     * Specifies that all of the specified values must not be matched, at the current position an {@link Iterator},
     * using {@link Predicate#isEqual(Object)} for matching.
     *
     * @param value  one value
     * @param values the other possible values
     * @return a {@link Matched}, allowing capture of the match and composition of additional {@link Condition}s.
     */
    default Matched<T, T> matchesNoneOf(final Object value, final Object... values) {
        final Predicate<?> predicate = values == null || values.length == 0
            ? Predicates.isEqual(value, value).negate()
            : Predicates.allOf(
                Stream.concat(
                    Stream.of(Predicates.isEqual(value, value).negate()),
                    Arrays.stream(values).map(v -> Predicates.isEqual(v, v).negate())));

        return matches(predicate);
    }

    /**
     * Specifies that a value must not be an instance of the specified {@link Class}, at the current position an
     * {@link Iterator}, using {@link Class#isInstance(Object)} for matching.
     *
     * @param classOfValue the {@link Class} of value
     * @return a {@link Matched}, allowing capture of the match and composition of additional {@link Condition}s.
     */
    @SuppressWarnings("unchecked")
    default Matched<T, T> matchesNoneOf(final Class<?> classOfValue) {
        return matchesNoneOf((Predicate<T>) Predicates.isInstance(classOfValue));
    }

    /**
     * Specifies that all of the specified values must not be matched, at the current position an {@link Iterator},
     * using a {@link Predicate} for matching.
     *
     * @param predicate the {@link Predicate}
     * @return a {@link Matched}, allowing capture of the match and composition of additional {@link Condition}s.
     */
    default Matched<T, T> matchesNoneOf(final Predicate<? super T> predicate) {
        return matches(predicate.negate());
    }

    /**
     * Specifies an {@link IteratorPatternMatcher} that must be matched at the current position in an {@link Iterator}.
     *
     * @param pattern the {@link IteratorPatternMatcher}
     * @return a {@link Composition}, allowing composition of additional {@link Condition}s.
     */
    Composition<T, T> satisfies(IteratorPatternMatcher<? super T> pattern);

    /**
     * Specifies that an {@link Iterator} must not produce any further values.
     *
     * @return an {@link IteratorPatternMatcher}
     */
    IteratorPatternMatcher<T> ends();

    /**
     * Specifies a number of values that must be skipped, commencing at the current position in an {@link Iterator}.
     *
     * @param count the {@link IteratorPatternMatcher}
     * @return a {@link Composition}, allowing composition of additional {@link Condition}s.
     */
    Composition<T, T> skip(int count);

    /**
     * Specifies a {@link Predicate} for skipping values (while {@code true}),
     * commencing at the current position in an {@link Iterator}.
     *
     * @param predicate the {@link IteratorPatternMatcher}
     * @return a {@link Composition}, allowing composition of additional {@link Condition}s.
     */
    Composition<T, T> skipWhile(Predicate<? super T> predicate);

    /**
     * Specifies a {@link Predicate} for skipping values (while {@code false}),
     * commencing at the current position in an {@link Iterator}.
     *
     * @param predicate the {@link IteratorPatternMatcher}
     * @return a {@link Composition}, allowing composition of additional {@link Condition}s.
     */
    default Composition<T, T> skipUntil(final Predicate<? super T> predicate) {
        return skipWhile(predicate == null
            ? Predicates.never()
            : descriptive(predicate.negate(), "not(" + predicate + ")"));
    }
}
