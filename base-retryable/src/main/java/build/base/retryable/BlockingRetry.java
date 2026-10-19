package build.base.retryable;

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

import build.base.configuration.Configuration;
import build.base.configuration.Option;
import build.base.option.Timeout;
import build.base.retryable.option.Delay;
import build.base.retryable.option.MaximumDelay;
import build.base.retryable.option.MinimumDelay;
import build.base.retryable.option.RetryFrequency;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Objects;

/**
 * A {@link Retryable} adapter that blocks the calling {@link Thread} while attempting to produce a value,
 * including <code>null</code> when invoking {@link Retryable#get} on a possibly faulty or unreliable underlying
 * {@link Retryable}, especially when the underlying {@link Retryable} throws a {@link EphemeralFailureException} or
 * some other recoverable {@link Exception}.
 * <p>
 * Upon calling {@link BlockingRetry#get()}, a {@link BlockingRetry} will repetitively attempt to obtain a value,
 * including <code>null</code>, from the underlying {@link Retryable} by invoking {@link Retryable#get()}, giving up
 * when either a value is produced, a specified timeout occurred, a specified number of retries is exceeded,
 * a non-recoverable {@link Exception} or a {@link PermanentFailureException} is thrown.  Should a value not be
 * obtainable after the specified timeout or number of retries is exceeded, a {@link PermanentFailureException} will
 * be thrown.
 * <p>
 * A {@link Thread} calling {@link BlockingRetry#get()} will block at most the specified {@link Timeout}
 * attempting to obtain a value from the underlying {@link Retryable}.
 *
 * @param <T> the type of value produced by the {@link Retryable}
 * @author brian.oliver
 * @see EphemeralFailureException
 * @see PermanentFailureException
 * @see Timeout
 * @see Delay
 * @see RetryFrequency
 * @since Dec-2017
 */
