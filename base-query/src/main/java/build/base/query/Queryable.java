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

import java.util.Optional;
import java.util.function.Function;

/**
 * A facility to create and perform queries to match {@link Object}s of a specific {@link Class} made available through
 * the implementing {@link Queryable}.
 *
 * @author brian.oliver
 * @since Aug-2025
 */
public interface Queryable {

    /**
     * Defines a query to match the specified {@link Class} of {@link Object}.
     *
     * @param <M>            the type of {@link Object} upon with to perform the match
     * @param matchableClass the {@link Class} of {@link Object} to match
     * @return a {@link Match} for the specified {@link Class} of {@link Object}
     */
    <M> Match<M> match(Class<M> matchableClass);

    /**
     * Attempts to obtain the first {@link Object} of the specified {@link Class} that matches the specified key
     * extracted from the {@link Object} using the specified extractor {@link Function}.
     *
     * @param <M>            the type of {@link Object} to match
     * @param <K>            the type of {@link Object} key to match
     * @param matchableClass the {@link Class} of {@link Object} to match
     * @param extractor      the extractor {@link Function}
     * @param key            the key
     * @return the first matching {@link Object} or {@link Optional#empty()}
     */
    default <M, K> Optional<M> get(final Class<M> matchableClass,
                                   final Function<M, K> extractor,
                                   final K key) {

        return match(matchableClass)
            .where(extractor)
            .isEqualTo(key)
            .findFirst();
    }
}
