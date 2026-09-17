package build.base.transport.json.codec;

/*-
 * #%L
 * base.build Transport (JSON)
 * %%
 * Copyright (C) 2025 Workday Inc
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
import build.base.foundation.stream.Streamable;
import build.base.json.JsonArray;
import build.base.json.JsonNull;
import build.base.json.JsonValue;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Parameter;
import build.base.transport.json.ConditionalCodec;
import build.base.transport.json.JsonTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * A {@link ConditionalCodec} for {@link Streamable} values. Encoded as a JSON array.
 *
 * @author brian.oliver
 * @since Nov-2024
 */
public class StreamableCodec<T>
    implements ConditionalCodec<Streamable<T>> {

    @Override
    public Class<?> codecClass() {
        return Streamable.class;
    }

    @Override
    public boolean ignore(final Streamable<T> streamable) {
        return streamable != null && streamable.isEmpty();
    }

    @Override
    public Streamable<T> defaultValue() {
        return Streamable.empty();
    }

    @Override
    public JsonValue encode(final JsonTransport transport,
                            final Parameter parameter,
                            final Type type,
                            final Streamable<T> streamable,
                            final Marshaller marshaller) {

        if (streamable == null) {
            return JsonNull.INSTANCE;
        }
        final var elementType = Introspection.getParameterType(type)
            .orElseThrow(() -> new IllegalStateException(
                "Failed to determine Streamable<T> element type for [" + parameter.name() + "]"));
        final var elements = new ArrayList<JsonValue>();
        for (final var element : streamable) {
            if (element != null) {
                elements.add(transport.encode(parameter, elementType, element, marshaller));
            }
        }
        return JsonArray.of(elements);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Streamable<T> decode(final JsonTransport transport,
                                final Parameter parameter,
                                final Type type,
                                final JsonValue value,
                                final Marshaller marshaller) {

        if (value instanceof JsonNull) {
            return defaultValue();
        }
        final var elementType = Introspection.getParameterType(type)
            .orElseThrow(() -> new IllegalStateException(
                "Failed to determine Streamable<T> element type for [" + parameter.name() + "]"));
        final var elements = new ArrayList<T>();
        for (final var element : value.asArray().values()) {
            elements.add((T) transport.decode(parameter, elementType, element, marshaller));
        }
        return Streamable.of(elements);
    }
}
