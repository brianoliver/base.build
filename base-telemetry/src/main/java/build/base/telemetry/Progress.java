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

import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;


/**
 * {@link Telemetry} information concerning the progress of a {@link Meter}.
 *
 * @author brian.oliver
 * @since Mar-2021
 */
public interface Progress
    extends Telemetry {

    /**
     * Obtains the current {@link Progress} value.
     *
     * @return the current {@link Progress} value
     */
    int current();

    /**
     * Obtains the maximum {@link Progress} value.
     *
     * @return the maximum {@link Progress} value
     */
    int maximum();

    /**
     * Obtains the percentage complete.
     *
     * @return the percentage complete
     */
    double percentage();

    /**
     * Obtains the {@link NamedUnit} of measure for this {@link Progress}.
     *
     * @return the {@link NamedUnit}, which is {@link NamedUnit#none()} when no unit has been specified
     */
    NamedUnit unit();

    /**
     * Creates {@link Progress} {@link Telemetry}.
     *
     * @param uri     the source {@link URI}
     * @param unit    the {@link NamedUnit} of measure
     * @param current the current value
     * @param maximum the maximum value
     * @param format  the formatted message
     * @param args    the arguments
     * @return new {@link Progress} {@link Telemetry}
     */
    static Progress create(final URI uri,
                           final NamedUnit unit,
                           final int current,
                           final int maximum,
                           final String format,
                           final Object... args) {

        Objects.requireNonNull(uri, "The source uri must not be null");
        Objects.requireNonNull(unit, "The NamedUnit must not be null");

        final Instant instant = Instant.now();
        final String message = format == null ? "" : String.format(format, args);

        return new Progress() {
            @Override
            public URI uri() {
                return uri;
            }

            @Override
            public Stream<Location> locations() {
                return Stream.empty();
            }

            @Override
            public boolean hasLocations() {
                return false;
            }

            @Override
            public int current() {
                return Math.min(Math.max(0, current), maximum());
            }

            @Override
            public int maximum() {
                return Math.max(0, maximum);
            }

            @Override
            public double percentage() {
                return maximum() == 0 ? 0.0 : (double) current() / maximum() * 100.0;
            }

            @Override
            public NamedUnit unit() {
                return unit;
            }

            @Override
            public String message() {
                return message;
            }

            @Override
            public Instant instant() {
                return instant;
            }

            @Override
            public String toString() {
                final String currentLabel = unit.isEmpty()
                    ? String.valueOf(current())
                    : current() + " " + unit.nameFor(current());

                final String maximumLabel = unit.isEmpty()
                    ? String.valueOf(maximum())
                    : maximum() + " " + unit.nameFor(maximum());

                return "[" + uri + "] "
                    + "[Progress] " + message
                    + " " + currentLabel + " of " + maximumLabel
                    + String.format(Locale.ROOT, " (%.1f%%)", percentage());
            }
        };
    }

    /**
     * Creates {@link Progress} {@link Telemetry} with no {@link NamedUnit}.
     *
     * @param uri     the source {@link URI}
     * @param current the current value
     * @param maximum the maximum value
     * @param format  the formatted message
     * @param args    the arguments
     * @return new {@link Progress} {@link Telemetry}
     */
    static Progress create(final URI uri,
                           final int current,
                           final int maximum,
                           final String format,
                           final Object... args) {

        return create(uri, NamedUnit.none(), current, maximum, format, args);
    }
}
