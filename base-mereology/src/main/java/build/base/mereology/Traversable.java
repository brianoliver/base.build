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

import build.base.foundation.iterator.Iterators;
import build.base.foundation.predicate.Predicates;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * An internal {@link Traversal} implementation for {@link Composite}s.
 *
 * @param <T> the type of {@link Object}s to be returned as {@link Iterator} elements
 * @author brian.oliver
 * @since Sep-2025
 */
class Traversable<T>
    implements Traversal<T, Traversable<T>> {

    /**
     * The {@link Composite} to traverse.
     */
    private final Composite composite;

    /**
     * The {@link Class} of {@link Object}s to be returned as {@link Iterator} elements.
     */
    private final Class<T> elementClass;

    /**
     * Indicates if the {@link Composite} itself should be included in the traversal.
     */
    private boolean reflexive;

    /**
     * The {@link Strategy} to use for the traversal.
     */
    private Strategy strategy;

    /**
     * The {@link Predicate} to filter elements during traversal.
     */
    private Predicate<? super T> filter;

    /**
     * The {@link Predicate} to exclude {@link Composite}s during traversal.
     */
    private Predicate<? super Composite> exclude;

    /**
     * The {@link Predicate} to abort traversal when satisfied.
     */
    private Predicate<? super T> abort;

    /**
     * Constructs a {@link Traversable}.
     *
     * @param composite    the {@link Composite} to traverse
     * @param elementClass the {@link Class} of {@link Object}s to be returned as {@link Iterator} elements
     */
    Traversable(final Composite composite,
                final Class<T> elementClass) {

        this.composite = Objects.requireNonNull(composite, "The Composite to traverse must not be null");
        this.elementClass = Objects.requireNonNull(elementClass, "The Element Class must not be null");

        this.reflexive = false;
        this.strategy = Strategy.Direct;
        this.filter = Predicates.always();
        this.exclude = Predicates.never();
        this.abort = Predicates.never();
    }

    @Override
    public Traversable<T> reflexive(final boolean reflexive) {
        this.reflexive = reflexive;
        return this;
    }

    @Override
    public Traversable<T> strategy(final Strategy strategy) {
        this.strategy = strategy;
        return this;
    }

    @Override
    public Traversable<T> filter(final Predicate<? super T> predicate) {
        this.filter = predicate == null
            ? Predicates.always()
            : predicate;

        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C, I extends Traversal<C, I>> I filter(final Class<C> type) {
        if (this.filter != Predicates.always()) {
            throw new IllegalStateException(
                "filter(Class) cannot be called after filter(Predicate) — the predicate would be silently dropped; " +
                "call filter(Class) first, then filter(Predicate)");
        }
        if (this.abort != Predicates.never()) {
            throw new IllegalStateException(
                "filter(Class) cannot be called after abort(Predicate) — the abort predicate would be silently dropped; " +
                "call filter(Class) first, then abort(Predicate)");
        }
        return (I) new Traversable<>(this.composite, type)
            .reflexive(this.reflexive)
            .strategy(this.strategy)
            .exclude(this.exclude);
    }

    @Override
    public Traversable<T> exclude(final Predicate<? super Composite> predicate) {
        this.exclude = predicate == null
            ? Predicates.never()
            : predicate;
        return this;
    }

    @Override
    public Traversable<T> abort(final Predicate<? super T> predicate) {
        this.abort = predicate == null
            ? Predicates.never()
            : predicate;
        return this;
    }

    @Override
    public Hierarchical<T> hierarchical() {
        return switch (this.strategy) {
            case Direct -> {
                final Predicate<T> include = element ->
                    ((element instanceof Composite c && !this.exclude.test(c)) || (!(element instanceof Composite)))
                        && this.filter.test(element);

                final boolean excludeRoot = this.exclude.test(this.composite);
                final boolean includeReflexive = !excludeRoot
                    && this.reflexive
                    && this.elementClass.isInstance(this.composite);

                yield new ParthoodTraversal<>(() -> {
                    if (excludeRoot) {
                        return Iterators.empty();
                    }
                    final var partIterator = Iterators.map(
                        Iterators.filter(Traversable.this.composite.iterator(Traversable.this.elementClass), include),
                        object -> Entity.of(object, Traversable.this.composite));
                    return includeReflexive
                        ? Iterators.of(
                            Iterators.of(Entity.boundary(Traversable.this.elementClass.cast(Traversable.this.composite))),
                            partIterator)
                        : partIterator;
                });
            }

            case DepthFirst -> new ParthoodTraversal<>(
                this.filter == Predicates.always()
                    ? () -> new ParthoodDepthFirstIterator<>(this.composite, this.elementClass, this.reflexive, this.exclude)
                    : () -> Iterators.filter(
                    new ParthoodDepthFirstIterator<>(this.composite, this.elementClass, this.reflexive, this.exclude),
                    parthood -> this.filter.test(parthood.object())));

            case BreadthFirst -> new ParthoodTraversal<>(
                this.filter == Predicates.always()
                    ? () -> new ParthoodBreadthFirstIterator<>(this.composite, this.elementClass, this.reflexive, this.exclude)
                    : () -> Iterators.filter(
                    new ParthoodBreadthFirstIterator<>(this.composite, this.elementClass, this.reflexive, this.exclude),
                    parthood -> this.filter.test(parthood.object())));
        };
    }

    @Override
    public Iterator<T> iterator() {
        final Iterator<T> baseIterator = switch (this.strategy) {
            case Direct -> {
                final Predicate<T> include = element ->
                    ((element instanceof Composite c && !this.exclude.test(c)) || (!(element instanceof Composite)))
                        && this.filter.test(element);

                // establish the direct part iterator
                final var iterator = this.exclude.test(this.composite)
                    ? Iterators.<T>empty()
                    : Iterators.filter(this.composite.iterator(this.elementClass), include);

                // include the composite if reflexive
                yield this.reflexive && this.elementClass.isInstance(this.composite) && !this.exclude.test(this.composite)
                    ? Iterators.of(Iterators.of(this.elementClass.cast(this.composite)), iterator)
                    : iterator;
            }

            case DepthFirst -> this.filter == Predicates.always()
                ? new DepthFirstIterator<>(this.composite, this.elementClass, this.reflexive, this.exclude)
                : Iterators.filter(new DepthFirstIterator<>(this.composite, this.elementClass, this.reflexive, this.exclude),
                this.filter);

            case BreadthFirst -> this.filter == Predicates.always()
                ? new BreadthFirstIterator<>(this.composite, this.elementClass, this.reflexive, this.exclude)
                : Iterators.filter(new BreadthFirstIterator<>(this.composite, this.elementClass, this.reflexive, this.exclude),
                this.filter);
        };

        return Iterators.abortable(baseIterator, this.abort);
    }

    /**
     * An internal {@link Hierarchical} implementation.
     */
    private static class ParthoodTraversal<T> implements Hierarchical<T> {
        /**
         * The {@link Supplier} of the {@link Iterator} of {@link Entity}s.
         */
        private final Supplier<Iterator<Entity<T>>> supplier;

        /**
         * The {@link Predicate} to abort the traversal when satisfied.
         */
        private Predicate<? super Entity<T>> abort;

        /**
         * Constructs a {@link ParthoodTraversal}.
         *
         * @param supplier the {@link Supplier} of the {@link Iterator} of {@link Entity}s
         */
        ParthoodTraversal(final Supplier<Iterator<Entity<T>>> supplier) {
            this.supplier = supplier;
            this.abort = Predicates.never();
        }

        @Override
        public Iterator<Entity<T>> iterator() {
            final var baseIterator = this.supplier.get();
            return Iterators.abortable(baseIterator, this.abort);
        }

        @Override
        public Hierarchical<T> abort(final Predicate<? super Entity<T>> predicate) {
            this.abort = predicate == null
                ? Predicates.never()
                : predicate;

            return this;
        }
    }
}
