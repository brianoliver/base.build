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

import build.base.foundation.iterator.Iterators;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Static functions for working with arrays.
 *
 * @author brian.oliver
 * @since Aug-2024
 */
public final class Arrays {

    /**
     * Private Constructor.
     */
    private Arrays() {
        // prevent instantiation
    }

    /**
     * Compare two {@code byte} arrays.
     *
     * @param array1 the first {@code byte} array
     * @param array2 the second {@code byte} array
     * @return 0 when both {@code byte} arrays are the same, -1 when the first {@code byte} array is smaller or
     * has smaller element values than the second, or +1 otherwise
     */
    public static int compare(final byte[] array1, final byte[] array2) {
        if (array1.length == array2.length) {
            for (int i = 0; i < array1.length; i++) {
                if (array1[i] != array2[i]) {
                    return array1[i] - array2[i];
                }
            }
            return 0;
        } else if (array1.length < array2.length) {
            for (int i = 0; i < array1.length; i++) {
                if (array1[i] != array2[i]) {
                    return array1[i] - array2[i];
                }
            }
            return -1;
        } else {
            for (int i = 0; i < array2.length; i++) {
                if (array1[i] != array2[i]) {
                    return array1[i] - array2[i];
                }
            }
            return 1;
        }
    }

    /**
     * Concatenates multiple {@code byte} arrays into a single {@code byte} array.
     *
     * @param arrays the {@code byte} arrays
     * @return the concatenated {@code byte} array
     */
    public static byte[] concat(final byte[]... arrays) {

        if (arrays == null || arrays.length == 0) {
            return new byte[0];
        }

        // determine the length of the resulting array
        int length = 0;
        for (final byte[] array : arrays) {
            length += array != null
                ? array.length
                : 0;
        }

        // copy the source arrays into the result
        final byte[] result = new byte[length];
        int offset = 0;

        for (final byte[] current : arrays) {
            if (current != null && current.length > 0) {
                System.arraycopy(current, 0, result, offset, current.length);
                offset += current.length;
            }
        }

        return result;
    }

    /**
     * Creates a sequential {@link Stream} with the specified array as its source.
     *
     * @param <T>   the type of the array element
     * @param array the array of elements to stream
     * @return a {@code Stream} of elements in the array
     */
    @SafeVarargs
    public static <T> Stream<T> stream(final T... array) {
        return array == null || array.length == 0
            ? Stream.empty()
            : java.util.Arrays.stream(array, 0, array.length);
    }

    /**
     * Creates an {@link Iterator} with the specified array as its source.
     *
     * @param <T>   the type of the array element
     * @param array the array of elements to stream
     * @return a {@code Iterator} of elements in the array
     */
    @SafeVarargs
    public static <T> Iterator<T> iterator(final T... array) {
        return array == null || array.length == 0
            ? Iterators.empty()
            : Iterators.of(array);
    }

    /**
     * Prepends zero or more elements on to an existing array to return a new array.
     *
     * @param array    the existing array
     * @param elements the elements to prepend
     * @return a new array
     */
    public static Object[] prepend(final Object[] array,
                                   final Object... elements) {

        if (array == null || array.length == 0) {
            return elements;
        }

        if (elements == null || elements.length == 0) {
            return array;
        }

        final Object[] result = new Object[array.length + elements.length];

        System.arraycopy(elements, 0, result, 0, elements.length);
        System.arraycopy(array, 0, result, elements.length, array.length);

        return result;
    }
}
