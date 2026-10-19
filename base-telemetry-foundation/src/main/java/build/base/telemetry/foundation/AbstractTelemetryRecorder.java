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
import build.base.telemetry.Commenced;
import build.base.telemetry.Completed;
import build.base.telemetry.Diagnostic;
import build.base.telemetry.Error;
import build.base.telemetry.Fatal;
import build.base.telemetry.Information;
import build.base.telemetry.Location;
import build.base.telemetry.Meter;
import build.base.telemetry.NamedUnit;
import build.base.telemetry.Notification;
import build.base.telemetry.Progress;
import build.base.telemetry.Telemetry;
import build.base.telemetry.TelemetryRecorder;
import build.base.telemetry.Warning;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * An abstract for {@link TelemetryRecorder}.
 *
 * @author brian.oliver
 * @since May-2024
 */
public abstract class AbstractTelemetryRecorder
    implements TelemetryRecorder {

    /**
     * The {@link java.net.URI} of the {@link TelemetryRecorder}.
     */
    private final URI uri;

    /**
     * Constructs an {@link AbstractTelemetryRecorder}.
     *
     * @param uri the {@link URI} of the {@link TelemetryRecorder}
     */
    public AbstractTelemetryRecorder(final URI uri) {
        this.uri = Objects.requireNonNull(uri, "The URI must not be null");
    }

    /**
     * Records the specified {@link Telemetry}.
     *
     * @param telemetry the {@link Telemetry}
     * @param <T>       the type of {@link Telemetry}
     * @return the {@link Telemetry} to permit fluent-style method invocation
     */
    protected abstract <T extends Telemetry> T record(T telemetry);

    @Override
    public URI uri() {
        return this.uri;
    }

    @Override
    public Information info(final Stream<? extends Location> locations, final String format, final Object... args) {
        return record(Information.create(this.uri, locations, format, args));
    }

    @Override
    public Advice advice(final Stream<? extends Location> locations, final String format, final Object... args) {
        return record(Advice.create(this.uri, locations, format, args));
    }

    @Override
    public Notification notify(final Stream<? extends Location> locations, final String format, final Object... args) {
        return record(Notification.create(this.uri, locations, format, args));
    }

    @Override
    public Warning warn(final Stream<? extends Location> locations,
                        final Throwable throwable,
                        final String format,
                        final Object... args) {

        return record(Warning.create(this.uri, locations, throwable, format, args));
    }

    @Override
    public Error error(final Stream<? extends Location> locations,
                       final Throwable throwable,
                       final String format,
                       final Object... args) {

        return record(Error.create(this.uri, locations, throwable, format, args));
    }

    @Override
    public Fatal fatal(final Stream<? extends Location> locations,
                       final Throwable throwable,
                       final String format,
                       final Object... args) {

        return record(Fatal.create(this.uri, locations, throwable, format, args));
    }

    @Override
    public Diagnostic diagnostic(final Stream<? extends Location> locations,
                                 final String format,
                                 final Object... args) {

        return record(Diagnostic.create(this.uri, locations, format, args));
    }

    @Override
    public Activity commence(final String format, final Object... args) {
        // determine a description
        final String description = format == null || format.isEmpty()
            ? UUID.randomUUID().toString()
            : String.format(format, args);

        // record that the Activity is commencing (use "%s" to pass already-formatted description without re-formatting)
        final var commenced = record(Commenced.create(this.uri, "%s", description));

        final AtomicBoolean completed = new AtomicBoolean(false);
        final var self = this;

        return new Activity() {
            @Override
            public <T> boolean complete(final T value) {
                if (completed.compareAndSet(false, true)) {
                    record(Completed.create(self.uri, value, commenced.instant(), description));
                    return true;
                }

                return false;
            }

            @Override
            public boolean complete() {
                return complete(null);
            }

            @Override
            public boolean completeExceptionally(final Throwable throwable) {
                if (completed.compareAndSet(false, true)) {
                    record(Error.create(self.uri, throwable, description));
                    return true;
                }

                return false;
            }

            @Override
            public void close() {
                complete();
            }
        };
    }

    @Override
    public Meter commence(final int total, final NamedUnit unit, final String format, final Object... args) {
        // determine a description
        final String description = format == null || format.isEmpty()
            ? UUID.randomUUID().toString()
            : String.format(format, args);

        // determine the maximum
        final int maximum = Math.max(0, total);

        // publish that the Activity is commencing
        final var commenced = record(Commenced.create(this.uri, description));

        // publish the initial progress value
        record(Progress.create(this.uri, unit, 0, maximum, description));

        final AtomicBoolean completed = new AtomicBoolean(false);
        final AtomicInteger progress = new AtomicInteger(0);
        final var self = this;

        return new Meter() {
            @Override
            public boolean progress(final int delta, final String format, final Object... arguments) {
                // NOTE: compareAndSet(false, false) is a non-atomic read — it returns true if
                // the current value is false but does not change it. A concurrent call to
                // complete() can set `completed` to true between this check and the record()
                // call below, resulting in a spurious Progress event after the Completed event.
                // The window is very narrow and the consequence is minor, but it is a real race.
                if (completed.compareAndSet(false, false)) {
                    // attempt to update the progress (not going over the maximum)
                    final int previous = progress.getAndUpdate(current -> Math.min((current + delta), maximum));
                    final int current = progress.get();

                    // determine the message to use
                    final String message = format == null || format.isEmpty()
                        ? description
                        : String.format(format, arguments);

                    // only output Progress when it's changed
                    if (current != previous) {
                        record(Progress.create(self.uri, unit, current, maximum, message));
                    }

                    return true;
                }

                return false;
            }

            @Override
            public <T> boolean complete(final T value) {
                if (completed.compareAndSet(false, true)) {

                    // output completed Progress if we're not already completed
                    if (progress.get() != maximum) {
                        progress.set(maximum);
                        record(Progress.create(self.uri, unit, maximum, maximum, description));
                    }

                    record(Completed.create(self.uri, value, commenced.instant(), description));

                    return true;
                }

                return false;
            }

            @Override
            public boolean complete() {
                return complete(null);
            }

            @Override
            public boolean completeExceptionally(final Throwable throwable) {
                if (completed.compareAndSet(false, true)) {
                    record(Error.create(self.uri, throwable, description));
                    return true;
                }

                return false;
            }

            @Override
            public void close() {
                complete();
            }
        };
    }
}
