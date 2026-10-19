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
import java.util.stream.Stream;

/**
 * Provides the ability to record various types of {@link Telemetry} to be presented to end-users and developers,
 * typically for display Integrated Development Environments.
 * <p>
 * Unlike logging and logs, which are primarily designed for technical diagnostics and debugging of source code behavior
 * by developers, {@link Telemetry} is designed to provide end-user level friendly information first, and developer
 * diagnostics second, in a form that is easily consumed.  All parts of the {@code Herculean Project} thus use
 * {@link TelemetryRecorder}s to capture and provide such information.
 * <p>
 * While end-user-focused, developers typically acquire {@link TelemetryRecorder}s for their implementations through
 * the use of dependency injection. For example:
 *
 * <pre>
 * class MyPlugin implements Plugin {
 *  {@literal @}Inject
 *  private TelemetryRecorder recorder;
 *
 *  ...
 * }</pre>
 * <p>
 * Each {@link TelemetryRecorder} method accepts {@link String}s formatted according to the Java Platform
 * {@link java.util.Formatter}. This is to allow locale-specific formatting of messages to occur. Should there are
 * more arguments than format specifiers a format {@link String}, the extra arguments are ignored. As the format
 * {@link String}s may contain a variable number of format specifiers, the number of arguments is variable and may be
 * zero. The formatting behaviour for {@code null} arguments depends on the type of format conversion.
 *
 * @author brian.oliver
 * @since Mar-2021
 */
public interface TelemetryRecorder {

    /**
     * The {@link URI} of the {@link TelemetryRecorder}.
     *
     * @return the {@link URI}
     */
    URI uri();

    /**
     * A {@link TelemetryRecorderFactory} that can be used to create new {@link TelemetryRecorder}s,
     * typically of the same type as this one.
     *
     * @return a {@link TelemetryRecorderFactory}
     */
    TelemetryRecorderFactory factory();

    /**
     * Records general purpose <i>information</i> to be presented to the end-user.
     *
     * @param locations the {@link Location}s
     * @param format    the formatted {@link String}
     * @param args      the arguments
     * @return the created {@link Information} {@link Telemetry}
     */
    Information info(Stream<? extends Location> locations, String format, Object... args);

    /**
     * Records general purpose <i>information</i> to be presented to the end-user.
     *
     * @param format the formatted {@link String}
     * @param args   the arguments
     * @return the created {@link Information} {@link Telemetry}
     */
    default Information info(final String format, final Object... args) {
        return info(Stream.empty(), format, args);
    }

    /**
     * Records general purpose <i>information</i> to be presented to the end-user.
     *
     * @param location the {@link Location}
     * @param format   the formatted {@link String}
     * @param args     the arguments
     * @return the created {@link Information} {@link Telemetry}
     */
    default Information info(final Location location, final String format, final Object... args) {
        return info(location == null ? Stream.empty() : Stream.of(location), format, args);
    }

    /**
     * Records actionable advice to be presented to the end-user.
     *
     * @param locations the {@link Location}s
     * @param format    the formatted {@link String}
     * @param args      the arguments
     * @return the created {@link Advice} {@link Telemetry}
     */
    Advice advice(Stream<? extends Location> locations, String format, Object... args);

    /**
     * Records actionable advice to be presented to the end-user.
     *
     * @param format the formatted {@link String}
     * @param args   the arguments
     * @return the created {@link Advice} {@link Telemetry}
     */
    default Advice advice(final String format, final Object... args) {
        return advice(Stream.empty(), format, args);
    }

    /**
     * Records actionable advice to be presented to the end-user.
     *
     * @param location the {@link Location}
     * @param format   the formatted {@link String}
     * @param args     the arguments
     * @return the created {@link Advice} {@link Telemetry}
     */
    default Advice advice(final Location location, final String format, final Object... args) {
        return advice(location == null ? Stream.empty() : Stream.of(location), format, args);
    }

    /**
     * Records a notification to be presented to the end-user.
     *
     * @param locations the {@link Location}
     * @param format    the formatted {@link String}
     * @param args      the arguments
     * @return the created {@link Notification} {@link Telemetry}
     */
    Notification notify(Stream<? extends Location> locations, String format, Object... args);

    /**
     * Records a notification to be presented to the end-user.
     *
     * @param format the formatted {@link String}
     * @param args   the arguments
     * @return the created {@link Notification} {@link Telemetry}
     */
    default Notification notify(final String format, final Object... args) {
        return notify(Stream.empty(), format, args);
    }

    /**
     * Records a notification to be presented to the end-user.
     *
     * @param locations the {@link Location}
     * @param format    the formatted {@link String}
     * @param args      the arguments
     * @return the created {@link Notification} {@link Telemetry}
     */
    default Notification notify(final Location locations, final String format, final Object... args) {
        return notify(locations == null ? Stream.empty() : Stream.of(locations), format, args);
    }

