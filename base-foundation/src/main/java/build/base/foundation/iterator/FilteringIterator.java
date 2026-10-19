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

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/**
 * An {@link Iterator} where items are filtered from another {@link Iterator} during iteration.
 *
 * @param <T> the type of elements
 * @author brian.oliver
 * @since Jul-2019
 */
public class FilteringIterator<T>
    implements Iterator<T> {

    /**
     * The underlying {@link Iterator}.
     */
    private final Iterator<T> iterator;

    /**
     * The {@link Predicate} for filtering.
     */
    private final Predicate<? super T> predicate;

    /**
     * The next element to return from the {@link FilteringIterator}.
     */
    private T next;

    /**
     * Indicates when the {@link #next} element is available.
     */
    private boolean available;

    /**
     * Constructs a {@link FilteringIterator}.
     *
     * @param iterator  the {@link Iterator}
     * @param predicate the {@link Predicate}
     */
    private FilteringIterator(final Iterator<T> iterator,
                              final Predicate<? super T> predicate) {

        this.iterator = iterator == null ? Collections.emptyIterator() : iterator;
        this.predicate = predicate == null ? Predicates.always() : predicate;
        this.next = null;
        this.available = false;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Unable to remove from a FilteringIterator");
    }

    @Override
    public boolean hasNext() {
        fetch();

        return this.available;
    }

    @Override
    public T next() {
        fetch();

        if (this.available) {
            this.available = false;
            return this.next;
        } else {
            throw new NoSuchElementException("No more filtered elements are available");
        }
    }

    @Override
    public String toString() {
        return "FilteringIterator{"
            + "iterator=" + this.iterator
            + ", predicate=" + this.predicate
            + (this.available ? ", next=" + this.next : "")
            + '}';
    }

    /**
     * Attempts to fetch the next item that satisfies the {@link Predicate} filter from the {@link Iterable}.
     */
    private void fetch() {
        while (!this.available && this.iterator.hasNext()) {
            final T item = this.iterator.next();
            if (this.predicate.test(item)) {
                this.next = item;
                this.available = true;
            }
        }
    }

    /**
     * Creates a {@link FilteringIterator} given an {@link Iterator} and {@link Predicate}.
     *
     * @param iterator  the {@link Iterator}
     * @param predicate the {@link Predicate}
     */
    public static <T> Iterator<T> of(final Iterator<T> iterator,
                                     final Predicate<? super T> predicate) {

        return iterator == null || !iterator.hasNext() || predicate == null || predicate == Predicates.never()
            ? Iterators.empty()
            : predicate == Predicates.always()
            ? iterator
            : new FilteringIterator<>(iterator, predicate);
    }
}
