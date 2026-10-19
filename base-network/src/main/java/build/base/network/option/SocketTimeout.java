package build.base.network.option;

/*-
 * #%L
 * base.build Network
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

import build.base.configuration.Default;
import build.base.configuration.Option;
import build.base.option.Timeout;

import java.net.Socket;
import java.time.Duration;

/**
 * An {@link Option} specifying how long to try to connect to a {@link Socket} before timing out.
 *
 * @author graeme.campbell
 * @since Mar-2019
 */
public class SocketTimeout
    extends Timeout {

    /**
     * A default {@link SocketTimeout} of {@link Duration#ZERO}.
     */
    private static final SocketTimeout DEFAULT = new SocketTimeout(Duration.ZERO);

    /**
     * Creates a {@link SocketTimeout} with the specified {@link Duration}.
     *
     * @param duration the {@link Duration} of the timeout
     */
    private SocketTimeout(final Duration duration) {
        super(duration);
    }

    /**
     * Creates a {@link SocketTimeout} with the provided {@link Duration}. If the {@link Duration} is less than or
     * equal to zero or null the {@link Socket} will wait indefinitely.
     *
     * @param duration the {@link Duration} that the {@link Socket} should wait before timing out
     * @return a {@link SocketTimeout} with the specified {@link Duration}
     */
    public static SocketTimeout of(final Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return DEFAULT;
        }
        return new SocketTimeout(duration);
    }

    /**
     * Creates a {@link SocketTimeout} with the default {@link Duration} of {@link Duration#ZERO}. This will cause
     * the {@link Socket} to wait indefinitely.
     *
     * @return a {@link SocketTimeout} with the default timeout of {@link Duration#ZERO}
     */
    @Default
    public static SocketTimeout getDefault() {
        return DEFAULT;
    }

    /**
     * Obtains a {@link SocketTimeout} based on the specified duration in milliseconds.
     *
     * @param millis the number of milliseconds the {@link SocketTimeout} should last
     * @return the {@link SocketTimeout}
     */
    public static SocketTimeout ofMillis(final long millis) {
        return SocketTimeout.of(Duration.ofMillis(millis));
    }

    /**
     * Obtains a {@link SocketTimeout} based on the specified duration in seconds.
     *
     * @param seconds the number of seconds the {@link SocketTimeout} should last
     * @return the {@link SocketTimeout}
     */
    public static SocketTimeout ofSeconds(final long seconds) {
        return SocketTimeout.of(Duration.ofSeconds(seconds));
    }

    /**
     * Obtains a {@link SocketTimeout} based on the specified duration in minutes.
     *
     * @param minutes the number of minutes the {@link SocketTimeout} should last
     * @return the {@link SocketTimeout}
     */
    public static SocketTimeout ofMinutes(final long minutes) {
        return SocketTimeout.of(Duration.ofMinutes(minutes));
    }
}
