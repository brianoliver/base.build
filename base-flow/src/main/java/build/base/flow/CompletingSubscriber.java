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

import build.base.foundation.Lazy;
import build.base.foundation.predicate.Predicates;

import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A {@link Subscriber} that completes a requested {@link CompletableFuture}s when an item is observed
 * satisfying a specified {@link Predicate}.
 * <p>
 * {@link CompletingSubscriber}s are useful when a program requires notification, through the completion of a
 * {@link CompletableFuture}, when an iterm published by a {@link Publisher} satisfies a {@link Predicate}.
 * <p>
 * For example:
 * <code>
 * // establish the CompletingSubscriber
 * CompletingSubscriber&lt;State&gt; subscriber = new CompletingSubscriber&lt;&gt;();
 * // subscribe to the Publisher
 * publisher.subscribe(observer);
 * // request a CompletableFuture to be completed when the State.RUNNING is published by the Publisher
 * CompletableFuture&lt;State&gt; future = subscriber.when(State.RUNNING);
 * </code>
 * <p>
 * Should the subscription for the {@link Subscriber} be cancelled or the associated {@link Publisher} complete, all
 * previously returned and incomplete {@link CompletableFuture}s will be cancelled.
 * <p>
 * Should the {@link Publisher} fail, all previously returned and incomplete {@link CompletableFuture}s will
 * be completed exceptionally with the {@link Throwable} provided by the {@link Publisher}.
 *
 * @param <T> the type of item
 * @author brian.oliver
 * @since Nov-2018
 */
