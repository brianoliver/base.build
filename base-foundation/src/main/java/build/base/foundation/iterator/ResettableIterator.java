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

import build.base.foundation.stream.Streamable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An {@link Iterator} that supports marking and resetting the next value in an iteration.
 *
 * @param <T> the type of element in the {@link Iterator}
 * @author brian.oliver
 */
public class ResettableIterator<T>
    implements Iterator<T> {

    /**
     * The underlying {@link Iterator} that supplies values for the {@link ResettableIterator}.
     */
    private final Iterator<T> iterator;

    /**
     * A buffer of values that have been fetched from the underlying {@link Iterator},
     * to allowing resetting the position in the iteration.
     */
    private final ArrayList<T> buffer;

    /**
     * The index of the next value to return using {@link #next()} from the buffer.
     */
    private int currentIndex;

    /**
     * The first index in the buffer that we can reset up to.
     */
    private int firstIndex;

    /**
     * Constructs a {@link ResettableIterator} given an {@link Iterator}
     * that supplies the underlying values for iteration.
     *
     * @param iterator the {@link Iterator}
     */
    public ResettableIterator(final Iterator<T> iterator) {
        this.iterator = iterator == null ? Collections.emptyIterator() : iterator;
        this.buffer = new ArrayList<>();
        this.currentIndex = 0;
        this.firstIndex = 0;
    }

    /**
     * Constructs a {@link ResettableIterator} given zero or more elements.
     *
     * @param elements the elements
     */
    @SafeVarargs
    public ResettableIterator(final T... elements) {
        this(elements == null ? Collections.emptyIterator() : Arrays.asList(elements).iterator());
    }

    /**
     * Constructs a {@link ResettableIterator} given a {@link Stream} of elements.
     *
     * @param stream the {@link Stream}
     */
    public ResettableIterator(final Stream<T> stream) {
        this(stream == null ? Collections.emptyIterator() : stream.iterator());
    }

    /**
     * Constructs a {@link ResettableIterator} given an {@link Iterable}.
     *
     * @param iterable the {@link Iterable}
     */
    public ResettableIterator(final Iterable<T> iterable) {
        this(iterable == null ? Collections.emptyIterator() : iterable.iterator());
    }

    /**
     * Attempts to fetch a value from the underlying {@link Iterator}.
     */
    private void fetch() {
        if (this.iterator.hasNext()) {
            this.buffer.add(this.iterator.next());
        }
    }

    @Override
    public boolean hasNext() {
        return this.currentIndex < this.firstIndex + this.buffer.size() || this.iterator.hasNext();
    }

    @Override
    public T next() {
        if (this.currentIndex < this.firstIndex + this.buffer.size()) {
            return this.buffer.get(this.currentIndex++ - this.firstIndex);
        }
        else if (this.iterator.hasNext()) {
            fetch();
            return next();
        }
        else {
            throw new NoSuchElementException("Exhausted values for iteration");
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("ResettableIterator does not support remove");
    }

    /**
     * Obtains a {@link Position} representing the current location with in the {@link ResettableIterator} iteration,
     * which may later be used with {@link #reset(Position)} to reset the {@link Iterator} back to the specified
     * {@link Position}.
     *
     * @return a {@link Position}
     */
    public Position mark() {
        return new Position(this.currentIndex);
    }

    /**
     * Resets the {@link Iterator} to the specified {@link Position}.
     *
     * @param position the {@link Position}
     * @throws NoSuchElementException when the {@link ResettableIterator} can't be reset to the specified {@link Position}
     */
    public void reset(final Position position)
        throws NoSuchElementException {

        final int index = position.index;

        if (index > this.firstIndex + this.buffer.size() || index < this.firstIndex) {
            throw new NoSuchElementException(
                "Unable to reset the Iterator to the specified position @" + index);
        }
        else {
            this.currentIndex = index;
        }
    }

    /**
     * Drops all positions prior to the specified {@link Position}.
     * The {@link Iterator} can no longer be reset prior to the position.
     *
     * @param position the {@link Position}
     * @throws NoSuchElementException when the {@link ResettableIterator} can't drop before the specified {@link Position}
     */
    public void drop(final Position position) {
        final int index = position.index;

        if (index > this.firstIndex + this.buffer.size() || index < this.firstIndex) {
            throw new NoSuchElementException(
                "Unable to drop from the Iterator at the specified position @" + index);
        }
        else {
            this.buffer.subList(0, index - this.firstIndex).clear();
            this.firstIndex = index;
        }
    }

    @Override
    public String toString() {
        return "ResettableIterator{"
            + "iterator=" + this.iterator
            + ", buffer=" + this.buffer.stream()
            .map(Object::toString)
            .collect(Collectors.joining(", ",
                this.firstIndex == 0 ? "[" : "[...",
                hasNext() ? "...]" : "]"))
            + '}';
    }

    /**
     * Attempts to obtain a {@link Streamable} immutable view of the elements from and including the specified starting
     * {@link Position} upto and including the specified ending {@link Position}.
     * <p>
     * Should elements no longer be available, an empty {@link Streamable} is returned.
     *
     * @param starting the starting {@link Position}
     * @param ending   the ending {@link Position}
     * @return a {@link Streamable}
     */
    public Streamable<T> range(final Position starting,
                               final Position ending) {

        if (starting != null && ending != null
            && starting.index >= this.firstIndex && starting.index <= this.currentIndex
            && ending.index >= this.firstIndex && ending.index <= this.currentIndex) {

            final int first = Math.min(starting.index, ending.index);
            final int last = Math.max(starting.index, ending.index);

            final ArrayList<T> list = new ArrayList<>(last - first + 1);
            for (int index = first; index < last; index++) {
                list.add(this.buffer.get(index));
            }

            return () -> list.iterator();
        }
        return Collections::emptyIterator;
    }

    /**
     * Represents a position in an iteration for a {@link ResettableIterator}, allowing resetting to
     * the said position at some later point in time.
     */
    public static class Position
        implements Comparable<Position> {

        /**
         * The index position.
         */
        private final int index;

        /**
         * Constructs a {@link Position}.
         *
         * @param index the index position
         */
        private Position(final int index) {
            this.index = index;
        }

        /**
         * Determines if the {@link Position} represents the start of an iteration.
         *
         * @return {@code true} if the {@link Position} represents the start of an iteration, {@code false} otherwise
         */
        public boolean isStart() {
            return this.index == 0;
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            final Position position = (Position) other;
            return this.index == position.index;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.index);
        }

        @Override
        public int compareTo(final Position other) {
            return this.index - other.index;
        }
    }
}
