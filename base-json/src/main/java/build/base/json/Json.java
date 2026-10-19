package build.base.json;

/*-
 * #%L
 * base.build JSON
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

import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * Entry point for parsing JSON text into {@link JsonValue} trees and writing them back out.
 * <p>
 * For a {@link String} result from an existing {@link JsonValue}, use {@link JsonValue#toJsonString()} instead.
 */
public final class Json {

    private Json() {
    }

    /**
     * Parses the given JSON string and returns the corresponding {@link JsonValue}.
     *
     * @param input the JSON text
     * @return the parsed value
     * @throws JsonParseException if the input is not valid JSON
     */
    public static JsonValue parse(final String input) {
        Objects.requireNonNull(input, "input");
        return JsonReader.read(input);
    }

    /**
     * Decodes the given UTF-8 bytes and parses them as JSON.
     *
     * @param input the UTF-8 encoded JSON bytes
     * @return the parsed value
     * @throws JsonParseException if the bytes are not valid UTF-8 or not valid JSON
     */
    public static JsonValue parse(final byte[] input) {
        Objects.requireNonNull(input, "input");
        final var decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return JsonReader.read(decoder.decode(ByteBuffer.wrap(input)).toString());
        } catch (final CharacterCodingException e) {
            throw new JsonParseException("Invalid UTF-8 encoding: " + e.getMessage(), 0, 0, 0, null);
        }
    }

    /**
     * Reads all characters from the given {@link Reader} and parses them as JSON.
     *
     * @param reader the source of JSON text
     * @return the parsed value
     * @throws JsonParseException if the input is not valid JSON
     */
    public static JsonValue parse(final Reader reader) {
        Objects.requireNonNull(reader, "reader");
        return JsonReader.read(reader);
    }

    /**
     * Writes the given {@link JsonValue} to the provided {@link Writer} using compact format.
     *
     * @param value  the value to write
     * @param writer the destination
     */
    public static void write(final JsonValue value, final Writer writer) {
        write(value, writer, JsonFormat.COMPACT);
    }

    /**
     * Writes the given {@link JsonValue} to the provided {@link Writer} using the specified format.
     *
     * @param value  the value to write
     * @param writer the destination
     * @param format the output format
     */
    public static void write(final JsonValue value, final Writer writer, final JsonFormat format) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(writer, "writer");
        Objects.requireNonNull(format, "format");
        JsonWriter.write(value, writer, format);
    }

    /**
     * Wraps the given Java value as a {@link JsonValue}.
     * <p>
     * Accepts {@code null} (→ {@link JsonNull#INSTANCE}), {@link String} (→ {@link JsonString}),
     * {@link Number} (→ {@link JsonNumber}), and {@link Boolean} (→ {@link JsonBoolean}).
     *
     * @param value the Java value to wrap
     * @return the corresponding {@link JsonValue}
     * @throws IllegalArgumentException if the value type is not supported
     */
    public static JsonValue of(final Object value) {
        if (value == null) {
            return JsonNull.INSTANCE;
        }
        if (value instanceof String s) {
            return JsonString.of(s);
        }
        if (value instanceof Number n) {
            return JsonNumber.of(n);
        }
        if (value instanceof Boolean b) {
            return JsonBoolean.of(b);
        }
        throw new IllegalArgumentException("Cannot wrap as JsonValue: " + value.getClass());
    }

    /**
     * Creates a {@link JsonObject} from the given map, defensively copying the entries.
     *
     * @param entries the members
     * @return the object
     */
    public static JsonObject object(final Map<String, JsonValue> entries) {
        return JsonObject.of(entries);
    }

    /**
     * Creates a {@link JsonArray} from the given elements.
     *
     * @param elements the array elements
     * @return the array
     */
    public static JsonArray array(final JsonValue... elements) {
        return JsonArray.of(Arrays.asList(elements));
    }

    /**
     * Creates a {@link JsonArray} from the given iterable of elements.
     *
     * @param elements the array elements
     * @return the array
     */
    public static JsonArray array(final Iterable<? extends JsonValue> elements) {
        Objects.requireNonNull(elements, "elements");
        final var list = new java.util.ArrayList<JsonValue>();
        for (final var element : elements) {
            list.add(element);
        }
        return JsonArray.of(list);
    }
}
