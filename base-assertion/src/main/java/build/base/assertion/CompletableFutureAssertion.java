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

import build.base.foundation.Capture;
import build.base.option.Timeout;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.error.ShouldNotHaveThrown.shouldNotHaveThrown;
import static org.assertj.core.error.future.ShouldBeCancelled.shouldBeCancelled;
import static org.assertj.core.error.future.ShouldBeCompleted.shouldBeCompleted;
import static org.assertj.core.error.future.ShouldBeCompletedExceptionally.shouldHaveCompletedExceptionally;
import static org.assertj.core.error.future.ShouldNotBeCancelled.shouldNotBeCancelled;
import static org.assertj.core.error.future.ShouldNotBeCompleted.shouldNotBeCompleted;

/**
 * An {@code assertj} Assertion Class for {@link CompletableFuture}s.
 *
 * @param <T> the type of result of the {@link CompletableFuture}.
 * @author brian.oliver
 * @since Nov-2024
 */
public class CompletableFutureAssertion<T>
    extends AbstractAssert<CompletableFutureAssertion<T>, CompletableFuture<T>> {

    /**
     * The {@link Optional} maximum {@link Duration} to wait for a result.
     */
    private Optional<Duration> timeout;

    /**
     * The unexpected {@link Capture}d {@link Throwable} throw while attempting to obtain the value.
     */
    private final Capture<Throwable> unexpected;

    /**
     * The {@link Capture}d {@link Throwable} when completed exceptionally.
     */
    private final Capture<Throwable> executionException;

    /**
     * Constructs the {@link CompletableFutureAssertion} for the specified {@link CompletableFuture}.
     *
     * @param future the {@link CompletableFuture}
     */
    public CompletableFutureAssertion(final CompletableFuture<T> future) {
        super(future, CompletableFutureAssertion.class);

        this.timeout = Optional.empty();
        this.unexpected = Capture.empty();
        this.executionException = Capture.empty();
    }

    /**
     * Creates an {@link CompletableFutureAssertion} assertion class for the specified {@link CompletableFuture}
     *
     * @param future the {@link CompletableFuture}
     * @param <T>    the type of {@link CompletableFuture}
     * @return the new {@link CompletableFutureAssertion} assertion
     */
    public static <T> CompletableFutureAssertion<T> assertThat(final CompletableFuture<T> future) {
        return new CompletableFutureAssertion<>(future);
    }

    /**
     * Attempts to wait the timeout for the {@link CompletableFuture} to be done.
     */
    private void waitForCompletion() {
        isNotNull();

        // only attempt to wait from completion once
        if (!this.actual.isDone()) {
            try {
                this.executionException.clear();
                this.unexpected.clear();

                if (this.timeout.isPresent()) {
                    final var duration = this.timeout.get();
                    this.actual.get(duration.toNanos(), TimeUnit.NANOSECONDS);
                }
                else {
                    this.actual.get();
                }
            }
            catch (final CancellationException | ExecutionException e) {
                this.executionException.set(e);
            }
            catch (final InterruptedException | TimeoutException e) {
                this.unexpected.set(e);
            }
        }
    }

    /**
     * Sets the desired {@link Timeout} waiting for the {@link CompletableFuture} fails.
     *
     * @param timeout the {@link Timeout}
     * @return this {@link CompletableFutureAssertion} to support fluent-style method invocation
     */
    public CompletableFutureAssertion<T> withTimeout(final Timeout timeout) {
        this.timeout = Optional.ofNullable(timeout)
            .map(Timeout::get);

        return this.myself;
    }

    /**
     * Sets the desired timeout waiting for the {@link CompletableFuture} fails.
     *
     * @param timeout the {@link Duration}
     * @return this {@link CompletableFutureAssertion} to support fluent-style method invocation
     */
    public CompletableFutureAssertion<T> withTimeout(final Duration timeout) {
        this.timeout = Optional.ofNullable(timeout);
        return this.myself;
    }

    /**
     * Attempts to immediately obtain the completed result, without waiting, returning {@code null} if not completed
     * or if an {@link Exception} occurs.
     *
     * @return the completed result or {@code null}
     */
    private T completed() {
        try {
            return this.actual.get(0, TimeUnit.SECONDS);
        }
        catch (final Exception _) {
            return null;
        }
    }

    /**
     * Verifies that the {@link CompletableFuture} is completed normally (i.e.{@link CompletableFuture#isDone() done}
     * but not {@link CompletableFuture#isCompletedExceptionally() completed exceptionally}) or
     * {@link CompletableFuture#isCancelled() cancelled}.
     * <p>
     * Assertion will pass :
     * <pre><code class='java'>Eventually.assertThat(CompletableFuture.completedFuture("something")).isCompleted();</code></pre>
     * <p>
     * Assertion will fail :
     * <pre><code class='java'>Eventually.assertThat(new CompletableFuture()).isCompleted();</code></pre>
     *
     * @return an {@link ObjectAssert} to support fluent-style method invocation
     */
    public ObjectAssert<T> isCompleted() {
        waitForCompletion();

        this.unexpected.ifPresent(throwable ->
            throwAssertionError(shouldNotHaveThrown(throwable)));

        if (this.actual.isCompletedExceptionally() || !this.actual.isDone()) {
            throwAssertionError(shouldBeCompleted(this.actual));
        }

        return Assertions.assertThat(completed());
    }

    /**
     * Verifies that the {@link CompletableFuture} is not completed normally (i.e. incomplete, failed or cancelled).
     * <p>
     * Assertion will pass :
     * <pre><code class='java'>Eventually.assertThat(new CompletableFuture()).isNotCompleted();</code></pre>
     * <p>
     * Assertion will fail :
     * <pre><code class='java'>Eventually.assertThat(CompletableFuture.completedFuture("something")).isNotCompleted();</code></pre>
     *
     * @return this {@link CompletableFutureAssertion} to support fluent-style method invocation
     */
    public CompletableFutureAssertion<T> isNotCompleted() {
        waitForCompletion();

        if ((this.actual.isDone() && this.actual.isCompletedExceptionally())
            || (this.executionException.isPresent() || this.unexpected.isPresent())) {

            throwAssertionError(shouldNotBeCompleted(actual));
        }

        return this.myself;
    }

    /**
     * Verifies that the {@link CompletableFuture} is {@link CompletableFuture#isCancelled() cancelled}.
     * <p>
     * Assertion will pass :
     * <pre><code class='java'>CompletableFuture future = new CompletableFuture();
     * future.cancel(true);
     * Eventually.assertThat(future).isCancelled();</code></pre>
     * <p>
     * Assertion will fail :
     * <pre><code class='java'>Eventually.assertThat(new CompletableFuture()).isCancelled();</code></pre>
     *
     * @return this {@link CompletableFutureAssertion} to support fluent-style method invocation
     * @see CompletableFuture#isCancelled()
     */
    public CompletableFutureAssertion<T> isCancelled() {
        waitForCompletion();

        if (!this.actual.isCancelled()) {
            throwAssertionError(shouldBeCancelled(this.actual));
        }
        return this.myself;
    }

    /**
     * Verifies that the {@link CompletableFuture} is not cancelled.
     * <p>
     * Assertion will pass :
     * <pre><code class='java'>Eventually.assertThat(new CompletableFuture()).isNotCancelled();</code></pre>
     * <p>
     * Assertion will fail :
     * <pre><code class='java'> CompletableFuture future = new CompletableFuture();
     * future.cancel(true);
     * Eventually.assertThat(future).isNotCancelled();</code></pre>
     *
     * @return this {@link CompletableFutureAssertion} to support fluent-style method invocation
     * @see CompletableFuture#isCancelled()
     */
    public CompletableFutureAssertion<T> isNotCancelled() {
        waitForCompletion();

        if (this.actual.isCancelled()) {
            throwAssertionError(shouldNotBeCancelled(this.actual));
        }
        return this.myself;
    }

    /**
     * Verifies that the {@link CompletableFuture} is completed normally with the {@code expected} result.
     * <p>
     * Assertion will pass :
     * <pre><code class='java'> assertThat(CompletableFuture.completedFuture("something"))
     *           .isCompletedWithValue("something");</code></pre>
     * <p>
     * Assertion will fail :
     * <pre><code class='java'> assertThat(CompletableFuture.completedFuture("something"))
     *           .isCompletedWithValue("something else");</code></pre>
     *
     * @param expected the expected result value of the {@link CompletableFuture}.
     * @return an {@link ObjectAssert} to support fluent-style method invocation
     */
    public ObjectAssert<T> isCompletedWithValue(final T expected) {
        isCompleted();

        final var completed = completed();

        return Assertions.assertThat(completed)
            .isEqualTo(expected);
    }

    /**
     * Verifies that the {@link CompletableFuture} is {@link CompletableFuture#isCompletedExceptionally() completed
     * exceptionally}.
     * <p>
     * Possible causes include cancellation, explicit invocation of completeExceptionally, and abrupt termination of a
     * CompletionStage action.
     * <p>
     * Assertion will pass :
     * <pre><code class='java'> CompletableFuture future = new CompletableFuture();
     * future.completeExceptionally(new RuntimeException());
     * Eventually.assertThat(future).isCompletedExceptionally();</code></pre>
     * <p>
     * Assertion will fail :
     * <pre><code class='java'>Eventually.assertThat(CompletableFuture.completedFuture("something")).isCompletedExceptionally();</code></pre>
     *
     * @return an {@link AbstractThrowableAssert} to support fluent-style method invocation using the {@link Throwable}
     * @see CompletableFuture#isCompletedExceptionally()
     */
    public AbstractThrowableAssert<?, ? extends Throwable> isCompletedExceptionally() {
        waitForCompletion();

        if (!this.actual.isCompletedExceptionally()) {
            throwAssertionError(shouldHaveCompletedExceptionally(this.actual));
        }
        return Assertions.assertThat(catchThrowable(this.actual::get));
    }
}
