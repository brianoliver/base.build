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

import build.base.foundation.iterator.matching.Sequence;
import org.assertj.core.api.Assertions;

import java.util.Objects;

/**
 * An {@code assertj}-like assertion step wrapping a {@link Sequence}, allowing the fluent
 * definition of ordering constraints and terminal evaluation.
 *
 * @param <T> the type of elements
 * @author brian.oliver
 * @since Feb-2026
 */
public class IteratorPatternMatcherSequenceAssertion<T> {

    /**
     * The {@link Sequence} being built.
     */
    final Sequence<T> sequence;

    /**
     * The {@link IteratorPatternMatcherPredicate} used to test the pattern against the source.
     */
    final IteratorPatternMatcherPredicate<T> predicate;

    /**
     * Constructs an {@link IteratorPatternMatcherSequenceAssertion} for the specified
     * {@link Sequence} and {@link IteratorPatternMatcherPredicate}.
     *
     * @param sequence  the {@link Sequence}
     * @param predicate the {@link IteratorPatternMatcherPredicate}
     */
    IteratorPatternMatcherSequenceAssertion(final Sequence<T> sequence,
                                            final IteratorPatternMatcherPredicate<T> predicate) {
        this.sequence = Objects.requireNonNull(sequence, "The sequence must not be null");
        this.predicate = Objects.requireNonNull(predicate, "The predicate must not be null");
    }

    /**
     * Specifies that the next {@link build.base.foundation.iterator.matching.Term} must
     * <strong>immediately</strong> match the next element.
     *
     * @return an {@link IteratorPatternMatcherTermAssertion} for defining the next condition
     */
    public IteratorPatternMatcherTermAssertion<T> then() {
        return new IteratorPatternMatcherTermAssertion<>(this.sequence.then(), this.predicate);
    }

    /**
     * Specifies that the next {@link build.base.foundation.iterator.matching.Term} must
     * <strong>eventually</strong> match some element.
     *
     * @return an {@link IteratorPatternMatcherTermAssertion} for defining the next condition
     */
    public IteratorPatternMatcherTermAssertion<T> thenLater() {
        return new IteratorPatternMatcherTermAssertion<>(this.sequence.thenLater(), this.predicate);
    }

    /**
     * Asserts that the held {@link Sequence} matches the source.
     */
    public void isTrue() {
        Assertions.assertThat(this.predicate.test(this.sequence)).isTrue();
    }

    /**
     * Asserts that the held {@link Sequence} does not match the source.
     */
    public void isFalse() {
        Assertions.assertThat(this.predicate.test(this.sequence)).isFalse();
    }
}
