package build.base.foundation.tuple;

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
 * An immutable n-<a href="https://en.wikipedia.org/wiki/Tuple">Tuple</a>.
 *
 * @author brian.oliver
 * @since Jun-2018
 */
public interface Tuple {

    /**
     * Obtains the number of values in the {@link Tuple}, always greater than zero.
     *
     * @return the number of values
     */
    int size();

    /**
     * Obtains the ith value in the {@link Tuple}, when i is greater than or equal to zero.
     *
     * @param <T> the type of the value
     * @param i   the index of the value to retrieve (zero is the first value)
     * @return the ith value
     * @throws IndexOutOfBoundsException should the index be invalid
     */
    <T> T get(int i)
        throws IndexOutOfBoundsException;
}
