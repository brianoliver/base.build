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

import build.base.foundation.iterator.ResettableIterator;

import java.util.stream.Stream;

/**
 * A {@link Matcher} to match another pattern later occurring later in a {@link Stream}.
 *
 * @param <T> the type of elements evaluate
 */
class ThenLater<T>
    extends AbstractTerm<T> {

    ThenLater() {
        super();
    }

    ThenLater(final AbstractMatcher<T> previous) {
        super(previous);
    }

    @Override
    public boolean match(final ResettableIterator<T> iterator) {
        // attempt to match the following pattern(s) somewhere in the iterator
        return next().search(iterator, false);
    }

    @Override
    public String describe() {
        return "thenLater()";
    }

    @Override
    public Matcher<T> step() {
        // as we've successfully matched the remainder of the matchers, we can terminate early
        return null;
    }
}
