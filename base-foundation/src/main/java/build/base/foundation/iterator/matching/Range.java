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
 * A {@link Matcher} to match another {@link Matcher} between a specified number of times.
 *
 * @param <T> the type of elements evaluate
 * @param <C> the type of elements that were matched
 */
class Range<T, C>
    extends AbstractRepeated<T, C> {

    /**
     * The {@link Matcher} to match multiple times.
     */
    private final Matcher<T> repeat;

    /**
     * The minimum number of times to match.
     */
    private final int minimum;

    /**
     * The maximum number of times to match.
     */
    private final int maximum;

    Range(final AbstractMatcher<T> pattern,
          final int minimum,
          final int maximum) {

        super(pattern);

        this.repeat = pattern;

        if (minimum < 0) {
            throw new IllegalArgumentException("Minimum must be >= 0, it was " + minimum);
        }

        if (maximum < 0) {
            throw new IllegalArgumentException("Maximum must be >= 0, it was " + maximum);
        }

        this.minimum = Math.min(minimum, maximum);
        this.maximum = Math.max(minimum, maximum);
    }

    @Override
    public boolean match(final ResettableIterator<T> iterator)
        throws AbortMatchingException {

        int satisfied = 0;

        // obtain the starting position
        // (so that we can eventually collect the matched elements)
        final var starting = iterator.mark();
        ResettableIterator.Position ending = null;

        // obtain the current position
        // (so we can rollback to the most recent successful match)
        // (this allows "greedy" consumption for matching)
        var current = iterator.mark();

        while (satisfied < this.maximum && this.repeat.match(iterator)) {
            satisfied++;

            current = iterator.mark();

            // maintain the ending position
            // (so that we can eventually collect the matched elements)
            ending = iterator.mark();
        }

        // initialize the stages
        initialize();

        iterator.reset(current);

        if (satisfied < this.minimum && !iterator.hasNext()) {
            // when the minimum can't be satisfied and we've run out of values, we can abort
            throw new AbortMatchingException(this);
        }

        return satisfied >= this.minimum && satisfied <= this.maximum && hasStages()
            ? collect(iterator.range(starting, ending).iterator())
            : satisfied >= this.minimum && satisfied <= this.maximum;
    }

    @Override
    public String describe() {
        return this.repeat.describe() + (this.minimum == 0 && this.maximum == 1
            ? ".optionally()"
            : ".times(" + this.minimum + ", " + this.maximum + ")");
    }
}
