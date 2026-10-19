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

public interface Subscription {

    /**
     * Requests the specified number of items to be provided to the {@link Subscriber}, before more items must be
     * requested. A value of {@link Long#MAX_VALUE} means an unbounded number may be provided.  A value of zero means
     * no items should be provided.
     * <p>
     * If the number is less than or equal to zero, the {@link Subscriber} will receive an
     * {@code onError} signal with an {@link IllegalArgumentException} argument.  Otherwise, the
     * {@link Subscriber} will receive up to the specified number of items.
     *
     * @param number the number of items to be provided
     */
    void request(long number);

    /**
     * Causes the {@link Subscriber} to (eventually) stop receiving messages.  Implementation is best-effort --
     * additional messages may be received after invoking this method.
     * <p>
     * A cancelled {@link Subscription} need not ever receive an {@code onComplete} or {@code onError} signal.
     */
    void cancel();
}
