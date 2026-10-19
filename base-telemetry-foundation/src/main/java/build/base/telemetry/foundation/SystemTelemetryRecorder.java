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

import build.base.foundation.UniformResource;
import build.base.telemetry.Telemetry;
import build.base.telemetry.TelemetryRecorder;
import build.base.telemetry.TelemetryRecorderFactory;

import java.net.URI;

/**
 * A {@link TelemetryRecorder} that writes {@link Telemetry} to {@link System#out} and {@link System#err}.
 *
 * @author brian.oliver
 * @since May-2025
 */
public class SystemTelemetryRecorder
    extends PrintStreamTelemetryRecorder {

    /**
     * Constructs a new {@link SystemTelemetryRecorder}.
     *
     * @param uri the {@link URI} of the {@link TelemetryRecorder}
     */
    protected SystemTelemetryRecorder(final URI uri) {
        super(uri, System.out, System.err);
    }

    @Override
    public TelemetryRecorderFactory factory() {
        return SystemTelemetryRecorder::of;
    }

    /**
     * Creates a new {@link SystemTelemetryRecorder} with the default URI.
     * <p>
     * The default URI is {@code telemetry://system/}.
     *
     * @return a new {@link SystemTelemetryRecorder}
     */
    public static SystemTelemetryRecorder create() {
        return SystemTelemetryRecorder.of(UniformResource.createURI("telemetry", "//system/"));
    }

    /**
     * Creates a new {@link SystemTelemetryRecorder}.
     *
     * @param uri the {@link URI} of the {@link TelemetryRecorder}
     * @return a new {@link SystemTelemetryRecorder}
     */
    public static SystemTelemetryRecorder of(final URI uri) {
        return new SystemTelemetryRecorder(uri);
    }
}
