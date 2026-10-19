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

import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * Provides a mechanism to match zero or more {@link Object}s of a specific {@link Class}.
 *
 * @param <Q> the type of {@link Class} of value
 * @author brian.oliver
 * @since Jun-2025
 */
public interface Match<Q>
    extends Terminal<Q, Match<Q>> {

    /**
     * Specifies a {@link Function} to extract {@link Indexable} {@link Method} values from {@link Object}s
     * for the purpose of querying and filtering said {@link Object}s with {@link Condition}s.
     *
     * @param <V>      the type of value
     * @param function the {@link Function} to extract a value from a {@link Object}s
     * @return a {@link Condition} that can be used to further refine the query
     */
    <V> Condition<Q, V> where(Function<? super Q, V> function);
}
