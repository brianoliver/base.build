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

import build.base.foundation.stream.Streams;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper methods for working with {@link Predicate}s.
 *
 * @author brian.oliver
 * @since Mar-2018
 */
public class Predicates {

    /**
     * Private constructor to prevent instantiation.
     */
    private Predicates() {
        // empty constructor
    }

    /**
     * Obtains a {@link Predicate} that returns the description {@link Object#toString()} when {@link #toString()} is
     * invoked on the {@link Predicate}
     *
     * @param <T>         the type of value
     * @param predicate   the {@link Predicate}
     * @param description the description
     * @return a {@link Predicate}
     */
    public static <T> Predicate<T> descriptive(final Predicate<T> predicate, final Object description) {
        return DescriptivePredicate.of(predicate, description);
    }

    /**
     * Obtains a {@link Predicate} that returns {@code true} if a value is an instance of the specified {@link Class}.
     *
     * @param <T>          the type of value
     * @param classOfValue the {@link Class}
     * @return a {@link Predicate}
     */
    public static <T> Predicate<T> isInstance(final Class<T> classOfValue) {
        return classOfValue == null
            ? never()
            : DescriptivePredicate.of(classOfValue::isInstance, classOfValue.getName() + "::isInstance");
    }

    /**
     * Obtains a {@link Predicate} that returns {@code true} if a value to be tested equals,
     * using {@link Objects#equals(Object, Object)}, the specified value, and {@code false} otherwise.
     * <p>
     * The specified description will be returned as the result of invoking {@link Object#toString()} on
     * the {@link Predicate}.
     *
     * @param <T>         the type of value
     * @param value       the value
     * @param description the description of the {@link Predicate}
     * @return a {@link Predicate}
     */
    public static <T> Predicate<T> isEqual(final T value, final Object description) {
        return DescriptivePredicate.of(t -> Objects.equals(t, value), description);
    }

    /**
     * Obtains a {@link Predicate} that returns {@code true} if a value to be tested equals,
     * using {@link Objects#equals(Object, Object)}, the specified value, and {@code false} otherwise.
     *
     * @param <T>   the type of value
     * @param value the value
     * @return a {@link Predicate}
     */
    public static <T> Predicate<T> isEqual(final T value) {
        return isEqual(value, "isEqual(" + value + ")");
    }

    /**
     * Obtains a {@link Predicate} that always returns {@code true}, regardless of the value provided.
     *
     * @param <T> the type of value
     * @return a {@link Predicate}
     */
    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> always() {
        return ALWAYS;
    }

    /**
     * Obtains a {@link Predicate} that always returns {@code false}, regardless of the value provided.
     *
     * @param <T> the type of value
     * @return a {@link Predicate}
     */
    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> never() {
        return NEVER;
    }

    /**
     * Obtains a {@link Predicate} that is the disjunction (or) of all of the specified {@link Predicate}s.
     *
     * @param predicates the {@link Predicate}s
     * @param <T>        the type of value
     * @return a {@link Predicate}
     */
    @SafeVarargs
    public static <T> Predicate<T> anyOf(final Predicate<? super T>... predicates) {
        return new AnyOf<>(predicates);
    }

    /**
     * Obtains a {@link Predicate} that is the disjunction (or) of all of the specified {@link Predicate}s.
     *
     * @param stream a {@link Stream} of {@link Predicate}s
     * @param <T>    the type of value
     * @return a {@link Predicate}
     */
    public static <T> Predicate<T> anyOf(final Stream<Predicate<? super T>> stream) {
        return new AnyOf<>(stream);
    }

    /**
     * Obtains a {@link Predicate} that is the conjunction (and) of all of the specified {@link Predicate}s.
     *
     * @param predicates the {@link Predicate}s
     * @param <T>        the type of value
     * @return a {@link Predicate}
     */
    @SafeVarargs
    public static <T> Predicate<T> allOf(final Predicate<? super T>... predicates) {
        return new AllOf<>(predicates);
    }

    /**
     * Obtains a {@link Predicate} that is the conjunction (and) of all of the specified {@link Predicate}s.
     *
     * @param stream the {@link Stream} of {@link Predicate}s
     * @param <T>    the type of value
     * @return a {@link Predicate}
     */
    public static <T> Predicate<T> allOf(final Stream<Predicate<? super T>> stream) {
        return new AllOf<>(stream);
    }

    /**
     * Returns a {@link Predicate} that is the negation of the supplied {@link Predicate}.
     * <p>
     * Inspired by <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/Predicate.html#not(java.util.function.Predicate)">Predicate.not(...)</a>
     * </p>
     *
     * @param predicate the {@link Predicate} to negate
     * @param <T>       the type of value
     * @return the negation of the supplied {@link Predicate}
     */
    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> not(final Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        return (Predicate<T>) predicate.negate();
    }

