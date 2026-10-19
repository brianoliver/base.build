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
 * A {@link Producer} and {@link Publisher} of items to {@link Subscriber}s.
 *
 * @param <T> the type of items
 * @author brian.oliver
 * @see Subscriber
 * @since Nov-2024
 */
public interface Publicist<T>
    extends Producer<T>, Publisher<T> {

    /**
     * Completes the currently subscribed {@link Subscriber}s by invoking {@link Subscriber#onComplete()}.
     * <p>
     * Once {@link Subscriber}s have been informed, they will be dropped from the {@link Publicist}.
     * {@link Subscriber}s in the process of being subscribed, completed or handling an error will not be effected.
     *
     * @return {@code true} if publishing had not already been completed, otherwise {@code false}
     */
    boolean complete();

    /**
     * Raises an error with the currently subscribed {@link Subscriber}s by invoking
     * {@link Subscriber#onError(Throwable)}.
     * <p>
     * Once {@link Subscriber}s have been informed, they will be dropped from the {@link Publicist}.
     * {@link Subscriber}s in the process of being subscribed, completed or handling an error will not be effected.
     *
     * @param throwable the {@link Throwable} for the {@link Subscriber}s
     * @return {@code true} if publishing had not already been completed, otherwise {@code false}
     */
    boolean error(Throwable throwable);

    @Override
    default void close() {
        complete();
    }
}
