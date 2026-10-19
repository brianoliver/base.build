package build.base.parsing;

/*-
 * #%L
 * base.build Parsing
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

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * A mechanism to produce a specific type of value from a sequence of characters provided by a {@link Scanner}.
 * <p>
 * An {@link Evaluator} defines a {@link Predicate} that can be used to match a sequence of characters provided by a
 * {@link Scanner}.  It also provides a {@link Function} to convert the said matched characters to a specified type of
 * value.
 *
 * @param <T> the type of value
 *
 * @see Scanner
 *
 * @author brian.oliver
 * @since Mar-2020
 */
public interface Evaluator<T>
    extends Predicate<Scanner>, Function<Scanner, T> {

    /**
     * Obtains a description of the {@link Evaluator}, often used for creating meaningful {@link ParseException}s.
     *
     * @return the {@link Evaluator} description
     */
    String getDescription();

    /**
     * Creates an {@link Evaluator} for matching the specified {@link Pattern}, converting matched content into a
     * specific type of value with the provided {@link Function}.
     *
     * @param <T> the type of value
     *
     * @param pattern the {@link Pattern}
     * @param converter the {@link Function} to convert the matched {@link Pattern} into the desired type
     * @param description the description of the {@link Evaluator}
     *
     * @return a new {@link Evaluator}
     */
    static <T> Evaluator<T> create(final Pattern pattern,
                                   final Function<String, T> converter,
                                   final String description) {
        Objects.requireNonNull(pattern, "The pattern must not be null");
        Objects.requireNonNull(converter, "The converter must not be null");

        return new Evaluator<T>() {
            @Override
            public T apply(final Scanner scanner) {
                final T value = converter.apply(scanner.consume(pattern));
                return value;
            }

            @Override
            public boolean test(final Scanner parser) {
                return parser.follows(pattern);
            }

            @Override
            public String getDescription() {
                return description == null ? pattern.toString() : description;
            }
        };
    }

    /**
     * Creates an {@link Evaluator} for matching the specified {@link Pattern}, converting matched content into a
     * specific type of value with the provided {@link Function}.
     *
     * @param <T> the type of value
     *
     * @param pattern the {@link Pattern}
     * @param converter the {@link Function} to convert the matched {@link Pattern} into the desired type
     *
     * @return a new {@link Evaluator}
     */
    static <T> Evaluator<T> create(final Pattern pattern, final Function<String, T> converter) {
        return create(pattern, converter, null);
    }

    /**
     * Creates an {@link Evaluator} for matching the specified {@link String}, to be compiled into a {@link Pattern},
     * converting matched content into a specific type of value with the provided {@link Function}.
     *
     * @param <T> the type of value
     *
     * @param pattern the {@link Pattern}
     * @param converter the {@link Function} to convert the matched {@link Pattern} into the desired type
     *
     * @return a new {@link Evaluator}
     */
    static <T> Evaluator<T> create(final String pattern, final Function<String, T> converter) {
        return create(Pattern.compile(pattern), converter, null);
    }
}
