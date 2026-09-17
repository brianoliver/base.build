package build.base.transport.json.codec;

/*-
 * #%L
 * base.build Transport (JSON)
 * %%
 * Copyright (C) 2026 Workday Inc
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
import build.base.foundation.Lazy;
import build.base.json.JsonArray;
import build.base.json.JsonNull;
import build.base.json.JsonValue;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Parameter;
import build.base.transport.json.ConditionalCodec;
import build.base.transport.json.JsonTransport;

import java.lang.reflect.Type;
import java.util.List;

/**
 * A {@link ConditionalCodec} for {@link Lazy} values. Encoded as a JSON array: {@code []} for empty,
 * {@code [element]} for present - mirroring {@link OptionalCodec}, since a {@link Lazy} is likewise either
 * empty or holds a single value.
 *
 * @param <T> the type of the {@link Lazy} value
 * @author reed.vonredwitz
 * @since Sep-2026
 */
public class LazyCodec<T>
    implements ConditionalCodec<Lazy<T>> {

    @Override
    public Class<?> codecClass() {
        return Lazy.class;
    }

    @Override
    public boolean ignore(final Lazy<T> lazy) {
        return lazy != null && lazy.isEmpty();
    }

    @Override
    public Lazy<T> defaultValue() {
        return Lazy.empty();
    }

    @Override
    public JsonValue encode(final JsonTransport transport,
                            final Parameter parameter,
                            final Type type,
                            final Lazy<T> lazy,
                            final Marshaller marshaller) {

        if (lazy == null) {
            return JsonNull.INSTANCE;
        }
        if (lazy.isEmpty()) {
            return JsonArray.of(List.of());
        }
        final var elementType = Introspection.getParameterType(type)
            .orElseThrow(() -> new IllegalStateException(
                "Failed to determine Lazy<T> element type for [" + parameter.name() + "]"));
        return JsonArray.of(List.of(transport.encode(parameter, elementType, lazy.get(), marshaller)));
    }

    @Override
    public Lazy<T> decode(final JsonTransport transport,
                          final Parameter parameter,
                          final Type type,
                          final JsonValue value,
                          final Marshaller marshaller) {

        if (value instanceof JsonNull) {
            return null;
        }
        final var array = value.asArray();
        if (array.values().isEmpty()) {
            return Lazy.empty();
        }
        final var elementType = Introspection.getParameterType(type)
            .orElseThrow(() -> new IllegalStateException(
                "Failed to determine Lazy<T> element type for [" + parameter.name() + "]"));
        return Lazy.ofNullable(transport.decode(parameter, elementType, array.element(0), marshaller));
    }
}
