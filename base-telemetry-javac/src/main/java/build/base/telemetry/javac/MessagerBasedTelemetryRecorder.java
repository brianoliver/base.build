package build.base.telemetry.javac;

/*-
 * #%L
 * base.build Telemetry (javac)
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
import build.base.telemetry.Advice;
import build.base.telemetry.Commenced;
import build.base.telemetry.Completed;
import build.base.telemetry.Error;
import build.base.telemetry.Fatal;
import build.base.telemetry.Information;
import build.base.telemetry.Notification;
import build.base.telemetry.Progress;
import build.base.telemetry.Telemetry;
import build.base.telemetry.TelemetryRecorder;
import build.base.telemetry.TelemetryRecorderFactory;
import build.base.telemetry.Warning;
import build.base.telemetry.foundation.AbstractTelemetryRecorder;

import java.net.URI;
import java.util.Objects;
import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

/**
 * A {@link Messager} based {@link TelemetryRecorder}.
 *
 * @author brian.oliver
 * @since May-2024
 */
public class MessagerBasedTelemetryRecorder
    extends AbstractTelemetryRecorder {

    /**
     * The underlying {@link Messager} to which {@link Telemetry} will be output.
     */
    private final Messager messager;

    /**
     * Constructs a {@link MessagerBasedTelemetryRecorder}.
     *
     * @param uri      the {@link URI} of the {@link TelemetryRecorder}
     * @param messager the {@link Messager}
     */
    private MessagerBasedTelemetryRecorder(final URI uri,
                                           final Messager messager) {

        super(Objects.requireNonNull(uri, "The URI must not be null"));
        this.messager = Objects.requireNonNull(messager, "The Messager must not be null");
    }

    @Override
    protected <T extends Telemetry> T record(final T telemetry) {

        Objects.requireNonNull(telemetry, "The Telemetry must not be null");

        final var element = telemetry.locations()
            .flatMap(location -> location.as(Element.class).stream())
            .findFirst()
            .orElse(null);

        final var annotationMirror = telemetry.locations()
            .flatMap(location -> location.as(AnnotationMirror.class).stream())
            .findFirst()
            .orElse(null);

        final var annotationValue = telemetry.locations()
            .flatMap(location -> location.as(javax.lang.model.element.AnnotationValue.class).stream())
            .findFirst()
            .orElse(null);

        // forward the Telemetry to the Message (based on its type)
        if (telemetry instanceof Advice advice) {
            messager.printMessage(
                Diagnostic.Kind.NOTE,
                telemetry.message(),
                element,
                annotationMirror,
                annotationValue);
        } else if (telemetry instanceof Commenced commenced) {
            this.messager.printMessage(
                Diagnostic.Kind.NOTE,
                telemetry.toString(),
                element,
                annotationMirror,
                annotationValue);

        } else if (telemetry instanceof Completed<?> completed) {
            this.messager.printMessage(
                Diagnostic.Kind.NOTE,
                telemetry.toString(),
                element,
                annotationMirror,
                annotationValue);

        } else if (telemetry instanceof build.base.telemetry.Diagnostic diagnostic) {
            this.messager.printMessage(
                Diagnostic.Kind.OTHER,
                telemetry.toString(),
                element,
                annotationMirror,
                annotationValue);

        } else if (telemetry instanceof Error error) {
            this.messager.printMessage(
                Diagnostic.Kind.ERROR,
                telemetry.message(),
                element,
                annotationMirror,
                annotationValue);

        } else if (telemetry instanceof Fatal fatal) {
            this.messager.printMessage(
                Diagnostic.Kind.ERROR,
                telemetry.message(),
                element,
                annotationMirror,
                annotationValue);

        } else if (telemetry instanceof Information information) {
            messager.printMessage(
                javax.tools.Diagnostic.Kind.NOTE,
                telemetry.message(),
                element,
                annotationMirror,
                annotationValue);
        } else if (telemetry instanceof Notification notification) {
            messager.printMessage(
                Diagnostic.Kind.NOTE,
                telemetry.message(),
                element,
                annotationMirror,
                annotationValue);
        } else if (telemetry instanceof Progress progress) {
            messager.printMessage(
                Diagnostic.Kind.NOTE,
                telemetry.toString(),
                element,
                annotationMirror,
                annotationValue);
        } else if (telemetry instanceof Warning warning) {
            messager.printMessage(
                Diagnostic.Kind.WARNING,
                telemetry.message(),
                element,
                annotationMirror,
                annotationValue);
        }

        return telemetry;
    }


    @Override
    public TelemetryRecorderFactory factory() {
        return uri -> new MessagerBasedTelemetryRecorder(uri, this.messager);
    }

    /**
     * Creates a {@link MessagerBasedTelemetryRecorder}.
     *
     * @param messager the {@link Messager}
     * @return a new {@link MessagerBasedTelemetryRecorder}
     */
    public static MessagerBasedTelemetryRecorder of(final Messager messager) {
        return new MessagerBasedTelemetryRecorder(
            UniformResource.createURI("javac", "messager"),
            messager);
    }
}
