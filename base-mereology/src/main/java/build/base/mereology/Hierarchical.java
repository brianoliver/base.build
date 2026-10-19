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

import java.util.Collections;
import java.util.Iterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A facility to traverse {@link Object}s as {@link Entity}s, allowing access to the hierarchy in which they are defined
 * during traversal.
 *
 * @param <T> the type of {@link Object} being traversed
 * @author brian.oliver
 * @see Composite
 * @since Sep-2025
 */
public interface Hierarchical<T> extends Iterable<Entity<T>> {

    /**
     * Obtains a {@link Stream} of the {@link Entity} {@link Object}s, allowing direct access to the
     * hierarchical relationship of the {@link Object}s.
     *
     * @return a {@link Stream} of {@link Entity}
     */
    default Stream<Entity<T>> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), 0), false);
    }

    /**
     * Specifies or replaces the previously specified {@link Predicate} to abort the hierarchical traversal
     * when satisfied by a {@link Entity}.
     *
     * @param predicate the {@link Predicate} that triggers abortion when {@code true}
     * @return this {@link Hierarchical} to support fluent-style method invocation
     */
    Hierarchical<T> abort(Predicate<? super Entity<T>> predicate);

    /**
     * An empty {@link Hierarchical}.
     */
    @SuppressWarnings("rawtypes")
    Hierarchical EMPTY = new Hierarchical() {
        @Override
        public Stream<Entity> stream() {
            return Stream.empty();
        }

        @Override
        public Iterator<Entity<?>> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public Hierarchical abort(final Predicate predicate) {
            return this;
        }
    };

    /**
     * Obtains an empty {@link Hierarchical}.
     *
     * @param <T> the type of elements being traversed
     * @return an empty {@link Hierarchical}
     */
    @SuppressWarnings("unchecked")
    static <T> Hierarchical<T> empty() {
        return EMPTY;
    }
}