    /**
     * A constant for the {@link #always()} {@link Predicate}.
     */
    @SuppressWarnings("rawtypes")
    private static final Predicate ALWAYS = new Predicate() {
        @Override
        public boolean test(final Object t) {
            return true;
        }

        @Override
        public String toString() {
            return "always()";
        }

        @Override
        public int hashCode() {
            return 1;
        }
    };

    /**
     * A constant for the {@link #never()} {@link Predicate}.
     */
    @SuppressWarnings("rawtypes")
    private static final Predicate NEVER = new Predicate() {
        @Override
        public boolean test(final Object t) {
            return false;
        }

        @Override
        public String toString() {
            return "never()";
        }

        @Override
        public int hashCode() {
            return 1;
        }
    };

    /**
     * A {@link Predicate} that performs a disjunction (or) of zero or more {@link Predicate}s.
     *
     * @param <T> the type of value
     */
    public static class AnyOf<T>
        implements Predicate<T> {

        /**
         * The {@link Predicate}s to test.
         */
        private final LinkedList<Predicate<? super T>> predicates;

        /**
         * Constructs an {@link AnyOf} {@link Predicate} with the given {@link Predicate}s.
         *
         * @param predicates the {@link Predicate}s
         */
        @SafeVarargs
        public AnyOf(final Predicate<? super T>... predicates) {
            this(predicates == null || predicates.length == 0 ? Stream.empty() : Arrays.stream(predicates));
        }

        /**
         * Constructs an {@link AnyOf} {@link Predicate} with the given {@link Stream} of {@link Predicate}s.
         *
         * @param stream the {@link Stream} of {@link Predicate}s
         */
        public AnyOf(final Stream<Predicate<? super T>> stream) {
            this.predicates = new LinkedList<>();

            if (stream != null) {
                stream.forEach(predicate -> {
                    // don't allow nesting of AnyOf predicates (makes things much more efficient)
                    if (predicate instanceof AnyOf) {
                        @SuppressWarnings("unchecked") final AnyOf<T> anyOf = (AnyOf<T>) predicate;

                        this.predicates.addAll(anyOf.predicates);
                    }

                    // we skip null and NEVER predicates as they have no effect
                    else if (predicate != null && !predicate.equals(NEVER)) {
                        this.predicates.add(predicate);
                    }
                });
            }
        }

        @Override
        public boolean test(final T t) {
            for (final Predicate<? super T> predicate : this.predicates) {
                if (predicate.test(t)) {
                    // terminate early - as soon as there's success
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "anyOf(" +
                this.predicates.stream().map(Objects::toString).collect(Collectors.joining(", ")) +
                ")";
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final AnyOf<?> anyOf = (AnyOf<?>) o;
            return Objects.equals(this.predicates, anyOf.predicates);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.predicates);
        }
    }

    /**
     * A {@link Predicate} that performs a conjunction (and) of zero or more {@link Predicate}s.
     *
     * @param <T> the type of value
     */
    public static class AllOf<T>
        implements Predicate<T> {

        /**
         * The {@link Predicate}s to test.
         */
        private final LinkedList<Predicate<? super T>> predicates;

        /**
         * Constructs an {@link AllOf} {@link Predicate} with the given {@link Predicate}s.
         *
         * @param predicates the {@link Predicate}s
         */
        @SafeVarargs
        public AllOf(final Predicate<? super T>... predicates) {
            this(predicates == null || predicates.length == 0 ? Stream.empty() : Streams.of(predicates));
        }

        /**
         * Constructs an {@link AllOf} {@link Predicate} with the given {@link Stream} of {@link Predicate}s.
         *
         * @param stream the {@link Stream} of {@link Predicate}s
         */
        public AllOf(final Stream<Predicate<? super T>> stream) {
            this.predicates = new LinkedList<>();

            if (stream != null) {
                stream.forEach(predicate -> {

                    // don't allow nesting of AllOf predicates (makes things much more efficient)
                    if (predicate instanceof AllOf) {
                        @SuppressWarnings("unchecked") final AllOf<T> allOf = (AllOf<T>) predicate;

                        this.predicates.addAll(allOf.predicates);
                    }

                    // we skip null and ALWAYS predicates as they have no effect
                    else if (predicate != null && !predicate.equals(ALWAYS)) {
                        this.predicates.add(predicate);
                    }
                });
            }
        }

        @Override
        public boolean test(final T t) {
            for (final Predicate<? super T> predicate : this.predicates) {
                if (!predicate.test(t)) {
                    // terminate early - as soon as there's success
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return "allOf(" +
                this.predicates.stream().map(Objects::toString).collect(Collectors.joining(", ")) +
                ")";
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final AllOf<?> allOf = (AllOf<?>) o;
            return Objects.equals(this.predicates, allOf.predicates);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.predicates);
        }
    }
}
