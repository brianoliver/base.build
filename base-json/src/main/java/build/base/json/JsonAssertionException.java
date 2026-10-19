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
 * Thrown when a {@link JsonValue} navigation helper is called on the wrong subtype, or when a member or element
 * lookup fails.
 */
public final class JsonAssertionException extends RuntimeException {

    private JsonAssertionException(final String message) {
        super(message);
    }

    static JsonAssertionException notA(final String expected,
                                       final JsonValue actual) {
        return new JsonAssertionException(
            "Expected a JSON " + expected + " but got: " + actual.getClass().getSimpleName());
    }

    static JsonAssertionException missingMember(final String name) {
        return new JsonAssertionException("No member named '" + name + "'");
    }

    static JsonAssertionException missingElement(final int index) {
        return new JsonAssertionException("No element at index " + index);
    }
}
