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

import java.util.function.Function;

/**
 * An {@link Subscriber} adapter that maps items from one type to another for observation by another {@link Subscriber}.
 *
 * @param <T> the type of item being observed
 * @param <R> the mapped type of item to observe
 * @author brian.oliver
 * @since Aug-2018
 */
public class MappingSubscriber<T, R>
    implements Subscriber<T> {

    /**
     * The {@link Function} mapping one type of item to another.
     */
    private final Function<T, R> function;

    /**
     * The {@link Subscriber}.
     */
    private final Subscriber<R> subscriber;

    /**
     * Constructs a {@link MappingSubscriber}.
     *
     * @param function   the {@link Function} to map the source items to the desired observable type of item
     * @param subscriber the {@link Subscriber} of the desired type of items
     */
    private MappingSubscriber(final Function<T, R> function,
                              final Subscriber<R> subscriber) {
        
        this.function = function;
        this.subscriber = subscriber;
    }

    @Override
    public void onSubscribe(final Subscription subscription) {
        this.subscriber.onSubscribe(subscription);
    }

    @Override
    public void onNext(final T item) {
        this.subscriber.onNext(this.function.apply(item));
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
     * Constructs a new {@link MappingSubscriber}, mapping items from one type to another for further observation.
     *
     * @param <T>      the type of item being observed
     * @param <R>      the mapped type of item to observe
     * @param function the {@link Function} to map the source items to the desired observable type
     * @param observer the {@link Subscriber}
     * @return the {@link MappingSubscriber}
     */
    public static <T, R> MappingSubscriber<T, R> of(final Function<T, R> function,
                                                    final Subscriber<R> observer) {

        return new MappingSubscriber<>(function, observer);
    }
}
