package build.base.option;

/*-
 * #%L
 * base.build Option
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

import build.base.configuration.AbstractValueOption;
import build.base.configuration.Default;
import build.base.configuration.Option;

import java.time.Duration;

/**
 * An {@link Option} to represent a timeout.
 *
 * @author brian.oliver
 * @since Dec-2017
 */
public class Timeout
    extends AbstractValueOption<Duration> {

    /**
     * Constructs a {@link Timeout} with a specified {@link Duration}.
     *
     * @param duration the {@link Duration}
     */
    public Timeout(final Duration duration) {
        super(duration);
    }

    /**
     * Obtains a {@link Timeout} based on the specified {@link Duration}.
     *
     * @param duration the {@link Duration}
     * @return the {@link Timeout}
     */
    public static Timeout of(final Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return zero();
        }
        else {
            return new Timeout(duration);
        }
    }

    /**
     * Auto-detects a {@link Timeout} based on the context of the calling {@link Thread}.
     *
     * @return the default {@link Timeout}
     */
    @Default
    public static Timeout autodetect() {
        return DEFAULT;
    }

    /**
     * A {@link Timeout} of zero.
     *
     * @return a {@link Timeout} of zero
     */
    public static Timeout zero() {
        return ZERO;
    }

    /**
     * Obtains a {@link Timeout} based on the specified duration in milliseconds.
     *
     * @param millis the number of milliseconds the {@link Timeout} should last
     * @return the {@link Timeout}
     */
    public static Timeout ofMillis(final long millis) {
        return Timeout.of(Duration.ofMillis(millis));
    }

    /**
     * Obtains a {@link Timeout} based on the specified duration in seconds.
     *
     * @param seconds the number of seconds the {@link Timeout} should last
     * @return the {@link Timeout}
     */
    public static Timeout ofSeconds(final long seconds) {
        return Timeout.of(Duration.ofSeconds(seconds));
    }

    /**
     * Obtains a {@link Timeout} based on the specified duration in minutes.
     *
     * @param minutes the number of minutes the {@link Timeout} should last
     * @return the {@link Timeout}
     */
    public static Timeout ofMinutes(final long minutes) {
        return Timeout.of(Duration.ofMinutes(minutes));
    }

    /**
     * A {@link Timeout} of zero.
     */
    private static final Timeout ZERO = new Timeout(Duration.ZERO);

    /**
     * A default {@link Timeout}.
     */
    private static final Timeout DEFAULT = new Timeout(
        Duration.ofSeconds(Integer.getInteger("build.base.timeout.default", 60)));
}
