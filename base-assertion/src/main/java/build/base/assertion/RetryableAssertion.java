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
import build.base.foundation.Capture;
import build.base.option.Timeout;
import build.base.retryable.BlockingRetry;
import build.base.retryable.PermanentFailureException;
import build.base.retryable.Retryable;
import build.base.retryable.RetryablePredicate;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;

import java.util.Objects;

/**
 * An {@code assertj}-like Assertion Class for {@link Retryable}s.
 *
 * @param <T> the type of result of the {@link Retryable}.
 * @author brian.oliver
 * @since Nov-2024
 */
public class RetryableAssertion<T> {

    /**
     * The {@link Retryable} to use to obtain a value for assertion.
     */
    private final Retryable<T> retryable;

    /**
     * The {@link ConfigurationBuilder} for the {@link RetryableAssertion}.
     */
    private final ConfigurationBuilder configurationBuilder;

    /**
     * The {@link Capture}d result.
     */
    private final Capture<T> capture;

    /**
     * The unexpected {@link Capture} {@link Throwable}.
     */
    private final Capture<Throwable> unexpected;

    /**
     * Constructs the {@link RetryableAssertion} for the specified {@link Retryable}.
     *
     * @param retryable the {@link Retryable}
     */
    public RetryableAssertion(final Retryable<T> retryable) {
        this.retryable = Objects.requireNonNull(retryable, "The retryable must not be null");
        this.configurationBuilder = ConfigurationBuilder.create();
        this.capture = Capture.empty();
        this.unexpected = Capture.empty();
    }

    /**
     * Creates a {@link RetryableAssertion} for the specified {@link Retryable}.
     *
     * @param retryable the {@link Retryable}
     * @param <T>       the type of {@link Retryable}
     * @return the new {@link RetryableAssertion}
     */
    public static <T> RetryableAssertion<T> assertThat(final Retryable<T> retryable) {
        return new RetryableAssertion<>(retryable);
    }

    /**
     * Includes the specified {@link Timeout} to use during assertion.
     *
     * @param timeout the {@link Timeout}
     * @return this {@link RetryableAssertion} to support fluent-style method invocation
     */
    public RetryableAssertion<T> withTimeout(final Timeout timeout) {
        return withOption(timeout);
    }

    /**
     * Includes the specified {@link Option} to use during assertion.
     *
     * @param option the {@link Option}
     * @return this {@link RetryableAssertion} to support fluent-style method invocation
     */
    public RetryableAssertion<T> withOption(final Option option) {
        this.configurationBuilder.add(option);
        return this;
    }

    /**
     * Verifies that the {@link Retryable} value is eventually is equal to the given one.
     * <p>
     * Example:
     * <pre><code class='java'> // assertions succeed
     * assertThat(&quot;abc&quot;).isEqualTo(&quot;abc&quot;);
     * assertThat(new HashMap&lt;String, Integer&gt;()).isEqualTo(new HashMap&lt;String, Integer&gt;());
     *
     * // assertions fail
     * assertThat(&quot;abc&quot;).isEqualTo(&quot;123&quot;);
     * assertThat(new ArrayList&lt;String&gt;()).isEqualTo(1);</code></pre>
     *
     * @param expected the given value to compare the actual value to.
     * @return {@code this} assertion object.
     * @throws AssertionError if the actual value is not equal to the given one.
     */
    public ObjectAssert<T> isEqualTo(final Object expected) {
        try {
            return new ObjectAssert<>(BlockingRetry.of(
                    RetryablePredicate.of(this.retryable, value -> Objects.equals(value, expected)),
                    this.configurationBuilder.build())
                .get());
        }
        catch (final Exception e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Verifies that the {@link Retryable} failed due to a {@link PermanentFailureException}.
     * <p>
     * Assertion will pass :
     * <pre><code class='java'> Retryable retryable = Retryable.of(new RuntimeException());
     * Eventually.assertThat(retryable).isCompletedExceptionally();</code></pre>
     * <p>
     * Assertion will fail :
     * <pre><code class='java'>Eventually.assertThat(Retryable.of(42)).isCompletedExceptionally();</code></pre>
     *
     * @return an {@link AbstractThrowableAssert} to support fluent-style method invocation using the {@link Throwable}
     */
    public AbstractThrowableAssert<?, ? extends Throwable> isCompletedExceptionally() {
        try {
            BlockingRetry.of(this.retryable, this.configurationBuilder.build()).get();
            throw new AssertionError(this.retryable + " should have completed exceptionally");
        }
        catch (final Throwable throwable) {
            return Assertions.assertThat(throwable);
        }
    }

    /**
     * Verifies that the {@link Retryable} produces a value.
     * <p>
     * Assertion will pass :
     * <pre><code class='java'> Retryable retryable = Retryable.of(42);
     * Eventually.assertThat(retryable).isCompleted();</code></pre>
     * <p>
     * Assertion will fail :
     * <pre><code class='java'>Eventually.assertThat(Retryable.of(new RuntimeException())).isCompleted();</code></pre>
     *
     * @return an {@link ObjectAssert} to support fluent-style method invocation using the value
     */
    public ObjectAssert<T> isCompleted() {
        try {
            return Assertions.assertThat(BlockingRetry.of(this.retryable, this.configurationBuilder.build()).get());
        }
        catch (final Throwable throwable) {
            throw new AssertionError(this.retryable + " should have completed exceptionally");
        }
    }
}
