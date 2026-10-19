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

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;

/**
 * An {@link Iterator} where items are mapped to another type during iteration.
 *
 * @param <T> the type of elements to be mapped
 * @param <R> the type of elements returned by this iterator
 * @author brian.oliver
 * @since Jul-2019
 */
public class MappingIterator<T, R>
    implements Iterator<R> {

    /**
     * The underlying {@link Iterator}.
     */
    private final Iterator<T> iterator;

    /**
     * The mapping {@link Function}.
     */
    private final Function<T, R> function;

    /**
     * Constructs a {@link MappingIterator}.
     *
     * @param iterator the {@link Iterator}
     * @param function the {@link Function}
     */
    private MappingIterator(final Iterator<T> iterator,
                            final Function<T, R> function) {

        Objects.requireNonNull(function, "The function must not be null");

        this.iterator = iterator == null ? Collections.emptyIterator() : iterator;
        this.function = function;
    }

    @Override
    public void remove() {
        this.iterator.remove();
    }

    @Override
    public boolean hasNext() {
        return this.iterator.hasNext();
    }

    @Override
    public R next() {
        return this.function.apply(this.iterator.next());
    }

    @Override
    public String toString() {
        return "MappingIterator{"
            + "iterator=" + this.iterator
            + ", function=" + this.function
            + '}';
    }

    /**
     * Creates a {@link MappingIterator} from the specified {@link Iterator} and {@link Function}.
     *
     * @param iterator the {@link Iterator}
     * @param function the {@link Function}
     */
    public static <T, R> Iterator<R> of(final Iterator<T> iterator,
                                        final Function<T, R> function) {

        return iterator == null || !iterator.hasNext() || function == null
            ? Iterators.empty()
            : new MappingIterator<>(iterator, function);
    }
}
