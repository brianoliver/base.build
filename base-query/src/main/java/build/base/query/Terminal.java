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

import build.base.foundation.Lazy;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Provides a mechanism to obtain the result of a query for a {@link Class} being queried.
 *
 * @param <Q> the type of {@link Class} being queried
 * @author brian.oliver
 * @since Jun-2025
 */
public interface Terminal<Q, T extends Terminal<Q, T>> {

    /**
     * Specifies the {@link Scope} for querying, the default being {@link Scope#Direct}.
     *
     * @param scope the {@link Scope}
     * @return this {@link Terminal} to support fluent-style method invocation
     */
    T scope(Scope scope);

    /**
     * Obtains a {@link Stream} of {@link Object}s that match the query.
     *
     * @return a {@link Stream} of matching {@link Object}s
     */
    Stream<Q> findAll();

    /**
     * Obtains the first {@link Object} that matches the query.
     *
     * @return an {@link Optional} containing the first matching {@link Object}, otherwise {@link Optional#empty()}
     */
    default Optional<Q> findFirst() {
        return findAll()
            .findFirst();
    }

    /**
     * Obtains a {@link Object} that matches the query.
     *
     * @return an {@link Object} containing the matching {@link Object}, otherwise {@link Optional#empty()}
     */
    default Optional<Q> findAny() {
        return findAll()
            .findAny();
    }

    /**
     * Determines if any {@link Object} matches the query.
     *
     * @return {@code true} if any {@link Object} matches the query, otherwise {@code false}
     */
    default boolean anyMatch() {
        return findFirst()
            .isPresent();
    }

    /**
     * Determines if no {@link Object} matches the query.
     *
     * @return {@code true} if no {@link Object} matches the query, otherwise {@code false}
     */
    default boolean noneMatch() {
        return !anyMatch();
    }

    /**
     * Obtains one and only one {@link Object} that matches the query.
     *
     * @return the single matching {@link Object}
     * @throws IllegalStateException  if more than one {@link Object} matches the query
     * @throws NoSuchElementException if no {@link Object} matches the query
     */
    default Q get() {
        return findAll()
            .collect(Lazy.collector());
    }

    /**
     * Obtains one and only one {@link Object} that matches the query, or returns {@code null} if no match is found.
     *
     * @return the single matching {@link Object}, or {@code null} if no match is found
     */
    default Q getOrNull() {
        try {
            return get();
        } catch (final IllegalStateException | NoSuchElementException e) {
            return null;
        }
    }

    /**
     * Obtains one and only one {@link Object} that matches the query, or throws a {@link NoSuchElementException} if no
     * match is found.
     *
     * @return the single matching {@link Object}
     * @throws NoSuchElementException if no {@link Object} matches the query
     */
    default Q getOrThrow() {
        try {
            return get();
        } catch (final IllegalStateException | NoSuchElementException e) {
            throw new NoSuchElementException("No single matching value available satisfying the query");
        }
    }

    /**
     * Obtains the number of {@link Object}s that match the query.
     *
     * @return the number of matching {@link Object}s
     */
    default long count() {
        return findAll().count();
    }
}
