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
 * Provides a mechanism to accept produced items to be published by a {@link Publisher}.
 *
 * @param <T> the type of items
 * @author brian.oliver
 * @see Consumer
 * @since Nov-2024
 */
public interface Producer<T>
    extends AutoCloseable {

    /**
     * Accepts the specified item to be published by a {@link Publisher}.
     *
     * @param item the item to publish
     */
    void publish(T item);
}
