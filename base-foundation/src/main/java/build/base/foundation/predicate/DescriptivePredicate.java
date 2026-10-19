package build.base.foundation.predicate;

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

import java.util.function.Predicate;

/**
 * A {@link Predicate} that provides a descriptive {@link Object#toString()}
 * for an underlying (wrapped) {@link Predicate}.
 *
 * @param <T> the type of value
 * @author brian.oliver
 * @since Mar-2020
 */
public class DescriptivePredicate<T>
    implements Predicate<T> {

    /**
     * The underlying {@link Predicate}.
     */
    private final Predicate<T> predicate;

    /**
     * The description.
     */
    private final String description;

    /**
     * Constructs a {@link DescriptivePredicate} for a given {@link Predicate}.
     *
     * @param predicate   the {@link Predicate}
     * @param description the description
     */
    private DescriptivePredicate(final Predicate<T> predicate, final String description) {
        this.predicate = predicate;
        this.description = description;
    }

    @Override
    public boolean test(final T t) {
        return this.predicate.test(t);
    }

    @Override
    public Predicate<T> or(final Predicate<? super T> other) {
        return DescriptivePredicate.of(
            this.predicate.or(other),
            this.description + ".or(" + other.toString() + ")");
    }

    @Override
    public Predicate<T> and(final Predicate<? super T> other) {
        return DescriptivePredicate.of(
            this.predicate.and(other),
            this.description + ".and(" + other.toString() + ")");
    }

    @Override
    public Predicate<T> negate() {
        return DescriptivePredicate.of(this.predicate.negate(), "not(" + this.description + ")");
    }

    @Override
    public String toString() {
        return this.description;
    }

    /**
     * Obtains a {@link DescriptivePredicate} for the specified {@link Predicate}, with the description being the result
     * of invoking {@link Object#toString()} on the provided value.
     * <p>
     * Should the specified {@link Predicate} be a {@link DescriptivePredicate}, the {@link Predicate} is
     * simply returned when the descriptions are the equal.  Otherwise a new {@link DescriptivePredicate} is returned
     * for the {@link Predicate}.
     * <p>
     * Should the specified {@link Predicate} be {@code null}, {@link Predicates#never()} is used.
     * <p>
     * Should the description value be {@code null}, the {@link Object#toString()} of the {@link Predicate} is used.
     *
     * @param <T>         the type of {@link Predicate} value
     * @param predicate   the {@link Predicate}
     * @param description the description value
     * @return the {@link DescriptivePredicate}
     */
    public static <T> DescriptivePredicate<T> of(final Predicate<T> predicate, final Object description) {
        final Predicate<T> actualPredicate = predicate == null ? Predicates.never() : predicate;
        final String actualDescription = description == null ? actualPredicate.toString() : description.toString();

        if (actualPredicate instanceof DescriptivePredicate) {
            final DescriptivePredicate<T> descriptivePredicate = (DescriptivePredicate<T>) actualPredicate;

            return descriptivePredicate.description.equals(actualDescription)
                ? descriptivePredicate
                : new DescriptivePredicate<>(actualPredicate, actualDescription);
        }
        else {
            return new DescriptivePredicate<>(actualPredicate, actualDescription);
        }
    }
}

