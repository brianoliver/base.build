package build.base.configuration;

/*-
 * #%L
 * base.build Configuration
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

/**
 * Provides access to the {@link ConfigurationBuilder} and thus configuration {@link Option}s for something that
 * is configurable.
 *
 * @author brian.oliver
 * @since Dec-2024
 */
public interface Configurable {

    /**
     * Obtains the {@link ConfigurationBuilder} allowing configuration of {@link Option}s.
     *
     * @return the {@link ConfigurationBuilder}
     */
    ConfigurationBuilder options();

    /**
     * Determines if the {@link Configurable} has one or more {@link Option}s defined.
     *
     * @return {@code true} if the {@link Configurable} has any {@link Option}s defined, {@code false} otherwise
     */
    default boolean hasOptions() {
        return !options().isEmpty();
    }

}
