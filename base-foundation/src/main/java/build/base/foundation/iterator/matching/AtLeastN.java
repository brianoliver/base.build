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
 * A {@link Matcher} to consecutively match another {@link Matcher} at least a specified number of times.
 *
 * @param <T> the type of elements evaluate
 * @param <C> the type of elements that were matched
 */
class AtLeastN<T, C>
    extends AbstractRepeated<T, C> {

    /**
     * The {@link Matcher} to match multiple times.
     */
    private final Matcher<T> repeat;

    /**
     * The minimum number of times a pattern should be repeated.
     */
    private final int minimum;

    AtLeastN(final AbstractMatcher<T> pattern, final int count) {
        super(pattern);

        this.repeat = pattern;

        if (count < 0) {
            throw new IllegalArgumentException("Repeat count must be >= 0, it was " + count);
        }

        this.minimum = count;
    }

    @Override
    public boolean match(final ResettableIterator<T> iterator)
        throws AbortMatchingException {

        int satisfied = 0;

        // obtain the starting position
        // (so that we can eventually collect the matched elements)
        final ResettableIterator.Position starting = iterator.mark();
        ResettableIterator.Position ending = null;

        // maintain the current position
        // (so we can rollback to the most recent successful match)
        // (this allows "greedy" consumption for matching)
        ResettableIterator.Position current = iterator.mark();

        while (this.repeat.match(iterator)) {
            satisfied++;

            current = iterator.mark();

            // maintain the ending position
            // (so that we can eventually collect the matched elements)
            ending = iterator.mark();
        }

        // initialize the stages
        initialize();

        if (satisfied < this.minimum && !iterator.hasNext()) {
            // when the minimum can't be satisfied and we've run out of values, we can abort
            throw new AbortMatchingException(this);
        }

        iterator.reset(current);

        return satisfied >= this.minimum && hasStages()
            ? collect(iterator.range(starting, ending).iterator())
            : satisfied >= this.minimum;
    }

    @Override
    public String describe() {
        return this.repeat.describe() + (this.minimum == 0
            ? ".zeroOrMoreTimes()"
            : (this.minimum == 1
                ? ".oneOrMoreTimes()"
                : ".atLeast(" + this.minimum + ")"));
    }
}
