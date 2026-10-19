package build.base.flow;

/*-
 * #%L
 * base.build Flow
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

import build.base.foundation.iterator.TransformingIterable;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * A thread-safe {@link Subscriber} that records all interactions.
 *
 * @param <T> the type of item received by the {@link Subscription}
 * @author brian.oliver
 * @since Dec-2017
 */
public class RecordingSubscriber<T>
    implements Subscriber<T>, TransformingIterable<T> {

    /**
     * The items received by from a {@link Publisher}.
     * <p>
     * NOTE: We make a new copy for each modification to allow independent observation and concurrent
     * streaming against any existing list.
     */
    private final CopyOnWriteArrayList<T> items;

    /**
     * The error {@link Throwable} provided by the {@link Publisher}.
     */
    private Throwable throwable;

    /**
     * The {@link Subscription} provided by the {@link Publisher}.
     */
    private Subscription subscription;

    /**
     * Was the {@link Subscription} {@link Subscription} completed?
     */
    private boolean completed;

    /**
     * Constructs a {@link RecordingSubscriber}.
     */
    public RecordingSubscriber() {
        this.items = new CopyOnWriteArrayList<>();
        this.subscription = null;
        this.completed = false;
        this.throwable = null;
    }

    @Override
    public void onSubscribe(final Subscription subscription) {
        Objects.requireNonNull(subscription, "The subscription can't be null");

        if (this.throwable != null || this.completed) {
            throw new IllegalStateException(
                "Attempted to subscribe using a previously used subscriber.  Subscribers may only be used once.");
        }
        else {
            this.subscription = subscription;

            // request an unbounded number of items
            subscription.request(Long.MAX_VALUE);
        }
    }

    @Override
    public void onNext(final T item) {
        validate("receive an item");

        this.items.add(item);
    }

    @Override
    public void onError(final Throwable throwable) {
        validate("receive an error");

        this.throwable = throwable;
    }

    @Override
    public void onComplete() {
        validate("complete");

        this.completed = true;
    }

    @Override
    public Iterator<T> iterator() {
        return this.items.iterator();
    }

    /**
     * Validate the {@link RecordingSubscriber} can perform an operation.
     *
     * @param description a description of the operation
     * @throws IllegalStateException should the {@link RecordingSubscriber} not be capable of performing the operation.
     */
    private void validate(final String description) {
        if (this.subscription == null) {
            throw new IllegalStateException("Attempted to " + description + " but the subscriber wasn't subscribed");
        }

        if (this.throwable != null) {
            throw new IllegalStateException(
                "Attempted to " + description + " but the subscriber previously received an error");
        }

        if (this.completed) {
            throw new IllegalStateException("Attempted to " + description + " but the subscriber previously completed");
        }
    }

    /**
     * Has the {@link Subscription} been completed by the {@link Publisher}?
     *
     * @return <code>true</code> if the {@link Subscription} was completed, <code>false</code> otherwise
     */
    public boolean isCompleted() {
        return this.completed;
    }

    /**
     * Has the {@link Subscription} been informed of an error by the {@link Publisher}?
     *
     * @return <code>true</code> if the {@link Subscription} received an error, <code>false</code> otherwise
     */
    public boolean isErrored() {
        return this.throwable != null;
    }

    /**
     * Has the {@link Subscription} been subscribed to a {@link Publisher}?
     *
     * @return <code>true</code> if the {@link Subscription} subscribed, <code>false</code> otherwise
     */
    public boolean isSubscribed() {
        return !isCompleted() && !isErrored() && this.subscription != null;
    }

    /**
     * Obtains the {@link Optional} {@link Subscription} provided to the {@link Subscription} by a {@link Publisher}.
     *
     * @return the {@link Subscription}
     */
    public Optional<Subscription> subscription() {
        return Optional.ofNullable(this.subscription);
    }

    /**
     * Obtain the {@link Optional} {@link Throwable} provided to the {@link Subscription} by a {@link Publisher}.
     *
     * @return the {@link Throwable}
     */
    public Optional<Throwable> throwable() {
        return Optional.ofNullable(this.throwable);
    }

    /**
     * Obtain a {@link Stream} of items received thus far from the {@link Publisher}.
     *
     * @return a {@link Stream}
     */
    public Stream<T> items() {
        return this.items.stream();
    }
}
