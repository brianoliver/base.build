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

import build.base.foundation.predicate.Predicates;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/**
 * An {@link Iterator} adapter that stops iteration when a {@link Predicate} evaluates to {@code true}.
 *
 * @param <T> the type of elements
 * @author brian.oliver
 * @since Sep-2025
 */
public class AbortableIterator<T> implements Iterator<T> {

    /**
     * The underlying {@link Iterator}.
     */
    private final Iterator<T> iterator;

    /**
     * The {@link Predicate} that triggers abortion of iteration.
     */
    private final Predicate<? super T> predicate;

    /**
     * The next element to return from the {@link AbortableIterator}.
     */
    private T next;

    /**
     * Indicates when the {@link #next} element is available.
     */
    private boolean available;

    /**
     * Indicates when iteration has been aborted.
     */
    private boolean aborted;

    /**
     * Constructs an {@link AbortableIterator}.
     *
     * @param predicate the {@link Predicate} that triggers abortion when {@code true}
     * @param iterator  the {@link Iterator} to adapt
     */
    private AbortableIterator(final Iterator<T> iterator,
                              final Predicate<? super T> predicate) {

        this.predicate = predicate == null
            ? Predicates.never()
            : predicate;

        this.iterator = iterator == null
            ? Iterators.empty()
            : iterator;

        this.next = null;
        this.available = false;
        this.aborted = false;
    }

    @Override
    public boolean hasNext() {
        if (!this.available && !this.aborted && this.iterator.hasNext()) {
            final var item = this.iterator.next();

            if (this.predicate.test(item)) {
                this.aborted = true;
            } else {
                this.next = item;
                this.available = true;
            }
        }

        return this.available;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more elements are available");
        }

        this.available = false;
        return this.next;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Unable to remove from an AbortableIterator");
    }

    /**
     * Creates an {@link AbortableIterator} from the specified {@link Iterator} and {@link Predicate}.
     *
     * @param <T>       the type of element
     * @param iterator  the {@link Iterator}
     * @param predicate the {@link Predicate} that triggers abortion when {@code true}
     * @return an {@link AbortableIterator}
     */
    public static <T> Iterator<T> of(final Iterator<T> iterator,
                                     final Predicate<? super T> predicate) {

        return predicate == null || predicate == Predicates.never()
            ? iterator
            : predicate == Predicates.always()
            ? Iterators.empty()
            : new AbortableIterator<>(iterator, predicate);
    }
}
