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

import build.base.foundation.stream.Streamable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * A breadth-first {@link java.util.Iterator} over the parts of a {@link Composite}, returning {@link Entity}
 * wrappers that expose parthood hierarchy relationships.
 *
 * @param <T> the type of elements returned (wrapped in {@link Entity})
 * @author brian.oliver
 * @since Sep-2025
 */
class ParthoodBreadthFirstIterator<T>
    extends AbstractBreadthFirstIterator<T, Entity<T>> {

    ParthoodBreadthFirstIterator(final Composite composite,
                                 final Class<T> elementClass,
                                 final boolean reflexive,
                                 final Predicate<? super Composite> exclude) {
        super(composite, elementClass, reflexive, exclude);
    }

    @Override
    protected Entity<T> wrapReflexive(final Composite composite) {
        return Entity.boundary(this.elementClass.cast(composite));
    }

    @Override
    protected Entity<T> wrap(final T element, final Composite current, final List<Composite> hierarchy) {
        final var h = new ArrayList<>(hierarchy);
        h.add(current);
        return Entity.of(element, Streamable.reversed(h.stream()).stream());
    }
}
