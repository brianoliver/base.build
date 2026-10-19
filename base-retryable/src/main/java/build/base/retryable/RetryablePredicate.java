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

import build.base.foundation.iterator.RecordingIterator;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * An {@link Retryable} that attempts to satisfy a {@link Predicate} using a value provided by an underlying
 * {@link Retryable}.
 * <p>
 * {@link #get} returns the value that successfully satisfies the {@link Predicate}, otherwise will it
 * will throw a {@link EphemeralFailureException}, when matching failed but can be retried, or a
 * {@link PermanentFailureException} when the underlying {@link Retryable} can't produce a value to match.
 *
 * @param <T> the type of the value to match
 * @author brian.oliver
 * @since Nov-2024
 */
public class RetryablePredicate<T>
    implements Retryable<T> {

    /**
     * The underlying {@link Retryable}, providing a value to match.
     */
    private final Retryable<T> retryable;

    /**
     * The {@link Predicate} to perform matching.
     */
    private final Predicate<? super T> predicate;

    /**
     * The values resolved thus far from the underlying {@link Retryable}, the most recent first in the list.
     */
    private LinkedList<T> resolvedValues;

    /**
     * Constructs a {@link RetryablePredicate} for a specified {@link Retryable} and {@link Predicate}.
     *
     * @param retryable the {@link Retryable}
     * @param predicate the {@link Predicate}
     */
    private RetryablePredicate(final Retryable<T> retryable, final Predicate<? super T> predicate) {
        this.retryable = Objects.requireNonNull(retryable, "The Retryable must not be null");
        this.predicate = Objects.requireNonNull(predicate, "The Predicate must not be null");

        this.resolvedValues = new LinkedList<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get() {
        try {
            // attempt to acquire the value from the retryable (supplier)
            T value = this.retryable.get();

            if (value instanceof Iterator) {
                // wrap value in a RecordingIterator so that we display last resolved sequence of iterated values
                // if match fails
                value = (T) new RecordingIterator<>((Iterator<T>) value);
            }

            // record the value we acquired
            this.resolvedValues.add(value);

            if (this.predicate.test(value)) {
                // the value we acquired matched our matcher!  (success)
                return value;
            }
            else {
                // the matcher didn't match...

                // when the retryable isDone(), there's no point in retrying (the outcome won't change)
                throw this.retryable.isDone()
                    ? new PermanentFailureException("Permanent failure to resolve " + this.retryable.toString())
                    : new EphemeralFailureException("Ephemeral failure to resolve " + this.retryable.toString());
            }
        }
        catch (final EphemeralFailureException e) {
            // re-throw when it's an ephemeral
            throw e;
        }
        catch (final Exception e) {
            if (e instanceof PermanentFailureException) {
                // rethrow permanent failures if we're not going to match them
                throw e;
            }
            else {
                // we didn't expect the exception, so assume it's ephemeral
                throw new EphemeralFailureException(e);
            }
        }
    }

    @Override
    public boolean isDone() {
        return this.retryable.isDone();
    }

    /**
     * Obtains a {@link Stream} over the values resolved from the underlying {@link Retryable} that have
     * attempted to be matched.
     *
     * @return the {@link Stream} of values
     */
    public Stream<T> values() {
        return this.resolvedValues.stream();
    }

    @Override
    public String toString() {
        return "RetryablePredicate{" +
            "retryable=" + this.retryable +
            ", predicate=" + this.predicate +
            ", previously resolved values=" +
            (this.resolvedValues.isEmpty() ? "none" : this.resolvedValues.getLast().toString()) +
            '}';
    }

    /**
     * Creates a new {@link RetryablePredicate} for a specified {@link Retryable} and {@link Predicate}.
     *
     * @param retryable the {@link Retryable}
     * @param predicate the {@link Predicate}
     * @return a new {@link RetryablePredicate}
     */
    public static <T> RetryablePredicate<T> of(final Retryable<T> retryable, final Predicate<? super T> predicate) {
        return new RetryablePredicate<>(retryable, predicate);
    }
}
