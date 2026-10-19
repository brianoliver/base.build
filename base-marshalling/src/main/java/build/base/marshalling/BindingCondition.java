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

import build.base.foundation.predicate.Predicates;

import java.util.function.Predicate;

/**
 * Specifies the type of {@link Binding} to produce for an {@link Object}.
 *
 * @param <T> the type of {@link Object} that is bound
 * @author brian.oliver
 * @since Aug-2025
 */
public interface BindingCondition<T> {

    /**
     * Creates a {@link Binding} using only the concrete {@link Class} of the {@link Object}.
     *
     * @return the {@link Binding}
     * @see BindingBuilder#to(Object)
     */
    Binding<T> withConcreteClass();

    /**
     * Creates a {@link Binding} for each of the interfaces transitively implemented by the {@link Object}, to the
     * {@link Object}.
     *
     * @return the {@link Binding}
     * @see BindingBuilder#to(Object)
     */
    default Binding<T> withAllInterfaces() {
        return where(Class::isInterface);
    }

    /**
     * Creates a {@link Binding} for each of the {@link Class}es and {@code interface}s transitively implemented by
     * the {@link Object}, to the {@link Object}, excluding the {@link Object} {@link Class}.
     *
     * @return the {@link Binding}
     * @see BindingBuilder#to(Object)
     */
    default Binding<T> universally() {
        return where(Predicates.always());
    }

    /**
     * Creates a {@link Binding} for each of the {@link Class}es and {@code interface}s transitively implemented by
     * of the {@link Object} that satisfy the specified {@link Predicate}, to the {@link Object}, excluding the
     * {@link Object} {@link Class}.
     *
     * @param predicate the {@link Predicate}
     * @return the {@link Binding}
     */
    Binding<T> where(Predicate<? super Class<?>> predicate);
}
