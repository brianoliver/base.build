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
import build.base.foundation.predicate.Predicates;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Represents a collected element that may be captured.
 *
 * @param <T> the type of the {@link IteratorPatternMatcher} element
 * @param <C> the type of the element that was collected
 * @author brian.oliver
 * @since Jun-2019
 */
public interface Collected<T, C> {

    /**
     * Captures the collected element using the specified {@link Capture}.  Should the match not occur, the
     * {@link Capture} will be cleared.
     *
     * @param capture the {@link Capture}
     * @return a {@link CapturedCollected} allowing further value capture
     */
    CapturedCollected<T, C> capture(Capture<? super C> capture);

    /**
     * Attempts to match the collected element using the specified {@link Predicate}.
     *
     * @param predicate the {@link Predicate}
     * @return a {@link Sequence} allowing further matching of elements in an {@link Iterator}
     */
    Sequence<T> match(Predicate<? super C> predicate);

    /**
     * Specifies a value to be matched against the collected element using {@link Objects#equals(Object, Object)}
     * for matching.
     *
     * @param value the value
     * @return a {@link Sequence} allowing further matching of elements in an {@link Iterator}
     */
    default Sequence<T> match(final Object value) {
        return match(Predicates.isEqual(value, value));
    }
}
