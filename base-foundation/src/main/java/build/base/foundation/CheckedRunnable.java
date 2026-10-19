package build.base.foundation;

/*-
 * #%L
 * base.build Foundation
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
 * Similar to {@link Runnable}, but has a generic type for an exception which may be thrown
 *
 * @param <E> The exception type which may be thrown by {@link #run()}
 * @author jonathan.walsh
 * @since Feb-2019
 */
@FunctionalInterface
public interface CheckedRunnable<E extends Exception> {

    /**
     * Execute some action
     *
     * @throws E if unable to execute
     */
    void run() throws E;
}
