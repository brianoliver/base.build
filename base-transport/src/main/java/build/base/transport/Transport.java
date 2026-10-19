package build.base.transport;

/*-
 * #%L
 * base.build Transport
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

import build.base.marshalling.Marshalled;
import build.base.marshalling.SchemaFactory;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A facility to support <i>transporting</i> {@link Marshalled} {@link Object}s.
 *
 * @author brian.oliver
 * @since Nov-2024
 */
public interface Transport {

    /**
     * Registers a {@link Transformer} for types that are naturally unmarshallable.
     *
     * @param transformer the {@link Transformer}
     * @param <X>         the type that is naturally unmarshallable
     * @param <Y>         a type that is marshalled or marshallable
     * @return this {@link SchemaFactory} to permit fluent-style method invocation
     */
    <X, Y> Transport register(Transformer<X, Y> transformer);

    /**
     * Obtains the currently registered {@link Transformer}s.
     *
     * @return a {@link Stream} of {@link Transformer}
     */
    Stream<Transformer<?, ?>> transformers();

    /**
     * Obtains the {@link Transformer} for the specified {@link Type}.
     *
     * @param <X>  the type of value to be transformed
     * @param <Y>  the transformed type of value
     * @param type the {@link Type}
     * @return the {@link Optional} {@link Transformer} or {@link Optional#empty()} if currently transformable
     */
    @SuppressWarnings("unchecked")
    default <X, Y> Optional<Transformer<X, Y>> getTransformer(final Type type) {
        return transformers()
            .filter(transformer -> transformer.isTransformable(type))
            .map(transformer -> (Transformer<X, Y>) transformer)
            .findFirst();
    }
}
