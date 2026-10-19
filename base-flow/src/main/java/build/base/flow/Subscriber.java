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
 * An {@link Subscriber} of items published by an {@link Publisher}.
 * <p>
 * {@link Subscriber}s are designated as "single-use" with respect to {@link Publisher}s.  Once an {@link Subscriber}
 * instance has been subscribed to a {@link Publisher} it should not be reused to subscribe at any time in the future
 * to the same or different {@link Publisher}s.
 * <p>
 * Methods on this interface defined as call-back methods, each of which is initiated by an {@link Publisher}.
 * While methods maybe invoked directly by a {@link Publisher}, there's no requirement or guarantee for such
 * semantics.  {@link Publisher}s may delegate the invocation of these methods to another thread on their
 * behalf, either synchronously or asynchronously.
 *
 * @param <T> the type of item published
 */
@FunctionalInterface
public interface Subscriber<T>
    extends Consumer<T> {

    /**
     * Invoked upon registration of an {@link Subscriber} with an {@link Publisher}, prior to all other methods
     * on this interface being invoked, allowing the {@link Subscriber} to perform initialization and request
     * an initial number of items to be provided.
     *
     * @param subscription the {@link Subscription}
     */
    default void onSubscribe(final Subscription subscription) {
        // by default, we're unbounded
        subscription.request(Long.MAX_VALUE);
    }

    /**
     * Invoked after an unrecoverable error occurred which prevents an {@link Publisher} publishing any further
     * items.  Once invoked, no further items will be provided to {@link #onNext(Object)}.
     *
     * @param throwable the {@link Throwable}
     */
    default void onError(final Throwable throwable) {
        // by default, we do nothing
    }

    /**
     * Invoked after an {@link Publisher} signals that it will no longer provide any further items to the
     * {@link Subscriber}.  Once invoked, no further items will be provided to {@link #onNext(Object)}.
     */
    default void onComplete() {
        // by default, we do nothing
    }
}
