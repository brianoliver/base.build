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
import java.util.Objects;

/**
 * An {@link Iterator} that flattens an {@link Iterator} of {@link Iterator}s of elements of {@code T} into
 * an {@link Iterator} of elements of {@code T}.
 *
 * @param <T> the type of elements
 * @author brian.oliver
 * @since Sep-2025
 */
public class FlatteningIterator<T> implements Iterator<T> {

    /**
     * The underlying {@link Iterator} of {@link Iterator}s.
     */
    private final Iterator<Iterator<T>> iterators;

    /**
     * The current {@link Iterator} being flattened.
     */
    private Iterator<T> current;

    /**
     * Constructs a {@link FlatteningIterator}.
     *
     * @param iterators the {@link Iterator} of {@link Iterator}s
     */
    private FlatteningIterator(final Iterator<Iterator<T>> iterators) {
        this.iterators = Objects.requireNonNull(iterators, "The iterators must not be null");
        this.current = null;
    }

    @Override
    public boolean hasNext() {
        while ((this.current == null || !this.current.hasNext()) && this.iterators.hasNext()) {
            this.current = this.iterators.next();
        }

        return this.current != null && this.current.hasNext();
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more elements to iterate over");
        }

        return this.current.next();
    }

    /**
     * Creates a {@link Iterator} that flattens the specified {@link Iterator} of {@link Iterator}s into a single
     * {@link Iterator} of elements.
     *
     * @param <T>       the type of elements
     * @param iterators the {@link Iterator} of {@link Iterator}s
     * @return an {@link Iterator} of elements
     */
    public static <T> Iterator<T> of(final Iterator<Iterator<T>> iterators) {
        return new FlatteningIterator<>(iterators);
    }
}
