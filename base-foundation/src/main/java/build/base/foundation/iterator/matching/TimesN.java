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

/**
 * A {@link Matcher} to match another {@link Condition} a specified number of times.
 *
 * @param <T> the type of elements evaluate
 * @param <C> the type of elements that were matched
 */
class TimesN<T, C>
    extends AbstractRepeated<T, C> {

    /**
     * The {@link Matcher} to match multiple times.
     */
    private final Matcher<T> repeat;

    /**
     * The number of required matches.
     */
    private final int count;

    TimesN(final AbstractMatcher<T> pattern, final int count) {
        super(pattern);

        this.repeat = pattern;

        if (count < 0) {
            throw new IllegalArgumentException("Repeat count must be >= 0, it was " + count);
        }

        this.count = count;
    }

    @Override
    public boolean match(final ResettableIterator<T> iterator)
        throws AbortMatchingException {

        int remaining = this.count;

        // obtain the starting position
        // (so that we can eventually collect the matched elements)
        final ResettableIterator.Position starting = iterator.mark();
        ResettableIterator.Position ending = null;

        while (remaining > 0 && this.repeat.match(iterator)) {
            remaining--;

            // maintain the ending position
            // (so that we can eventually collect the matched elements)
            ending = iterator.mark();
        }

        // initialize the stages
        initialize();

        if (remaining > 0 && !iterator.hasNext()) {
            // when can't match and we've run out of values, we can abort
            throw new AbortMatchingException(this);
        }

        return remaining == 0 && hasStages()
            ? collect(iterator.range(starting, ending).iterator())
            : remaining == 0;
    }

    @Override
    public String describe() {
        return this.repeat.describe() + ".times(" + this.count + ")";
    }
}
