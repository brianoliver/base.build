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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A JSON object value — an ordered collection of named members that preserves insertion order.
 */
public sealed interface JsonObject extends JsonValue permits JsonObjectImpl {

    Map<String, JsonValue> members();

    static JsonObject of(final Map<String, JsonValue> members) {
        final var copy = new LinkedHashMap<String, JsonValue>(members.size());
        members.forEach((k, v) -> {
            Objects.requireNonNull(k, "key");
            Objects.requireNonNull(v, "value");
            copy.put(k, v);
        });
        return new JsonObjectImpl(Collections.unmodifiableMap(copy));
    }

    static Builder builder() {
        return new JsonObjectBuilderImpl();
    }

    default boolean has(final String key) {
        return members().containsKey(key);
    }

    @Override
    default JsonObject asObject() {
        return this;
    }

    /**
     * A mutable builder that produces an immutable {@link JsonObject}.
     */
    interface Builder {

        Builder put(String key, JsonValue value);

        Builder put(String key, String value);

        Builder put(String key, Number value);

        Builder put(String key, boolean value);

        Builder putNull(String key);

        JsonObject build();
    }
}
