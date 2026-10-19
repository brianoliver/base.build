package build.base.retryable.option;

/*-
 * #%L
 * base.build Retryable
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
 * An {@link Option} to represent a minimum delay.
 *
 * @author brian.oliver
 * @since Dec-2017
 */
public class MinimumDelay
    extends AbstractValueOption<Duration> {

    /**
     * Constructs a {@link MinimumDelay} with a specified {@link Duration}.
     *
     * @param duration the {@link Duration}
     */
    private MinimumDelay(final Duration duration) {
        super(duration);
    }

    /**
     * Determine if the {@link MinimumDelay} is none (zero).
     *
     * @return <code>true</code> if the {@link MinimumDelay} is none, <code>false</code> otherwise
     */
    public boolean isNone() {
        return get().isZero();
    }

    /**
     * Obtains a {@link MinimumDelay} based on the specified {@link Duration}.
     *
     * @param duration the {@link Duration}
     * @return the {@link MinimumDelay}
     */
    public static MinimumDelay of(final Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return none();
        }
        else {
            return new MinimumDelay(duration);
        }
    }

    /**
     * Auto-detects a {@link MinimumDelay} based on the context of the calling {@link Thread}.
     *
     * @return the default {@link MinimumDelay}
     */
    @Default
    public static MinimumDelay autodetect() {
        return NONE;
    }

    /**
     * A {@link MinimumDelay} of none.
     *
     * @return a {@link MinimumDelay} of none
     */
    public static MinimumDelay none() {
        return NONE;
    }

    /**
     * No {@link MinimumDelay}.
     */
    private static final MinimumDelay NONE = new MinimumDelay(Duration.ZERO);
}
