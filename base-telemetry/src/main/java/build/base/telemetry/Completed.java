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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link Telemetry} information recording successfully completion of an {@link Activity}.
 *
 * @author brian.oliver
 * @since Mar-2021
 */
public interface Completed<T>
    extends Telemetry {

    /**
     * Obtains the {@link Optional} result.
     *
     * @return the {@link Optional} result
     */
    Optional<T> get();

    /**
     * Obtains the {@link Duration} of the {@link Activity}.
     *
     * @return the {@link Duration}
     */
    Duration duration();

    /**
     * Creates {@link Completed} {@link Telemetry}.
     *
     * @param <T>       the type of the result
     * @param uri       the source {@link URI}
     * @param locations the {@link Optional} {@link Location}
     * @param result    the {@code null}able result
     * @param commenced the {@link Instant} when the {@link Activity}
     * @param format    the formatted message
     * @param args      the arguments
     * @return new {@link Completed} {@link Telemetry}
     */
    static <T> Completed<T> create(final URI uri,
                                   final Stream<? extends Location> locations,
                                   final T result,
                                   final Instant commenced,
                                   final String format,
                                   final Object... args) {

        Objects.requireNonNull(uri, "The source uri must not be null");

        final Instant instant = Instant.now();
        final String message = format == null ? "" : String.format(format, args);
        final Optional<T> optional = Optional.ofNullable(result);
        final Duration duration = Duration.between(commenced, instant);
        final ArrayList<Location> locationList = locations.collect(Collectors.toCollection(ArrayList::new));

        return new Completed<>() {
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
            public Optional<T> get() {
                return optional;
            }

            @Override
            public Duration duration() {
                return duration;
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
                final long days = duration.toDays();
                final long hours = duration.toHours() % 24;
                final long minutes = duration.toMinutes() % 60;
                final long seconds = duration.getSeconds() % 60;
                final long millis = duration.toMillis() % 1000;
                final boolean showMillis = millis > 0 || duration.toMillis() == 0;

                final String duration = (days > 0 ? days + " days " : "")
                    + (hours > 0 ? hours + " hrs " : "")
                    + (minutes > 0 ? minutes + " mins " : "")
                    + (seconds > 0 ? seconds + " secs " : "")
                    + (showMillis ? millis + " ms" : "");

                return "[" + uri + "] "
                    + "[Completed] " + message
                    + (hasLocations()
                    ? locations()
                    .map(Location::toString)
                    .collect(Collectors.joining(", ", " locations [", "]"))
                    : "")
                    + " (" + duration + ")";
            }
        };
    }

    /**
     * Creates {@link Completed} {@link Telemetry}.
     *
     * @param <T>       the type of the result
     * @param uri       the source {@link URI}
     * @param result    the {@code null}able result
     * @param commenced the {@link Instant} when the {@link Activity}
     * @param format    the formatted message
     * @param args      the arguments
     * @return new {@link Completed} {@link Telemetry}
     */
    static <T> Completed<T> create(final URI uri,
                                   final T result,
                                   final Instant commenced,
                                   final String format,
                                   final Object... args) {

        return create(uri, Stream.empty(), result, commenced, format, args);
    }
}
