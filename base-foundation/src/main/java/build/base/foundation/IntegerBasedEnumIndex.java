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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Provides {@code static} facilities to efficiently index and lookup {@link Enum} {@link Class} values using an
 * {@code int} index with constant O(1) time efficiency.
 * <p>
 * To enabled indexing, an {@link Enum} {@link Class} together with its {@link Function} to obtain an indexed value
 * must first be provided to {@link #createIndex(Class, Function)}.  This is usually best achieved by defining a
 * {@code static} initializer in the {@link Enum} {@link Class}.  For example:
 * <p>
 * <pre>
 * {@code
 *      public enum Color {
 *          RED(1),
 *          GREEN(2),
 *          BLUE(3);
 *
 *          Color(final int index) {
 *              this.index = index;
 *          }
 *
 *          private final int index;
 *
 *          public int getIndex() {
 *              return this.index;
 *          }
 *
 *          static {
 *              IntegerBasedEnumIndex.createIndex(Color.class, Color::getIndex);
 *          }
 *      }
 * }
 * </pre>
 * <p>
 * Once an index is created, {@link #getValue(Class, int)} may be used to obtain an {@link Enum} value based on an
 * index.
 * <p>
 * <pre>
 * {@code
 *      final var Color red = IntegerBasedEnumIndex.getValue(Color.class, 1);
 * }
 * </pre>
 *
 * @author brian.oliver
 * @since Feb-2025
 */
public class IntegerBasedEnumIndex {

    /**
     * The {@link Index}es by {@link Class} of {@link Enum}.
     */
    private static final ConcurrentHashMap<Class<? extends Enum<?>>, Index> indexesByEnumClass;

    /**
     * Private Constructor.
     */
    private IntegerBasedEnumIndex() {
        // prevent construction
    }

    /**
     * Creates an index for the specified {@link Class} of {@link Enum} using the provided {@link Function}
     * to obtain the unique index value for each {@link Enum} value.
     *
     * @param <T> the type of {@link Enum}
     * @param enumClass the {@link Class} of {@link Enum}
     * @param indexFunction the {@link Function} to obtain the unique index value for an {@link Enum} value
     */
    public static <T extends Enum<T>> void createIndex(final Class<T> enumClass,
                                                       final Function<T, Integer> indexFunction) {

        Objects.requireNonNull(enumClass, "The enum class must not be null");
        Objects.requireNonNull(indexFunction, "The index function must not be null");

        indexesByEnumClass.computeIfAbsent(enumClass, (_) -> {
            final var enumValues = enumClass.getEnumConstants();

            // determine the lowest and highest index values for the enum values
            var lowestIndex = indexFunction.apply(enumValues[0]);
            var highestIndex = lowestIndex;

            for (final T enumValue : enumValues) {
                final var index = indexFunction.apply(enumValue);
                if (index < lowestIndex) {
                    lowestIndex = index;
                }

                if (index > highestIndex) {
                    highestIndex = index;
                }
            }

            final var indexedValues = new Enum[Math.abs(highestIndex - lowestIndex) + 1];

            // calculate the offset of the first value
            final var offset = lowestIndex < 0
                ? Math.abs(lowestIndex)
                : -lowestIndex;

            // fill the array with the values at their appropriate index positions
            for (final T enumValue : enumValues) {
                final var index = indexFunction.apply(enumValue);
                indexedValues[index + offset] = enumValue;
            }

            return new Index(enumClass, lowestIndex, highestIndex, offset, indexedValues);
        });
    }

    /**
     * Attempts to obtain the {@link Enum} value of the {@link Enum} {@link Class} with the specified index value.
     *
     * @param <T> the type of {@link Enum}
     * @param enumClass the {@link Class} of {@link Enum}
     * @param indexedValue the index of the {@link Enum} value
     * @return the {@link Enum} value with the specified index value
     * @throws IllegalArgumentException should the {@link Enum} not be indexed or the {@link Enum} value not be found
     */
    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T getValue(final Class<T> enumClass,
                                                 final int indexedValue)
        throws IllegalArgumentException {

        final var index = indexesByEnumClass.get(enumClass);

        if (index == null) {
            throw new IllegalArgumentException("The Enum Class " + enumClass.getName() + " has not been indexed");
        }

        final var enumValue = (T) index.values[indexedValue + index.offset];
        if (enumValue == null) {
            throw new IllegalArgumentException(
                "Unknown index value [" + indexedValue + "] for Enum Class " + enumClass.getName());
        }

        return enumValue;
    }

    /**
     * Defines information concerning an index for an individual {@link Class} of {@link Enum}.
     *
     * @param enumClass the {@link Enum} {@link Class}
     * @param lowestIndex the lowest index value for the {@link Enum}
     * @param highestIndex the highest indexed value for the {@link Enum}
     * @param offset the offset to obtain the first indexed {@link Enum} value
     * @param values the {@link Enum} values arranged by index value
     */
    private record Index(Class<? extends Enum<?>> enumClass,
                         int lowestIndex,
                         int highestIndex,
                         int offset,
                         Enum<?>[] values) {

    }

    static {
        indexesByEnumClass = new ConcurrentHashMap<>();
    }
}
