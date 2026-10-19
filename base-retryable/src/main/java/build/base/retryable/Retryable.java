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

import java.util.function.Supplier;

/**
 * Represents a retryable {@link Supplier} of values.
 * <p>
 * Like {@link Supplier}s, there's no requirement a new or distinct value be returned each
 * time the {@link #get()} is invoked.
 * <p>
 * Unlike {@link Supplier}s, invoking {@link #get()} may fail and throw either a
 * {@link PermanentFailureException} when a value can't be produced and retrying is futile, or a
 * {@link EphemeralFailureException} when a value isn't currently available, but the request may be retried.
 * <p>
 * Any other exception thrown by {@link #get()} is assumed to be ephemeral in nature and thus assumed to
 * indicate retrying the request is possible.
 *
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #get()}.
 *
 * @param <T> the type of values produced
 * @author brian.oliver
 * @since Dec-2017
 */
@FunctionalInterface
public interface Retryable<T>
    extends Supplier<T> {

    @Override
    T get()
        throws EphemeralFailureException,
        PermanentFailureException;

    /**
     * Determines if the {@link Retryable} is done producing results, exceptions or otherwise as of when
     * {@link #get()} was lasted invoked, indicating if the {@link Retryable} will produce a different result
     * upon further invocations of {@link #get()}.
     * <p>
     * By default, {@link Retryable}s are never done and thus invoking this method will always return {@code false}.
     * <p>
     * Overriding this method and the default behavior enables short-circuit evaluation of {@link Retryable}s.
     *
     * @return {@code true} if the {@link Retryable} is now done, {@code false} otherwise
     */
    default boolean isDone() {
        return false;
    }

    /**
     * Obtains a {@link Retryable} for a {@link Supplier} value.
     *
     * @param <T>      the type of the {@link Supplier} value
     * @param supplier the {@link Supplier}
     * @return a {@link Retryable} for the {@link Supplier}
     */
    static <T> Retryable<T> of(final Supplier<T> supplier) {
        return RetryableSupplier.of(supplier);
    }

    /**
     * Obtains a {@link Retryable} for a constant value.
     *
     * @param <T>   the type of the value
     * @param value the value
     * @return a {@link Retryable}
     */
    static <T> Retryable<T> of(final T value) {
        return RetryableConstant.of(value);
    }

    /**
     * Obtains a {@link Retryable} for a {@link Runnable}.
     *
     * @param runnable the {@link Runnable} to execute
     * @return a {@link Retryable}
     */
    static Retryable<Void> of(final Runnable runnable) {
        return RetryableRunnable.of(runnable);
    }
}
