package build.base.foundation.unit;

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

import build.base.foundation.Strings;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Standard <a href="https://www.iec.ch/">IEC</a> measures of memory capacity.
 *
 * @author brian.oliver
 * @since Nov-2018
 */
public enum MemorySize {

    /**
     * A byte.
     */
    B(1L, "Byte", "B", "b"),

    /**
     * A Kibibyte.
     */
    KiB(1024L, "Kibibyte", "KiB", "K", "k"),

    /**
     * A Mebibyte.
     */
    MiB(1024L * 1024L, "Mebibyte", "MiB", "M", "m"),

    /**
     * A Gibibyte.
     */
    GiB(1024L * 1024L * 1024L, "Gibibyte", "GiB", "G", "g"),

    /**
     * A Tebibyte.
     */
    TiB(1024L * 1024L * 1024L * 1024L, "Tebibyte", "TiB", "T", "t"),

    /**
     * A Pebibyte.
     */
    PiB(1024L * 1024L * 1024L * 1024L * 1024L, "Pebibyte", "PiB", "P", "p"),

    /**
     * A Kilobyte.
     */
    KB(1000L, "Kilobyte", "KB"),

    /**
     * A Megabyte.
     */
    MB(1000L * 1000L, "Megabyte", "MB"),

    /**
     * A Gigabyte.
     */
    GB(1000L * 1000L * 1000L, "Gigabyte", "GB"),

    /**
     * A Terabyte.
     */
    TB(1000L * 1000L * 1000L * 1000L, "Terabyte", "TB"),

    /**
     * A Petabyte.
     */
    PB(1000L * 1000L * 1000L * 1000L * 1000L, "Petabyte", "PB");

    /**
     * A cached copy of {@link #values()}, private so we can be assured its contents aren't modifired.
     */
    private static final MemorySize[] VALUES = values();

    /**
     * The number of bytes in a unit.
     */
    private final long bytes;

    /**
     * The description for an individual unit.
     */
    private final String description;

    /**
     * The abbreviations for the unit.
     */
    private final List<String> abbreviations;

    /**
     * Constructs a {@link MemorySize}.
     *
     * @param bytes         the number of bytes in the {@link MemorySize}
     * @param description   the description of the unit of {@link MemorySize}.
     * @param abbreviations the optional abbreviations for the unit
     */
    MemorySize(final long bytes,
               final String description,
               final String... abbreviations) {

        this.bytes = bytes;
        this.description = description;
        this.abbreviations = Arrays.asList(abbreviations);
    }

    /**
     * Obtains the number of bytes in the unit.
     *
     * @return the number of bytes
     */
    public long bytes() {
        return this.bytes;
    }

    /**
     * Obtains the description of the unit.
     *
     * @return the description
     */
    public String description() {
        return this.description;
    }

    /**
     * Obtains the abbreviations of the unit.
     *
     * @return the abbreviations
     */
    public Stream<String> abbreviations() {
        return this.abbreviations.stream();
    }

    /**
     * Parse a string containing a memory size and return the number of bytes it represents.
     *
     * @param value the memory size value
     * @return the byte count
     * @throws IllegalArgumentException if inputted String is formatted incorrectly
     */
    public static long parse(final String value) {
        if (Strings.isEmpty(value)) {
            throw new IllegalArgumentException("Invalid input");
        }

        int suffixOffset = value.length() - 1;
        while (suffixOffset > 0 && Character.isAlphabetic(value.charAt(suffixOffset - 1))) {
            --suffixOffset;
        }

        try {
            final var prefix = suffixOffset == 0 ? 1L : Long.parseLong(value.substring(0, suffixOffset));
            final var suffix = value.substring(suffixOffset);

            for (var memorySize : MemorySize.VALUES) {
                for (var abbreviation : memorySize.abbreviations) {
                    if (abbreviation.equals(suffix)) {
                        return prefix * memorySize.bytes();
                    }
                }
            }

            // either just a number, or a number with an invalid suffix
            return Long.parseLong(value);
        }
        catch (final NumberFormatException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Obtain the byte count for a multiple of the magnitude.
     * <p>
     * For example:
     * <pre>{@code
     * long bytes = MemorySize.MiB.of(5); // i.e. 5 MiB
     * }</pre>
     *
     * @param count the number of units
     * @return the byte size
     */
    public long of(final long count) {
        return this.bytes * count;
    }

    /**
     * Return a "pretty printed" string containing an approximation of the specified byte based memory size.
     *
     * @param bytes the byte size to "pretty print"
     * @return the "pretty printed" form
     */
    public static String toString(final long bytes) {
        long memory = bytes;
        int units = 0;
        final var sizes = MemorySize.VALUES;
        final var maxUnit = sizes.length - 1;

        // find the "best" unit to represent the value without discarding too much information
        final var nextLimit = 1024 * 10;
        while ((Math.abs(memory) >= nextLimit || Math.abs(memory) % 1024 == 0) && memory != 0 && units < maxUnit) {
            memory /= 1024;
            ++units;
        }

        return memory + sizes[units].abbreviations.get(0);
    }
}