public class BlockingRetry<T>
    implements Retryable<T> {

    /**
     * The underlying {@link Retryable} to use and retry.
     */
    private final Retryable<T> retryable;

    /**
     * The {@link RetryFrequency} providing and {@link Iterable} of {@link Duration}s to be used
     * for backing off for retry should the underlying {@link Retryable} fail when invoking {@link Retryable#get()}.
     */
    private final RetryFrequency retryFrequency;

    /**
     * The initial {@link Duration} to delay prior to attempting to acquire a value.
     */
    private Duration initialDelay;

    /**
     * The minimum {@link Duration} for any retry.
     */
    private Duration minimumRetryDelay;

    /**
     * The maximum {@link Duration} for any retry.
     */
    private Duration maximumRetryDelay;

    /**
     * The timeout {@link Duration]} for retrying.
     */
    private Duration timeout;

    /**
     * Privately constructs a {@link BlockingRetry} for an underlying {@link Retryable} using
     * the provided {@link Option}s to specify retry and timeout constraints.
     *
     * @param retryable the underlying {@link Retryable}
     * @param options   the {@link Option}s
     */
    private BlockingRetry(final Retryable<T> retryable, final Option... options) {
        this(retryable, Configuration.of(options));
    }

    /**
     * Privately constructs a {@link BlockingRetry} for an underlying {@link Retryable} using
     * the provided {@link Configuration}s to specify retry and timeout constraints.
     *
     * @param retryable     the underlying {@link Retryable}
     * @param configuration the {@link Configuration}
     */
    private BlockingRetry(final Retryable<T> retryable,
                          final Configuration configuration) {

        Objects.requireNonNull(retryable, "The Retryable can't be null");
        Objects.requireNonNull(retryable, "The Configuration can't be null");

        // un-adapt the retryable if it's already a BlockingRetry
        this.retryable = retryable instanceof BlockingRetry ? ((BlockingRetry<T>) retryable).getRetryable() : retryable;

        // determine the initial delay prior to acquiring a value
        this.initialDelay = configuration.getValue(Delay.class);

        // determine the minimum retry duration
        this.minimumRetryDelay = configuration.getValue(MinimumDelay.class);

        // ensure the minimum retry delay isn't negative
        if (this.minimumRetryDelay.isNegative()) {
            this.minimumRetryDelay = Duration.ZERO;
        }

        // ensure the initial delay isn't less than the minimum delay
        if (this.initialDelay.compareTo(this.minimumRetryDelay) < 0) {
            this.initialDelay = this.minimumRetryDelay;
        }

        // determine the maximum retry duration
        this.maximumRetryDelay = configuration.getValue(MaximumDelay.class);

        // ensure the maximum retry delay isn't negative
        if (this.maximumRetryDelay != null && this.maximumRetryDelay.isNegative()) {
            this.maximumRetryDelay = null;
        }

        // the maximum retry duration is a Timeout
        this.timeout = configuration.getValue(Timeout.class);

        // ensure the timeout isn't negative
        if (this.timeout.isNegative()) {
            // default to the Timeout default
            this.timeout = Timeout.autodetect().get();
        }

        // ensure the maximum retry delay isn't greater than the timeout
        if (this.maximumRetryDelay != null && this.timeout.compareTo(this.maximumRetryDelay) < 0) {
            this.maximumRetryDelay = this.timeout;
        }

        // ensure that the initial delay isnt larger then the timeout
        if (this.initialDelay.compareTo(this.timeout) > 0) {
            this.initialDelay = this.timeout;
        }

        // the RetryFrequency provides retry durations
        this.retryFrequency = configuration.get(RetryFrequency.class);
    }

    @Override
    public T get() {
        // establish the remaining duration for retrying
        Duration remainingDuration = this.timeout;

        // wait the initial delay
        if (!this.initialDelay.isZero()) {

            sleep(this.initialDelay.toMillis());

            remainingDuration = remainingDuration.minus(this.initialDelay);
        }

        // acquire the durations for retry
        final Iterator<Duration> durations = this.retryFrequency.iterator();

        // attempt to acquire a value from the underlying retryable
        while (!remainingDuration.isNegative() && !remainingDuration.isZero()) {
            // determine when we attempted to acquire a value
            final Instant started = Instant.now();

            try {
                return this.retryable.get();
            }
            catch (final PermanentFailureException e) {
                // we can't retry if the underlying retryable has failed
                throw e;
            }
            catch (final UnsupportedOperationException e) {
                // we assume operation not supported means failure
                throw new PermanentFailureException(e);
            }
            catch (final EphemeralFailureException e) {
                // we can retry if the underlying retryable isn't ready and not done!
                if (this.retryable.isDone()) {
                    throw new PermanentFailureException(e);
                }
            }
            catch (final AssertionError | RuntimeException e) {
                // we assume any AssertionError(s) and RuntimeException(s) are retryable
            }

            // determine when we stopped attempting to acquire a value
            // (this may be behind the start instant if there's clock jitter)
            final Instant stopped = Instant.now();

            // determine the elapsed time
            final Duration elapsed = Duration.between(started, stopped);

            // adjust the remaining time (when non-negative)
            if (!elapsed.isNegative()) {
                remainingDuration = remainingDuration.minus(elapsed);
            }

            // wait some time before retrying
            if (!remainingDuration.isNegative() && !remainingDuration.isZero()) {

                if (durations.hasNext()) {
                    // obtain the next duration to wait
                    Duration proposedWaitingDuration = durations.next();

                    // ensure the proposed duration is not less than the minimum retry duration
                    if (proposedWaitingDuration.compareTo(this.minimumRetryDelay) < 0) {
                        proposedWaitingDuration = this.minimumRetryDelay;
                    }

                    // ensure the proposed duration is not greater than the maximum retry duration
                    if (this.maximumRetryDelay != null
                        && proposedWaitingDuration.compareTo(this.maximumRetryDelay) > 0) {
                        proposedWaitingDuration = this.maximumRetryDelay;
                    }

                    if (!proposedWaitingDuration.isNegative() && !proposedWaitingDuration.isZero()) {

                        final Duration waitingDuration;

                        // don't wait longer than the remaining duration
                        waitingDuration = proposedWaitingDuration.toMillis() > remainingDuration.toMillis()
                            ? remainingDuration
                            : proposedWaitingDuration;

                        // now wait the required duration
                        sleep(waitingDuration.toMillis());

                        // adjust the remaining time (assume we waited for at least the specified time)
                        remainingDuration = remainingDuration.minus(waitingDuration);
                    }
                }
                else {
                    throw new PermanentFailureException("Timed out attempting to resolve a value");
                }
            }
        }

        // try a final time before timing out
        try {
            return this.retryable.get();
        }
        catch (final PermanentFailureException e) {
            throw e;
        }
        catch (final Throwable e) {
            // failed to determine a value with in the specified timeout constraints
            throw new PermanentFailureException(e);
        }
    }

    @Override
    public boolean isDone() {
        return this.retryable.isDone();
    }

    /**
     * Obtains the underlying {@link Retryable} that will be retried if necessary.
     *
     * @return the underlying {@link Retryable}
     */
    public Retryable<T> getRetryable() {
        return this.retryable;
    }

    @Override
    public String toString() {
        return "BlockingRetry{retryable=" + this.retryable + "}";
    }

    /**
     * Obtains a {@link BlockingRetry} for the specified {@link Retryable}.
     *
     * @param <T>           the type of the {@link Retryable} value
     * @param retryable     the {@link Retryable}
     * @param configuration the {@link Configuration} for retrying
     * @return a {@link BlockingRetry}
     */
    public static <T> BlockingRetry<T> of(final Retryable<T> retryable,
                                          final Configuration configuration) {

        Objects.requireNonNull(retryable, "The retryable can't be null");

        return retryable instanceof BlockingRetry
            ? (BlockingRetry<T>) retryable
            : new BlockingRetry<>(retryable, configuration == null ? Configuration.empty() : configuration);
    }

    /**
     * Obtains a {@link BlockingRetry} for the specified {@link Retryable}.
     *
     * @param <T>       the type of the {@link Retryable} value
     * @param retryable the {@link Retryable}
     * @param options   the {@link Option}s for retrying
     * @return a {@link BlockingRetry}
     */
    public static <T> BlockingRetry<T> of(final Retryable<T> retryable, final Option... options) {
        return of(retryable, Configuration.of(options));
    }

    /**
     * Attempts to retry the specified {@link Retryable} using the provided {@link Option}s in
     * a blocking manner with the current thread.
     *
     * @param <T>       the type of the {@link Retryable} value
     * @param retryable the {@link Retryable}
     * @param options   the {@link Option}s for retrying
     * @return the {@link Retryable} value
     * @throws PermanentFailureException if the {@link Retryable} can't produce a value
     */
    public static <T> T retry(final Retryable<T> retryable, final Option... options) {

        // create a BlockingRetry from the Retryable
        final BlockingRetry<T> blockingRetry = BlockingRetry.of(retryable, options);

        // attempt to get the result
        return blockingRetry.get();
    }

    /**
     * Sleep the {@link Thread#currentThread()} for the specified milliseconds and rethrow any
     * {@link InterruptedException} as a {@link PermanentFailureException}s.
     *
     * @param millis milliseconds to sleep the {@link Thread#currentThread()} for
     */
    private static void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        }
        catch (final InterruptedException e) {
            // ensure we're interrupted
            Thread.currentThread().interrupt();

            //re-throw the interrupt as a permanent failure
            throw new PermanentFailureException(e);
        }
    }
}
