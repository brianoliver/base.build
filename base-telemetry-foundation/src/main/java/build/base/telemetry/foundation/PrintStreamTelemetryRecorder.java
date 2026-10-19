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

import build.base.telemetry.Error;
import build.base.telemetry.Fatal;
import build.base.telemetry.Telemetry;
import build.base.telemetry.TelemetryRecorder;
import build.base.telemetry.TelemetryRecorderFactory;
import build.base.telemetry.Warning;

import java.io.PrintStream;
import java.net.URI;
import java.util.Objects;

/**
 * A {@link TelemetryRecorder} that writes telemetry data to {@link PrintStream}s.
 *
 * @author brian.oliver
 * @since May-2025
 */
public class PrintStreamTelemetryRecorder
    extends AbstractTelemetryRecorder {

    /**
     * The {@link PrintStream} for standard output.
     */
    private final PrintStream outputStream;

    /**
     * The {@link PrintStream} for error output.
     */
    private final PrintStream errorStream;

    /**
     * Constructs a new {@link PrintStreamTelemetryRecorder}.
     *
     * @param uri          the {@link URI} of the {@link TelemetryRecorder}
     * @param outputStream the {@link PrintStream} for standard output
     * @param errorStream  the {@link PrintStream} for error output
     */
    protected PrintStreamTelemetryRecorder(final URI uri,
                                           final PrintStream outputStream,
                                           final PrintStream errorStream) {

        super(uri);

        this.outputStream = Objects.requireNonNull(outputStream, "The Output PrintStream must not be null");
        this.errorStream = errorStream == null ? outputStream : errorStream;
    }

    @Override
    protected <T extends Telemetry> T record(final T telemetry) {

        if (telemetry instanceof Error || telemetry instanceof Fatal || telemetry instanceof Warning) {
            this.errorStream.println(telemetry);
        } else {
            this.outputStream.println(telemetry);
        }

        return telemetry;
    }

    @Override
    public TelemetryRecorderFactory factory() {
        return uri -> new PrintStreamTelemetryRecorder(uri, this.outputStream, this.errorStream);
    }

    /**
     * Creates a new {@link PrintStreamTelemetryRecorder}.
     *
     * @param uri          the {@link URI} of the {@link TelemetryRecorder}
     * @param outputStream the {@link PrintStream} for standard output
     * @param errorStream  the {@link PrintStream} for error output
     * @return a new {@link PrintStreamTelemetryRecorder}
     */
    public static PrintStreamTelemetryRecorder of(final URI uri,
                                                  final PrintStream outputStream,
                                                  final PrintStream errorStream) {

        return new PrintStreamTelemetryRecorder(uri, outputStream, errorStream);
    }
}
