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

import build.base.foundation.Capture;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A @link Predicate} that attempts to match one or more {@link Condition}s against the values provided by
 * an {@link Iterator}.
 * <p>
 * {@link IteratorPatternMatcher}s consist of a sequence of one or more {@link Condition}s, each {@link Condition}
 * to be evaluated in the order of their definition against the values provided by an {@link Iterator}.
 * <p>
 * Example 1: The following pattern will be satisfied by a {@link String} {@link Iterator} that provides
 * "Barney" as the starting value, after which it is immediately followed by the value "Rubble".
 * <p>
 * {@code
 * IteratorPatternMatcher<String> pattern = IteratorPatternMatchers.<String>starts()
 * .then().matches("Barney")
 * .then().matches("Rubble");
 * }
 * <p>
 * Example 2: The following pattern will be satisfied by a {@link String} {@link Iterator} that at some point
 * provides the value "Barney", after which it is immediately followed by the value "Rubble".
 * <p>
 * {@code
 * IteratorPatternMatcher<String> pattern = IteratorPatternMatchers.<String>starts()
 * .thenLater().matches("Barney")
 * .then().matches("Rubble");
 * }
 * <p>
 * Example 3: The following pattern will be satisfied by a {@link String} {@link Iterator} that at some point
 * provides the value "Barney", then later, not necessarily immediately, is followed by the value "Rubble".
 * <p>
 * {@code
 * IteratorPatternMatcher<String> pattern = IteratorPatternMatchers.<String>starts()
 * .thenLater().matches("Barney")
 * .thenLater().matches("Rubble");
 * }
 * <p>
 * Example 4: The following pattern will be satisfied by a {@link String} {@link Iterator} that at some point provides
 * the value "Barney", then later is followed by the value "Rubble", after which the {@link Iterator} will no longer
 * provide any more values.  That is, "Rubble" is the last value.
 * <p>
 * {@code
 * IteratorPatternMatcher<String> pattern = IteratorPatternMatchers.<String>starts()
 * .thenLater().matches("Barney")
 * .thenLater().matches("Rubble")
 * .then().ends();
 * }
 * <p>
 * Example 5: The following pattern will be satisfied by a {@link String} {@link Iterator} that provides no values.
 * <p>
 * {@code
 * IteratorPatternMatcher<String> pattern = IteratorPatternMatchers.starts().then().ends();
 * }
 * <p>
 * The {@link Condition}s that comprise an {@link IteratorPatternMatcher} are categorized as follows:
 * <ol>
 *     <li>{@link Term}s provide the ability to define {@link Condition}s for evaluating zero or more values
 *         at some point in an iteration</li>
 *     <li>{@link Sequence}s provide the ability to define the ordering constraints between two or more
 *         {@link Condition}s</li>
 *     <li>{@link Repeatable}s provide the ability to define repetition of a {@link Condition}</li>
 *     <li>{@link Composition}s provide the ability to compose {@link Repeatable} and
 *         {@link Sequence} {@link Condition}s</li>
 * </ol>
 * <p>
 * Through the composition of these {@link Condition}s, {@link IteratorPatternMatcher}s may define complex sequences of
 * patterns to match a variety of non-trivial conditions and values produced by an {@link Iterator}.
 * <p>
 * Like regular expressions, often it is desirable to <i>capture</i> a matched value during pattern matching, that may
 * later be used for additional processing.  {@link Capture}s provide this capability.
 * <p>
 * Example 6: The following pattern will be satisfied by a {@link String} {@link Iterator} that at some point provides
 * the value "Barney", followed by some other {@link String}, which must be captured, followed by the value "Rubble".
 * <p>
 * {@code
 *     Capture<String> middleName = Capture.empty();
 *     IteratorPatternMatcher<String> pattern = IteratorPatternMatchers.<String>starts()
 *                                                       .thenLater().matches("Barney")
 *                                                       .then().matchesAny().capture(middleName)
 *                                                       .then().matches("Rubble");
 * }
 * <p>
 * With the exception of those using {@link Capture}s, {@link IteratorPatternMatcher}s are thread-safe by design,
 * meaning an instance of an {@link IteratorPatternMatcher} may be used for matching two or more different
 * {@link Iterator}s concurrently.  More specifically, an {@link IteratorPatternMatcher} using {@link Capture}s
 * <strong>must not be used</strong> by multiple threads as {@link Capture}s are not thread-safe.
 * <p>
 * {@link IteratorPatternMatcher}s will continue to consume the values provided by an {@link Iterator} until the
 * defined pattern sequence is matched, terminating as-soon-as-possible.   Consequently care should be taken when an
 * attempting to pattern match using {@link Iterator}s that produce an infinite number of values.
 * <p>
 * To create a {@link IteratorPatternMatcher}, applications typically use the helper methods provided by the
 * {@link IteratorPatternMatchers} class.  These methods provide the ability to create an initial
 * {@link IteratorPatternMatcher}, which may later be customized with additional {@link Condition}s.  This mechanism
 * provides a simple, fluent-style, builder-like approach for expressing complex pattern sequences, without requiring
 * additional builder or intermediate classes.
 *
 * @param <T> the type of elements provided by an {@link Iterator}
 * @author brian.oliver
 * @see IteratorPatternMatchers
 * @see Capture
 * @since Jun-2019
 */
public interface IteratorPatternMatcher<T>
    extends Predicate<Iterator<T>> {

    /**
     * Evaluates the <strong>ordered</strong> {@link Stream} of elements using the {@link IteratorPatternMatcher}.
     *
     * @param stream the ordered {@link Stream} of elements
     * @return {@code true} if the {@link Stream} of elements satisfies the {@link IteratorPatternMatcher},
     * {@code false} otherwise
     */
    default boolean test(final Stream<T> stream) {
        return test(stream == null ? Collections.emptyIterator() : stream.sequential().iterator());
    }

    /**
     * Evaluates the <strong>ordered</strong> elements produced by an {@link Iterable} using the
     * {@link IteratorPatternMatcher}.
     *
     * @param iterable the {@link Iterable}
     * @return {@code true} if the {@link Iterable} of elements satisfies the {@link IteratorPatternMatcher},
     * {@code false} otherwise
     */
    default boolean test(final Iterable<T> iterable) {
        return test(iterable == null ? Collections.emptyIterator() : iterable.iterator());
    }

}
