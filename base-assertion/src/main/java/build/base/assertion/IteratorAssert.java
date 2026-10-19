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

import build.base.foundation.iterator.matching.IteratorPatternMatcher;
import build.base.foundation.iterator.matching.IteratorPatternMatchers;
import org.assertj.core.api.Assertions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An {@code assertj}-like Assertion Class for {@link IteratorPatternMatcher}s evaluated against
 * an immediately-available source of elements.
 * <p>
 * This class serves as both the root entry point (via the {@link #assertThat} factories and
 * {@link #starts()}) and as the terminal node reached after calling
 * {@link IteratorPatternMatcherTermAssertion#ends()}.
 * <p>
 * Example:
 * <pre><code>
 * PatternAssert.assertThat(List.of("Fred", "Barney", "Rubble"))
 *     .starts()
 *     .thenLater().matches("Barney")
 *     .then().matches("Rubble")
 *     .isTrue();
 * </code></pre>
 *
 * @param <T> the type of elements
 * @author brian.oliver
 * @since Feb-2026
 */
public class IteratorAssert<T> {

    /**
     * The {@link IteratorPatternMatcher} to evaluate; may be {@code null} at the root entry point.
     */
    private final IteratorPatternMatcher<T> pattern;

    /**
     * The {@link IteratorPatternMatcherPredicate} used to test the pattern against the source.
     */
    private final IteratorPatternMatcherPredicate<T> predicate;

    /**
     * Constructs a {@link IteratorAssert} for the specified {@link IteratorPatternMatcher} and
     * {@link IteratorPatternMatcherPredicate}.
     *
     * @param pattern   the {@link IteratorPatternMatcher}; may be {@code null} at the root
     * @param predicate the {@link IteratorPatternMatcherPredicate}
     */
    IteratorAssert(final IteratorPatternMatcher<T> pattern,
                   final IteratorPatternMatcherPredicate<T> predicate) {
        this.pattern = pattern;
        this.predicate = Objects.requireNonNull(predicate, "The predicate must not be null");
    }

    /**
     * Creates a {@link IteratorAssert} for the specified {@link Iterable}.
     *
     * @param <T>      the type of elements
     * @param iterable the {@link Iterable}
     * @return a new {@link IteratorAssert}
     */
    public static <T> IteratorAssert<T> assertThat(final Iterable<T> iterable) {
        return new IteratorAssert<>(null, IteratorPatternMatcherPredicate.of(iterable));
    }

    /**
     * Creates a {@link IteratorAssert} for the specified {@link Iterator}.
     * <p>
     * The {@link Iterator} is eagerly materialized into a {@link List} so that the source may be
     * evaluated more than once.
     *
     * @param <T>      the type of elements
     * @param iterator the {@link Iterator}
     * @return a new {@link IteratorAssert}
     */
    public static <T> IteratorAssert<T> assertThat(final Iterator<T> iterator) {
        final List<T> list = new ArrayList<>();
        iterator.forEachRemaining(list::add);
        return assertThat((Iterable<T>) list);
    }

    /**
     * Creates a {@link IteratorAssert} for the specified {@link Stream}.
     * <p>
     * The {@link Stream} is eagerly materialized into a {@link List} so that the source may be
     * evaluated more than once.
     *
     * @param <T>    the type of elements
     * @param stream the {@link Stream}
     * @return a new {@link IteratorAssert}
     */
    public static <T> IteratorAssert<T> assertThat(final Stream<T> stream) {
        final List<T> list = stream.collect(Collectors.toList());
        return assertThat((Iterable<T>) list);
    }

    /**
     * Starts an {@link IteratorPatternMatcher} pattern sequence at the beginning of the source.
     *
     * @return an {@link IteratorPatternMatcherSequenceAssertion} for the starting position
     */
    public IteratorPatternMatcherSequenceAssertion<T> starts() {
        return new IteratorPatternMatcherSequenceAssertion<>(IteratorPatternMatchers.starts(), this.predicate);
    }

    /**
     * Asserts that the held {@link IteratorPatternMatcher} matches the source.
     */
    public void isTrue() {
        Assertions.assertThat(this.predicate.test(this.pattern)).isTrue();
    }

    /**
     * Asserts that the held {@link IteratorPatternMatcher} does not match the source.
     */
    public void isFalse() {
        Assertions.assertThat(this.predicate.test(this.pattern)).isFalse();
    }
}
