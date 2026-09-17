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
import build.base.json.JsonArray;
import build.base.json.JsonNull;
import build.base.json.JsonValue;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Parameter;
import build.base.transport.json.ConditionalCodec;
import build.base.transport.json.JsonTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

/**
 * A {@link ConditionalCodec} for {@link Optional} values. Encoded as a JSON array: {@code []} for empty,
 * {@code [element]} for present.
 *
 * @author brian.oliver
 * @since Nov-2024
 */
@SuppressWarnings("rawtypes")
public class OptionalCodec
    implements ConditionalCodec<Optional<?>> {

    @Override
    public Class<? extends Optional> codecClass() {
        return Optional.class;
    }

    @Override
    public boolean ignore(final Optional<?> optional) {
        return optional != null && optional.isEmpty();
    }

    @Override
    public Optional<?> defaultValue() {
        return Optional.empty();
    }

    @Override
    public JsonValue encode(final JsonTransport transport,
                            final Parameter parameter,
                            final Type type,
                            final Optional<?> optional,
                            final Marshaller marshaller) {

        if (optional == null) {
            return JsonNull.INSTANCE;
        }
        if (optional.isEmpty()) {
            return JsonArray.of(List.of());
        }
        final var elementType = Introspection.getParameterType(type)
            .orElseThrow(() -> new IllegalStateException(
                "Failed to determine Optional<T> element type for [" + parameter.name() + "]"));
        return JsonArray.of(List.of(transport.encode(parameter, elementType, optional.get(), marshaller)));
    }

    @Override
    public Optional<?> decode(final JsonTransport transport,
                              final Parameter parameter,
                              final Type type,
                              final JsonValue value,
                              final Marshaller marshaller) {

        if (value instanceof JsonNull) {
            return null;
        }
        final var array = value.asArray();
        if (array.values().isEmpty()) {
            return Optional.empty();
        }
        final var elementType = Introspection.getParameterType(type)
            .orElseThrow(() -> new IllegalStateException(
                "Failed to determine Optional<T> element type for [" + parameter.name() + "]"));
        return Optional.ofNullable(transport.decode(parameter, elementType, array.element(0), marshaller));
    }
}
