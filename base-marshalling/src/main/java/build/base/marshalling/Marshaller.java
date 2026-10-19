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

import build.base.foundation.Capture;
import build.base.foundation.Introspection;

import java.util.function.Predicate;

/**
 * Provides the ability to <a href="https://en.wikipedia.org/wiki/Marshalling_(computer_science)">Marshal</a> and
 * unmarshal {@link Object}s.
 *
 * @author brian.oliver
 * @see Marshalled
 * @see Marshal
 * @see Unmarshal
 * @since Nov-2024
 */
public interface Marshaller {

    /**
     * Determine if the specified {@link Class} is marshallable.
     *
     * @param marshallableClass the {@link Class} which may or not be marshallable
     * @return {@code true} when the specified {@link Class} is marshallable, {@code false} otherwise
     */
    boolean isMarshallable(Class<?> marshallableClass);

    /**
     * Marshal the specified {@link Object} into its {@link Marshalled} representation.
     *
     * @param object the {@code nullable} {@link Object}
     * @param <T>    the type of {@link Object}
     * @return a {@link Marshalled}
     * @see Marshal
     */
    <T> Marshalled<T> marshal(T object);

    /**
     * Unmarshal the specified {@link Marshalled} representation into an equivalent {@link Object}.
     *
     * @param marshalled the {@link Marshalled}
     * @param <T>        the type of {@link Object}
     * @return a {@code nullable} {@link Object}
     * @see Unmarshal
     */
    <T> T unmarshal(Marshalled<T> marshalled);

    /**
     * Creates a new fluent {@link BindingBuilder} for the specified {@link Class} of {@link Binding},
     * allowing definition, configuration and construction of a {@link Binding} for {@code this} {@link Marshaller}.
     *
     * @param <T>          the type of {@link Binding}
     * @param bindingClass the {@link Class} for which to create a {@link Binding}
     * @return a new {@link BindingBuilder}
     */
    <T> BindingBuilder<T> bind(Class<T> bindingClass);

    /**
     * Creates a new fluent {@link BindingCondition} for the specified non-{@code null} value.
     *
     * @param <T>   the type of value
     * @param value the non-{@code null} value
     * @return a {@link BindingCondition} from which to create the {@link Binding}.
     */
    default <T> BindingCondition<T> bind(final T value) {
        if (value == null) {
            throw new IllegalArgumentException("The value must not be null");
        }

        @SuppressWarnings("unchecked") final Class<T> valueClass = (Class<T>) value.getClass();

        return new BindingCondition<>() {
            @Override
            public Binding<T> withConcreteClass() {
                return bind(valueClass).to(value);
            }

            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public Binding<T> where(final Predicate<? super Class<?>> predicate) {
                final var capture = Capture.<Binding<T>>empty();

                // transitively include the interfaces that satisfy the specified predicate
                Introspection.getAllInterfaces(valueClass)
                    .filter(predicate)
                    .map(interfaceClass -> bind((Class) interfaceClass).to(interfaceClass.cast(value)))
                    .forEach(capture::setIfAbsent);

                // transitively include the classes and superclasses that satisfy the specified predicate
                Class<?> current = valueClass;
                while (current != Object.class) {
                    if (predicate.test(current)) {
                        capture.setIfAbsent(bind((Class) current).to((Object) value));
                    }

                    current = current.getSuperclass();
                }

                return capture.orElseThrow(() -> new IllegalArgumentException("The class [" + valueClass.getName() + "] does not satisfy the predicate [" + predicate + "]"));
            }
        };
    }
}
