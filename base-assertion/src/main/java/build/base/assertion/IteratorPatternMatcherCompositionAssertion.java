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

import build.base.foundation.iterator.matching.Composition;

/**
 * An {@code assertj}-like assertion step wrapping a {@link Composition}, allowing the current
 * matched condition to be followed by a {@link build.base.foundation.iterator.matching.Sequence}
 * or qualified with repetition constraints.
 * <p>
 * Inherits {@link #then()}, {@link #thenLater()}, {@link #isTrue()}, and {@link #isFalse()} from
 * {@link IteratorPatternMatcherSequenceAssertion} since {@link Composition} IS a
 * {@link build.base.foundation.iterator.matching.Sequence}.
 *
 * @param <T> the type of elements
 * @author brian.oliver
 * @since Feb-2026
 */
public class IteratorPatternMatcherCompositionAssertion<T>
    extends IteratorPatternMatcherSequenceAssertion<T> {

    /**
     * The {@link Composition} being built.
     */
    @SuppressWarnings("unchecked")
    private final Composition<T, T> composition;

    /**
     * Constructs an {@link IteratorPatternMatcherCompositionAssertion} for the specified
     * {@link Composition} and {@link IteratorPatternMatcherPredicate}.
     *
     * @param composition the {@link Composition}
     * @param predicate   the {@link IteratorPatternMatcherPredicate}
     */
    @SuppressWarnings("unchecked")
    IteratorPatternMatcherCompositionAssertion(final Composition<T, ?> composition,
                                               final IteratorPatternMatcherPredicate<T> predicate) {
        super(composition, predicate);
        this.composition = (Composition<T, T>) composition;
    }

    /**
     * Specifies that the current condition must be matched exactly the specified number of times.
     *
     * @param count the number of times to match
     * @return an {@link IteratorPatternMatcherSequenceAssertion} for further composition
     */
    public IteratorPatternMatcherSequenceAssertion<T> times(final int count) {
        return new IteratorPatternMatcherSequenceAssertion<>(this.composition.times(count), this.predicate);
    }

    /**
     * Specifies that the current condition must be matched at least the specified number of times.
     *
     * @param count the minimum number of times to match
     * @return an {@link IteratorPatternMatcherSequenceAssertion} for further composition
     */
    public IteratorPatternMatcherSequenceAssertion<T> atLeast(final int count) {
        return new IteratorPatternMatcherSequenceAssertion<>(this.composition.atLeast(count), this.predicate);
    }

    /**
     * Specifies that the current condition must be matched zero or more times.
     *
     * @return an {@link IteratorPatternMatcherSequenceAssertion} for further composition
     */
    public IteratorPatternMatcherSequenceAssertion<T> zeroOrMoreTimes() {
        return new IteratorPatternMatcherSequenceAssertion<>(this.composition.zeroOrMoreTimes(), this.predicate);
    }

    /**
     * Specifies that the current condition must be matched one or more times.
     *
     * @return an {@link IteratorPatternMatcherSequenceAssertion} for further composition
     */
    public IteratorPatternMatcherSequenceAssertion<T> oneOrMoreTimes() {
        return new IteratorPatternMatcherSequenceAssertion<>(this.composition.oneOrMoreTimes(), this.predicate);
    }

    /**
     * Specifies that the current condition may optionally be matched.
     *
     * @return an {@link IteratorPatternMatcherSequenceAssertion} for further composition
     */
    public IteratorPatternMatcherSequenceAssertion<T> optionally() {
        return new IteratorPatternMatcherSequenceAssertion<>(this.composition.optionally(), this.predicate);
    }
}
