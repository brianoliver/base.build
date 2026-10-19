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
 * Represents the relationship between two {@link Condition}s in a sequence.
 *
 * @param <T> the type of elements to be evaluated
 * @author brian.oliver
 * @since Jun-2019
 */
public interface Sequence<T>
    extends IteratorPatternMatcher<T> {

    /**
     * Specifies that the next {@link Term} defined in the {@link IteratorPatternMatcher} must <strong>immediately</strong>
     * match the next value produced by an {@link Iterator}.
     *
     * @return the next {@link Term} to define
     */
    Term<T> then();

    /**
     * Specifies that the next {@link Term} defined in the {@link IteratorPatternMatcher} must <strong>eventually</strong>
     * match the a value produced by an {@link Iterator}.
     *
     * @return the next {@link Term} to define
     */
    Term<T> thenLater();
}
