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
import build.base.query.AbstractHeapBasedIndex;
import build.base.query.HeapBasedIndex;
import build.base.query.Scope;

import java.util.Iterator;
import java.util.Objects;

/**
 * A {@link HeapBasedIndex} which uses an underlying {@link Composite} to query and match for {@link Object}s for
 * {@link Class}es that have not been {@link #index(Object)}ed.
 *
 * @author brian.oliver
 * @since Sep-2025
 */
public class HeapBasedCompositeIndex extends AbstractHeapBasedIndex {

    /**
     * The {@link Composite} to traverse for unindexed {@link Object}s.
     */
    private final Composite composite;

    /**
     * Constructs an empty {@link HeapBasedCompositeIndex}, with the ability to query across a {@link Composite}.
     *
     * @param composite the {@link Composite}
     */
    public HeapBasedCompositeIndex(final Composite composite) {
        super();
        this.composite = Objects.requireNonNull(composite, "The Composite must not be null");
    }

    @Override
    protected <T> Iterator<T> traverse(final Class<T> matchableClass,
                                       final Scope scope) {

        return switch (scope) {
            case Indexed -> Iterators.empty();
            case Direct -> this.composite
                .traverse(matchableClass)
                .strategy(Strategy.Direct)
                .iterator();
            case DepthFirst -> this.composite
                .traverse(matchableClass)
                .strategy(Strategy.DepthFirst)
                .iterator();
            case BreadthFirst -> this.composite
                .traverse(matchableClass)
                .strategy(Strategy.BreadthFirst)
                .iterator();
        };
    }

    @Override
    protected Iterator<Object> traverse(final Scope scope) {
        return switch (scope) {
            case Indexed -> Iterators.empty();
            case Direct -> this.composite
                .traverse()
                .strategy(Strategy.Direct)
                .iterator();
            case DepthFirst -> this.composite
                .traverse()
                .strategy(Strategy.DepthFirst)
                .iterator();
            case BreadthFirst -> this.composite
                .traverse()
                .strategy(Strategy.BreadthFirst)
                .iterator();
        };
    }
}
