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

import java.util.function.Predicate;

/**
 * The basis of a {@link Matcher} for an {@link build.base.foundation.iterator.matching.Term}.
 *
 * @param <T> the type of elements evaluate
 */
abstract class AbstractTerm<T>
    extends AbstractMatcher<T>
    implements Term<T> {

    AbstractTerm(final AbstractMatcher<T> previous) {
        super(previous);
    }

    AbstractTerm() {
        this(null);
    }

    @Override
    public Matched<T, T> matches(final Predicate<? super T> predicate) {
        return new Element<>(this, predicate);
    }

    @Override
    public Composition<T, T> satisfies(final IteratorPatternMatcher<? super T> predicate) {
        return new NestedIteratorPatternMatcher<>(this, predicate);
    }

    @Override
    public IteratorPatternMatcher<T> ends() {
        return new Ends<>(this);
    }

    @Override
    public Composition<T, T> skip(final int count) {
        return new SkipN<>(this, count);
    }

    @Override
    public Composition<T, T> skipWhile(final Predicate<? super T> predicate) {
        return new SkipWhile<>(this, predicate);
    }
}
