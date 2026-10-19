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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class JsonArrayBuilderImpl implements JsonArray.Builder {

    private final List<JsonValue> values = new ArrayList<>();

    @Override
    public JsonArray.Builder add(final JsonValue value) {
        Objects.requireNonNull(value, "value");
        this.values.add(value);
        return this;
    }

    @Override
    public JsonArray.Builder add(final String value) {
        Objects.requireNonNull(value, "value");
        return add(JsonString.of(value));
    }

    @Override
    public JsonArray.Builder add(final Number value) {
        Objects.requireNonNull(value, "value");
        return add(JsonNumber.of(value));
    }

    @Override
    public JsonArray.Builder add(final boolean value) {
        return add(JsonBoolean.of(value));
    }

    @Override
    public JsonArray.Builder addNull() {
        this.values.add(JsonNull.INSTANCE);
        return this;
    }

    @Override
    public JsonArray.Builder addAll(final Iterable<String> values) {
        Objects.requireNonNull(values, "values");
        values.forEach(this::add);
        return this;
    }

    @Override
    public JsonArray build() {
        return new JsonArrayImpl(List.copyOf(this.values));
    }
}
