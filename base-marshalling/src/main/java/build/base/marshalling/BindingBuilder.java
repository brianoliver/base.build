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

import java.util.function.Supplier;

/**
 * A fluent builder for {@link Binding}s.
 *
 * @param <T> the type of {@link Object} that is bound
 * @author brian.oliver
 * @since Dec-2024
 */
public interface BindingBuilder<T> {

    /**
     * Creates a {@link Binding} of the specified {@link Dependency} value.
     *
     * @param value the value
     * @return the newly created {@link Binding}
     */
    Binding<T> to(T value);

    /**
     * Creates a {@link Binding} that uses the specified {@link Supplier} for acquiring {@link Dependency} values.
     *
     * @param supplier the {@link Supplier}
     * @return a newly created {@link Binding}
     */
    Binding<T> to(Supplier<T> supplier);
}
