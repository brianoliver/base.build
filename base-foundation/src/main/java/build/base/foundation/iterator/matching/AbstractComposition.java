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
 * The basis of a {@link Matcher} for an {@link build.base.foundation.iterator.matching.Composition}.
 *
 * @param <T> the type of elements evaluate
 * @param <C> the type of elements that were matched
 * @author brian.oliver
 * @since Jun-2019
 */
abstract class AbstractComposition<T, C>
    extends AbstractSequence<T>
    implements Composition<T, C> {

    /**
     * Constructs an {@link AbstractComposition} after the specified "previous" {@link AbstractMatcher}.
     *
     * @param previous the previous {@link AbstractMatcher}
     */
    AbstractComposition(final AbstractMatcher<T> previous) {
        super(previous);
    }

    @Override
    public Term<T> then() {
        return new Then<>(this);
    }

    @Override
    public Term<T> thenLater() {
        return new ThenLater<>(this);
    }

    @Override
    public Repeated<T, C> times(final int count) {
        return new TimesN<>(this, count);
    }

    @Override
    public Repeated<T, C> atLeast(final int count) {
        return new AtLeastN<>(this, count);
    }

    @Override
    public Repeated<T, C> times(final int minimum, final int maximum) {
        return new Range<>(this, minimum, maximum);
    }
}
