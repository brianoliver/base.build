package build.base.query;

/*-
 * #%L
 * base.build Query
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

import java.util.function.Function;

/**
 * A facility upon which queries and matching may be performed based on copies or references to underlying indexed
 * values and/or {@link Object}s directly added to the {@link Index}.
 *
 * @author brian.oliver
 * @since Aug-2025
 * @see Queryable
 */
public interface Index extends Queryable {

    /**
     * Indexes the specified {@link Object} using its defined {@link Indexable} {@link Function}s so that it may be
     * queried and matched.
     * <p>
     * {@code public} {@code static} {@code final} {@link Function}s annotated with {@link Indexable} will be
     * considered for indexing to facilitate efficient querying and matching.
     *
     * @param object the {@link Object}
     * @see #unindex(Object)
     * @throws UnsupportedOperationException when indexing fails
     */
    void index(Object object) throws UnsupportedOperationException;

    /**
     * Removes the previously indexed {@link Object} so that it may no longer be queried or matched using the
     * {@link Index}.
     *
     * @param object the {@link Object}
     * @see #index(Object)
     * @throws UnsupportedOperationException when unindexing fails
     */
    void unindex(Object object);

    /**
     * Re-indexes the specified {@link Object} by first unindexing it and then re-indexing it.
     * <p>
     * Use this after mutating an object whose indexed values may have changed, so that the
     * forward and reverse maps reflect the new state.
     *
     * @param object the {@link Object} to re-index
     * @see #index(Object)
     * @see #unindex(Object)
     */
    default void reindex(final Object object) {
        unindex(object);
        index(object);
    }

    /**
     * Partially re-indexes the specified {@link Object} by updating only the index entries for
     * {@link Indexable} {@link Dynamic} {@link java.util.function.Function} fields.
     * <p>
     * Use this after mutating state that is reflected by {@link Dynamic}-annotated functions, when stable
     * (non-{@link Dynamic}) indexed values are known not to have changed. Stable index entries and
     * class-level {@link Indexable} entries are left untouched.
     * <p>
     * Implementations that do not distinguish dynamic from stable fields fall back to a full {@link #reindex}.
     *
     * @param object the {@link Object} to partially re-index
     * @see #reindex(Object)
     * @see Dynamic
     */
    default void reindexDynamic(final Object object) {
        reindex(object);
    }

    /**
     * Adds the specified value {@link Object} to the index so that is may be returned when the specified {@link Class}
     * is queried or matched.
     *
     * @param <T>        the type of the value {@link Object}
     * @param valueClass the {@link Class} of the value {@link Object}
     * @param value      the value {@link Object}
     * @see #remove(Class, Object)
     */
    <T> void add(Class<T> valueClass, T value);

    /**
     * Removes the specified value {@link Object} from the index so that it may no longer be returned when the
     * specified {@link Class} is queried or matched.
     *
     * @param <T>        the type of the value {@link Object}
     * @param valueClass the {@link Class} of the value {@link Object}
     * @param value      the value {@link Object}
     */
    <T> void remove(Class<T> valueClass, T value);
}
