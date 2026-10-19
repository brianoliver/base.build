package build.base.telemetry;

/*-
 * #%L
 * base.build Telemetry
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
 * Represents a named unit of measure for the progress tracked by a {@link Meter}.
 * <p>
 * {@link NamedUnit}s provide singular and plural names to allow renderers to produce
 * grammatically correct output (e.g., "1 item" vs "2 items", or "1 MB" vs "2 MB").
 * <p>
 * When no unit is applicable, use {@link NamedUnit#none()}, which is the default for all
 * {@link TelemetryRecorder} meter methods that do not accept a {@link NamedUnit}.
 *
 * @author brian.oliver
 * @since Feb-2026
 * @see Meter
 * @see Progress
 * @see TelemetryRecorder
 */
public interface NamedUnit {

    /**
     * Obtains the singular name of this {@link NamedUnit}
     * (e.g., {@code "item"}, {@code "file"}, {@code "MB"}).
     *
     * @return the singular name, never {@code null}
     */
    String singular();

    /**
     * Obtains the plural name of this {@link NamedUnit}
     * (e.g., {@code "items"}, {@code "files"}, {@code "MB"}).
     *
     * @return the plural name, never {@code null}
     */
    String plural();

    /**
     * Determines whether this {@link NamedUnit} has no name.
     *
     * @return {@code true} if this {@link NamedUnit} has no singular or plural name,
     *         {@code false} otherwise
     */
    default boolean isEmpty() {
        return singular().isEmpty() && plural().isEmpty();
    }

    /**
     * Obtains a display name appropriate for the specified count.
     * <p>
     * Returns the {@link #singular()} name when {@code count} is {@code 1}, and the
     * {@link #plural()} name otherwise. Returns an empty {@link String} when this
     * {@link NamedUnit} {@link #isEmpty()}.
     *
     * @param count the count of items
     * @return the display name appropriate for the count, never {@code null}
     */
    default String nameFor(final int count) {
        if (isEmpty()) {
            return "";
        }

        return count == 1 ? singular() : plural();
    }

    /**
     * Creates a {@link NamedUnit} with the specified singular and plural names.
     *
     * @param singular the singular name of the unit (e.g., {@code "item"})
     * @param plural   the plural name of the unit (e.g., {@code "items"})
     * @return a new {@link NamedUnit}
     * @throws NullPointerException     if {@code singular} or {@code plural} is {@code null}
     * @throws IllegalArgumentException if {@code singular} or {@code plural} is blank
     */
    static NamedUnit of(final String singular, final String plural) {
        Objects.requireNonNull(singular, "The singular unit name must not be null");
        Objects.requireNonNull(plural, "The plural unit name must not be null");

        if (singular.isBlank()) {
            throw new IllegalArgumentException("The singular unit name must not be blank");
        }

        if (plural.isBlank()) {
            throw new IllegalArgumentException("The plural unit name must not be blank");
        }

        return new NamedUnit() {
            @Override
            public String singular() {
                return singular;
            }

            @Override
            public String plural() {
                return plural;
            }

            @Override
            public String toString() {
                return singular + "/" + plural;
            }
        };
    }

    /**
     * Creates a {@link NamedUnit} with the same singular and plural name.
     * <p>
     * This is appropriate for units that do not have distinct plural forms
     * (e.g., {@code "MB"}, {@code "GB"}).
     *
     * @param name the singular and plural name of the unit (e.g., {@code "MB"})
     * @return a new {@link NamedUnit}
     * @throws NullPointerException     if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    static NamedUnit of(final String name) {
        return of(name, name);
    }

    /**
     * Obtains a {@link NamedUnit} representing no unit of measure.
     * <p>
     * This is the default for all {@link TelemetryRecorder} and {@link Meter} operations
     * that do not specify a {@link NamedUnit}.
     *
     * @return a {@link NamedUnit} with no name
     */
    static NamedUnit none() {
        return NoneHolder.INSTANCE;
    }

    /**
     * Private holder for the {@link NamedUnit#none()} singleton.
     */
    final class NoneHolder {

        private NoneHolder() {
        }

        private static final NamedUnit INSTANCE = new NamedUnit() {
            @Override
            public String singular() {
                return "";
            }

            @Override
            public String plural() {
                return "";
            }

            @Override
            public String toString() {
                return "";
            }
        };
    }
}
