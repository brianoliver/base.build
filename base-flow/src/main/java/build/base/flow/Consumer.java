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
 * A {@link Consumer} of items, typically created by {@link Producer}s.
 *
 * @param <T> the type of item published
 * @author brian.oliver
 * @see Producer
 * @since Jan-2025
 */
@FunctionalInterface
public interface Consumer<T> {

    /**
     * Invoked to consume an item that was produced.
     *
     * @param item the item
     */
    void onNext(T item);
}
