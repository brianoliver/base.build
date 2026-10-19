package build.base.transport.transformer;

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
import build.base.foundation.Strings;
import build.base.marshalling.Marshaller;
import build.base.transport.Transformer;

import java.lang.reflect.Type;
import java.util.Optional;

/**
 * A {@link Transformer} for {@link Enum} types, transforming them to and from {@link String}s.
 *
 * @param <T> the type of {@link Optional} value
 * @author brian.oliver
 * @since Nov-2024
 */
public class EnumTransformer<T extends Enum<T>>
    implements Transformer<Enum<T>, String> {

    @Override
    public Class<?> sourceClass() {
        return Enum.class;
    }

    @Override
    public Class<?> targetClass() {
        return String.class;
    }

    @Override
    public String transform(final Marshaller marshaller,
                            final Enum<T> value) {

        return value == null ? null : value.name();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Enum<T> reform(final Marshaller marshaller,
                       final Type type,
                       final String string) {

        final var enumClass = (Class<T>) Introspection.getClassFromType(type)
            .filter(Class::isEnum)
            .orElseThrow(() -> new ClassCastException("The specified type [" + type + "] is not an Enum<?>"));

        return Strings.isEmpty(string)
            ? null
            : Enum.valueOf(enumClass, string);
    }
}
