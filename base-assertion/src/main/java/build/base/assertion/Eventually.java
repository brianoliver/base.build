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

import build.base.foundation.stream.Streamable;
import build.base.retryable.Retryable;

import java.util.concurrent.CompletableFuture;

/**
 * A helper {@code assertj} Assertion Class for types that eventually satisfy an assertion.
 *
 * @author brian.oliver
 * @since Nov-2024
 */
public class Eventually<T> {

    /**
     * Prevent instantiation.
     */
    private Eventually() {
        // prevent instantiation
    }

    /**
     * Creates a {@link CompletableFutureAssertion} for the specified {@link CompletableFuture}.
     *
     * @param completableFuture the {@link CompletableFuture}
     * @param <T>               the type of {@link CompletableFuture} value
     * @return a new {@link CompletableFutureAssertion}
     */
    public static <T> CompletableFutureAssertion<T> assertThat(final CompletableFuture<T> completableFuture) {
        return new CompletableFutureAssertion<>(completableFuture);
    }

    /**
     * Creates a {@link RetryableAssertion} for the specified {@link Retryable}.
     *
     * @param retryable the {@link Retryable}
     * @param <T>       the type of {@link Retryable} value
     * @return a new {@link RetryableAssertion}
     */
    public static <T> RetryableAssertion<T> assertThat(final Retryable<T> retryable) {
        return new RetryableAssertion<>(retryable);
    }

    /**
     * Creates a {@link RetryableAssertion} for the specified {@link Runnable}.
     *
     * @param runnable the {@link Runnable}
     * @return a new {@link RetryableAssertion}
     */
    public static RetryableAssertion<Void> assertThat(final Runnable runnable) {
        return new RetryableAssertion<>(Retryable.of(runnable));
    }

    /**
     * Creates an {@link IteratorPatternMatcherRetryableAssertion} for the specified {@link Iterable}.
     *
     * @param <T>      the type of elements
     * @param iterable the {@link Iterable}
     * @return a new {@link IteratorPatternMatcherRetryableAssertion}
     */
    public static <T> IteratorPatternMatcherRetryableAssertion<T> assertThat(final Iterable<T> iterable) {
        return IteratorPatternMatcherRetryableAssertion.assertThat(() -> iterable);
    }

    /**
     * Creates an {@link IteratorPatternMatcherRetryableAssertion} for the specified {@link Streamable}.
     *
     * @param <T>        the type of elements
     * @param streamable the {@link Streamable}
     * @return a new {@link IteratorPatternMatcherRetryableAssertion}
     */
    public static <T> IteratorPatternMatcherRetryableAssertion<T> assertThat(final Streamable<T> streamable) {
        return IteratorPatternMatcherRetryableAssertion.assertThat(() -> streamable);
    }
}
