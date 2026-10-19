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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link Telemetry} request to notify the end-user, as soon as possible, with some information.
 *
 * @author brian.oliver
 * @since Jan-2023
 */
public interface Notification
    extends Telemetry {

    /**
     * Creates {@link Notification} {@link Telemetry}.
     *
     * @param uri       the source {@link URI}
     * @param locations the {@link Location}
     * @param format    the format
     * @param args      the arguments
     * @return new {@link Advice} {@link Telemetry}
     */
    static Notification create(final URI uri,
                               final Stream<? extends Location> locations,
                               final String format,
                               final Object... args) {

        Objects.requireNonNull(uri, "The source uri must not be null");

        final Instant instant = Instant.now();
        final String message = format == null ? "" : String.format(format, args);
        final ArrayList<Location> locationList = locations.collect(Collectors.toCollection(ArrayList::new));

        return new Notification() {
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
                    + message
                    + (hasLocations()
                    ? locations()
                    .map(Location::toString)
                    .collect(Collectors.joining(", ", " locations [", "]"))
                    : "");
            }
        };
    }

    /**
     * Creates {@link Notification} {@link Telemetry}.
     *
     * @param uri    the source {@link URI}
     * @param format the format
     * @param args   the arguments
     * @return new {@link Notification} {@link Telemetry}
     */
    static Notification create(final URI uri,
                               final String format,
                               final Object... args) {

        return create(uri, Stream.empty(), format, args);
    }
}
