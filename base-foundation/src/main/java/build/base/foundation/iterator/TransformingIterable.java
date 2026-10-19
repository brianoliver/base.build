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
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An {@link Iterable} that may transform and filter elements in returned {@link java.util.Iterator}s.
 *
 * @param <T> the type of elements returned by an {@link Iterator} produced by the {@link Iterable}
 * @author brian.oliver
 * @since Jul-2019
 */
public interface TransformingIterable<T>
    extends Iterable<T> {

    /**
     * Creates a {@link TransformingIterable} where by the elements returned by the produced {@link Iterator}s
     * are mapped according to the specified {@link Function}.
     *
     * @param function the {@link Function}
     * @param <R>      the type of elements returned by the {@link Iterator}
     * @return a {@link TransformingIterable}
     */
    default <R> TransformingIterable<R> map(final Function<T, R> function) {
        return of(() -> Iterators.map(iterator(), function));
    }

    /**
     * Creates a {@link TransformingIterable} where by the elements returned by the produced {@link Iterator}s
     * satisfy the specified {@link Predicate}.
     *
     * @param predicate the {@link Predicate}
     * @return a {@link TransformingIterable}
     */
    default TransformingIterable<T> filter(final Predicate<? super T> predicate) {
        return of(() -> Iterators.filter(iterator(), predicate));
    }

    /**
     * Creates a {@link TransformingIterable} where by the elements returned by the produced {@link Iterator}
     * are assignable to the specified {@link Class}.
     *
     * @param <R>          the type of the desired {@link Class}
     * @param desiredClass the desired {@link Class}
     * @return a {@link TransformingIterable}
     */
    default <R> TransformingIterable<R> filter(final Class<? extends R> desiredClass) {
        return filter(desiredClass::isInstance).map(desiredClass::cast);
    }

    /**
     * Creates a {@link TransformingIterable} where by the elements returned by the produced {@link Iterator}
     * have a property extracted and are tested against a {@link Predicate}.
     *
     * @param <R>       the type of the desired {@link Class}
     * @param extractor the extractor {@link Function} to be passed to the {@link Predicate} predicate
     * @param predicate the {@link Predicate}
     * @return a {@link TransformingIterable}
     */
    default <R> TransformingIterable<T> filter(final Function<T, R> extractor, final Predicate<? super R> predicate) {
        return filter(t -> predicate.test(extractor.apply(t)));
    }

    /**
     * Creates a {@link TransformingIterable} where by the elements returned by the produced {@link Iterator}
     * have a property extracted and compared against a value.
     *
     * @param <R>       the type of the desired {@link Class}
     * @param extractor the extractor {@link Function} to be passed to the {@link Predicate} predicate
     * @param value     the value to be used in an equality check with the extractor
     * @return a {@link TransformingIterable}
     */
    default <R> TransformingIterable<T> filter(final Function<T, R> extractor, final R value) {
        return filter(t -> Objects.equals(extractor.apply(t), value));
    }

    /**
     * Creates an {@link TransformingIterable} based on the specified elements.
     *
     * @param <T>      the type of the elements
     * @param elements the elements
     * @return a {@link TransformingIterable}
     */
    @SafeVarargs
    static <T> TransformingIterable<T> of(final T... elements) {
        if (elements == null) {
            return of((Iterable<T>) null);
        } else {
            final ArrayList<T> list = new ArrayList<>(elements.length);

            Collections.addAll(list, elements);

            return of(list);
        }
    }

    /**
     * Obtains a {@link TransformingIterable} based on the specified {@link Iterable}.
     *
     * @param iterable the {@link Iterable}
     * @param <T>      the type of elements returned by the iterator
     * @return a {@link TransformingIterable}
     */
    static <T> TransformingIterable<T> of(final Iterable<T> iterable) {

        if (iterable == null) {
            return of(Collections.emptyList());
        } else if (iterable instanceof TransformingIterable) {
            return (TransformingIterable<T>) iterable;
        } else {
            return iterable::iterator;
        }
    }
}
