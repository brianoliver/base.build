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

/**
 * Represents an element that was captured using a {@link Capture}, which may be further captured or followed
 * by another {@link Sequence} of elements to match.
 *
 * @param <T> the type of the {@link IteratorPatternMatcher} element
 * @param <C> the type of the element that has been matched
 * @author brian.oliver
 * @since Jun-2019
 */
public interface Captured<T, C>
    extends Sequence<T>, Capturable<T, C> {

}
