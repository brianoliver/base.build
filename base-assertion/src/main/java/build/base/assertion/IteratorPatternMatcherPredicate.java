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

import java.util.function.Predicate;

/**
 * A {@link Predicate} that tests whether an {@link IteratorPatternMatcher} matches a source of
 * elements, supporting both immediate and retrying assertion forms.
 *
 * @param <T> the type of elements
 * @author brian.oliver
 * @since Feb-2026
 */
interface IteratorPatternMatcherPredicate<T>
    extends Predicate<IteratorPatternMatcher<T>> {

    /**
     * Creates an immediate {@link IteratorPatternMatcherPredicate} for the specified {@link Iterable}.
     *
     * @param <T>      the type of elements
     * @param iterable the {@link Iterable} source
     * @return a new {@link IteratorPatternMatcherPredicate}
     */
    static <T> IteratorPatternMatcherPredicate<T> of(final Iterable<T> iterable) {
        return pattern -> pattern.test(iterable);
    }
}
