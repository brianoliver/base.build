package build.base.marshalling;

/*-
 * #%L
 * base.build Marshalling
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

import build.base.foundation.stream.Streamable;

import java.util.Optional;

/**
 * Provides runtime type information concerning the structure of a <i>specific instance</i> of a {@link Marshalled}
 * type.
 *
 * @param <T> the type for which the {@link Schema} was created
 * @author brian.oliver
 * @since Nov-2024
 */
public interface Schema<T> {

    /**
     * The {@link Class} of <i>marshallable</i> type for which the {@link Schema} was established.
     *
     * @return the owner {@link Class}
     */
    Class<T> owner();

    /**
     * The {@link Parameter}s defined by the {@link Schema}.
     *
     * @return the {@link Streamable} of {@link Parameter}s defined by the {@link Schema}
     */
    Streamable<Parameter> parameters();

    /**
     * Obtains the {@link Parameter} with the specified name.
     *
     * @param name the name of the {@link Parameter}
     * @return the {@link Optional} {@link Parameter} or {@link Optional#empty()} if not defined
     */
    Optional<Parameter> getParameter(String name);

    /**
     * The {@link Bound} {@link Dependency}s defined by the {@link Schema}.
     *
     * @return the {@link Streamable} of {@link Bound} {@link Dependency}s
     */
    Streamable<Dependency> dependencies();
}
