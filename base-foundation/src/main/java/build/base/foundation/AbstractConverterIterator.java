package build.base.foundation;

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
 * An {@code abstract} {@link Iterator} which converts the contents of another {@link Iterator} during iteration.
 *
 * @param <T> the iteration entry type
 * @param <U> the underlying entry type
 * @author mark.falco
 * @author brian.oliver
 * @since March-2022
 */
public abstract class AbstractConverterIterator<T, U>
    implements Iterator<T> {

    /**
     * The underlying {@link Iterator} from which to iterate and convert entries.
     */
    protected final Iterator<U> underlying;

    /**
     * Constructs an {@link AbstractConverterIterator} from an underlying {@link Iterator}.
     *
     * @param underlying the underlying {@link Iterator} from which to convert entries
     */
    protected AbstractConverterIterator(final Iterator<U> underlying) {
        this.underlying = underlying;
    }

    @Override
    public boolean hasNext() {
        return this.underlying.hasNext();
    }

    @Override
    public T next() {
        return up(this.underlying.next());
    }

    @Override
    public void remove() {
        this.underlying.remove();
    }

    /**
     * Convert from the underlying {@link Iterator} type entry to the {@link Iterator} type entry.
     *
     * @param value the underlying {@link Iterator} type entry
     * @return the converted value
     */
    protected abstract T up(U value);
}
