package build.base.transport.json;

/*-
 * #%L
 * base.build Transport (JSON)
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
 * A {@link Codec} that conditionally ignores values to transport, resulting in them not being output to JSON.
 *
 * @param <T> the type of value
 * @author brian.oliver
 * @since Jun-2025
 */
public interface ConditionalCodec<T>
    extends Codec<T> {

    /**
     * Determines if the specified value should be ignored when writing to JSON.  When a value is ignored, it should
     * not be written to the JSON output.  When reading from JSON, an ignored value should be replaced with a
     * defaultValue().
     *
     * @param value the value to check
     * @return {@code true} if the value should be ignored, otherwise {@code false}
     */
    boolean ignore(T value);

    /**
     * Obtains the default value to use when a value is missing (was {@link #ignore(Object)}d) when reading from JSON.
     *
     * @return the default value
     */
    T defaultValue();
}
