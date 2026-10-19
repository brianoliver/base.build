package build.base.foundation.memoizer;

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

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A thread-safe {@link Memoizer} that holds memoized results with strong references.
 * Results are retained until {@link #clear()} is called or this instance is garbage collected.
 * <p>
 * For a non-concurrent variant see {@link StrongMemoizer}.
 *
 * @param <T> the input type
 * @param <R> the result type
 * @author reed.vonredwitz
 * @since Jun-2026
 */
final class StrongConcurrentMemoizer<T, R> extends AbstractConcurrentMemoizer<T, R> {

    StrongConcurrentMemoizer(
        final Function<T, R> function,
        final Supplier<Map<Object, Object>> mapSupplier,
        final Supplier<ConcurrentMap<Object, ReentrantLock>> locksSupplier) {
        super(function, mapSupplier, locksSupplier);
    }
}
