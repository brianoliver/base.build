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
import java.util.stream.Stream;

/**
 * Immutable information recorded by a {@link TelemetryRecorder} from a source identifiable by a {@link URI}.
 *
 * @author brian.oliver
 * @since Mar-2021
 */
public interface Telemetry {

    /**
     * Obtains the {@link URI} representing the {@link TelemetryRecorder} that produced the {@link Telemetry}.
     *
     * @return the {@link URI}
     */
    URI uri();

    /**
     * Obtains the formatted {@link Telemetry} message.
     *
     * @return the formatted message
     */
    String message();

    /**
     * The {@link Instant} the {@link Telemetry} was created.
     *
     * @return the {@link Instant}
     */
    Instant instant();

    /**
     * The {@link Stream} {@link Location}s of the {@link Telemetry}.
     *
     * @return {@link Stream} of {@link Location}s or an {@link Stream#empty()} when no {@link Location}s are available
     */
    Stream<Location> locations();

    /**
     * Determines if there are any {@link Location}s for the {@link Telemetry}.
     *
     * @return {@code true} if there are {@link Location}s, {@code false} otherwise
     */
    default boolean hasLocations() {
        return locations().findAny().isPresent();
    }
}
