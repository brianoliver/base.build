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

import build.base.foundation.predicate.Predicates;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * An adapter for a {@link Retryable} causing it to be retried when one or more of the supplied
 * {@link Predicate}s are satisfied.
 *
 * @param <T> the type of {@link Retryable}
 * @author Brian Oliver
 * @since Mar-2017
 */
public class ConditionallyRetryable<T>
    implements Retryable<T> {

    /**
     * The {@link Retryable}.
     */
    private final Retryable<T> retryable;

    /**
     * The {@link Predicate} to evaluate for each value produced by the {@link Retryable}.
     */
    private final Predicate<? super T> predicate;

    /**
     * Constructs a {@link ConditionallyRetryable} for a {@link Retryable}.
     *
     * @param retryable  the {@link Retryable}
     * @param predicates the {@link Predicate}s
     */
    @SafeVarargs
    private ConditionallyRetryable(final Retryable<T> retryable, final Predicate<? super T>... predicates) {
        Objects.requireNonNull(retryable, "The retryable can not be null");
        this.retryable = retryable;
        this.predicate = Predicates.anyOf(predicates);
    }

    @Override
    public T get()
        throws EphemeralFailureException,
        PermanentFailureException {

        final T value = this.retryable.get();

        if (this.predicate.test(value)) {
            throw new EphemeralFailureException(new IllegalStateException(
                "The value [" + value + "] satisfied the predicate [" + this.predicate + "]"));
        }
        else {
            return value;
        }
    }

    @Override
    public boolean isDone() {
        return this.retryable.isDone();
    }

    /**
     * Creates a new {@link ConditionallyRetryable} based on this {@link ConditionallyRetryable}, including
     * the specified {@link Predicate} in addition to those specified by this {@link ConditionallyRetryable}.
     *
     * @param predicate the {@link Predicate}
     * @return a new {@link ConditionallyRetryable}
     */
    public ConditionallyRetryable<T> when(final Predicate<? super T> predicate) {
        return new ConditionallyRetryable<>(this.retryable, this.predicate, predicate);
    }

    /**
     * Creates a new {@link ConditionallyRetryable} based on this {@link ConditionallyRetryable}, including
     * a call to invoke {@link #equals(Object)} with the specified value, in addition to those specified by
     * this {@link ConditionallyRetryable}.
     *
     * @param value the {@link Predicate}
     * @return a new {@link ConditionallyRetryable}
     */
    public ConditionallyRetryable<T> when(final T value) {
        return new ConditionallyRetryable<>(this.retryable, this.predicate, (v) -> v.equals(value));
    }

    /**
     * Creates a new {@link ConditionallyRetryable} based on the specified {@link Retryable}.
     *
     * @param retryable the {@link Retryable}
     * @param <T>       the type of the {@link Retryable}
     * @return a new {@link ConditionallyRetryable}
     */
    public static <T> ConditionallyRetryable<T> retrying(final Retryable<T> retryable) {
        return new ConditionallyRetryable<>(retryable);
    }

    /**
     * Creates a new {@link ConditionallyRetryable} based on the specified {@link Supplier}.
     *
     * @param supplier the {@link Supplier}
     * @param <T>      the type of the {@link Supplier}
     * @return a new {@link ConditionallyRetryable}
     */
    public static <T> ConditionallyRetryable<T> retrying(final Supplier<T> supplier) {
        return new ConditionallyRetryable<>(Retryable.of(supplier));
    }
}
