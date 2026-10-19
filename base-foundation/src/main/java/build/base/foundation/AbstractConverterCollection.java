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

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

/**
 * A {@link Collection} which stores its entries in another {@link Collection} in a converted form.
 *
 * @param <T> the collection entry type
 * @param <U> the underlying collection entry type
 * @author mark.falco
 * @author brian.oliver
 * @since March-2022
 */
public abstract class AbstractConverterCollection<T, U>
    extends AbstractCollection<T> {

    /**
     * The underlying {@link Collection}.
     */
    protected final Collection<U> underlying;

    /**
     * Constructs an {@link AbstractConverterCollection}.
     *
     * @param underlying the underlying {@link Collection}
     */
    protected AbstractConverterCollection(final Collection<U> underlying) {
        this.underlying = underlying;
    }

    /**
     * Invoked prior to mutating operations against the underlying {@link Collection}.
     */
    protected void preWrite() {
    }

    /**
     * Invoked prior to non-mutating operations against the underlying {@link Collection}.
     */
    protected void preRead() {
    }

    @Override
    public int size() {
        preRead();
        return this.underlying.size();
    }

    @Override
    public boolean isEmpty() {
        preRead();
        return this.underlying.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
        preRead();
        return isOuter(o) && this.underlying.contains(downRead(o));
    }

    @Override
    public boolean add(final T t) {
        preWrite();
        return this.underlying.add(down(t));
    }

    @Override
    public boolean remove(final Object o) {
        preWrite();
        return isOuter(o) && this.underlying.remove(downRead(o));
    }

    @Override
    public void clear() {
        preWrite();
        this.underlying.clear();
    }

    @Override
    public Iterator<T> iterator() {
        preRead();
        return new AbstractConverterIterator<T, U>(this.underlying.iterator()) {
            @Override
            protected T up(final U value) {
                return AbstractConverterCollection.this.up(value);
            }
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean removeAll(final Collection<?> c) {
        preWrite();
        return this.underlying.removeAll(inverted((Collection<T>) c));
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean retainAll(final Collection<?> c) {
        preWrite();
        return this.underlying.retainAll(inverted((Collection<T>) c));
    }

    /**
     * Produce an inverted version of the supplied collection.
     *
     * @param c the collection to invert
     * @return the inverted collection
     */
    protected Collection<U> inverted(final Collection<T> c) {
        return new AbstractConverterCollection<U, T>(c) {
            @Override
            protected T down(final U value) {
                return AbstractConverterCollection.this.up(value);
            }

            @Override
            protected U up(final T value) {
                return AbstractConverterCollection.this.down(value);
            }
        };
    }

    /**
     * Return {@code true} if the supplied value is compatible with this collection
     *
     * @param value the value to test
     * @return {@code true} if compatible
     */
    protected boolean isOuter(final Object value) {
        return true;
    }

    /**
     * Convert the collection's type to the storage type.
     *
     * @param value the value to convert
     * @return the converted value
     */
    abstract protected U down(T value);

    /**
     * A special variant of {@link #down} which produces a down value which is only meant to be used in operations which
     * will *not* store the value in the collection
     *
     * @param value the value
     * @return the non-storable storage value
     */
    @SuppressWarnings("unchecked")
    protected Object downRead(final Object value) {
        return down((T) value);
    }

    /**
     * Convert the storage type to the collection's type
     *
     * @param value the value to convert
     * @return the converted value
     */
    abstract protected T up(U value);
}
