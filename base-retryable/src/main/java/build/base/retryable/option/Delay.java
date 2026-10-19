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
 * An {@link Option} to represent a delay.
 *
 * @author brian.oliver
 * @since Dec-2017
 */
public class Delay
    extends AbstractValueOption<Duration> {

    /**
     * Constructs a {@link Delay} with a specified {@link Duration}.
     *
     * @param duration the {@link Duration}
     */
    private Delay(final Duration duration) {
        super(duration);
    }

    /**
     * Obtains a {@link Delay} based on the specified {@link Duration}.
     *
     * @param duration the {@link Duration}
     * @return the {@link Delay}
     */
    public static Delay of(final Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return none();
        }
        else {
            return new Delay(duration);
        }
    }

    /**
     * Obtains a {@link Delay} based on the specified {@link Duration}.
     * <p>
     * This is an alias of the {@link #of(Duration)} method to improve readability in some circumstances.
     *
     * @param duration the {@link Duration}
     * @return the {@link Delay}
     */
    public static Delay by(final Duration duration) {
        return of(duration);
    }

    /**
     * Auto-detects a {@link Delay} based on the context of the calling {@link Thread}.
     *
     * @return the default {@link Delay}
     */
    @Default
    public static Delay autodetect() {
        return NONE;
    }

    /**
     * A {@link Delay} of none.
     *
     * @return a {@link Delay} of none
     */
    public static Delay none() {
        return NONE;
    }

    /**
     * No {@link Delay}.
     */
    private static final Delay NONE = new Delay(Duration.ZERO);
}
