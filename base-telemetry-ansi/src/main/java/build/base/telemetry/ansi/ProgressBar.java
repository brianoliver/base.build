package build.base.telemetry.ansi;

/*-
 * #%L
 * base.build Telemetry (ANSI)
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

import build.base.foundation.Capture;
import build.base.foundation.Strings;
import build.base.telemetry.Meter;
import build.base.telemetry.NamedUnit;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An ANSI-based progress bar for representing the progress of an underling {@link Meter}.
 *
 * @author brian.oliver
 * @since May-2025
 */
public class ProgressBar
    implements Meter {

    /**
     * The {@link ANSIBasedTelemetryRecorder} that created the {@link ProgressBar}.
     */
    private final ANSIBasedTelemetryRecorder telemetryRecorder;

    /**
     * The underlying {@link Meter} for which this {@link ProgressBar} tracks progress.
     */
    private final Meter meter;

    /**
     * The total progress possible for the {@link Meter}.
     */
    private final int total;

    /**
     * The current progress made towards the total.
     */
    private final AtomicInteger progress;

    /**
     * The currently {@link Capture}d message for the {@link ProgressBar}.
     */
    private final Capture<String> message;

    /**
     * The {@link NamedUnit} of measure for the progress tracked by this {@link ProgressBar}.
     */
    private final NamedUnit unit;

    /**
     * The {@link Instant} when the {@link ProgressBar} commenced.
     */
    private final Instant commenced;

    /**
     * Constructs a new {@link ProgressBar} with the specified total and {@link NamedUnit}.
     *
     * @param telemetryRecorder the {@link build.base.telemetry.TelemetryRecorder} to manage rendering of this {@link ProgressBar}
     * @param meter             the {@link Meter} to track progress for
     * @param total             the total amount of progress to be made
     * @param unit              the {@link NamedUnit} of measure for the progress
     * @param format            the initial formatted message for the {@link ProgressBar}
     * @param args              the arguments for the formatted message
     */
    public ProgressBar(final ANSIBasedTelemetryRecorder telemetryRecorder,
                       final Meter meter,
                       final int total,
                       final NamedUnit unit,
                       final String format,
                       final Object... args) {

        this.telemetryRecorder = Objects.requireNonNull(telemetryRecorder, "The TelemetryRecorder must not be null");
        this.meter = Objects.requireNonNull(meter, "The Meter must not be null");
        this.total = total;
        this.unit = Objects.requireNonNull(unit, "The NamedUnit must not be null");
        this.progress = new AtomicInteger(0);
        this.message = Capture.of(String.format(format, args));
        this.commenced = Instant.now();
    }

    @Override
    public boolean progress(final int delta, final String format, final Object... arguments) {
        this.progress.getAndUpdate(current -> Math.min(Math.max(0, current + delta), this.total));

        if (!Strings.isEmpty(format)) {
            this.message.set(String.format(format, arguments));
        }

        return this.meter.progress(delta, format, arguments);
    }

    @Override
    public <T> boolean complete(final T value) {
        return this.meter.complete(value);
    }

    @Override
    public boolean complete() {
        return this.meter.complete();
    }

    @Override
    public boolean completeExceptionally(final Throwable throwable) {
        return this.meter.completeExceptionally(throwable);
    }

    @Override
    public void close() {
        this.progress.set(this.total);
        this.meter.close();

        // remove this ProgressBar from the ActivityManager
        this.telemetryRecorder.remove(this);
    }

    @Override
    public String toString() {
        final var progress = this.progress.get();
        final var percentage = this.total == 0 ? 0 : (int) ((double) progress / this.total * 100);

        final StringBuilder builder = new StringBuilder();
        // set the color to green
        builder.append("\u001b[92m"); // ANSI escape code for green text
        builder.append(String.format("%3d%%", percentage));
        builder.append("\u001b[0m"); // ANSI escape code to reset text color
        builder.append(" [");

        final var width = 20; // total width of the progress bar
        final var charactersPerPercentage = 100 / width; // how many percentage points each character represents
        final var filledLength = percentage / charactersPerPercentage;
        builder.append("\u001b[94m"); // ANSI escape code for blue text
        builder.append(Strings.repeat("#", filledLength));
        builder.append("\u001b[0m"); // ANSI escape code to reset text color
        builder.append(Strings.repeat(" ", width - filledLength));

        builder.append("]");
        this.message.ifPresent(message -> {
            builder.append(" ");
            builder.append(message);
        });
        if (this.unit.isEmpty()) {
            builder.append(String.format(" (%d of %d completed)", progress, this.total));
        } else {
            builder.append(String.format(" (%d %s of %d %s completed)",
                progress, this.unit.nameFor(progress),
                this.total, this.unit.nameFor(this.total)));
        }

        // append the elapsed time and estimated time to completion when elapsed >= 3 seconds and estimated total >= 10 seconds
        if (progress > 0) {
            final var elapsed = Duration.between(this.commenced, Instant.now());

            if (elapsed.toSeconds() >= 3) {
                final var estimatedTotalMillis = (long) ((double) elapsed.toMillis() * this.total / progress);

                if (estimatedTotalMillis >= 10_000) {
                    final var remainingMillis = Math.max(0, estimatedTotalMillis - elapsed.toMillis());
                    final var remaining = Duration.ofMillis(remainingMillis);

                    final String elapsedLabel = Strings.of(elapsed);
                    final String remainingLabel = Strings.of(remaining);

                    if (!elapsedLabel.isBlank() && !remainingLabel.isBlank()) {
                        builder.append(" (");
                        builder.append(elapsedLabel);
                        builder.append(" elapsed, about ");
                        builder.append(remainingLabel);
                        builder.append(" remaining)");
                    }
                }
            }
        }

        builder.append("\u001b[0K"); // ANSI clear to end of line to prevent rendering artifacts

        return builder.toString();
    }

}
