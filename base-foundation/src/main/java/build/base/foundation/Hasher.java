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

/**
 * A mechanism for computing hashes and equality of a given type.
 *
 * @author mark.falco
 * @since March-2022
 */
public interface Hasher<T> {

    /**
     * Compute a hash for a value.
     *
     * @param value the value to hash
     * @return the hash
     */
    int hashCode(T value);

    /**
     * Compare two values for equality.
     *
     * @param valueA the first value
     * @param valueB the second value
     * @return {@code true} iff the values are equal
     */
    boolean equals(T valueA, T valueB);
}
