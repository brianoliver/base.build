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

/**
 * An {@link Iterator} that returns {@link Object}s that are {@code instanceOf} a specified {@link Class} from
 * an underlying {@link Iterator}.
 *
 * @param <T> the type of {@link Object}
 * @author brian.oliver
 * @since Sep-2025
 */
public class InstanceOfIterator<T>
    implements Iterator<T> {

    /**
     * The underlying {@link Iterator}.
     */
    private final Iterator<?> iterator;

    /**
     * The required {@link Class}.
     */
    private final Class<T> requiredClass;

    /**
     * The next {@link Object} to return.
     */
    private T next;

    /**
     * Whether there is a next {@link Object} to return.
     */
    private boolean hasNext;

    /**
     * Constructs an {@link InstanceOfIterator}.
     *
     * @param iterator      the underlying {@link Iterator}
     * @param requiredClass the required {@link Class}
     */
    private InstanceOfIterator(final Iterator<?> iterator,
                               final Class<T> requiredClass) {

        this.iterator = iterator;
        this.requiredClass = requiredClass;
        this.next = null;
        this.hasNext = false;
    }

    @Override
    public boolean hasNext() {
        if (this.hasNext) {
            return true;
        }

        while (this.iterator.hasNext()) {
            final var next = this.iterator.next();
            if (this.requiredClass.isInstance(next)) {
                this.next = this.requiredClass.cast(next);
                this.hasNext = true;
                return true;
            }
        }

        return false;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new IllegalStateException("No more elements to iterate over");
        }

        this.hasNext = false;
        return this.next;
    }

    /**
     * Creates an {@link InstanceOfIterator} from the specified {@link Iterator} and required {@link Class}.
     *
     * @param <T>           the type of {@link Object}
     * @param iterator      the underlying {@link Iterator}
     * @param requiredClass the required {@link Class}
     * @return an {@link Iterator}
     */
    @SuppressWarnings("unchecked")
    public static <T> Iterator<T> of(final Iterator<?> iterator,
                                     final Class<T> requiredClass) {

        return iterator == null || !iterator.hasNext()
            ? Iterators.empty()
            : (requiredClass == null || Object.class == requiredClass)
            ? (Iterator<T>) iterator
            : new InstanceOfIterator<>(iterator, requiredClass);
    }
}
