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

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An {@link Iterator} adapter that only returns distinct non-{@code null} elements from an underlying {@link Iterator}.
 *
 * @param <T> the type of element
 * @author brian.oliver
 * @since Sep-2025
 */
public class DistinctIterator<T> implements Iterator<T> {

    /**
     * The underlying {@link Iterator}.
     */
    private final Iterator<T> iterator;

    /**
     * The set of elements we have already seen.
     */
    private final HashSet<T> seen;

    /**
     * The next element to return from the {@link DistinctIterator}.
     */
    private T next;

    /**
     * Indicates when the {@link #next} element is available.
     */
    private boolean hasNext;

    /**
     * Constructs a {@link DistinctIterator}.
     *
     * @param iterator the underlying {@link Iterator}
     */
    private DistinctIterator(final Iterator<T> iterator) {
        this.iterator = iterator == null
            ? Iterators.empty()
            : iterator;

        this.seen = new HashSet<>();
        this.next = null;
        this.hasNext = false;
    }

    @Override
    public boolean hasNext() {
        while (!this.hasNext && this.iterator.hasNext()) {
            final var element = this.iterator.next();
            if (this.seen.add(element)) {
                this.next = element;
                this.hasNext = true;
            }
        }

        return this.hasNext;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more elements to iterate over");
        }

        this.hasNext = false;
        return this.next;
    }

    /**
     * Creates a {@link Iterator} that returns distinct non-{@code null} elements from the specified {@link Iterator}.
     *
     * @param iterator the {@link Iterator}
     * @param <T>      the type of element
     * @return a new {@link Iterator}
     */
    public static <T> Iterator<T> of(final Iterator<T> iterator) {
        return new DistinctIterator<>(iterator);
    }
}
