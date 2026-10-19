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
 * A special {@link Option} that is identified by key in addition to its type,
 * thus allowing multiple instances of the same type to be managed by an {@link Configuration}.
 *
 * @param <K> the type of key
 * @author brian.oliver
 * @since Nov-2017
 */
public interface MappedOption<K>
    extends Option {

    /**
     * Obtains the key for identifying a {@link MappedOption} instance.
     *
     * @return the key
     */
    K key();
}
