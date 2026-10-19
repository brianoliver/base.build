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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.stream.Collectors;

/**
 * An {@link Iterator} which records values returned by an underlying {@link Iterator#next()}, and returns those
 * values (along with the {@link Iterator}) in {@link #toString()} invocations.
 *
 * @author ben.horowitz
 * @author brian.oliver
 * @since Aug-2021
 */
public class RecordingIterator<T>
    implements Iterator<T> {

    /**
     * The {@link Iterator} from which recording will take place.
     */
    private final Iterator<T> delegate;

    /**
     * Sequence of values returned by {@link Iterator#next()}.
     */
    private final ArrayList<T> iterated;

    /**
     * Constructs a {@link RecordingIterator} with the given delegate.
     *
     * @param delegate an {@link Iterator} to which calls are delegated
     */
    public RecordingIterator(final Iterator<T> delegate) {
        this.delegate = delegate;
        this.iterated = new ArrayList<>();
    }

    @Override
    public boolean hasNext() {
        return this.delegate.hasNext();
    }

    @Override
    public T next() {
        final T next = this.delegate.next();
        this.iterated.add(next);
        return next;
    }

    @Override
    public void remove() {
        this.delegate.remove();
        this.iterated.removeLast();
    }

    @Override
    public String toString() {
        final String separator = System.lineSeparator() + "\t";
        return '['
            + this.iterated.stream()
            .map(Object::toString)
            .collect(Collectors.joining(separator, separator, System.lineSeparator()))
            + ']';
    }
}
