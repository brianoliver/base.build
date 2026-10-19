package build.base.foundation;

/*-
 * #%L
 * base.build Foundation
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

import java.util.Objects;

/**
 * A {@link Hasher} utilizing {@link Object}-based hashing and equality.
 *
 * @param <T> the value type
 * @author mark.falco
 * @since March-2022
 */
public class ObjectHasher<T>
    implements Hasher<T> {

    /**
     * The singleton instance.
     */
    private static final ObjectHasher<?> INSTANCE = new ObjectHasher<>();


    @Override
    public int hashCode(final T value) {
        return Objects.hashCode(value);
    }

    @Override
    public boolean equals(final T valueA, final T valueB) {
        return Objects.equals(valueA, valueB);
    }

    /**
     * Obtain the singleton instance of the {@link ObjectHasher}.
     *
     * @param <U> the type to hash
     * @return the instance
     */
    @SuppressWarnings("unchecked")
    public static <U> ObjectHasher<U> instance() {
        return (ObjectHasher<U>) INSTANCE;
    }
}
