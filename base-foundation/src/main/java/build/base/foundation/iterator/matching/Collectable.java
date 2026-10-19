package build.base.foundation.iterator.matching;

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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;

/**
 * Represents zero or more elements to be collected.
 *
 * @param <T> the type of the {@link IteratorPatternMatcher} element
 * @param <C> the type of the element in the {@link Collectable}
 * @author brian.oliver
 * @since Jun-2019
 */
public interface Collectable<T, C> {

    /**
     * Maps the currently matched elements in the {@link Collectable} using the provided {@link Function}.
     *
     * @param <R>      the return type of the {@link Function}
     * @param function the {@link Function}
     * @return a {@link Collectable} where the matched elements will be mapped using the {@link Function}
     */
    <R> Collectable<T, R> map(Function<? super C, R> function);

    /**
     * Filters the currently matched elements in the {@link Collectable} using the provided {@link Predicate}.
     *
     * @param predicate the {@link Predicate}
     * @return a {@link Collectable} where the matched elements will be filtered using the {@link Predicate}
     */
    Collectable<T, C> filter(Predicate<? super C> predicate);

    /**
     * Specifies the matched elements are to be collected using the specified {@link Collector}.
     *
     * @param <A>       the type of the container for accumulating collected values
     * @param <R>       the type of the collected result
     * @param collector the {@link Collector}
     * @return the {@link Collected} element
     */
    <A, R> Collected<T, R> collect(Collector<C, A, R> collector);

    /**
     * Consumes the currently matched element using the specified {@link Consumer}.
     *
     * @param consumer the {@link Consumer}
     * @return the newly {@link Captured} element
     */
    default Collectable<T, C> peek(final Consumer<? super C> consumer) {
        return map(value -> {
            if (consumer != null) {
                consumer.accept(value);
            }
            return value;
        });
    }
}
