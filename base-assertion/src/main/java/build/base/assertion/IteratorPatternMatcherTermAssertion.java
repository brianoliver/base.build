package build.base.assertion;

/*-
 * #%L
 * base.build Assertion
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

import build.base.foundation.iterator.matching.Term;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * An {@code assertj}-like assertion step wrapping a {@link Term}, allowing conditions to be
 * specified for the current position in the element sequence.
 * <p>
 * This step does not expose {@code isTrue()} or {@code isFalse()} because a {@link Term} alone is
 * not evaluable — a condition must first be applied via one of the {@code matches*} methods or
 * the iterator must be terminated via {@link #ends()}.
 *
 * @param <T> the type of elements
 * @author brian.oliver
 * @since Feb-2026
 */
public class IteratorPatternMatcherTermAssertion<T> {

    /**
     * The {@link Term} being built.
     */
    private final Term<T> term;

    /**
     * The {@link IteratorPatternMatcherPredicate} used to test the pattern against the source.
     */
    private final IteratorPatternMatcherPredicate<T> predicate;

    /**
     * Constructs an {@link IteratorPatternMatcherTermAssertion} for the specified {@link Term}
     * and {@link IteratorPatternMatcherPredicate}.
     *
     * @param term      the {@link Term}
     * @param predicate the {@link IteratorPatternMatcherPredicate}
     */
    IteratorPatternMatcherTermAssertion(final Term<T> term,
                                        final IteratorPatternMatcherPredicate<T> predicate) {
        this.term = Objects.requireNonNull(term, "The term must not be null");
        this.predicate = Objects.requireNonNull(predicate, "The predicate must not be null");
    }

    /**
     * Specifies a value to be matched at the current position using
     * {@link Objects#equals(Object, Object)}.
     *
     * @param value the value to match
     * @return an {@link IteratorPatternMatcherCompositionAssertion} for further composition
     */
    public IteratorPatternMatcherCompositionAssertion<T> matches(final Object value) {
        return new IteratorPatternMatcherCompositionAssertion<>(this.term.matches(value), this.predicate);
    }

    /**
     * Specifies a {@link Class} of value to be matched at the current position using
     * {@link Class#isInstance(Object)}.
     *
     * @param classOfValue the {@link Class} of value to match
     * @return an {@link IteratorPatternMatcherCompositionAssertion} for further composition
     */
    public IteratorPatternMatcherCompositionAssertion<T> matches(final Class<? extends T> classOfValue) {
        return new IteratorPatternMatcherCompositionAssertion<>(
            this.term.matches(classOfValue::isInstance),
            this.predicate);
    }

    /**
     * Specifies a {@link Predicate} to be evaluated at the current position.
     *
     * @param predicate the {@link Predicate}
     * @return an {@link IteratorPatternMatcherCompositionAssertion} for further composition
     */
    public IteratorPatternMatcherCompositionAssertion<T> matches(final Predicate<? super T> predicate) {
        return new IteratorPatternMatcherCompositionAssertion<>(this.term.matches(predicate), this.predicate);
    }

    /**
     * Specifies that any value matches at the current position.
     *
     * @return an {@link IteratorPatternMatcherCompositionAssertion} for further composition
     */
    public IteratorPatternMatcherCompositionAssertion<T> matchesAny() {
        return new IteratorPatternMatcherCompositionAssertion<>(this.term.matchesAny(), this.predicate);
    }

    /**
     * Specifies that the source must produce no further elements from this position.
     *
     * @return an {@link IteratorAssert} allowing {@code isTrue()} or {@code isFalse()}
     */
    public IteratorAssert<T> ends() {
        return new IteratorAssert<>(this.term.ends(), this.predicate);
    }
}
