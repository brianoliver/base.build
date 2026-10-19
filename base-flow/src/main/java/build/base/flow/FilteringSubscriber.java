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

import java.util.function.Predicate;

/**
 * A {@link Subscriber} adapter that filters items as they are observed, passing only those that satisfy a supplied
 * {@link Predicate} by another {@link Subscriber}.
 *
 * @param <T> the type of item
 * @author brian.oliver
 * @since Aug-2018
 */
public class FilteringSubscriber<T>
    implements Subscriber<T> {

    /**
     * The {@link Predicate} to satisfy.
     */
    private final Predicate<? super T> predicate;

    /**
     * The {@link Subscriber}.
     */
    private final Subscriber<? super T> subscriber;

    /**
     * Constructs a {@link FilteringSubscriber}.
     *
     * @param predicate  the {@link Predicate} to filter items
     * @param subscriber the {@link Subscriber} of the items satisfying the {@link Predicate}
     */
    private FilteringSubscriber(final Predicate<? super T> predicate, final Subscriber<? super T> subscriber) {
        this.predicate = predicate;
        this.subscriber = subscriber;
    }

    @Override
    public void onSubscribe(final Subscription subscription) {
        this.subscriber.onSubscribe(subscription);
    }

    @Override
    public void onNext(final T item) {
        if (this.predicate.test(item)) {
            this.subscriber.onNext(item);
        }
    }

    @Override
    public void onError(final Throwable throwable) {
        this.subscriber.onError(throwable);
    }

    @Override
    public void onComplete() {
        this.subscriber.onComplete();
    }

    /**
     * Constructs a new {@link FilteringSubscriber}.
     *
     * @param <T>       the type of item being observed
     * @param predicate the {@link Predicate} to filter items
     * @param observer  the {@link Subscriber} of the items satisfying the {@link Predicate}
     * @return the {@link FilteringSubscriber}
     */
    public static <T> FilteringSubscriber<T> of(final Predicate<? super T> predicate,
                                                final Subscriber<? super T> observer) {

        return new FilteringSubscriber<>(predicate, observer);
    }
}
