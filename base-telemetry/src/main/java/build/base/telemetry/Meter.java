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

/**
 * Represents an {@link Activity} being performed for which incremental {@link Telemetry} will be recorded.
 *
 * @author brian.oliver
 * @since Mar-2021
 */
public interface Meter
    extends Activity {

    /**
     * Notifies the {@link Meter} that the specified amount of progress towards completion has been made,
     * with the provided formatted message.
     *
     * @param delta the amount of progress towards completion that was made
     * @param format the @code null}able formatted {@link String}
     * @param arguments the arguments for the formatted message
     *
     * @return {@code true} if the {@link Meter} progress was updated,
     *         {@code false} if the {@link Meter} was previously completed
     */
    boolean progress(int delta, String format, Object... arguments);

    /**
     * Notifies the {@link Meter} that the specified amount of progress towards completion has been made.
     *
     * @param delta the amount of progress towards completion that was made
     *
     * @return {@code true} if the {@link Meter} progress was updated,
     *         {@code false} if the {@link Meter} was previously completed
     */
    default boolean progress(final int delta) {
        return progress(delta, null);
    }

    /**
     * Notifies the {@link Meter} that a single unit of progress towards completion has been made,
     * with the provided formatted message.
     *
     * @param format the {@code null}able formatted {@link String}
     * @param arguments the arguments for the formatted message
     *
     * @return {@code true} if the {@link Meter} progress was updated,
     *         {@code false} if the {@link Meter} was previously completed
     */
    default boolean progress(final String format, final Object... arguments) {
        return progress(1, format, arguments);
    }

    /**
     * Notifies the {@link Meter} that a single unit of progress towards completion has been made.
     *
     * @return {@code true} if the {@link Meter} progress was updated,
     *         {@code false} if the {@link Meter} was previously completed
     */
    default boolean progress() {
        return progress(1);
    }
}
