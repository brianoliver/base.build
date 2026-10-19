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
 * A facility to specify a means to traverse a {@link Composite}, possibly reflexively including the {@link Composite}
 * itself, but also including its direct or indirect transitive <i>parts</i>.
 *
 * @author brian.oliver
 * @since Sep-2025
 */
public interface Traversal<T, B extends Traversal<T, B>>
    extends Iterable<T> {

    /**
     * Specifies whether the {@link Composite} on which the {@link Traversal} was defined should be included in the
     * traversal, by default excluding it.
     *
     * @param reflexive {@code true} if the {@link Composite} itself should be included, otherwise {@code false}
     * @return this {@link Traversal} to support fluent-style method invocation
     */
    B reflexive(boolean reflexive);

    /**
     * Specifies the {@link Strategy} for traversal, by default using {@link Strategy#Direct} only.
     *
     * @param strategy the traversal {@link Strategy}
     * @return this {@link Traversal} to support fluent-style method invocation
     */
    B strategy(Strategy strategy);

    /**
     * Specifies or replaces the previously specified {@link Predicate} to filter the <i>parts</i> included in the
     * traversal.
     *
     * @param predicate the {@link Predicate}
     * @return this {@link Traversal} to support fluent-style method invocation
     * @see #filter(Class)
     */
    B filter(Predicate<? super T> predicate);

    /**
     * Creates a new {@link Traversal} specifically for the specified {@link Class} of {@link Object}s.
     * <p>
     * This method must be called <em>before</em> {@link #filter(Predicate)} and {@link #abort(Predicate)}.
     * Because it replaces the element type entirely, any previously-set predicate or abort filter would be
     * silently invalidated, so an {@link IllegalStateException} is thrown if either has been set.
     * The correct order is: {@code filter(MyClass.class).strategy(...).filter(predicate).abort(predicate)}.
     *
     * @param <C>  the type of {@link Object}
     * @param <I>  the type of {@link Traversal}
     * @param type the {@link Class} of {@link Object}
     * @return a new {@link Traversal} for the specified {@link Class} of {@link Object}
     * @throws IllegalStateException if a {@link #filter(Predicate)} or {@link #abort(Predicate)} has already been set
     * @see #filter(Predicate)
     */
    <C, I extends Traversal<C, I>> I filter(Class<C> type);

    /**
     * Specifies or replaces the previously specified {@link Predicate} to exclude the {@link Composite}s to be
     * traversed.  When the specified {@link Predicate} is satisfied by a {@link Composite}, the {@link Composite}
     * and all of its <i>parts</i>, <p>direct or indirect</p> will be excluded from the traversal.
     *
     * @param predicate the {@link Composite} {@link Predicate}
     * @return this {@link Traversal} to support fluent-style method invocation
     */
    B exclude(Predicate<? super Composite> predicate);

    /**
     * Specifies or replaces the previously specified {@link Predicate} to abort the traversal when satisfied.
     * When the specified {@link Predicate} is satisfied by an element, the traversal will stop immediately,
     * without returning the element.
     *
     * @param predicate the {@link Predicate} that triggers abortion when {@code true}
     * @return this {@link Traversal} to support fluent-style method invocation
     */
    B abort(Predicate<? super T> predicate);

    /**
     * Obtains a {@link Stream} of the traversed {@link Object}s.
     *
     * @return a {@link Stream} of the traversed {@link Object}s
     */
    default Stream<T> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), 0), false);
    }

    /**
     * Obtains a facility to view the traversal of {@link Object}s as {@link Entity}s, allowing access to the
     * hierarchy of the {@link Object}s during traversal, with respect to the {@link Composite}s in which they
     * are defined.
     *
     * @return a mechanism to construct a {@link Hierarchical} traversal
     */
    Hierarchical<T> hierarchical();

    /**
     * An empty {@link Traversal}.
     */
    @SuppressWarnings("rawtypes")
    Traversal EMPTY = new Traversal() {
        @Override
        public Traversal reflexive(final boolean reflexive) {
            return this;
        }

        @Override
        public Traversal strategy(final Strategy strategy) {
            return this;
        }

        @Override
        public Traversal filter(final Predicate predicate) {
            return this;
        }

        @Override
        public Traversal filter(final Class type) {
            return this;
        }

        @Override
        public Traversal exclude(final Predicate predicate) {
            return this;
        }

        @Override
        public Traversal abort(final Predicate predicate) {
            return this;
        }

        @Override
        public Hierarchical hierarchical() {
            return Hierarchical.empty();
        }

        @Override
        public Iterator iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public Stream stream() {
            return Stream.empty();
        }
    };

    /**
     * Obtains an empty {@link Traversal}.
     *
     * @param <T> the type of elements being traversed
     * @return an empty {@link Traversal}
     */
    @SuppressWarnings("unchecked")
    static <T> Traversal<T, ?> empty() {
        return EMPTY;
    }
}
