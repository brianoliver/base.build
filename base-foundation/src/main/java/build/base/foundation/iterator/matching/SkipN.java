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
 * A {@link Matcher} to skip a specific number of elements in a {@link Stream}.
 *
 * @param <T> the type of elements evaluate
 * @param <C> the type of elements that were matched
 */
class SkipN<T, C>
    extends AbstractComposition<T, C> {

    /**
     * The number of elements to skip.
     */
    private final int count;

    SkipN(final AbstractMatcher<T> previous, final int count) {
        super(previous);

        if (count < 0) {
            throw new IllegalArgumentException("SkipWhile count must be >=0, it was " + count);
        }

        this.count = count;
    }

    @Override
    public boolean match(final ResettableIterator<T> iterator)
        throws AbortMatchingException {

        var n = this.count;
        while (n > 0 && iterator.hasNext()) {
            iterator.next();
            n--;
        }

        if (n > 0) {
            // when we can't skip the required number, we've run out of values, so we can abort
            throw new AbortMatchingException(this);
        }

        return true;
    }

    @Override
    public String describe() {
        return "skip(" + this.count + ")";
    }
}