    /**
     * Records a <i>warning</i> to be presented to the end-user.
     *
     * @param locations the {@link Location}s
     * @param throwable the optional ({@code nullable}) {@link Throwable}
     * @param format    the formatted {@link String}
     * @param args      the arguments
     * @return the created {@link Warning} {@link Telemetry}
     */
    Warning warn(Stream<? extends Location> locations, Throwable throwable, String format, Object... args);

    /**
     * Records a <i>warning</i> to be presented to the end-user.
     *
     * @param location the {@link Location}s
     * @param format   the formatted {@link String}
     * @param args     the arguments
     * @return the created {@link Warning} {@link Telemetry}
     */
    default Warning warn(final Stream<? extends Location> location, final String format, final Object... args) {
        return warn(location, null, format, args);
    }

    /**
     * Records a <i>warning</i> to be presented to the end-user.
     *
     * @param location  the optional ({@code nullable}) {@link Location}
     * @param throwable the optional ({@code nullable}) {@link Throwable}
     * @param format    the formatted {@link String}
     * @param args      the arguments
     * @return the created {@link Warning} {@link Telemetry}
     */
    default Warning warn(final Location location,
                         final Throwable throwable,
                         final String format,
                         final Object... args) {

        return warn(location == null ? Stream.empty() : Stream.of(location), throwable, format, args);
    }

    /**
     * Records a <i>warning</i> to be presented to the end-user.
     *
     * @param location the optional ({@code nullable}) {@link Location}
     * @param format   the formatted {@link String}
     * @param args     the arguments
     * @return the created {@link Warning} {@link Telemetry}
     */
    default Warning warn(final Location location, final String format, final Object... args) {
        return warn(location == null ? Stream.empty() : Stream.of(location), null, format, args);
    }

    /**
     * Records a <i>warning</i> to be presented to the end-user.
     *
     * @param throwable the optional ({@code null}) {@link Throwable}
     * @param format    the formatted {@link String}
     * @param args      the arguments
     * @return the created {@link Warning} {@link Telemetry}
     */
    default Warning warn(final Throwable throwable, final String format, final Object... args) {
        return warn(Stream.empty(), throwable, format, args);
    }

    /**
     * Records a <i>warning</i> to be presented to the end-user.
     *
     * @param format the formatted {@link String}
     * @param args   the arguments
     * @return the created {@link Warning} {@link Telemetry}
     */
    default Warning warn(final String format, final Object... args) {
        return warn(Stream.empty(), null, format, args);
    }

    /**
     * Records a <i>error</i> to be presented to the end-user.
     *
     * @param locations the {@link Location}s
     * @param throwable the optional ({@code null}) {@link Throwable}
     * @param format    the formatted {@link String}
     * @param args      the arguments
     * @return the created {@link Error} {@link Telemetry}
     */
    Error error(Stream<? extends Location> locations, Throwable throwable, String format, Object... args);

    /**
     * Records a <i>error</i> to be presented to the end-user.
     *
     * @param locations the {@link Location}s
     * @param format    the formatted {@link String}
     * @param args      the arguments
     * @return the created {@link Error} {@link Telemetry}
     */
    default Error error(final Stream<? extends Location> locations, final String format, final Object... args) {
        return error(locations, null, format, args);
    }

    /**
     * Records a <i>error</i> to be presented to the end-user.
     *
     * @param location  the {@link Location}
     * @param throwable the optional ({@code null}) {@link Throwable}
     * @param format    the formatted {@link String}
     * @param args      the arguments
     * @return the created {@link Error} {@link Telemetry}
     */
    default Error error(final Location location, final Throwable throwable, final String format, final Object... args) {
        return error(location == null ? Stream.empty() : Stream.of(location), throwable, format, args);
    }

    /**
     * Records a <i>error</i> to be presented to the end-user.
     *
     * @param location the {@link Location}
     * @param format   the formatted {@link String}
     * @param args     the arguments
     * @return the created {@link Error} {@link Telemetry}
     */
    default Error error(final Location location, final String format, final Object... args) {
        return error(location == null ? Stream.empty() : Stream.of(location), null, format, args);
    }

    /**
     * Records a <i>error</i> to be presented to the end-user.
     *
     * @param throwable the optional ({@code null}) {@link Throwable}
     * @param format    the formatted {@link String}
     * @param args      the arguments
     * @return the created {@link Error} {@link Telemetry}
     */
    default Error error(final Throwable throwable, final String format, final Object... args) {
        return error(Stream.empty(), throwable, format, args);
    }

    /**
     * Records a <i>error</i> to be presented to the end-user.
     *
     * @param format the formatted {@link String}
     * @param args   the arguments
     * @return the created {@link Error} {@link Telemetry}
     */
    default Error error(final String format, final Object... args) {
        return error(Stream.empty(), null, format, args);
    }

