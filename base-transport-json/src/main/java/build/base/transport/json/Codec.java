package build.base.transport.json;

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

import build.base.json.JsonValue;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Parameter;

import java.lang.reflect.Type;

/**
 * A JSON <a href="https://en.wikipedia.org/wiki/Codec">Codec</a> for a specific type of value.
 *
 * @param <T> the type of value
 * @author brian.oliver
 * @since Nov-2024
 */
public interface Codec<T> {

    /**
     * Obtains the {@link Class} of value supported by the {@link Codec}.
     *
     * @return the {@link Class} of value
     */
    Class<?> codecClass();

    /**
     * Encodes the specified value as a {@link JsonValue}.
     *
     * @param transport  the {@link JsonTransport}
     * @param parameter  the {@link Parameter}
     * @param type       the actual {@link Type} being encoded at this point - the {@link Codec}'s own operand
     *                   type, which may be nested inside {@code parameter}'s declared type (e.g. the {@code T}
     *                   of an {@code Optional<T>} or {@code Stream<T>} a {@link Codec} for {@code T} is invoked
     *                   from within) - use this, not {@code parameter.type()}, to derive any further nested
     *                   type arguments this {@link Codec} needs
     * @param value      the value to encode
     * @param marshaller the {@link Marshaller}
     * @return the encoded {@link JsonValue}
     */
    JsonValue encode(JsonTransport transport,
                     Parameter parameter,
                     Type type,
                     T value,
                     Marshaller marshaller);

    /**
     * Decodes a value of type {@code T} from the provided {@link JsonValue}.
     *
     * @param transport  the {@link JsonTransport}
     * @param parameter  the {@link Parameter}
     * @param type       the actual {@link Type} being decoded at this point - see {@link #encode} for why this,
     *                   not {@code parameter.type()}, is what a {@link Codec} needing a nested type argument
     *                   should derive it from
     * @param value      the {@link JsonValue} to decode (never absent; may be {@link build.base.json.JsonNull})
     * @param marshaller the {@link Marshaller}
     * @return the decoded value
     */
    T decode(JsonTransport transport,
             Parameter parameter,
             Type type,
             JsonValue value,
             Marshaller marshaller);
}
