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

/**
 * A sealed representation of any JSON value.
 * <p>
 * Mirrors the type surface proposed in JEP draft 8344154. Use pattern matching over the six permitted subtypes for
 * exhaustive handling:
 * <pre>{@code
 * switch (value) {
 *     case JsonString  s -> ...
 *     case JsonNumber  n -> ...
 *     case JsonBoolean b -> ...
 *     case JsonObject  o -> ...
 *     case JsonArray   a -> ...
 *     case JsonNull    _ -> ...
 * }
 * }</pre>
 */
public sealed interface JsonValue
    permits JsonString, JsonNumber, JsonBoolean, JsonObject, JsonArray, JsonNull {

    default JsonObject asObject() {
        throw JsonAssertionException.notA("object", this);
    }

    default JsonArray asArray() {
        throw JsonAssertionException.notA("array", this);
    }

    default JsonString asString() {
        throw JsonAssertionException.notA("string", this);
    }

    default JsonNumber asNumber() {
        throw JsonAssertionException.notA("number", this);
    }

    default JsonBoolean asBoolean() {
        throw JsonAssertionException.notA("boolean", this);
    }

    default JsonValue get(final String name) {
        throw JsonAssertionException.notA("object", this);
    }

    default String getString(final String name) {
        return get(name).asString().value();
    }

    default JsonValue element(final int index) {
        throw JsonAssertionException.notA("array", this);
    }

    default String toJsonString() {
        return toJsonString(JsonFormat.COMPACT);
    }

    default String toJsonString(final JsonFormat format) {
        final var sb = new StringBuilder();
        JsonWriter.write(this, sb, format);
        return sb.toString();
    }
}
