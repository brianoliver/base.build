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
 * An {@link Option} to represent a maximum delay.
 *
 * @author brian.oliver
 * @since Dec-2017
 */
public class MaximumDelay
    extends AbstractValueOption<Duration> {

    /**
     * Constructs a {@link MaximumDelay} with a specified {@link Duration}.
     *
     * @param duration the {@link Duration}, <code>null</code> meaning forever
     */
    private MaximumDelay(final Duration duration) {
        super(duration == null ? Duration.ofMillis(Long.MAX_VALUE) : duration);
    }

    /**
     * Determine if the {@link MaximumDelay} is forever.
     *
     * @return <code>true</code> if the {@link MaximumDelay} is forever, <code>false</code> otherwise
     */
    public boolean isForever() {
        return get().toMillis() == Long.MAX_VALUE;
    }

    /**
     * Obtains a {@link MaximumDelay} based on the specified {@link Duration}.
     *
     * @param duration the {@link Duration}
     * @return the {@link MaximumDelay}
     */
    public static MaximumDelay of(final Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return forever();
        }
        else {
            return new MaximumDelay(duration);
        }
    }

    /**
     * Obtains the default {@link MaximumDelay} based on the context of the calling {@link Thread}.
     *
     * @return the default {@link MaximumDelay}
     */
    @Default
    public static MaximumDelay autodetect() {
        return DEFAULT;
    }

    /**
     * The forever {@link MaximumDelay}.
     *
     * @return a {@link MaximumDelay}
     */
    public static MaximumDelay forever() {
        return FOREVER;
    }

    /**
     * No {@link MaximumDelay}.
     */
    private static final MaximumDelay FOREVER = new MaximumDelay(null);

    /**
     * The default {@link MaximumDelay} (250 milliseconds).
     */
    private static final MaximumDelay DEFAULT = new MaximumDelay(Duration.ofMillis(250));
}
