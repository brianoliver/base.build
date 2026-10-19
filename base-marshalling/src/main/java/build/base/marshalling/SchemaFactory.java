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

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A factory for {@link Schema}s for <i>marshallable</i> {@link Class}es.
 *
 * @author brian.oliver
 * @since Nov-2024
 */
public interface SchemaFactory {

    /**
     * Registers the specified {@link Class} with {@link Marshal} and {@link Unmarshal} methods from which to generate
     * {@link Schema}s.
     * <p>
     * Should the {@link Class} already be registered, the request is ignored.
     *
     * @param marshallableClass the <i>marshallable</i> {@link Class} to register
     * @param <T>               the type of <i>marshallable</i> {@link Class}
     */
    default <T> void register(final Class<T> marshallableClass) {

        Objects.requireNonNull(marshallableClass, "The marshallable class must not be null");

        final var lookup = MethodHandles.lookup().in(marshallableClass);
        register(marshallableClass, lookup);
    }

    /**
     * Registers the specified {@link Class} with an associated {@link MethodHandles.Lookup} to lookup
     * {@link Marshal} and {@link Unmarshal} methods from which to generate {@link Schema}s.
     * <p>
     * Should the {@link Class} already be registered, the request is ignored.
     *
     * @param marshallableClass the <i>marshallable</i> {@link Class} to register
     * @param lookup            the {@link MethodHandles.Lookup} for the {@link Class}
     * @param <T>               the type of <i>marshallable</i> {@link Class}
     */
    <T> void register(Class<T> marshallableClass, MethodHandles.Lookup lookup);

    /**
     * Registers an enum {@link Class} as marshallable, using its {@link Enum#name()} for serialization
     * and {@link Enum#valueOf(Class, String)} for deserialization.
     * <p>
     * Enums cannot declare {@link Marshal}/{@link Unmarshal} methods and must be explicitly opted in
     * via this method to be marshallable.
     * <p>
     * Should the enum already be registered, the request is ignored.
     *
     * @param enumClass the enum {@link Class} to register
     * @param <E>       the enum type
     */
    <E extends Enum<E>> void registerEnum(Class<E> enumClass);

    /**
     * Obtains the {@link Schema} to <i>marshal</i> the specified {@link Class}.
     *
     * @param marshallableClass the <i>marshallable</i> {@link Class}
     * @param <T>               the type of <i>marshallable</i> {@link Class}
     * @return the {@link Optional} {@link Schema} or {@link Optional#empty()} if no such registration exists
     */
    <T> Optional<Schema<T>> getMarshallingSchema(Class<T> marshallableClass);

    /**
     * Obtains the {@link Schema} that can be used to unmarshal the specified {@link Class}.
     *
     * @param marshallableClass the <i>marshallable</i> {@link Class}
     * @param <T>               the type of <i>marshallable</i> {@link Class}
     * @return the {@link Stream} of {@link Schema} for unmarshalling the specified {@link Class}
     */
    <T> Stream<Schema<T>> getUnmarshallingSchemas(Class<T> marshallableClass);

    /**
     * Determine if the specified {@link Class} is marshallable.
     *
     * @param marshallableClass the {@link Class} being queried
     * @return {@code true} when the specified {@link Class} is marshallable, {@code false} otherwise
     */
    boolean isMarshallable(Class<?> marshallableClass);

    /**
     * Obtains a new {@link Marshaller} based on the {@link SchemaFactory}.
     *
     * @return a new {@link Marshaller}
     */
    Marshaller newMarshaller();
}
