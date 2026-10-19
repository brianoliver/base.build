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

/**
 * A producer of items (and related control messages) received by {@link Subscriber}s.
 * <p>
 * Each current {@link Subscriber} receives the same items (via method onNext) in the same order, unless drops or errors
 * are encountered.  If a {@link Publisher} encounters an error that does not allow items to be issued to a
 * {@link Subscriber}, that {@link Subscriber} receives {@link Subscriber#onError(Throwable)}, and then receives no
 * further messages. Otherwise, when it is known that no further messages will be issued to it, a {@link Subscriber}
 * receives {@link Subscriber#onComplete()}.
 * <p>
 * {@link Publisher}s ensure that {@link Subscriber} method invocations for each subscription are strictly ordered in
 * happens-before order.  {@link Publisher}s may vary in policy about whether drops (failures to issue an item because
 * of resource limitations) are treated as unrecoverable errors.  {@link Publisher}s may also vary about whether
 * {@link Subscriber}s receive items that were produced or available before they subscribed.
 * <p>
 * {@link Subscriber}s may cancel receiving items at any time by calling {@link Subscription#cancel()}.
 * <p>
 * {@link Subscriber}s are expected to process published items immediately, without causing unnecessary delay to the
 * {@link Publisher} publishing said items.
 *
 * @param <T> the type of item published
 * @author brian.oliver
 * @since Oct-2024
 */
public interface Publisher<T> {

    /**
     * Adds the given {@link Subscriber} if possible.  If already subscribed, or the attempt to subscribe fails due to
     * policy violations or errors, the {@link Subscriber}'s {@link Subscriber#onError(Throwable)} method is invoked with
     * an {@link IllegalStateException}.  Otherwise, the {@link Subscriber}'s {@link Subscriber#onSubscribe} method is
     * invoked with a new {@link Subscription}.  {@link Subscriber}s enable receiving items by invoking the
     * {@link Subscription#request(long)} method of their {@link Subscription}, and may unsubscribe from receiving items
     * invoking {@link Subscription#cancel()}.
     *
     * @param subscriber the subscriber
     */
    void subscribe(Subscriber<? super T> subscriber);
}
