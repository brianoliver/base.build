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
 * An {@link Option} that may be composed with another {@link Option} of the same type to produce a new
 * {@link Option}, representing the composition of both {@link Option}s.
 *
 * @param <T> the type of {@link Option} into which the {@link Option} will be composed
 * @author brian.oliver
 * @since Nov-2017
 */
public interface ComposedOption<T extends ComposedOption<T>>
    extends Option {

    /**
     * Composes this {@link Option} with another {@link Option} to produce a new {@link Option}
     * of the same type.
     *
     * @param other the {@link Option} to compose with this {@link Option} to produce a new {@link Option}
     * @return a newly composed {@link Option}
     */
    T compose(T other);
}
