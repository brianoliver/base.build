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
import java.util.Objects;

/**
 * A {@link TelemetryRecorder} that does nothing with provided {@link Telemetry}.
 *
 * @author brian.oliver
 * @since May-2024
 */
public final class NoOpTelemetryRecorder
    extends AbstractTelemetryRecorder {

    /**
     * Constructs a {@link NoOpTelemetryRecorder} with the specified {@link URI}.
     *
     * @param uri the {@link URI} to use for the {@link TelemetryRecorder}
     */
    private NoOpTelemetryRecorder(final URI uri) {
        super(Objects.requireNonNull(uri, "The URI must not be null"));
    }

    @Override
    protected <T extends Telemetry> T record(final T telemetry) {
        // simply return the created Telemetry
        return telemetry;
    }

    @Override
    public TelemetryRecorderFactory factory() {
        return NoOpTelemetryRecorder::new;
    }

    /**
     * Constructs a {@link NoOpTelemetryRecorder} with a default {@link URI}.
     *
     * @return a new {@link NoOpTelemetryRecorder}
     */
    public static NoOpTelemetryRecorder create() {
        return of(UniformResource.createURI("telemetry/noop"));
    }

    /**
     * Constructs a {@link NoOpTelemetryRecorder} with the specified {@link URI}.
     *
     * @param uri the {@link URI} to use for the {@link TelemetryRecorder}
     * @return a new {@link NoOpTelemetryRecorder}
     */
    public static NoOpTelemetryRecorder of(final URI uri) {
        Objects.requireNonNull(uri, "The URI must not be null");
        return new NoOpTelemetryRecorder(uri);
    }
}
