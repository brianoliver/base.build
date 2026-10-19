package build.base.foundation.stream;

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

import build.base.foundation.iterator.FilteringIterator;
import build.base.foundation.iterator.Iterators;
import build.base.foundation.iterator.MappingIterator;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A structure that supports {@link Stream}ing of a known, non-infinite, number of elements.
 *
 * @param <T> the type of element
 * @author mark.falco
 * @author brian.oliver
 * @since Jan-2019
 */
@FunctionalInterface
public interface Streamable<T>
    extends Iterable<T> {

    /**
     * A constant representing an empty {@link Streamable}.
     */
    Streamable<Object> EMPTY = new Streamable<>() {
        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public long count() {
            return 0;
        }

        @Override
        public Optional<Object> first() {
            return Optional.empty();
        }

        @Override
        public Optional<Object> last() {
            return Optional.empty();
        }

        @Override
        public Optional<Object> findAny() {
            return Optional.empty();
        }

        @Override
        public <R> Streamable<R> map(final Function<Object, R> mapper) {
            return Streamable.empty();
        }

        @Override
        public Streamable<Object> filter(final Predicate<? super Object> predicate) {
            return Streamable.empty();
        }

        @Override
        public Iterator<Object> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public Spliterator<Object> spliterator() {
            return Spliterators.emptySpliterator();
        }
    };

    /**
     * Determines if the {@link Streamable} is empty.  This is semantically equivalent to
     * invoking {@link #stream()}{@code .count() == 0}.
     *
     * @return {@code true} if the {@link Streamable} is empty, otherwise {@code false}
     */
    default boolean isEmpty() {
        return !iterator().hasNext();
    }

    /**
     * Determines if the {@link Streamable} is not empty.  This is semantically equivalent to
     * invoking {@link #stream()}{@code .count() > 0}.
     *
     * @return {@code true} if the {@link Streamable} is not empty, otherwise {@code false}
     */
    default boolean isNotEmpty() {
        return !isEmpty();
    }

    /**
     * Counts the number of elements in the {@link Streamable}.  This is semantically equivalent to
     * invoking {@link #stream()}{@code .count()}.
     *
     * @return {@code true} if the {@link Streamable} is empty, otherwise {@code false}
     */
    default long count() {
        final var iterator = iterator();

        var count = 0L;
        for (; iterator.hasNext(); iterator.next()) {
            count++;
        }
        return count;
    }

    /**
     * Attempts to obtain the first element from the {@link Streamable}.
     *
     * @return {@link Optional} element if one or more is available, otherwise {@link Optional#empty()}
     */
    default Optional<T> first() {
        return isEmpty()
            ? Optional.empty()
            : Optional.of(iterator().next());
    }

    /**
     * Attempts to obtain the last element from the {@link Streamable}.
     *
     * @return {@link Optional} element if one or more is available, otherwise {@link Optional#empty()}
     */
    default Optional<T> last() {
        if (isEmpty()) {
            return Optional.empty();
        }

        var last = iterator().next();
        while (iterator().hasNext()) {
            last = iterator().next();
        }

        return Optional.of(last);
    }

    /**
     * Attempts to obtain any element from the {@link Streamable}.
     *
     * @return {@link Optional} element if one or more is available, otherwise {@link Optional#empty()}
     */
    default Optional<T> findAny() {
        return isEmpty()
            ? Optional.empty()
            : Optional.of(iterator().next());
    }

    /**
     * Return a sequential {@link Stream} with this data structure as its source.
     *
     * @return a sequential {@link Stream} with this data structure as its source
     * @see java.util.Collection#stream()
     */
    default Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Return a possibly parallel {@link Stream} with this data structure as its source.
     *
     * @return a possibly parallel {@link Stream} with this data structure as its source
     * @see Collection#parallelStream()
     */
    default Stream<T> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }

    /**
     * Obtains a {@link Stream} of elements of the {@link Streamable} that are assignable to the specified {@link Class}.
     *
     * @param u   the {@link Class} of element
     * @param <U> the type of element
     * @return a {@link Stream} of elements
     */
    default <U> Stream<U> stream(final Class<U> u) {
        return u == null
            ? Stream.empty()
            : stream()
            .filter(u::isInstance)
            .map(u::cast);
    }

    /**
     * Obtains a {@link Streamable} of elements of the {@link Streamable} that are assignable to the specified
     * {@link Class}.
     *
     * @param u   the {@link Class} of element
     * @param <U> the type of element
     * @return a {@link Streamable} of elements
     */
    default <U> Streamable<U> streamable(final Class<U> u) {
        return () -> stream(u).iterator();
    }

    /**
     * Maps the elements of the {@link Streamable} to another type.
     *
     * @param mapper the {@link Function} to map the elements
     * @param <R>    the type of the mapped elements
     * @return a {@link Streamable} of the mapped elements
     */
    default <R> Streamable<R> map(final Function<T, R> mapper) {
        return new Streamable<>() {
            @Override
            public boolean isEmpty() {
                return Streamable.super.isEmpty();
            }

            @Override
            public long count() {
                return Streamable.super.count();
            }

            @Override
            public Iterator<R> iterator() {
                return MappingIterator.of(Streamable.this.iterator(), mapper);
            }
        };
    }

    /**
     * Creates a new {@link Streamable} containing only those elements of this {@link Streamable} that satisfy
     * the specified {@link Predicate}.
     *
     * @param predicate the {@link Predicate}
     * @return a new {@link Streamable} of the filtered elements
     */
    default Streamable<T> filter(final Predicate<? super T> predicate) {
        return new Streamable<>() {
            @Override
            public boolean isEmpty() {
                return Streamable.super.isEmpty();
            }

            @Override
            public long count() {
                return this.stream().count();
            }

            @Override
            public Iterator<T> iterator() {
                return FilteringIterator.of(Streamable.this.iterator(), predicate);
            }
        };
    }

    /**
     * Attempts to return the {@link Collection} backing the {@link Streamable}, assuming it is assignable to
     * the specified {@link Class} of {@link Collection}.
     *
     * @param <C>             the type of {@link Collection}
     * @param collectionClass the {@link Class} of {@link Collection}
     * @return the {@link Optional} {@link Collection}, {@link Optional#empty()} otherwise
     */
    default <C extends Collection<T>> Optional<C> unwrap(final Class<C> collectionClass) {
        return Optional.empty();
    }

    /**
     * Creates a {@link Streamable} for an empty {@link Stream}s of a specific type.
     *
     * @param <T> the type of element
     * @return a {@link Streamable} for an empty {@link Stream}s
     */
    @SuppressWarnings("unchecked")
    static <T> Streamable<T> empty() {
        return (Streamable<T>) EMPTY;
    }

    /**
     * Creates a {@link Streamable} of elements from the specified {@link Stream}.
     *
     * @param stream the {@link Stream}
     * @param <T>    the type of element
     * @return a {@link Streamable} of elements from the specified {@link Stream}.
     */
    static <T> Streamable<T> of(final Stream<T> stream) {

        if (stream == null) {
            return empty();
        }

        final var elements = stream.toList();

        return elements.isEmpty()
            ? empty()
            : of(elements);
    }

    /**
     * Creates a {@link Streamable} of the specified {@link Optional}.
     *
     * @param optional the {@link Optional}
     * @param <T>      the type of element
     * @return a {@link Streamable} of the specified {@link Optional}
     */
    static <T> Streamable<T> of(final Optional<T> optional) {

        if (optional == null || optional.isEmpty()) {
            return empty();
        }

        final var value = optional.orElseThrow();

        return new Streamable<>() {
            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public long count() {
                return 1;
            }

            @Override
            public Iterator<T> iterator() {
                return Iterators.ofNullable(value);
            }
        };
    }

    /**
     * Creates a {@link Streamable} of the specified elements.
     *
     * @param elements the elements
     * @param <T>      the type of element
     * @return a {@link Streamable} of elements
     */
    @SafeVarargs
    static <T> Streamable<T> of(final T... elements) {

        return elements == null || elements.length == 0
            ? empty()
            : new Streamable<>() {
            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public Optional<T> first() {
                return Optional.ofNullable(elements[0]);
            }

            @Override
            public Optional<T> last() {
                return Optional.ofNullable(elements[elements.length - 1]);
            }

            @Override
            public long count() {
                return elements.length;
            }

            @Override
            public Iterator<T> iterator() {
                return new Iterator<>() {
                    int index = 0;

                    @Override
                    public boolean hasNext() {
                        return index < elements.length;
                    }

                    @Override
                    public T next() {
                        return elements[index++];
                    }
                };
            }

            @Override
            public Spliterator<T> spliterator() {
                return Spliterators.spliterator(elements, Spliterator.IMMUTABLE);
            }
        };
    }

    /**
     * Obtains a {@link Streamable} given a {@link Collection}.
     *
     * @param collection the {@link Collection}
     * @param <T>        the type of element
     * @return the {@link Streamable} over the {@link Collection}
     */
    static <T> Streamable<T> of(final Collection<T> collection) {

        return collection == null || collection.isEmpty()
            ? empty()
            : new Streamable<T>() {
            @Override
            public boolean isEmpty() {
                return collection.isEmpty();
            }

            @Override
            public Optional<T> first() {
                return isEmpty()
                    ? Optional.empty()
                    : (collection instanceof SequencedCollection<T> sequenced
                    ? Optional.of(sequenced.getFirst())
                    : Optional.of(collection.iterator().next()));
            }

            @Override
            public Optional<T> last() {
                return isEmpty()
                    ? Optional.empty()
                    : (collection instanceof SequencedCollection<T> sequenced
                    ? Optional.of(sequenced.getLast())
                    : Optional.of(collection.iterator().next()));
            }

            @Override
            public long count() {
                return collection.size();
            }

            @Override
            public Iterator<T> iterator() {
                return collection.iterator();
            }

            @Override
            public Spliterator<T> spliterator() {
                return collection.spliterator();
            }

            @Override
            public <C extends Collection<T>> Optional<C> unwrap(final Class<C> collectionClass) {
                return collectionClass != null
                    && collectionClass.isInstance(collection)
                    ? Optional.of(collectionClass.cast(collection))
                    : Optional.empty();
            }
        };
    }

    /**
     * Obtains a {@link Streamable} given a {@link Iterable}.
     *
     * @param iterable the {@link Iterable}
     * @param <T>      the type of element
     * @return the {@link Streamable} over the {@link Iterable}
     */
    static <T> Streamable<T> of(final Iterable<T> iterable) {
        return iterable == null
            ? empty()
            : iterable::iterator;
    }

    /**
     * Obtains a {@link Streamable} over zero or more {@link Streamable}s.
     *
     * @param streamables the {@link Streamable}s
     * @param <T>         the type of element
     * @return a {@link Streamable} over the {@link Streamable}s
     */
    @SafeVarargs
    static <T> Streamable<T> of(final Streamable<? extends T>... streamables) {
        if (streamables == null || streamables.length == 0) {
            return empty();
        }

        final var array = Arrays.asList(streamables);

        return new Streamable<>() {
            @Override
            public boolean isEmpty() {
                return array.stream()
                    .allMatch(Streamable::isEmpty);
            }

            @Override
            public long count() {
                return array.stream()
                    .map(Streamable::count)
                    .reduce(0L, Long::sum);
            }

            @Override
            @SuppressWarnings("unchecked")
            public Iterator<T> iterator() {
                return Iterators.of(array.stream()
                    .map(Streamable::iterator)
                    .toArray(Iterator[]::new));
            }
        };
    }

    /**
     * Produces a {@link Streamable} that is a reversed {@link Stream}.
     *
     * @param stream the {@link Stream}
     * @param <T>    the type of elements
     * @return a {@link Streamable} containing the elements of the provided {@link Stream} in reverse order
     */
    static <T> Streamable<T> reversed(final Stream<T> stream) {
        final ArrayDeque<T> deque = new ArrayDeque<>();
        if (stream != null) {
            stream.forEach(deque::addFirst);
        }

        return deque.isEmpty()
            ? Streamable.empty()
            : new Streamable<>() {
            @Override
            public boolean isEmpty() {
                return deque.isEmpty();
            }

            @Override
            public long count() {
                return deque.size();
            }

            @Override
            public Optional<T> first() {
                return isEmpty()
                    ? Optional.empty()
                    : Optional.of(deque.getFirst());
            }

            @Override
            public Optional<T> last() {
                return isEmpty()
                    ? Optional.empty()
                    : Optional.of(deque.getLast());
            }

            @Override
            public Optional<T> findAny() {
                return Optional.of(deque.getFirst());
            }

            @Override
            public Iterator<T> iterator() {
                return deque.iterator();
            }

            @Override
            public Spliterator<T> spliterator() {
                return Spliterators.spliterator(iterator(), deque.size(), Spliterator.IMMUTABLE);
            }

            @Override
            public <C extends Collection<T>> Optional<C> unwrap(final Class<C> collectionClass) {
                return collectionClass != null
                    && collectionClass.isInstance(deque)
                    ? Optional.of(collectionClass.cast(deque))
                    : Optional.empty();
            }
        };
    }
}
