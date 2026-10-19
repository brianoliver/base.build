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

import java.util.Iterator;
import java.util.function.Predicate;

import static build.base.foundation.predicate.Predicates.always;

/**
 * A {@link Matcher} to skip over an number of elements in an {@link Iterator} while a
 * {@link Predicate} is satisfied.
 *
 * @param <T> the type of elements evaluate
 * @param <C> the type of elements that were matched
 */
class SkipWhile<T, C>
    extends AbstractComposition<T, C> {

    /**
     * The {@link Predicate} to be satisfied.
     */
    private final Predicate<? super T> predicate;

    SkipWhile(final AbstractMatcher<T> previous, final Predicate<? super T> predicate) {
        super(previous);

        this.predicate = predicate == null ? always() : predicate;
    }

    @Override
    public boolean match(final ResettableIterator<T> iterator) {

        // mark the current position so we can backtrack when the predicate fails
        ResettableIterator.Position position = iterator.mark();

        while (iterator.hasNext() && this.predicate.test(iterator.next())) {

            // mark the new position
            position = iterator.mark();
        }

        // reset to the last position
        iterator.reset(position);

        return true;
    }

    @Override
    public String describe() {
        return "skipWhile(" + this.predicate + ")";
    }
}