    /**
     * Records a <i>fatal</i> error.
     *
     * @param locations the {@link Location}s
     * @param throwable the optional ({@code null}) {@link Throwable}
     * @param format    the formatted {@link String}
     * @param args      the arguments
     * @return the created {@link Fatal} {@link Telemetry}
     */
    Fatal fatal(Stream<? extends Location> locations, Throwable throwable, String format, Object... args);

    /**
     * Records a <i>fatal</i> error.
     *
     * @param locations the {@link Location}s
     * @param format    the formatted {@link String}
     * @param args      the arguments
     * @return the created {@link Fatal} {@link Telemetry}
     */
    default Fatal fatal(final Stream<? extends Location> locations, final String format, final Object... args) {
        return fatal(locations, null, format, args);
    }

    /**
     * Records a <i>fatal</i> error.
     *
     * @param location  the {@link Location}
     * @param throwable the optional ({@code null}) {@link Throwable}
     * @param format    the formatted {@link String}
     * @param args      the arguments
     * @return the created {@link Fatal} {@link Telemetry}
     */
    default Fatal fatal(final Location location, final Throwable throwable, final String format, final Object... args) {
        return fatal(location == null ? Stream.empty() : Stream.of(location), throwable, format, args);
    }

    /**
     * Records a <i>fatal</i> error.
     *
     * @param location the {@link Location}
     * @param format   the formatted {@link String}
     * @param args     the arguments
     * @return the created {@link Fatal} {@link Telemetry}
     */
    default Fatal fatal(final Location location, final String format, final Object... args) {
        return fatal(location == null ? Stream.empty() : Stream.of(location), null, format, args);
    }

    /**
     * Records a <i>fatal</i> error.
     *
     * @param throwable the optional ({@code null}) {@link Throwable}
     * @param format    the formatted {@link String}
     * @param args      the arguments
     * @return the created {@link Fatal} {@link Telemetry}
     */
    default Fatal fatal(final Throwable throwable, final String format, final Object... args) {
        return fatal(Stream.empty(), throwable, format, args);
    }

    /**
     * Records a <i>fatal</i> error.
     *
     * @param format the formatted {@link String}
     * @param args   the arguments
     * @return the created {@link Fatal} {@link Telemetry}
     */
    default Fatal fatal(final String format, final Object... args) {
        return fatal(Stream.empty(), null, format, args);
    }

    /**
     * Records diagnostic <i>information</i> intended to be presented to a developer.
     *
     * @param locations the {@link Location}s
     * @param format    the formatted {@link String}
     * @param args      the arguments
     * @return the created {@link Diagnostic} {@link Telemetry}
     */
    Diagnostic diagnostic(Stream<? extends Location> locations, String format, Object... args);

    /**
     * Records diagnostic <i>information</i> intended to be presented to a developer.
     *
     * @param location the {@link Location}
     * @param format   the formatted {@link String}
     * @param args     the arguments
     * @return the created {@link Diagnostic} {@link Telemetry}
     */
    default Diagnostic diagnostic(final Location location, final String format, final Object... args) {
        return diagnostic(location == null ? Stream.empty() : Stream.of(location), format, args);
    }

    /**
     * Records diagnostic <i>information</i> intended to be presented to a developer.
     *
     * @param format the formatted {@link String}
     * @param args   the arguments
     * @return the created {@link Diagnostic} {@link Telemetry}
     */
    default Diagnostic diagnostic(final String format, final Object... args) {
        return diagnostic(Stream.empty(), format, args);
    }

    /**
     * Creates an {@link Activity} in which further {@link Telemetry} will be recorded with the {@link TelemetryRecorder}.
     *
     * @param format the formatted description of the {@link Activity}
     * @param args   the formatted arguments for the {@link Activity} description
     * @return a new {@link Activity}
     */
    Activity commence(String format, Object... args);

    /**
     * Creates a {@link Meter} to track incremental progress of an {@link Activity} with the {@link TelemetryRecorder},
     * reporting progress in the specified {@link NamedUnit}.
     *
     * @param maximum the maximum amount of progress that will be metered
     * @param unit    the {@link NamedUnit} of measure for the progress
     * @param format  the formatted description of the {@link Activity}
     * @param args    the formatted arguments for the {@link Activity} description
     * @return a new {@link Meter}
     */
    Meter commence(int maximum, NamedUnit unit, String format, Object... args);

    /**
     * Creates a {@link Meter} to track incremental progress of an {@link Activity} with the {@link TelemetryRecorder}.
     *
     * @param maximum the maximum amount of progress that will be metered
     * @param format  the formatted description of the {@link Activity}
     * @param args    the formatted arguments for the {@link Activity} description
     * @return a new {@link Meter}
     */
    default Meter commence(final int maximum, final String format, final Object... args) {
        return commence(maximum, NamedUnit.none(), format, args);
    }
}
