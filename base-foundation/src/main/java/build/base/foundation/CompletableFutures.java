package build.base.foundation;

/*-
 * #%L
 * base.build Foundation
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

import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Helper methods for {@link CompletableFuture}s.
 *
 * @author brian.oliver
 * @since Dec-2017
 */
public class CompletableFutures {

    /**
     * Private constructor to prevent instantiation.
     */
    private CompletableFutures() {
        // empty constructor
    }

    /**
     * Obtain a new completed {@link CompletableFuture}.
     *
     * @param <T> the return type of the {@link CompletableFuture}
     * @return a new completed {@link CompletableFuture}
     */
    public static <T> CompletableFuture<T> completedFuture() {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Complete the specified future with the supplied exception if non-{@code null}, otherwise complete it with the value.
     *
     * @param future    the future
     * @param value     the value or {@code null}
     * @param exception the exception or {@code null}
     * @param <T>       the future's value type
     * @return the future
     */
    public static <T> CompletableFuture<T> complete(final CompletableFuture<T> future,
                                                    final T value,
                                                    final Throwable exception) {

        if (exception == null) {
            future.complete(value);
        }
        else {
            future.completeExceptionally(exception);
        }

        return future;
    }

    /**
     * Creates and exceptionally completes a {@link CompletableFuture} with the specified {@link Throwable}.
     *
     * @param <T>       the type of the {@link CompletableFuture}
     * @param throwable the {@link Throwable}
     * @return the {@link CompletableFuture}
     */
    public static <T> CompletableFuture<T> completeExceptionally(final Throwable throwable) {

        final var completableFuture = new CompletableFuture<T>();
        completableFuture.completeExceptionally(throwable);
        return completableFuture;
    }

    /**
     * Attempts to extract and return the exception from a future.
     * <p>
     * Note the returned exception is not wrapped via either {@link ExecutionException} or
     * {@link CompletionException}, it is the true exception which triggered the completion.
     * </p>
     *
     * @param future the future to retrieve the exception from
     * @return the {@link Optional} exception or {@link Optional#empty()} if the future is incomplete or completed
     * normally
     */
    public static Optional<Throwable> getException(final CompletableFuture<?> future) {

        if (future.isCompletedExceptionally()) {
            try {
                future.getNow(null);
            }
            catch (final CompletionException e) {
                return Optional.of(e.getCause());
            }
            catch (final Throwable e) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    /**
     * Return the result of the specified future, or re-throw its unwrapped exceptional completion if any.
     *
     * @param future the future
     * @param <V>    the result type
     * @return the result
     * @throws Exception             the unwrapped exceptional completion if any
     * @throws InterruptedException  if this thread was interrupted
     * @throws CancellationException if the future was cancelled
     */
    public static <V> V getOrThrow(final CompletableFuture<V> future)
        throws Exception {

        try {
            return future.get();
        }
        catch (final ExecutionException | CompletionException e) {
            // hide ExecutionException/CompletionException and re-throw cause. from the CompletableFuture javadoc,
            // it seems like future.get() will not throw CompletionException; we catch it just in case
            final Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            else if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw e; // rethrow if cause is null, or is neither an Exception nor an Error
        }
        // else; rethrow InterruptedException or CancellationException
    }

    /**
     * Wait if needed for at most the given amount of time for the specified future to be completed, then return the
     * result of the future, or re-throw its unwrapped exceptional completion if any.
     *
     * @param future  the future
     * @param timeout the maximum time to wait
     * @param unit    the {@link TimeUnit} for the timeout argument
     * @param <V>     the result type
     * @return the result
     * @throws Exception             the unwrapped exceptional completion if any
     * @throws InterruptedException  if this thread was interrupted
     * @throws CancellationException if the future was cancelled
     * @throws TimeoutException      if the timeout expired before the future was completed
     */
    public static <V> V getOrThrow(final CompletableFuture<V> future, final long timeout, final TimeUnit unit)
        throws Exception {

        try {
            return future.get(timeout, unit);
        }
        catch (final ExecutionException | CompletionException e) {
            // hide ExecutionException/CompletionException and re-throw cause. from the CompletableFuture javadoc,
            // it seems like future.get(timeout, unit) will not throw CompletionException; we catch it just in case
            final Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            else if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw e; // rethrow if cause is null, or is neither an Exception nor an Error
        }
        // else; rethrow InterruptedException or CancellationException
    }
}
