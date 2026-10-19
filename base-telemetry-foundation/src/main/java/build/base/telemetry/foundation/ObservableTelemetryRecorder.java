package build.base.telemetry.foundation;

/*-
 * #%L
 * base.build Telemetry Foundation
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

import build.base.telemetry.Activity;
import build.base.telemetry.Advice;
import build.base.telemetry.Diagnostic;
import build.base.telemetry.Error;
import build.base.telemetry.Fatal;
import build.base.telemetry.Information;
import build.base.telemetry.Location;
import build.base.telemetry.Meter;
import build.base.telemetry.NamedUnit;
import build.base.telemetry.Notification;
import build.base.telemetry.Telemetry;
import build.base.telemetry.TelemetryRecorder;
import build.base.telemetry.TelemetryRecorderFactory;
import build.base.telemetry.Warning;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * An adapter for {@link TelemetryRecorder}s permitting observation and query of produced {@link Telemetry}.
 *
 * @author brian.oliver
 * @since May-2024
 */
public class ObservableTelemetryRecorder
    implements TelemetryRecorder {

    /**
     * The URI of the {@link TelemetryRecorder}.
     */
    private final URI uri;

    /**
     * The adapted {@link TelemetryRecorder}.
     */
    private final TelemetryRecorder telemetryRecorder;

    /**
     * The observed {@link Telemetry}.
     */
    private final LinkedList<Telemetry> telemetry;

    /**
     * Constructs an {@link ObservableTelemetryRecorder}.
     *
     * @param telemetryRecorder the {@link TelemetryRecorder}
     */
    protected ObservableTelemetryRecorder(final URI uri,
                                          final TelemetryRecorder telemetryRecorder) {

        this.uri = Objects.requireNonNull(uri, "The URI must not be null");
        this.telemetryRecorder = Objects.requireNonNull(telemetryRecorder, "The TelemetryRecorder must not be null");
        this.telemetry = new LinkedList<>();
    }

    /**
     * Records the specified {@link Telemetry}.
     *
     * @param telemetry the {@link Telemetry}
     * @param <T>       the type of {@link Telemetry}
     * @return the {@link Telemetry} to permit fluent-style method invocation
     */
    private <T extends Telemetry> T record(final T telemetry) {

        // Synchronize only for the add so that writes are O(1). Reads (stream, hasObserved)
        // take a snapshot under the same lock and then traverse outside it, keeping write
        // throughput independent of how long callers spend consuming the stream.
        synchronized (this.telemetry) {
            this.telemetry.add(telemetry);
        }
        return telemetry;
    }

    /**
     * Obtains the {@link Telemetry} that has been observed.
     *
     * @return the {@link Stream} of observed {@link Telemetry}
     */
    public Stream<Telemetry> stream() {
        // Snapshot under lock, traverse outside — lock held only for the O(n) copy,
        // not for the duration of stream consumption by the caller.
        synchronized (this.telemetry) {
            return new ArrayList<>(this.telemetry).stream();
        }
    }

    /**
     * Determines if {@link Telemetry} satisfying the specified {@link Predicate} has been observed.
     *
     * @param predicate the {@link Predicate}
     * @return {@code true} when one or more of the observed {@link Telemetry} satisfies the specified {@link Predicate},
     * {@code false} otherwise
     */
    public boolean hasObserved(final Predicate<? super Telemetry> predicate) {
        if (predicate == null) {
            return false;
        }
        // Same snapshot pattern as stream() — lock released before predicate evaluation
        // so that a slow predicate does not block recording threads.
        final List<Telemetry> snapshot;
        synchronized (this.telemetry) {
            snapshot = new ArrayList<>(this.telemetry);
        }
        return snapshot.stream().anyMatch(predicate);
    }

    /**
     * Determines if {@link Telemetry} of the specified {@link Class} has been observed.
     *
     * @param telemetryClass the {@link Class} of {@link Telemetry}
     * @return {@code true} when one or more of the observed {@link Telemetry} is an instance of the specified {@link Class},
     * {@code false} otherwise
     */
    public boolean hasObserved(final Class<? extends Telemetry> telemetryClass) {
        return telemetryClass != null && hasObserved(telemetryClass::isInstance);
    }

    @Override
    public URI uri() {
        return this.uri;
    }

    @Override
    public Information info(final Stream<? extends Location> locations, final String format, final Object... args) {
        return record(this.telemetryRecorder.info(locations, format, args));
    }

    @Override
    public Advice advice(final Stream<? extends Location> locations, final String format, final Object... args) {
        return record(this.telemetryRecorder.advice(locations, format, args));
    }

    @Override
    public Notification notify(final Stream<? extends Location> locations, final String format, final Object... args) {
        return record(this.telemetryRecorder.notify(locations, format, args));
    }

    @Override
    public Warning warn(final Stream<? extends Location> location,
                        final Throwable throwable,
                        final String format,
                        final Object... args) {

        return record(this.telemetryRecorder.warn(location, throwable, format, args));
    }

    @Override
    public Error error(final Stream<? extends Location> locations,
                       final Throwable throwable,
                       final String format,
                       final Object... args) {

        return record(this.telemetryRecorder.error(locations, throwable, format, args));
    }

    @Override
    public Fatal fatal(final Stream<? extends Location> location,
                       final Throwable throwable,
                       final String format,
                       final Object... args) {

        return record(this.telemetryRecorder.fatal(location, throwable, format, args));
    }

    @Override
    public Diagnostic diagnostic(final Stream<? extends Location> location,
                                 final String format,
                                 final Object... args) {

        return record(this.telemetryRecorder.diagnostic(location, format, args));
    }

    @Override
    public Activity commence(final String format, final Object... args) {
        return this.telemetryRecorder.commence(format, args);
    }

    @Override
    public Meter commence(final int maximum, final NamedUnit unit, final String format, final Object... args) {
        return this.telemetryRecorder.commence(maximum, unit, format, args);
    }

    @Override
    public TelemetryRecorderFactory factory() {
        return uri -> ObservableTelemetryRecorder.of(uri, this.telemetryRecorder.factory().apply(uri));
    }

    /**
     * Creates an {@link ObservableTelemetryRecorder}.
     *
     * @param uri               the {@link URI} of the {@link TelemetryRecorder}
     * @param telemetryRecorder the {@link TelemetryRecorder}
     * @return a new {@link ObservableTelemetryRecorder}
     */
    public static ObservableTelemetryRecorder of(final URI uri,
                                                 final TelemetryRecorder telemetryRecorder) {

        return new ObservableTelemetryRecorder(uri, telemetryRecorder);
    }

    /**
     * Creates an {@link ObservableTelemetryRecorder}.
     *
     * @param telemetryRecorder the {@link TelemetryRecorder}
     * @return a new {@link ObservableTelemetryRecorder}
     */
    public static ObservableTelemetryRecorder of(final TelemetryRecorder telemetryRecorder) {

        Objects.requireNonNull(telemetryRecorder, "TelemetryRecorder must not be null");
        return new ObservableTelemetryRecorder(telemetryRecorder.uri(), telemetryRecorder);
    }
}
