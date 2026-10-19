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

import build.base.foundation.Introspection;
import build.base.marshalling.Marshal;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Unmarshal;

import java.lang.reflect.Type;

/**
 * Defines a symmetric mechanism to transform a {@link Class} that is not naturally marshallable, because it can't
 * directly use the {@code build.base.marshalling} facilities, including the {@link Marshal} and {@link Unmarshal}
 * annotations, into one that may be used for marshalling and unmarshalling.
 * <p>
 * A good example of these types of {@link Class}es are those defined by the Java Platform itself or third-party
 * libraries that similarly don't or can't depend upon the {@code base.build.marshalling} module, but whose types may
 * feasibility be transformed into some other type that can support marshalling.
 *
 * @param <X> the type to be transformed
 * @param <Y> the type of the transformed type
 * @author brian.oliver
 * @since Nov-2024
 */
public interface Transformer<X, Y> {

    /**
     * Determines if the specified {@link Type} is transformable with this {@link Transformer}.
     *
     * @param type the {@link Type}
     * @return {@code true} if the {@link Type} of value should use this {@link Transformer}, {@code false} otherwise
     */
    default boolean isTransformable(final Type type) {
        return Introspection.getClassFromType(type)
            .filter(classType -> sourceClass().isAssignableFrom(classType))
            .isPresent();
    }

    /**
     * The {@link Class} of the source type
     *
     * @return the {@link Class} of the source type
     */
    Class<?> sourceClass();

    /**
     * The {@link Class} of the target type
     *
     * @return the {@link Class} of the target type
     */
    Class<?> targetClass();

    /**
     * Transforms the specified unmarshalled value into another form that could be marshalled.
     *
     * @param marshaller the {@link Marshaller} to use for additional marshaling
     * @param x          the value to transform
     * @return a transformed value
     */
    Y transform(Marshaller marshaller,
                X x);

    /**
     * Reforms a transformed marshalled value into it's natural unmarshalled representation for the specified
     * {@link Type}.
     *
     * @param marshaller the {@link Marshaller} to use for unmarshalling
     * @param type       the {@link Type} into which to reform the transformed value
     * @param y          the transformed value
     * @return the natural unmarshalled representation of the transformed value
     */
    X reform(Marshaller marshaller,
             Type type,
             Y y);
}
