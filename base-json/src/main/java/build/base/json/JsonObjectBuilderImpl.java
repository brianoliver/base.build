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

final class JsonObjectBuilderImpl implements JsonObject.Builder {

    private final Map<String, JsonValue> members = new LinkedHashMap<>();

    @Override
    public JsonObject.Builder put(final String key, final JsonValue value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        this.members.put(key, value);
        return this;
    }

    @Override
    public JsonObject.Builder put(final String key, final String value) {
        Objects.requireNonNull(value, "value");
        return put(key, JsonString.of(value));
    }

    @Override
    public JsonObject.Builder put(final String key, final Number value) {
        Objects.requireNonNull(value, "value");
        return put(key, JsonNumber.of(value));
    }

    @Override
    public JsonObject.Builder put(final String key, final boolean value) {
        return put(key, JsonBoolean.of(value));
    }

    @Override
    public JsonObject.Builder putNull(final String key) {
        Objects.requireNonNull(key, "key");
        this.members.put(key, JsonNull.INSTANCE);
        return this;
    }

    @Override
    public JsonObject build() {
        return new JsonObjectImpl(Collections.unmodifiableMap(new LinkedHashMap<>(this.members)));
    }
}
