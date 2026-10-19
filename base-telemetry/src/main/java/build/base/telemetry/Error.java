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
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link Telemetry} information concerning a non-fatal, non-recoverable error.
 *
 * @author brian.oliver
 * @see Warning
 * @see Fatal
 * @since Mar-2021
 */
public interface Error
    extends ExceptionalTelemetry {

    /**
     * Creates {@link Error} {@link Telemetry}.
     *
     * @param uri       the source {@link URI}
     * @param locations the {@link Location}
     * @param throwable the optional {@code null}able {@link Throwable}
     * @param format    the format
     * @param args      the arguments
     * @return new {@link Error} {@link Telemetry}
     */
    static Error create(final URI uri,
                        final Stream<? extends Location> locations,
                        final Throwable throwable,
                        final String format,
                        final Object... args) {

        Objects.requireNonNull(uri, "The source uri must not be null");

        final Instant instant = Instant.now();
        final String message = format == null ? "" : String.format(format, args);
        final Optional<Throwable> optional = Optional.ofNullable(throwable);
        final ArrayList<Location> locationList = locations.collect(Collectors.toCollection(ArrayList::new));

        return new Error() {
            @Override
            public URI uri() {
                return uri;
            }

            @Override
            public Stream<Location> locations() {
                return locationList.stream();
            }

            @Override
            public boolean hasLocations() {
                return !locationList.isEmpty();
            }

            @Override
            public Optional<Throwable> throwable() {
                return optional;
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
                return "[" + uri + "] "
                    + "[Error] " + message
                    + (hasLocations()
                    ? locations()
                    .map(Location::toString)
                    .collect(Collectors.joining(", ", " locations [", "]"))
                    : " ")
                    + optional.map(Object::toString).orElse("");
            }
        };
    }

    /**
     * Creates {@link Error} {@link Telemetry}.
     *
     * @param uri       the source {@link URI}
     * @param throwable the optional {@code null}able {@link Throwable}
     * @param format    the format
     * @param args      the arguments
     * @return new {@link Error} {@link Telemetry}
     */
    static Error create(final URI uri,
                        final Throwable throwable,
                        final String format,
                        final Object... args) {

        return create(uri, Stream.empty(), throwable, format, args);
    }
}
