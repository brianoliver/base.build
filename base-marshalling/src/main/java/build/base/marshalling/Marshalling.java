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

/**
 * Provides <a href="https://en.wikipedia.org/wiki/Marshalling_(computer_science)">Marshalling</a> information for
 * {@link Marshaller}s, including the ability to determine <i>marshable</i> {@link Class}es, those that define
 * {@link Marshal} and {@link Unmarshal} methods, or register themselves with
 *
 * @author brian.oliver
 * @since Nov-2024
 */
public interface Marshalling {

    /**
     * The global {@link SchemaFactory}.
     */
    SchemaFactory GLOBAL_SCHEMA_FACTORY = new ConcurrentSchemaFactory();

    /**
     * Registers the specified {@link Class} with its associated {@link MethodHandles.Lookup} with the
     * {@link #globalSchemaFactory()} {@link SchemaFactory#register(Class, MethodHandles.Lookup)} to enable lookup of
     * {@link Marshal} and {@link Unmarshal} methods with which to generate {@link Schema}s.
     * <p>
     * Should the {@link Class} already be registered, the request is ignored.
     *
     * @param registrationClass the {@link Class} to register
     * @param lookup            the {@link MethodHandles.Lookup} for the {@link Class}
     * @param <T>               the type of <i>marshallable</i> {@link Class}
     */
    static <T> void register(final Class<T> registrationClass, final MethodHandles.Lookup lookup) {
        GLOBAL_SCHEMA_FACTORY.register(registrationClass, lookup);
    }

    /**
     * Registers the specified {@link Class} with {@link Marshal} and {@link Unmarshal} methods from which to generate
     * {@link Schema}s with {@link #globalSchemaFactory()} {@link SchemaFactory#register(Class)}.
     * <p>
     * Should the {@link Class} already be registered, the request is ignored.
     *
     * @param marshallableClass the <i>marshallable</i> {@link Class} to register
     * @param <T>               the type of <i>marshallable</i> {@link Class}
     */
    static <T> void register(final Class<T> marshallableClass) {
        GLOBAL_SCHEMA_FACTORY.register(marshallableClass);
    }

    /**
     * Registers an enum {@link Class} as marshallable, using its {@link Enum#name()} for serialization
     * and {@link Enum#valueOf(Class, String)} for deserialization.
     *
     * @param enumClass the enum {@link Class} to register
     * @param <E>       the enum type
     */
    static <E extends Enum<E>> void registerEnum(final Class<E> enumClass) {
        GLOBAL_SCHEMA_FACTORY.registerEnum(enumClass);
    }

    /**
     * Obtains a new {@link Marshaller} based from the {@link #globalSchemaFactory()}.
     *
     * @return a new {@link Marshaller}
     */
    static Marshaller newMarshaller() {
        return globalSchemaFactory().newMarshaller();
    }

    /**
     * Obtains the global {@link SchemaFactory}.
     *
     * @return the global {@link SchemaFactory}
     */
    static SchemaFactory globalSchemaFactory() {
        return GLOBAL_SCHEMA_FACTORY;
    }
}
