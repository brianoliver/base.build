package build.base.mereology;

/*-
 * #%L
 * base.build Mereology
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

import build.base.foundation.tuple.Pair;

import java.util.Deque;
import java.util.Iterator;
import java.util.function.Predicate;

/**
 * A depth-first {@link Iterator} over the parts of a {@link Composite}, returning raw elements.
 *
 * @param <T> the type of elements returned
 * @author brian.oliver
 * @since Sep-2025
 */
class DepthFirstIterator<T>
    extends AbstractDepthFirstIterator<T, T> {

    DepthFirstIterator(final Composite composite,
                       final Class<T> elementClass,
                       final boolean reflexive,
                       final Predicate<? super Composite> exclude) {
        super(composite, elementClass, reflexive, exclude);
    }

    @Override
    protected T wrapReflexive(final Composite composite) {
        return this.elementClass.cast(composite);
    }

    @Override
    protected T wrap(final T element, final Deque<Pair<Composite, Iterator<?>>> stack) {
        return element;
    }
}
