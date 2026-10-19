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

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * A {@link Composite} or <i>whole</i> {@link Object} is one which is composed of <i>parts</i>, for which
 * <i>traversal</i>, either directly or transitively, via other {@link Composite}s, is possible.
 * <p>
 * The concepts and terminology used here is based on those defined by the science of
 * <a href="https://en.wikipedia.org/wiki/Mereology">Mereology</a>.
 * <p>
 * <b>Cycle detection.</b> Transitive traversal ({@link Strategy#DepthFirst}, {@link Strategy#BreadthFirst}) tracks
 * visited {@link Composite}s by <em>identity</em> ({@code ==}), not by {@link Object#equals(Object)}.  This means two
 * structurally-equal but distinct {@link Composite} instances are treated as separate nodes and both are visited.
 * It also means that implementing {@link Object#equals(Object)} on a {@link Composite} to express structural equality
 * has no effect on cycle detection; only object identity determines whether a node has already been visited.
 *
 * @author brian.oliver
 * @since Sep-2025
 */
@FunctionalInterface
public interface Composite {

    /**
     * Obtains a {@link Stream} of the <i>direct</i> polymorphic <i>parts</i> of the {@link Composite}, excluding itself.
     *
     * @return a {@link Stream} of the <i>direct</i> polymorphic <i>parts</i> of the {@link Composite}
     */
    default Stream<Object> parts() {
        return traverse()
            .reflexive(false)
            .strategy(Strategy.Direct)
            .stream();
    }

    /**
     * Obtains a {@link Stream} of the <i>direct</i> <i>parts</i> of the {@link Composite} that are assignable to
     * the specified {@link Class}, excluding itself.
     *
     * @param type the {@link Class} of <i>part</i>
     * @param <T>  the type of <i>part</i>
     * @return a {@link Stream} of the <i>direct</i> <i>parts</i> of the {@link Composite} assignable to the specified
     * {@link Class}
     */
    default <T> Stream<T> parts(final Class<T> type) {
        return traverse(type)
            .reflexive(false)
            .strategy(Strategy.Direct)
            .stream();
    }

    /**
     * Obtains a {@link Stream} of the <i>composition</i> of the {@link Composite}, including all of its <i>parts</i>,
     * directly or transitively, using a {@link Strategy#DepthFirst} strategy, excluding itself.
     *
     * @return a {@link Stream} of the <i>composition</i> of the {@link Composite}
     */
    default Stream<Object> composition() {
        return traverse()
            .reflexive(false)
            .strategy(Strategy.DepthFirst)
            .stream();
    }

    /**
     * Obtains a {@link Stream} of the <i>composition</i> of the {@link Composite} that are assignable to
     * the specified {@link Class}, including all of its <i>parts</i>, directly or transitively, using a
     * {@link Strategy#DepthFirst} strategy, excluding itself.
     *
     * @param type the {@link Class} of <i>part</i>
     * @param <T>  the type of <i>part</i>
     * @return a {@link Stream} of the <i>composition</i> of the {@link Composite} assignable to the specified {@link Class}.
     */
    default <T> Stream<T> composition(final Class<T> type) {
        return traverse(type)
            .reflexive(false)
            .strategy(Strategy.DepthFirst)
            .stream();
    }

    /**
     * Obtains an {@link Iterator} of the <i>direct</i> <i>parts</i> of the {@link Composite} that are assignable to
     * the specified {@link Class}.  Should all types of <i>direct</i> <i>parts</i> be required, {@link Object}.class
     * should be specified.
     * <p>
     * <b>Implementor note.</b> During transitive traversal the framework calls both
     * {@code iterator(Composite.class)} and {@code iterator(elementClass)} and de-duplicates the results by identity
     * before visiting them.  Implementors therefore do <em>not</em> need to ensure that those two iterators are
     * disjoint: a part that is both a {@link Composite} and an instance of the element class (e.g. a type that
     * implements {@link Composite}) will be yielded exactly once regardless.
     *
     * @param <T> the type of the {@link Class}
     * @return an {@link Iterator} of the assignable <i>direct</i> <i>parts</i>
     */
    <T> Iterator<T> iterator(Class<T> type);

    /**
     * Creates a new polymorphic {@link Traversal} for the {@link Composite}.
     *
     * @return a new polymorphic {@link Traversal}
     */
    default Traversal<Object, ?> traverse() {
        return new Traversable<>(this, Object.class);
    }

    /**
     * Creates a new {@link Traversal} for the {@link Composite} for a specific type of part.
     *
     * @param <T>  the type of part
     * @param type the {@link Class} of part
     * @return a new {@link Traversal}
     */
    default <T> Traversal<T, ? extends Traversal<T, ?>> traverse(final Class<T> type) {
        return new Traversable<>(this, type);
    }

    /**
     * An empty {@link Composite}.
     */
    Composite EMPTY = new Composite() {
        @Override
        public <T> Iterator<T> iterator(final Class<T> type) {
            return Iterators.empty();
        }

        @Override
        public Stream<Object> parts() {
            return Stream.empty();
        }

        @Override
        public <T> Stream<T> parts(final Class<T> type) {
            return Stream.empty();
        }

        @Override
        public Stream<Object> composition() {
            return Stream.empty();
        }

        @Override
        public <T> Stream<T> composition(final Class<T> type) {
            return Stream.empty();
        }

        @Override
        public Traversal<Object, ?> traverse() {
            return Traversal.empty();
        }
    };

    /**
     * Obtains an empty {@link Composite}.
     *
     * @return an empty {@link Composite}
     */
    static Composite empty() {
        return EMPTY;
    }
}
