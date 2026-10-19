package build.base.assertion;

/*-
 * #%L
 * base.build Assertion
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

import build.base.configuration.ConfigurationBuilder;
import build.base.configuration.Option;
import build.base.foundation.iterator.matching.IteratorPatternMatchers;
import build.base.option.Timeout;
import build.base.retryable.BlockingRetry;
import build.base.retryable.EphemeralFailureException;
import build.base.retryable.Retryable;

import java.util.Objects;

/**
 * An {@code assertj}-like Assertion Class for {@link build.base.foundation.iterator.matching.IteratorPatternMatcher}s
 * evaluated against a {@link Retryable} source of elements.
 * <p>
 * The fluent chain is identical to {@link IteratorAssert}, but each call to
 * {@code isTrue()} or {@code isFalse()} retries the pattern evaluation until it succeeds or the
 * configured timeout expires.
 * <p>
 * Example:
 * <pre><code>
 * IteratorPatternMatcherRetryableAssertion.assertThat(Retryable.of(() -> service.getItems()))
 *     .withTimeout(Timeout.ofSeconds(5))
 *     .starts()
 *     .thenLater().matches("expectedItem")
 *     .isTrue();
 * </code></pre>
 *
 * @param <T> the type of elements
 * @author brian.oliver
 * @since Feb-2026
 */
public class IteratorPatternMatcherRetryableAssertion<T> {

    /**
     * The {@link Retryable} source of {@link Iterable} elements.
     */
    private final Retryable<? extends Iterable<T>> retryable;

    /**
     * The {@link ConfigurationBuilder} for retry options.
     */
    private final ConfigurationBuilder configurationBuilder;

    /**
     * Constructs an {@link IteratorPatternMatcherRetryableAssertion} for the specified
     * {@link Retryable}.
     *
     * @param retryable the {@link Retryable} source of {@link Iterable} elements
     */
    private IteratorPatternMatcherRetryableAssertion(final Retryable<? extends Iterable<T>> retryable) {
        this.retryable = Objects.requireNonNull(retryable, "The retryable must not be null");
        this.configurationBuilder = ConfigurationBuilder.create();
    }

    /**
     * Creates an {@link IteratorPatternMatcherRetryableAssertion} for the specified {@link Retryable}.
     *
     * @param <T>       the type of elements
     * @param retryable the {@link Retryable} source of {@link Iterable} elements
     * @return a new {@link IteratorPatternMatcherRetryableAssertion}
     */
    public static <T> IteratorPatternMatcherRetryableAssertion<T> assertThat(
        final Retryable<? extends Iterable<T>> retryable) {
        return new IteratorPatternMatcherRetryableAssertion<>(retryable);
    }

    /**
     * Includes the specified {@link Timeout} to use during assertion.
     *
     * @param timeout the {@link Timeout}
     * @return this {@link IteratorPatternMatcherRetryableAssertion} to support fluent-style invocation
     */
    public IteratorPatternMatcherRetryableAssertion<T> withTimeout(final Timeout timeout) {
        return withOption(timeout);
    }

    /**
     * Includes the specified {@link Option} to use during assertion.
     *
     * @param option the {@link Option}
     * @return this {@link IteratorPatternMatcherRetryableAssertion} to support fluent-style invocation
     */
    public IteratorPatternMatcherRetryableAssertion<T> withOption(final Option option) {
        this.configurationBuilder.add(option);
        return this;
    }

    /**
     * Starts an {@link build.base.foundation.iterator.matching.IteratorPatternMatcher} pattern
     * sequence, backed by a retrying {@link IteratorPatternMatcherPredicate}.
     *
     * @return an {@link IteratorPatternMatcherSequenceAssertion} for the starting position
     */
    public IteratorPatternMatcherSequenceAssertion<T> starts() {
        final var config = this.configurationBuilder.build();
        final Retryable<? extends Iterable<T>> source = this.retryable;

        final IteratorPatternMatcherPredicate<T> predicate = pattern -> {
            try {
                BlockingRetry.of((Retryable<Void>) () -> {
                    if (!pattern.test(source.get())) {
                        throw new EphemeralFailureException("Pattern did not match");
                    }
                    return null;
                }, config).get();
                return true;
            }
            catch (final Exception e) {
                return false;
            }
        };

        return new IteratorPatternMatcherSequenceAssertion<>(IteratorPatternMatchers.starts(), predicate);
    }
}
