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

/**
 * Represents a pattern that may be proceeded by either a {@link Sequence} or {@link Repeatable} pattern.
 *
 * @param <T> the type of elements to be evaluated
 * @param <C> the type of the element that has been matched
 * @author brian.oliver
 * @since Jun-2019
 */
public interface Composition<T, C>
    extends IteratorPatternMatcher<T>, Sequence<T>, Repeatable<T, C> {

}
