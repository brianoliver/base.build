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

import build.base.configuration.Default;
import build.base.configuration.Option;
import build.base.foundation.Primes;
import build.base.foundation.Strings;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * An {@link Option} and {@link Iterable} of {@link Duration}s, each {@link Duration} returned specifying
 * a time to wait before or between retrying some operation.
 *
 * @author brian.oliver
 * @since Dec-2017
 */
public class RetryFrequency
    implements Option, Iterable<Duration> {

    /**
     * An {@link Iterable} for {@link Duration}s.
     */
    private final Iterable<Duration> durations;

    /**
     * Constructs a {@link RetryFrequency} based on the specified {@link Iterable} of {@link Duration}s.
     *
     * @param durations the {@link Iterable} of {@link Duration}s
     */
    private RetryFrequency(final Iterable<Duration> durations) {
        Objects.requireNonNull(durations, "Durations can't be null");

        this.durations = durations;
    }

    @Override
    public Iterator<Duration> iterator() {
        return this.durations.iterator();
    }

    /**
     * Return a {@link Supplier} of {@link Duration}s based on this frequency.
     *
     * @return the supplier
     */
    public Supplier<Duration> supplier() {
        return iterator()::next;
    }

    /**
     * Obtains a new {@link RetryFrequency} based on the current frequency but where values will not be less then a
     * specified floor value.
     *
     * @param limit the minimum duration
     * @return the limited frequency
     */
    public RetryFrequency floor(final Duration limit) {
        return within(limit, Duration.ofSeconds(Long.MAX_VALUE));
    }

    /**
     * Obtains a new {@link RetryFrequency} based on the current frequency but where values will not exceed a specified
     * ceiling value.
     *
     * @param limit the maximum duration
     * @return the limited frequency
     */
    public RetryFrequency ceil(final Duration limit) {
        return within(Duration.ZERO, limit);
    }

    /**
     * Obtains a new {@link RetryFrequency} based on the current frequency but where values will be forced to fit within
     * the specified range.
     *
     * @param floor   the minimum duration (inclusive)
     * @param ceiling the maximum duration (inclusive)
     * @return the limited frequency
     */
    public RetryFrequency within(final Duration floor, final Duration ceiling) {
        Objects.requireNonNull(floor, "floor can't be null");
        Objects.requireNonNull(ceiling, "ceiling can't be null");

        if (floor.isNegative()) {
            throw new IllegalArgumentException("floor must not be negative");
        }

        if (ceiling.compareTo(floor) < 0) {
            throw new IllegalArgumentException("ceiling must be greater than or equal to floor");
        }

        final RetryFrequency frequency = this;
        return new RetryFrequency(new Iterable<>() {
            @Override
            public Iterator<Duration> iterator() {
                return new Iterator<>() {
                    final Iterator<Duration> delegate = frequency.iterator();

                    @Override
                    public boolean hasNext() {
                        return this.delegate.hasNext();
                    }

                    @Override
                    public Duration next() {
                        final var next = this.delegate.next();
                        return next.compareTo(ceiling) > 0
                            ? ceiling
                            : next.compareTo(floor) < 0
                                ? floor
                                : next;
                    }
                };
            }

            @Override
            public String toString() {
                return "RetryFrequency.within{frequency=" + frequency + ", within={"
                    + Strings.of(floor) + ".." + Strings.of(ceiling) + "}}";
            }
        });
    }

    /**
     * Obtains a new {@link RetryFrequency} which is limited to the specified number of retry attempts and which uses
     * this {@link RetryFrequency} to determine the duration between attempts.
     * <p>
     * More specifically the {@link Iterator}s and {@link Supplier}s produced by this {@link RetryFrequency} will each
     * return the specified number of {@link Duration}s and then always return <code>false</code> for
     * {@link Iterator#hasNext()} and throw {@link RetriesExhaustedException} from {@link Iterator#next()} and
     * {@link Supplier#get()}.
     * <p>
     * If the originating frequency already represents a finite retry count the effective retry count will be the lower
     * of the two limits, i.e. this method can only further limit the retry count and can not expand it.
     *
     * @param limit the retry limit
     * @return the new frequency, or {@code this} if limit == {@link Long#MAX_VALUE}.
     */
    public RetryFrequency maxRetriesOf(final long limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit can't be negative");
        }

        if (limit == Long.MAX_VALUE) {
            return this;
        }

        return new RetryFrequency(new Iterable<>() {
            @Override
            public Iterator<Duration> iterator() {
                return new Iterator<>() {
                    final Iterator<Duration> delegate = RetryFrequency.this.iterator();
                    long count;

                    @Override
                    public boolean hasNext() {
                        return this.count < limit && this.delegate.hasNext();
                    }

                    @Override
                    public Duration next() {
                        if (hasNext()) {
                            ++this.count;

                            return this.delegate.next();
                        }

                        throw new RetriesExhaustedException("retry limit exceeded: " + RetryFrequency.this.toString());
                    }
                };
            }

            @Override
            public String toString() {
                return "RetryFrequency.retriesOf{frequency=" + RetryFrequency.this + ", limit=" + limit + "}";
            }
        });
    }

    /**
     * Obtains a {@link RetryFrequency} that produces randomized values greater than or equal to 0 and less than
     * those values provided by this {@link RetryFrequency}.
     *
     * @return a randomized {@link RetryFrequency}
     */
    public RetryFrequency randomized() {
        return randomized(this);
    }

    /**
     * Auto-detect the {@link RetryFrequency} based on the context of the calling {@link Thread}.
     *
     * @return a {@link RetryFrequency}
     */
    @Default
    public static RetryFrequency autoDetect() {
        return randomized(fibonacci(ChronoUnit.MILLIS));
    }

    /**
     * Obtains an infinite periodic {@link RetryFrequency} of a specified {@link Duration}.
     *
     * @param duration the {@link Duration}
     * @return a {@link RetryFrequency}
     */
    public static RetryFrequency every(final Duration duration) {
        Objects.requireNonNull(duration, "The Duration unit can't be null");

        return new RetryFrequency(new Iterable<Duration>() {
            @Override
            public Iterator<Duration> iterator() {
                return new Iterator<Duration>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Duration next() {
                        return duration;
                    }
                };
            }

            @Override
            public String toString() {
                return "RetryFrequency{every=" + Strings.of(duration) + "}";
            }
        });
    }

    /**
     * Obtains a {@link RetryFrequency} that produces randomized values greater than or equal to 0 and less than
     * those values provided by another {@link RetryFrequency}.
     *
     * @param retryFrequency the {@link RetryFrequency}
     * @return a randomized {@link RetryFrequency}
     */
    public static RetryFrequency randomized(final RetryFrequency retryFrequency) {
        Objects.requireNonNull(retryFrequency, "The RetryFrequency unit can't be null");

        return new RetryFrequency(new Iterable<Duration>() {
            @Override
            public Iterator<Duration> iterator() {

                // obtain an iterator of durations from the underlying RetryFrequency
                final Iterator<Duration> durations = retryFrequency.iterator();

                return new Iterator<Duration>() {
                    @Override
                    public boolean hasNext() {
                        return durations.hasNext();
                    }

                    @Override
                    public Duration next() {
                        // obtain the next duration in milliseconds
                        final long millis = durations.next().toMillis();

                        // randomize the duration
                        return millis <= 0
                            ? Duration.ZERO
                            : Duration.ofMillis(ThreadLocalRandom.current().nextLong(millis));
                    }
                };
            }

            @Override
            public String toString() {
                return "RetryFrequency.randomized{" + retryFrequency + "}";
            }
        });
    }

    /**
     * Obtains an infinite {@link RetryFrequency} that produces an increasingly larger prime-number based {@link Duration}
     * using a specific {@link ChronoUnit}, up to {@link Primes#maximum()} after which only the maximum will be returned.
     *
     * @param chronoUnit the unit of {@link Duration}
     * @return a {@link RetryFrequency}
     */
    public static RetryFrequency primes(final ChronoUnit chronoUnit) {
        Objects.requireNonNull(chronoUnit, "The ChronoUnit can't be null");

        return new RetryFrequency(new Iterable<>() {
            @Override
            public Iterator<Duration> iterator() {
                return new Iterator<>() {

                    /**
                     * Obtains an {@link Iterator} of prime numbers.
                     */
                    private final Iterator<Integer> primes = Primes.get().iterator();

                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Duration next() {
                        return Duration.of(this.primes.hasNext() ? this.primes.next() : Primes.maximum(), chronoUnit);
                    }
                };
            }

            @Override
            public String toString() {
                return "RetryFrequency.primes{" + chronoUnit.name() + "}";
            }
        });
    }

    /**
     * Obtains an infinite {@link RetryFrequency} based on a Fibonacci sequence with returned {@link Duration}s using
     * a specific {@link ChronoUnit}. Once the maximum representable Fibonacci number has been reached that value will
     * continue to be used as the frequency.
     *
     * @param chronoUnit the unit of {@link Duration}
     * @return a {@link RetryFrequency}
     */
    public static RetryFrequency fibonacci(final ChronoUnit chronoUnit) {
        Objects.requireNonNull(chronoUnit, "The ChronoUnit can't be null");

        return new RetryFrequency(new Iterable<>() {
            @Override
            public Iterator<Duration> iterator() {
                return new Iterator<>() {

                    /**
                     * The current nth value of the sequence.
                     */
                    private long first = 1;

                    /**
                     * The current n+1th value of the sequence.
                     */
                    private long second = 1;

                    /**
                     * The number of values returned.
                     */
                    private int count;

                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Duration next() {
                        final var result = this.first + this.second;
                        if (result <= 0) {
                            // we would overflow, just stick with our last value
                            return Duration.of(this.second, chronoUnit);
                        }

                        this.count++;

                        if (this.count <= 2) {
                            return Duration.of(1, chronoUnit);
                        }
                        else {
                            this.first = this.second;
                            this.second = result;
                            return Duration.of(result, chronoUnit);
                        }
                    }
                };
            }

            @Override
            public String toString() {
                return "RetryFrequency.fibonacci{" + chronoUnit.name() + "}";
            }
        });
    }

    /**
     * Obtains a {@link RetryFrequency} that provides a single {@link Duration} for retry (only once).
     * <p>
     * More specifically the {@link Iterator}s and {@link Supplier}s produced by this {@link RetryFrequency} will each
     * return a single {@link Duration} and then always return <code>false</code> for {@link Iterator#hasNext()} and
     * throw {@link RetriesExhaustedException} from {@link Iterator#next()} and {@link Supplier#get()}.
     *
     * @param duration the {@link Duration}
     * @return a {@link RetryFrequency}
     */
    public static RetryFrequency once(final Duration duration) {
        return every(duration).maxRetriesOf(1);
    }

    /**
     * Obtains a {@link RetryFrequency} that doesn't provide any retry {@link Duration}s when requested.  That is
     * all {@link Duration} {@link Iterator}s produced by the {@link RetryFrequency} will always return
     * <code>false</code> for {@link Iterator#hasNext()} and throw {@link RetriesExhaustedException} from
     * {@link Iterator#next()} and {@link Supplier#get()}.
     *
     * @return a {@link RetryFrequency}
     */
    public static RetryFrequency never() {
        return every(Duration.ZERO).maxRetriesOf(0);
    }

    /**
     * Obtains an infinite {@link RetryFrequency} that uses a randomized exponential back-off algorithm to
     * determine retry {@link Duration}s as defined by
     * <a href="https://en.wikipedia.org/wiki/Exponential_backoff">wikipedia</a> using a specific {@link ChronoUnit}.
     *
     * @param chronoUnit the unit of {@link Duration}
     * @return a {@link RetryFrequency}
     */
    public static RetryFrequency exponentialBackoff(final ChronoUnit chronoUnit) {
        Objects.requireNonNull(chronoUnit, "The ChronoUnit can't be null");

        return new RetryFrequency(new Iterable<Duration>() {
            @Override
            public Iterator<Duration> iterator() {
                return new Iterator<Duration>() {

                    /**
                     * The number of requests made thus far.
                     */
                    private int request;

                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Duration next() {
                        return Duration.of(ThreadLocalRandom.current().nextLong(
                            this.request == Long.SIZE - 1 ? Long.MAX_VALUE : 1L << this.request++), chronoUnit);
                    }
                };
            }

            @Override
            public String toString() {
                return "RetryFrequency.exponentialBackoff{" + chronoUnit + "}";
            }
        });
    }

    /**
     * An extension of {@link NoSuchElementException} indicating that the retries have been exhausted.
     */
    public static class RetriesExhaustedException
        extends NoSuchElementException {

        /**
         * Construct a {@link RetriesExhaustedException}.
         *
         * @param message the exception message
         */
        public RetriesExhaustedException(final String message) {
            super(message);
        }
    }
}
