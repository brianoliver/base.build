package build.base.foundation.iterator;

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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Static functions for working with {@link Iterator}.
 *
 * @author brian.oliver
 * @since May-2025
 */
public final class Iterators {

    /**
     * An empty {@link Iterator}.
     */
    private static final Iterator<Object> EMPTY = new Iterator<>() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException("Attempt to get next element from empty iterator");
        }
    };

    /**
     * Private Constructor.
     */
    private Iterators() {
        // prevent instantiation
    }

    /**
     * Returns an empty {@link Iterator}.
     *
     * @param <T> the type of element
     * @return an empty {@link Iterator}
     */
    @SuppressWarnings("unchecked")
    public static <T> Iterator<T> empty() {
        return (Iterator<T>) EMPTY;
    }

    /**
     * Creates an {@link Iterator} from the given elements.
     *
     * @param elements the elements
     * @param <T>      the type of elements
     * @return an {@link Iterator} over the specified elements
     */
    @SafeVarargs
    public static <T> Iterator<T> of(final T... elements) {

        if (elements == null || elements.length == 0) {
            return Iterators.empty();
        }

        return new Iterator<T>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < elements.length;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("No more elements in iterator");
                }
                return elements[index++];
            }
        };
    }

    /**
     * Creates an {@link Iterator} from the given {@link Optional}.
     *
     * @param <T>      the type of {@link Optional}
     * @param optional the {@link Optional}
     * @return an {@link Iterator}
     */
    public static <T> Iterator<T> of(final Optional<T> optional) {
        return (optional == null || optional.isEmpty())
            ? Iterators.empty()
            : Iterators.ofNullable(optional.get());
    }

    /**
     * Creates an {@link Iterator} from the given {@code null}able value.
     *
     * @param <T>   the type of value
     * @param value the {@code null}able value
     * @return an {@link Iterator}
     */
    public static <T> Iterator<T> ofNullable(final T value) {

        return new Iterator<>() {
            private boolean hasNext = true;

            @Override
            public boolean hasNext() {
                return this.hasNext;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("No more elements in iterator");
                }
                this.hasNext = false;
                return value;
            }
        };
    }

    /**
     * Creates an {@link Iterator} from the given {@link Iterator}s.
     *
     * @param iterators the {@link Iterator}s
     * @param <T>       the type of element
     * @return an {@link Iterator} over the specified {@link Iterator}s
     */
    @SafeVarargs
    public static <T> Iterator<T> of(final Iterator<T>... iterators) {

        return iterators == null || iterators.length == 0
            ? Iterators.empty()
            : new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                while (this.index < iterators.length) {
                    if (iterators[this.index].hasNext()) {
                        return true;
                    }
                    this.index++;
                }
                return false;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("No more elements in iterator");
                }
                return iterators[this.index].next();
            }
        };
    }

    /**
     * Concatenates zero or more {@link Iterator}s of a specified type.
     *
     * @param requiredClass the required {@link Class}
     * @param iterators     the {@link Iterator}s to concatenate
     * @param <T>           the type of element
     * @return an {@link Iterator} over all elements from the specified {@link Iterator}s
     */
    @SuppressWarnings("unchecked")
    public static <T> Iterator<T> concat(final Class<T> requiredClass,
                                         final Iterator<? extends T>... iterators) {

        return (Iterator<T>) concat(iterators);
    }

    /**
     * Concatenates zero or more {@link Iterator}s of an unknown type.
     *
     * @param iterators the {@link Iterator}s to concatenate
     * @return an {@link Iterator} over all elements from the specified {@link Iterator}s
     */
    public static Iterator<?> concat(final Iterator<?>... iterators) {

        return iterators == null || iterators.length == 0
            ? Iterators.empty()
            : new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                while (this.index < iterators.length) {
                    if (iterators[this.index] != null && iterators[this.index].hasNext()) {
                        return true;
                    }
                    this.index++;
                }
                return false;
            }

            @Override
            public Object next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("No more elements in iterator");
                }
                return iterators[this.index].next();
            }
        };
    }

    /**
     * Creates an {@link Iterator} that returns distinct non-{@code null} elements from the specified {@link Iterator}.
     *
     * @param <T>      the type of element
     * @param iterator the {@link Iterator}
     * @return a distinct {@link Iterator}
     * @see DistinctIterator
     */
    public static <T> Iterator<T> distinct(final Iterator<T> iterator) {
        return DistinctIterator.of(iterator);
    }

    /**
     * Creates an {@link Iterator} that flattens the specified {@link Iterator} of {@link Iterator}s into a single
     * {@link Iterator} of elements.
     *
     * @param <T>      the type of elements
     * @param iterator the {@link Iterator} of {@link Iterator}s
     * @return an {@link Iterator} of elements
     * @see FlatteningIterator
     */
    public static <T> Iterator<T> flatten(final Iterator<Iterator<T>> iterator) {
        return FlatteningIterator.of(iterator);
    }

    /**
     * Creates a {@link MappingIterator} from the specified {@link Iterator} and {@link Function}.
     *
     * @param iterator the {@link Iterator}
     * @param function the {@link Function}
     */
    public static <T, R> Iterator<R> map(final Iterator<T> iterator,
                                         final Function<T, R> function) {

        return MappingIterator.of(iterator, function);
    }

    /**
     * Creates a {@link FilteringIterator} given an {@link Iterator} and {@link Predicate}.
     *
     * @param iterator  the {@link Iterator}
     * @param predicate the {@link Predicate}
     */
    public static <T> Iterator<T> filter(final Iterator<T> iterator,
                                         final Predicate<? super T> predicate) {

        return FilteringIterator.of(iterator, predicate);
    }

    /**
     * Creates an {@link Iterator} based on the specified {@link Iterator} that aborts iteration when the specified
     * {@link Predicate} is satisfied.
     *
     * @param <T>       the type of element
     * @param iterator  the {@link Iterator}
     * @param predicate the {@link Predicate} that triggers immediate termination when {@code true}
     * @return an {@link Iterator}
     * @see AbortableIterator
     */
    public static <T> Iterator<T> abortable(final Iterator<T> iterator,
                                            final Predicate<? super T> predicate) {

        return AbortableIterator.of(iterator, predicate);
    }

    /**
     * Creates an {@link Iterator} based on the specified {@link Iterator} that returns only elements that are
     * instances of the specified {@link Class}.
     *
     * @param <T>           the type of element
     * @param iterator      the {@link Iterator}
     * @param requiredClass the required {@link Class}
     * @return an {@link Iterator}
     * @see InstanceOfIterator
     */
    public static <T> Iterator<T> isInstanceOf(final Iterator<?> iterator,
                                               final Class<T> requiredClass) {

        return InstanceOfIterator.of(iterator, requiredClass);
    }
}
