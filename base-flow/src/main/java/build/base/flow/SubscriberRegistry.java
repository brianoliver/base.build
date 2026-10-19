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

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

/**
 * A {@link Publicist} for registered {@link Subscriber}s to which items may be published.
 *
 * @param <T> the type of item published
 * @author brian.oliver
 * @since Jan-2017
 */
public class SubscriberRegistry<T>
    implements Publicist<T> {

    /**
     * The registered {@link Subscriber}s and their {@link SubscriberSubscription}s.
     */
    private final ConcurrentHashMap<Subscriber<? super T>, SubscriberSubscription> subscriptions;

    /**
     * A future that is completed when publishing is complete.
     */
    private final CompletableFuture<?> onCompleted;

    /**
     * Called when a {@link Subscriber} throws from {@link Subscriber#onNext(Object)}, before the subscriber
     * is terminated.  Defaults to a no-op.
     */
    private BiConsumer<Subscriber<?>, Throwable> subscriberErrorObserver = (_, _) -> {};

    /**
     * Constructs a {@link SubscriberRegistry}.
     */
    public SubscriberRegistry() {
        this.subscriptions = new ConcurrentHashMap<>();
        this.onCompleted = new CompletableFuture<>();
    }

    /**
     * Sets the observer to be notified when a {@link Subscriber} throws from {@link Subscriber#onNext(Object)}.
     *
     * @param observer a {@link BiConsumer} receiving the failing subscriber and the throwable it raised
     * @return this registry, for fluent configuration
     */
    public SubscriberRegistry<T> onSubscriberError(final BiConsumer<Subscriber<?>, Throwable> observer) {
        this.subscriberErrorObserver = Objects.requireNonNull(observer, "The observer can't be null");
        return this;
    }

    @Override
    public void subscribe(final Subscriber<? super T> subscriber) {
        Objects.requireNonNull(subscriber, "The subscriber can't be null");

        if (!this.onCompleted.isDone()) {
            final var subscription = this.subscriptions.compute(subscriber, (_, existing) ->
                existing == null
                    ? new SubscriberSubscription(subscriber)
                    : existing);

            // notify the Subscriber of the Subscription
            subscription.subscribe();
        }
        else {
            throwCompletedException();
        }
    }

    /**
     * Unsubscribe a {@link Subscriber}.
     *
     * @param subscriber the {@link Subscriber} to unsubscribe
     */
    public void unsubscribe(final Subscriber<?> subscriber) {
        if (subscriber != null) {
            Optional.ofNullable(this.subscriptions.remove(subscriber))
                .ifPresent(SubscriberSubscription::cancel);
        }
    }

    @Override
    public void publish(final T item) {
        if (!this.onCompleted.isDone()) {
            this.subscriptions.values().forEach(subscription -> subscription.accept(item));
        }
    }

    @Override
    public boolean complete() {
        final var changed = this.onCompleted.complete(null);

        if (changed) {
            this.subscriptions.values().forEach(SubscriberSubscription::complete);
        }

        return changed;
    }

    @Override
    public boolean error(final Throwable throwable) {
        final var changed = this.onCompleted.completeExceptionally(throwable);

        if (changed) {
            this.subscriptions.values().forEach(subscription -> subscription.error(throwable));
        }

        return changed;
    }

    /**
     * Provides a future that will be completed, normally or exceptionally, when publishing is completed when
     * {@link #complete()} or {@link #error(Throwable)} is called.
     *
     * @return the future of publishing completion
     */
    public CompletableFuture<Void> onComplete() {
        // a continuation to prevent modification of the underlying future;
        return this.onCompleted.thenRun(() -> {
        });
    }

    /**
     * Throws an {@link IllegalStateException} due to this publisher having already completed; with the
     * completing throwable (if any) as the cause.
     */
    private void throwCompletedException() {
        try {
            this.onCompleted.getNow(null);
            throw new IllegalStateException("The publisher has already completed publishing");
        }
        catch (final CompletionException e) {
            final Throwable cause = e.getCause();
            throw new IllegalStateException("The publisher has already completed publishing due to error", cause);
        }
    }

    /**
     * The state of the {@link Subscriber}.
     */
    private enum State {
        /**
         * The {@link Subscriber} is in the process of initializing as part of subscribing.
         */
        INITIALIZING,

        /**
         * The {@link Subscriber} is actively observing items.
         */
        SUBSCRIBED,

        /**
         * The {@link Subscriber} has been terminated, and will be removed.
         */
        TERMINATED
    }

    /**
     * The {@link Subscriber} {@link Subscription} for the {@link SubscriberRegistry}.
     */
    public class SubscriberSubscription
        implements Subscription {

        /**
         * The {@link Subscriber}.
         */
        private final Subscriber<? super T> subscriber;

        /**
         * The number of remaining items to provide to the {@link Subscriber}.  When this is {@link Long#MAX_VALUE}
         * an infinite number of items may be accepted by the {@link Subscriber}, which means no buffering is required.
         */
        private final AtomicLong remainingItemCount;

        /**
         * The state of the {@link Subscriber}.
         */
        private volatile State state;

        /**
         * The queue of items, yet to be provided to the {@link Subscriber}.
         */
        private final ConcurrentLinkedQueue<T> queue;

        /**
         * Constructs a {@link SubscriberSubscription} for the {@link Subscriber}.
         *
         * @param subscriber the {@link Subscriber}
         */
        private SubscriberSubscription(final Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
            this.state = State.INITIALIZING;
            this.remainingItemCount = new AtomicLong(0);
            this.queue = new ConcurrentLinkedQueue<>();
        }

        /**
         * Attempts to notify the {@link Subscriber} of the {@link Subscription}.
         */
        public void subscribe() {
            synchronized (this) {
                if (this.state == State.INITIALIZING) {
                    this.subscriber.onSubscribe(this);
                    this.state = State.SUBSCRIBED;
                }
                else if (this.state == State.SUBSCRIBED) {
                    throw new IllegalStateException("The subscriber " + this.subscriber + " is already subscribed");
                }
                else if (this.state == State.TERMINATED) {
                    throw new IllegalStateException("The subscriber " + this.subscriber + " has terminated");
                }
            }
        }

        public void complete() {
            synchronized (this) {
                if (this.state != State.TERMINATED) {
                    SubscriberRegistry.this.subscriptions.remove(this.subscriber);
                    this.state = State.TERMINATED;
                    this.queue.clear();
                    this.subscriber.onComplete();
                }
            }
        }

        public void error(final Throwable throwable) {
            synchronized (this) {
                if (this.state != State.TERMINATED) {
                    SubscriberRegistry.this.subscriptions.remove(this.subscriber);
                    this.state = State.TERMINATED;
                    this.queue.clear();
                    this.subscriber.onError(throwable);
                }
            }
        }

        /**
         * Accepts the specified item for publishing to the {@link Subscriber}.
         * <p>
         * If the {@link Subscriber} is {@link State#INITIALIZING} or {@link State#TERMINATED}, the item is ignored
         * and thus dropped.  If the {@link Subscriber} is {@link State#SUBSCRIBED} and it requires an
         * unbounded number of items, it is provided immediately.   If there's pending items, it is queued
         * and those in the queue are then provided until the remaining count is satisfied.
         *
         * @param item the item
         */
        public void accept(final T item) {
            if (this.state != State.TERMINATED) {
                this.queue.add(item);
                deliver();
            }
        }

        @Override
        public void request(final long number) {
            if (number >= 0) {
                this.remainingItemCount.set(number);
                deliver();
            }
        }

        /**
         * Attempt to deliver the required number of items, or as many as possible.
         */
        private void deliver() {
            while (this.state == State.SUBSCRIBED
                && this.remainingItemCount.get() > 0
                && !this.queue.isEmpty()) {

                synchronized (this) {
                    final var count = this.remainingItemCount.get();

                    if (count > 0) {
                        final var item = this.queue.poll();

                        if (item != null) {
                            try {
                                this.subscriber.onNext(item);

                                if (count != Long.MAX_VALUE) {
                                    this.remainingItemCount.compareAndSet(count, count - 1);
                                }
                            }
                            catch (final Throwable throwable) {
                                SubscriberRegistry.this.subscriberErrorObserver.accept(this.subscriber, throwable);
                                error(throwable);
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void cancel() {
            synchronized (this) {
                if (this.state != State.TERMINATED) {
                    this.state = State.TERMINATED;
                    SubscriberRegistry.this.subscriptions.remove(this.subscriber);
                    this.queue.clear();
                    this.subscriber.onComplete();
                }
            }
        }
    }
}
