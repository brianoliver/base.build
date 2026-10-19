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
 * Represents an activity being performed, for which {@link Telemetry} will be recorded.
 *
 * @author brian.oliver
 * @since Mar-2021
 */
public interface Activity
    extends AutoCloseable {

    /**
     * The {@link Activity} was completed successfully with the specified result.
     *
     * @param <T> the type of the completed value
     * @param value the completed value
     * @return {@code true} if the {@link Activity} was completed,
     *         {@code false} if the {@link Activity} was previously completed
     */
    <T> boolean complete(T value);

    /**
     * The {@link Activity} was completed successfully (without a result).
     *
     * @return {@code true} if the {@link Activity} was completed,
     *         {@code false} if the {@link Activity} was previously completed
     */
    boolean complete();

    /**
     * The {@link Activity} failed due to the specified {@link Throwable}.
     *
     * @param throwable the {@link Throwable} causing the failure
     * @return {@code true} if the {@link Activity} was completed,
     *         {@code false} if the {@link Activity} was previously completed
     */
    boolean completeExceptionally(Throwable throwable);

    /**
     * Signals that the {@link Activity} is no longer being performed.   Should {@link #complete()},
     * {@link #complete(Object)} or {@link #completeExceptionally(Throwable)} not be invoked prior to being closed,
     * the {@link Activity} is automatically {@link #complete()}d.
     */
    @Override
    void close();
}
