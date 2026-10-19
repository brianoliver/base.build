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
import build.base.foundation.tuple.Pair;

import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Base depth-first {@link Iterator} over the parts of a {@link Composite}.
 * <p>
 * Subclasses decide how to wrap each matched element into the return type {@code R} via
 * {@link #wrapReflexive} and {@link #wrap}.  Cycle detection is handled here via an identity-based
 * visited set so that a {@link Composite} that appears as its own (transitive) part does not cause
 * infinite traversal.
 *
 * @param <T> the element type matched against {@code elementClass}
 * @param <R> the type returned by the {@link Iterator}
 * @author reed.vonredwitz
 * @since May-2026
 */
abstract class AbstractDepthFirstIterator<T, R>
    implements Iterator<R> {

    /**
     * The {@link Class} of element over which iteration is being performed.
     */
    protected final Class<T> elementClass;

    /**
     * The stack of {@link Composite}s and their direct part {@link Iterator}s remaining to be processed.
     */
    private final Deque<Pair<Composite, Iterator<?>>> stack;

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

    AbstractDepthFirstIterator(final Composite composite,
                               final Class<T> elementClass,
                               final boolean reflexive,
                               final Predicate<? super Composite> exclude) {

        this.elementClass = elementClass;
        this.stack = new LinkedList<>();
        this.visited = Collections.newSetFromMap(new IdentityHashMap<>());

        this.exclude = exclude == null
            ? Predicates.never()
            : exclude;

        this.nextElement = reflexive && this.elementClass.isInstance(composite) && !this.exclude.test(composite)
            ? Capture.of(wrapReflexive(composite))
            : Capture.empty();

        if (!this.exclude.test(composite)) {
            this.visited.add(composite);
            this.stack.push(Pair.of(composite,
                Iterators.distinct(
                    Iterators.concat(composite.iterator(Composite.class), composite.iterator(this.elementClass)))));
        }
    }

    /**
     * Wraps the root {@link Composite} when {@code reflexive} is {@code true}.
     */
    protected abstract R wrapReflexive(Composite composite);

    /**
     * Wraps a matched element into the return type, given the current traversal stack for hierarchy access.
     */
    protected abstract R wrap(T element, Deque<Pair<Composite, Iterator<?>>> stack);

    @Override
    @SuppressWarnings("unchecked")
    public boolean hasNext() {
        while (this.nextElement.isEmpty() && !this.stack.isEmpty()) {
            final var current = this.stack.peek();
            if (current.second().hasNext()) {
                final var next = current.second().next();

                final var exclude = next instanceof Composite composite
                    && this.exclude.test(composite);

                if (this.elementClass.isInstance(next) && !exclude) {
                    this.nextElement.set(wrap((T) next, this.stack));
                }

                if (next instanceof Composite composite && !exclude && this.visited.add(composite)) {
                    this.stack.push(Pair.of(composite,
                        Iterators.distinct(
                            Iterators.concat(composite.iterator(Composite.class), composite.iterator(this.elementClass)))));
                }
            } else {
                this.stack.pop();
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
