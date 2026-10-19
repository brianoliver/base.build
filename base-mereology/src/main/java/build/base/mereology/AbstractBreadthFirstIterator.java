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

import build.base.foundation.Capture;
import build.base.foundation.iterator.Iterators;
import build.base.foundation.predicate.Predicates;
import build.base.foundation.tuple.Triple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Base breadth-first {@link Iterator} over the parts of a {@link Composite}.
 * <p>
 * Each queue entry carries the current {@link Composite}, its remaining part {@link Iterator}, and the
 * ancestor hierarchy accumulated so far.  Subclasses decide how to wrap each matched element into the
 * return type {@code R} via {@link #wrapReflexive} and {@link #wrap}.  Cycle detection is handled here
 * via an identity-based visited set.
 *
 * @param <T> the element type matched against {@code elementClass}
 * @param <R> the type returned by the {@link Iterator}
 * @author reed.vonredwitz
 * @since May-2026
 */
abstract class AbstractBreadthFirstIterator<T, R>
    implements Iterator<R> {

    /**
     * The {@link Class} of element over which iteration is being performed.
     */
    protected final Class<T> elementClass;

    /**
     * Queue entries: (current composite, its part iterator, ancestor hierarchy above it).
     */
    private final Queue<Triple<Composite, Iterator<?>, List<Composite>>> queue;

    /**
     * The captured next element to return.
     */
    private final Capture<R> nextElement;

    /**
     * The {@link Predicate} to exclude {@link Composite}s during traversal.
     */
    private final Predicate<? super Composite> exclude;

    /**
     * Tracks visited {@link Composite}s by identity to break cycles.
     */
    private final Set<Composite> visited;

    AbstractBreadthFirstIterator(final Composite composite,
                                 final Class<T> elementClass,
                                 final boolean reflexive,
                                 final Predicate<? super Composite> exclude) {

        this.elementClass = elementClass;
        this.queue = new LinkedList<>();
        this.visited = Collections.newSetFromMap(new IdentityHashMap<>());

        this.exclude = exclude == null
            ? Predicates.never()
            : exclude;

        this.nextElement = reflexive && this.elementClass.isInstance(composite) && !this.exclude.test(composite)
            ? Capture.of(wrapReflexive(composite))
            : Capture.empty();

        if (!this.exclude.test(composite)) {
            this.visited.add(composite);
            this.queue.add(Triple.of(composite,
                Iterators.distinct(
                    Iterators.concat(composite.iterator(this.elementClass), composite.iterator(Composite.class))),
                List.of()));
        }
    }

    /**
     * Wraps the root {@link Composite} when {@code reflexive} is {@code true}.
     */
    protected abstract R wrapReflexive(Composite composite);

    /**
     * Wraps a matched element into the return type.
     *
     * @param element   the matched element
     * @param current   the {@link Composite} that directly contains {@code element}
     * @param hierarchy the ancestor composites above {@code current}, nearest-last
     */
    protected abstract R wrap(T element, Composite current, List<Composite> hierarchy);

    @Override
    @SuppressWarnings("unchecked")
    public boolean hasNext() {
        while (this.nextElement.isEmpty() && !this.queue.isEmpty()) {
            final var current = this.queue.peek();
            if (current.second().hasNext()) {
                final var next = current.second().next();

                final var exclude = next instanceof Composite composite
                    && this.exclude.test(composite);

                if (this.elementClass.isInstance(next) && !exclude) {
                    this.nextElement.set(wrap((T) next, current.first(), current.third()));
                }

                if (next instanceof Composite nextComposite && !exclude && this.visited.add(nextComposite)) {
                    final var iterator = Iterators.distinct(
                        Iterators.concat(
                            nextComposite.iterator(this.elementClass),
                            nextComposite.iterator(Composite.class)));

                    final var hierarchy = new ArrayList<>(current.third());
                    hierarchy.add(current.first());

                    this.queue.add(Triple.of(nextComposite, iterator, hierarchy));
                }
            } else {
                this.queue.remove();
            }
        }

        return this.nextElement.isPresent();
    }

    @Override
    public R next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more elements to iterate over");
        }

        return this.nextElement.consume();
    }
}