public class CompletingSubscriber<T>
    implements Subscriber<T> {

    /**
     * The currently unsatisfied {@link Condition}s to evaluate while observing items
     * produced by an {@link Subscriber}.
     */
    private final LinkedList<Condition<T, ?>> conditions;

    /**
     * The {@link Lazy}ily initialized {@link Subscription} for the {@link Subscriber}.
     */
    private final Lazy<Subscription> subscription;

    /**
     * Constructs a {@link CompletingSubscriber}.
     */
    public CompletingSubscriber() {
        this.conditions = new LinkedList<>();
        this.subscription = Lazy.empty();
    }

    /**
     * Creates a {@link CompletableFuture} that will be completed when the specified {@link Predicate} is
     * satisfied by an observed item.
     * <p>
     * The provided {@link BiConsumer} allows for custom completion of the {@link CompletableFuture}
     * based on the observed item.
     *
     * @param <V>       the type of value
     * @param predicate the {@link Predicate}
     * @param consumer  the {@link BiConsumer} to complete the returned {@link CompletableFuture}
     * @return a new {@link CompletableFuture}
     */
    public synchronized <V> CompletableFuture<V> when(final Predicate<? super T> predicate,
                                                      final BiConsumer<T, CompletableFuture<V>> consumer) {

        Objects.requireNonNull(consumer, "The BiConsumer must not be null");

        final var condition = new Condition<T, V>(predicate, consumer);

        this.conditions.add(condition);

        return condition.future;
    }

    /**
     * Creates a {@link CompletableFuture} that will be completed when the specified {@link Predicate} is
     * satisfied by an observed item.
     * <p>
     * The provided {@link Function} will be used to produce the value for completing the {@link CompletableFuture}.
     *
     * @param <V>       the type of value
     * @param predicate the {@link Predicate}
     * @param function  the {@link Function} to complete the returned {@link CompletableFuture}
     * @return a new {@link CompletableFuture}
     */
    public synchronized <V> CompletableFuture<V> when(final Predicate<? super T> predicate,
                                                      final Function<T, V> function) {

        Objects.requireNonNull(function, "The Function must not be null");

        return when(predicate, (i, f) -> f.complete(function.apply(i)));
    }

    /**
     * Creates a {@link CompletableFuture} that will be completed when the specified item is observed and equal
     * using {@link Objects#equals(Object, Object)}.
     * <p>
     * The provided {@link BiConsumer} allows for custom completion of the {@link CompletableFuture}
     * based on the observed item.
     *
     * @param <V>      the type of value
     * @param item     the item
     * @param consumer the {@link BiConsumer} to complete the returned {@link CompletableFuture}
     * @return a new {@link CompletableFuture}
     */
    public synchronized <V> CompletableFuture<V> when(final T item,
                                                      final BiConsumer<T, CompletableFuture<V>> consumer) {

        Objects.requireNonNull(consumer, "The BiConsumer must not be null");

        return when(i -> Objects.equals(i, item), consumer);
    }

    /**
     * Creates a {@link CompletableFuture} that will be completed when the specified item is observed and equal
     * using {@link Objects#equals(Object, Object)}.
     * <p>
     * The provided {@link Function} will be used to produce the value for completing the {@link CompletableFuture}.
     *
     * @param <V>      the type of value
     * @param item     the item
     * @param function the {@link Function} to complete the returned {@link CompletableFuture}
     * @return a new {@link CompletableFuture}
     */
    public synchronized <V> CompletableFuture<V> when(final T item, final Function<T, V> function) {

        Objects.requireNonNull(function, "The Function must not be null");

        return when(i -> Objects.equals(i, item), function);
    }

    /**
     * Creates a {@link CompletableFuture} that will be completed when the specified {@link Predicate} is
     * satisfied by an observed item.
     *
     * @param predicate the {@link Predicate}
     * @return a new {@link CompletableFuture}
     */
    public synchronized CompletableFuture<T> when(final Predicate<? super T> predicate) {

        return when(predicate, (i, f) -> f.complete(i));
    }

    /**
     * Creates a {@link CompletableFuture} that will be completed when the specified item is observed and equal
     * using {@link Objects#equals(Object, Object)}.
     *
     * @param item the item
     * @return a new {@link CompletableFuture}
     */
    public synchronized CompletableFuture<T> when(final T item) {

        return when(i -> Objects.equals(i, item), (i, f) -> f.complete(i));
    }

    @Override
    public void onSubscribe(final Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
        this.subscription.set(subscription);
    }

    @Override
    public synchronized void onNext(final T item) {
        // remove those Condition(s) that consume the item
        this.conditions.removeIf(c -> c.consume(item));
    }

    @Override
    public synchronized void onError(final Throwable throwable) {
        // remove all of the Condition(s) due to the error
        this.conditions.removeIf(c -> {
            c.future.completeExceptionally(throwable);
            return true;
        });
    }

    @Override
    public synchronized void onComplete() {
        // remove all of the Condition(s) as the Subscription is complete
        this.conditions.removeIf(c -> {
            c.future.cancel(true);
            return true;
        });
    }

    /**
     * Cancels the {@link Subscription}.  Once cancelled a {@link Subscriber} will no longer receive items from an
     * {@link Publisher}.
     */
    public void cancel() {
        this.subscription
            .ifPresent(Subscription::cancel);
    }

    /**
     * Represents a {@link Predicate} to be evaluated when an item is published by a {@link Publisher}
     * together with a {@link CompletableFuture} to be consumed and completed when the {@link Predicate} is satisfied.
     *
     * @param <T> the type of item
     * @param <V> the type of value to complete with the {@link CompletableFuture}.
     */
    private static class Condition<T, V> {

        /**
         * The {@link Predicate} for the {@link Condition}.
         */
        private final Predicate<? super T> predicate;

        /**
         * The {@link CompletableFuture} to complete when the {@link Condition} is satisfied.
         */
        private final CompletableFuture<V> future;

        /**
         * The {@link BiConsumer} to complete the {@link CompletableFuture}.
         */
        private final BiConsumer<T, CompletableFuture<V>> consumer;

        /**
         * Constructs a {@link Condition}.
         *
         * @param predicate the {@link Predicate}
         * @param consumer  the {@link BiConsumer} to consume the item and {@link CompletableFuture}
         */
        Condition(final Predicate<? super T> predicate, final BiConsumer<T, CompletableFuture<V>> consumer) {
            this.predicate = predicate == null ? Predicates.always() : predicate;
            this.future = new CompletableFuture<>();
            this.consumer = consumer;
        }

        /**
         * Evaluates and conditionally consumes the specified item published by a {@link Publisher}.
         *
         * @param item the item
         * @return {@code true} if the {@link Predicate} was satisfied and the {@link CompletableFuture} was
         * subsequently consumed, {@code false} if the {@link Predicate} was not satisfied or consumed.
         */
        boolean consume(final T item) {
            if (this.predicate.test(item)) {
                this.consumer.accept(item, this.future);
                return true;
            }
            else {
                return false;
            }
        }
    }
}
