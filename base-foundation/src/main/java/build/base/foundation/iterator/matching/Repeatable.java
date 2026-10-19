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

import java.util.Iterator;

/**
 * Represents a {@link Condition} that may be qualified, typically allowing repetition constraints to be expressed.
 *
 * @param <T> the type of elements to be evaluated
 * @param <C> the type of the element that has been matched
 * @author brian.oliver
 * @since Jun-2019
 */
public interface Repeatable<T, C>
    extends IteratorPatternMatcher<T> {

    /**
     * Specifies that the current {@link Condition} must be matched specified number of times, commencing with the
     * values at the current position in an {@link Iterator}.
     *
     * @param count the number of times the {@link Condition} should be matched
     * @return a {@link Repeated}, allowing definition of an additional {@link Condition}s.
     */
    Repeated<T, C> times(int count);

    /**
     * Specifies that the current {@link Condition} must be matched at least the specified number of times,
     * commencing with the values at the current position in an {@link Iterator}.
     *
     * @param count the number of times the {@link Condition} should be matched
     * @return a {@link Repeated}, allowing definition of an additional {@link Condition}s.
     */
    Repeated<T, C> atLeast(int count);

    /**
     * Specifies that the current {@link Condition} must be matched zero or more times, commencing with the
     * values at the current position in an {@link Iterator}.
     *
     * @return a {@link Repeated}, allowing definition of an additional {@link Condition}s.
     */
    default Repeated<T, C> zeroOrMoreTimes() {
        return atLeast(0);
    }

    /**
     * Specifies that the current {@link Condition} must be matched one or more times, commencing with the
     * values at the current position in an {@link Iterator}.
     *
     * @return a {@link Repeated}, allowing definition of an additional {@link Condition}s.
     */
    default Repeated<T, C> oneOrMoreTimes() {
        return atLeast(1);
    }

    /**
     * Specifies that the current {@link Condition} must be matched the specified number of times,
     * commencing with the values at the current position in an {@link Iterator}.
     *
     * @param minimum the minimum number of times the {@link Condition} should be matched
     * @param maximum the maximum number of times the {@link Condition} should be matched
     * @return a {@link Repeated}, allowing definition of an additional {@link Condition}s.
     */
    Repeated<T, C> times(int minimum, int maximum);

    /**
     * Specifies that the current {@link Condition} may optionally be matched at the current position in an
     * {@link Iterator}.
     *
     * @return a {@link Repeated}, allowing definition of an additional {@link Condition}s.
     */
    default Repeated<T, C> optionally() {
        return times(0, 1);
    }
}
