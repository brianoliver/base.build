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

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An {@link Optional}-inspired, thread-safe lazily once-and-only-once initialized, {@link Supplier} of an immutable
 * reference to a non-{@code null} value.
 * <p>
 * Attempts to access the lazily initialized reference before it has been initialized or set, or set it multiple times
 * will result in an {@link IllegalStateException} being thrown.
 *
 * @param <T> the type of value
 * @author brian.oliver
 * @since Feb-2026
 */
public interface Lazy<T>
    extends Supplier<T> {

    /**
     * Attempts to obtain the value of the {@link Lazy} value, initializing it if necessary once-and-only-once.
     *
     * @return the value or {@code null} when the value is not present or can not  be initialized
     */
    T getOrNull();

    /**
     * Determines if a {@link Lazy} value is not present, or can not be initialized.
     *
     * @return {@code true} if a {@link Lazy} value is not present and/or can not be initialized, otherwise
     * {@code false} if it is present, has or can can be initialized
     */
    default boolean isEmpty() {
        return !isPresent();
    }

    /**
     * Determines if the {@link Lazy} value is present, has or can be initialized.
     *
     * @return {@code true} if a {@link Lazy} value is present, has or can be initialized, otherwise {@code false}
     * @see #isEmpty()
     */
    boolean isPresent();

    /**
     * Attempts to set the {@link Lazy} value to the specified value.
     *
     * @param value the value
     * @return this {@link Lazy} to permit fluent-method calls
     * @throws IllegalStateException if the {@link Lazy} has previously been set or initialized to a non-{@code null} value
     */
    Lazy<T> set(T value)
        throws IllegalStateException;

    /**
     * Obtains the value of the {@link Lazy} value.
     *
     * @return the value
     * @throws NoSuchElementException when the {@link Lazy} value has not been set or can not be initialized
     */
    @Override
    default T get() throws NoSuchElementException {
        final var value = getOrNull();

        if (value == null) {
            final var stack = Arrays.stream(Thread.currentThread().getStackTrace()).sequential()
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n"));

            throw new NoSuchElementException("The Lazy<T> has not been set\n" + stack);
        } else {
            return value;
        }
    }

    /**
     * Computes the value of the {@link Lazy} and stores it, if and only if it's not present, otherwise does nothing.
     * Should the {@link Supplier} need to be evaluated and be {@code null} or produce a {@code null}, the {@link Lazy}
     * remains unset.
     *
     * @param supplier the {@link Supplier} of the {@link Lazy} value
     * @return this {@link Lazy} value
     */
    Lazy<T> computeIfAbsent(Supplier<? extends T> supplier);

    /**
     * Obtains an {@link Optional} of the {@link Lazy} value.  Should the {@link Lazy} value not be initialized,
     * an {@link Optional#empty()} will be returned.
     *
     * @return an {@link Optional#empty()} should the {@link Lazy} value not be set, otherwise a
     * {@link Optional} of the {@link Lazy} value
     */
    default Optional<T> optional() {
        return Optional.ofNullable(getOrNull());
    }

    /**
     * Lazily applies the specified {@link Function} to map the {@link Lazy} value to another {@link Lazy} value.
     *
     * @param function the {@link Function}
     * @param <R>      the type of the new {@link Lazy} value
     * @return the result of the {@link Function} applied to the {@link Lazy} value
     */
    default <R> Lazy<R> map(final Function<? super T, R> function) {
        return function == null
            ? Lazy.empty()
            : new LazilyMapped<>(this, function);
    }

    /**
     * Lazily attempts to cast the {@link Lazy} value to the specified {@link Class}, returning a {@link Lazy}
     * of the converted value.
     *
     * @param requiredClass the required {@link Class}
     * @param <R>           the type of the new {@link Lazy} value
     * @return the result of casting the currently {@link Lazy} value to the specified {@link Class}
     */
    default <R> Lazy<R> map(final Class<R> requiredClass) {
        return requiredClass == null
            ? Lazy.empty()
            : filter(requiredClass::isInstance)
            .map(requiredClass::cast);
    }

    /**
     * Lazily returns a {@link Lazy} value if and only if the {@link Lazy} value satisfies the specified {@link Predicate}.
     *
     * @param predicate the {@link Predicate}
     * @return this {@link Lazy} when the {@link Predicate} is satisfied, otherwise {@link Lazy#empty()}
     */
    default Lazy<T> filter(final Predicate<? super T> predicate) {
        return predicate == null
            ? this
            : new LazilyFiltered<>(this, predicate);
    }

    /**
     * If the value is present, performs the given action with the value, otherwise does nothing.
     *
     * @param action the action to be performed, if a value is present
     * @throws NullPointerException if value is present and the given action is {@code null}
     */
    default void ifPresent(final Consumer<? super T> action) {
        final var value = getOrNull();

        if (value != null) {
            action.accept(value);
        }
    }

    /**
     * If the value is present, performs the given action with the value, otherwise performs the given
     * empty-based action.
     *
     * @param action      the action to be performed, if a value is present
     * @param emptyAction the empty-based action to be performed, if no value is present
     * @throws NullPointerException if a value is present and the given action is {@code null}, or no
     *                              value is present and the given empty-based action is
     *                              {@code null}.
     */
    default void ifPresentOrElse(final Consumer<? super T> action,
                                 final Runnable emptyAction) {

        final var value = getOrNull();

        if (value != null) {
            action.accept(value);
        } else {
            emptyAction.run();
        }
    }

    /**
     * If the value is present, returns a sequential {@link Stream} containing only that value,
     * otherwise returns an empty {@code Stream}. This method can be used to transform a {@code Stream} of optional
     * elements to a {@code Stream} of present value elements:
     * <pre>{@code
     *     Stream<Optional<T>> os = ..
     *     Stream<T> s = os.flatMap(Optional::stream)
     * }</pre>
     *
     * @return the value as a {@code Stream}
     */
    default Stream<T> stream() {
        return Stream.ofNullable(getOrNull());
    }

    /**
     * If the value is present, returns the value, otherwise returns {@code other}.
     *
     * @param other the value to be returned, if no value is present. May be {@code null}.
     * @return the value, if present, otherwise {@code other}
     */
    default T orElse(final T other) {
        final var value = getOrNull();

        return value == null ? other : value;
    }

    /**
     * If the value is present, returns the value, otherwise returns the result produced by the
     * supplying function.
     *
     * @param supplier the supplying function that produces a value to be returned
     * @return the value, if present, otherwise the result produced by the supplying function
     * @throws NullPointerException if no value is present and the supplying function is {@code null}
     */
    default T orElseGet(final Supplier<? extends T> supplier) {
        final var value = getOrNull();

        return value == null ? supplier.get() : value;
    }

    /**
     * If the value is present, returns the value, otherwise throws {@code NoSuchElementException}.
     *
     * @return the non-{@code null} value described by this {@code Optional}
     * @throws NoSuchElementException if no value is present
     */
    default T orElseThrow()
        throws NoSuchElementException {

        return get();
    }

    /**
     * If the value is present, returns the value, otherwise throws an exception produced by the
     * exception supplying function.
     *
     * @param <X>               Type of the exception to be thrown
     * @param exceptionSupplier the supplying function that produces an exception to be thrown
     * @return the value, if present
     * @throws X                    if no value is present
     * @throws NullPointerException if no value is present and the exception supplying function is
     *                              {@code null}
     */
    default <X extends Throwable> T orElseThrow(final Supplier<? extends X> exceptionSupplier)
        throws X {

        final var value = getOrNull();

        if (value != null) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }

    /**
     * If the value is present, returns this {@link Lazy} value, otherwise returns the {@link Lazy} result produced by
     * the supplying function.
     *
     * @param supplier the supplying function that produces a {@link Lazy} value to be returned
     * @return this {@link Lazy} value, if present, otherwise the result produced by the supplying function, or
     * {@link Lazy#empty()} if the supplying function is {@code null} or the returned result is {@code null}
     */
    default Lazy<T> or(final Supplier<? extends Lazy<T>> supplier) {
        final var value = getOrNull();

        if (value == null) {
            if (supplier == null) {
                return Lazy.empty();
            } else {
                final var lazy = supplier.get();
                return lazy == null ? Lazy.empty() : lazy;
            }
        } else {
            return this;
        }
    }

    /**
     * Obtains an empty {@link Lazy} value.
     *
     * @param <T> the type of {@link Lazy} value
     * @return an empty {@link Lazy} value
     */
    static <T> Lazy<T> empty() {
        return LazilySupplied.empty();
    }

    /**
     * Obtains a {@link Lazy} representation of the {@code null}able value.
     *
     * @param <T>   the type of {@link Lazy} value
     * @param value the type
     * @return a new {@link Lazy} value
     */
    static <T> Lazy<T> ofNullable(final T value) {
        return LazilySupplied.ofNullable(value);
    }

    /**
     * Obtains a {@link Lazy} representation of the non-{@code null} value.
     *
     * @param <T>   the type of {@link Lazy} value
     * @param value the type
     * @return a new {@link Lazy} value
     */
    static <T> Lazy<T> of(final T value) {
        return LazyValue.of(value);
    }

    /**
     * Obtains a {@link Lazy} that will be lazily initialized with a non-{@code null} value provided by the
     * {@link Supplier}.
     *
     * @param <T>      the type of {@link Lazy} value
     * @param supplier the {@link Supplier} of a non-{@code null} value
     * @return a new {@link Lazy} value
     */
    static <T> Lazy<T> of(final Supplier<? extends T> supplier) {
        return LazilySupplied.of(supplier);
    }


    /**
     * Obtains a {@link Collector} that collects a single value.
     * <p>
     * The collector will throw an {@link IllegalStateException} if no value or more than one value is collected.
     *
     * @param <T> the type of value
     * @return a new {@link Collector} that collects values into a {@link Lazy} value
     */
    static <T> Collector<T, Lazy<T>, T> collector() {
        return new Collector<>() {
            @Override
            public Supplier<Lazy<T>> supplier() {
                return Lazy::empty;
            }

            @Override
            public BiConsumer<Lazy<T>, T> accumulator() {
                return Lazy::set;
            }

            @Override
            public BinaryOperator<Lazy<T>> combiner() {
                return (lazy1, lazy2) -> {
                    if (lazy1.isEmpty()) {
                        return lazy2;
                    } else if (lazy2.isEmpty()) {
                        return lazy1;
                    } else {
                        throw new IllegalStateException("Cannot combine two Lazy<T> values that are both set");
                    }
                };
            }

            @Override
            public Function<Lazy<T>, T> finisher() {
                return Lazy::get;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Set.of(Characteristics.UNORDERED);
            }
        };
    }
}
