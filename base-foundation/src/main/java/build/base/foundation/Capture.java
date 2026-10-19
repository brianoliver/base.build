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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An {@link Optional}-inspired {@link Supplier} of a mutable reference to a non-{@code null} value.
 * <p>
 * Attempts to access a {@link Capture} when it has not been initialized or has been cleared will result in an
 * {@link IllegalStateException},
 *
 * @param <T> the type of value
 * @author brian.oliver
 * @since Jun-2024
 */
public final class Capture<T>
    implements Supplier<T> {

    /**
     * The value, {@code null} meaning non-yet-set.
     */
    private volatile T value;

    /**
     * The {@code null}able {@link Supplier} of a value when one has not been provided and one is requested.
     */
    private final Supplier<? extends T> supplier;

    /**
     * Constructs a {@link Capture} given the specified value, a {@code null} meaning an initially empty
     * {@link Capture}.
     *
     * @param value the value
     */
    private Capture(final T value) {
        this.value = value;
        this.supplier = null;
    }

    /**
     * Constructs a {@link Capture} given the specified value, a {@code null} meaning an initially empty
     * {@link Capture}.
     *
     * @param supplier the {@link Optional} {@link Supplier}
     */
    private Capture(final Supplier<? extends T> supplier) {
        this.value = null;
        this.supplier = supplier;
    }

    /**
     * Obtains the value of the {@link Capture} value, lazily initializing it if necessary.
     *
     * @return the value or {@code null} if not present
     */
    private T getOrNull()
        throws NoSuchElementException {

        var value = this.value;

        if (value == null && this.supplier != null) {
            synchronized (this) {
                value = this.value;

                if (value == null) {
                    value = this.supplier.get();

                    if (value == null) {
                        throw new IllegalArgumentException("Supplier " + supplier + " returned a null value");
                    }

                    this.value = value;
                }
            }
        }

        return value;
    }

    /**
     * Determines if the {@link Capture} value is not present.
     *
     * @return {@code true} if a {@link Capture} value is not present, {@code false} if it is
     * @see #isPresent()
     */
    public boolean isEmpty() {
        synchronized (this) {
            return getOrNull() == null;
        }
    }

    /**
     * Determines if the {@link Capture} value is present.
     *
     * @return {@code true} if a {@link Capture} value is present, {@code false} if it is not
     * @see #isEmpty()
     */
    public boolean isPresent() {
        return !isEmpty();
    }

    /**
     * Clears any previously captured value for the {@link Capture}.
     *
     * @return this {@link Capture} to permit fluent-method calls
     */
    public Capture<T> clear() {
        synchronized (this) {
            this.value = null;
            return this;
        }
    }

    /**
     * Sets the {@link Capture} to the specified value, replacing any previously captured value.
     *
     * @param value the value
     * @return this {@link Capture} to permit fluent-method calls
     */
    public Capture<T> set(final T value) {
        synchronized (this) {
            this.value = value;
            return this;
        }
    }

    /**
     * Sets the {@link Capture} to the specified value, if and only if there isn't a previously captured value.
     *
     * @param value the value
     * @return this {@link Capture} to permit fluent-method calls
     */
    public Capture<T> setIfAbsent(final T value) {
        synchronized (this) {
            if (getOrNull() == null) {
                this.value = value;
            }
            return this;
        }
    }

    /**
     * Sets the {@link Capture} to the specified value, if and only if there is a previously captured value.
     *
     * @param value the value
     * @return this {@link Capture} to permit fluent-method calls
     */
    public Capture<T> setIfPresent(final T value) {
        synchronized (this) {
            if (getOrNull() != null) {
                this.value = value;
            }
            return this;
        }
    }

    /**
     * Obtains the value of the {@link Capture} value.
     *
     * @return the value
     * @throws NoSuchElementException when the {@link Capture} value has not been set
     */
    @Override
    public T get()
        throws NoSuchElementException {

        final var value = getOrNull();

        if (value == null) {
            final var stack = Arrays.stream(Thread.currentThread().getStackTrace())
                .sequential()
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n"));

            throw new NoSuchElementException("The Capture<T> has not been set\n" + stack);
        }
        else {
            return value;
        }
    }

    /**
     * Attempts to obtain and simultaneously {@link Capture#clear()} the currently captured value.
     * <p>
     * Should a value not be present in the {@link Capture}, an attempt is made to use the current {@link Supplier}
     * to capture a value.  After capturing a value, the {@link Capture} is cleared.
     *
     * @return the captured value
     * @throws NoSuchElementException if a value could not be captured or there is no {@link Supplier} of values
     */
    public T consume()
        throws NoSuchElementException {

        final var value = getOrNull();

        if (value == null) {
            throw new NoSuchElementException("No value and no supplier from which to capture an element");
        }

        clear();

        return value;
    }

    /**
     * Computes the value of the {@link Capture} and stores it, if and only if it's not present, otherwise does nothing.
     * Should the {@link Supplier} need to be evaluated and be {@code null} or produce a {@code null}, the {@link Capture}
     * remains unset.
     *
     * @param supplier the {@link Supplier} of the {@link Capture} value
     * @return this {@link Capture} value
     */
    public Capture<T> compute(final Supplier<? extends T> supplier) {
        if (supplier == null) {
            return this;
        }

        synchronized (this) {
            if (getOrNull() == null) {
                this.value = supplier.get();
            }

            return this;
        }
    }

    /**
     * Computes the value of the {@link Capture} and stores it based on the existing value, if and only if it's present,
     * otherwise does nothing.  Should the {@link Function} need to be evaluated and be {@code null} or produce a
     * {@code null}, the {@link Capture} is cleared.
     *
     * @param function the {@link Function} of the {@link Capture} value
     * @return this {@link Capture} value
     */
    public Capture<T> compute(final Function<T, ? extends T> function) {
        synchronized (this) {
            final var value = getOrNull();

            if (value != null) {
                this.value = function == null
                    ? null
                    : function.apply(value);
            }

            return this;
        }
    }

    /**
     * Obtains an {@link Optional} of the {@link Capture} value.  Should the {@link Capture} value not be initialized,
     * an {@link Optional#empty()} will be returned.
     *
     * @return an {@link Optional#empty()} should the {@link Capture} value not be set, otherwise a
     * {@link Optional} of the {@link Capture} value
     */
    public Optional<T> optional() {
        return Optional.ofNullable(getOrNull());
    }

    /**
     * Applies the specified {@link Function} to map the {@link Capture} value to another {@link Capture} value.
     *
     * @param function the {@link Function}
     * @param <U>      the type of the new {@link Capture} value
     * @return the result of the {@link Function} applied to the {@link Capture} value
     */
    public <U> Capture<U> map(final Function<? super T, ? extends U> function) {
        if (function == null) {
            return Capture.empty();
        }

        final var value = getOrNull();

        if (value == null) {
            return Capture.empty();
        }
        else {
            return Capture.ofNullable(function.apply(value));
        }
    }

    /**
     * Attempts to cast the currently captured value to the specified {@link Class}, returning a {@link Capture}
     * of the converted value.
     *
     * @param requiredClass the required {@link Class}
     * @param <U>           the type of the new {@link Capture} value
     * @return the result of casting the currently captured value to the specified {@link Class}
     */
    public <U> Capture<U> map(final Class<U> requiredClass) {
        if (requiredClass == null) {
            return Capture.empty();
        }

        final var value = getOrNull();

        return requiredClass.isInstance(value)
            ? Capture.of(requiredClass.cast(value))
            : Capture.empty();
    }

    /**
     * Returns this {@link Capture} if and only if the captured value satisfies the specified {@link Predicate}.
     *
     * @param predicate the {@link Predicate}
     * @return this {@link Capture} when the {@link Predicate} is satisfied, otherwise {@link Capture#empty()}
     */
    public Capture<T> filter(final Predicate<? super T> predicate) {
        final var value = getOrNull();

        return predicate == null || value == null || !predicate.test(value)
            ? Capture.empty()
            : this;
    }

    /**
     * If the value is present, performs the given action with the value, otherwise does nothing.
     *
     * @param action the action to be performed, if a value is present
     * @throws NullPointerException if value is present and the given action is {@code null}
     */
    public void ifPresent(final Consumer<? super T> action) {
        final var value = getOrNull();

        if (value != null) {
            action.accept(value);
        }
    }

    /**
     * Executes the specified {@link Runnable} when the {@link Capture#isEmpty()}.
     *
     * @param runnable the {@link Runnable} to execute
     */
    public void ifEmpty(final Runnable runnable) {
        if (runnable != null && !isEmpty()) {
            runnable.run();
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
    public void ifPresentOrElse(final Consumer<? super T> action, final Runnable emptyAction) {
        final var value = getOrNull();

        if (value != null) {
            action.accept(value);
        }
        else {
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
    public Stream<T> stream() {
        return Stream.ofNullable(getOrNull());
    }

    /**
     * If the value is present, returns the value, otherwise returns {@code other}.
     *
     * @param other the value to be returned, if no value is present. May be {@code null}.
     * @return the value, if present, otherwise {@code other}
     */
    public T orElse(final T other) {
        final var value = getOrNull();

        return value != null ? value : other;
    }

    /**
     * If the value is present, returns the value, otherwise returns the result produced by the
     * supplying function.
     *
     * @param supplier the supplying function that produces a value to be returned
     * @return the value, if present, otherwise the result produced by the supplying function
     * @throws NullPointerException if no value is present and the supplying function is {@code null}
     */
    public T orElseGet(final Supplier<? extends T> supplier) {
        final var value = getOrNull();

        return value != null ? value : supplier.get();
    }

    /**
     * If the value is present, returns the value, otherwise throws {@code NoSuchElementException}.
     *
     * @return the non-{@code null} value described by this {@code Optional}
     * @throws NoSuchElementException if no value is present
     */
    public T orElseThrow()
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
    public <X extends Throwable> T orElseThrow(final Supplier<? extends X> exceptionSupplier)
        throws X {

        final var value = getOrNull();

        if (value != null) {
            return value;
        }
        else {
            throw exceptionSupplier.get();
        }
    }

    /**
     * If the value is present, returns this {@link Capture} value, otherwise returns the {@link Capture} result produced by
     * the supplying function.
     *
     * @param supplier the supplying function that produces a {@link Capture} value to be returned
     * @return this {@link Capture} value, if present, otherwise the result produced by the supplying function, or
     * {@link Capture#empty()} if the supplying function is {@code null} or the returned result is {@code null}
     */
    public Capture<T> or(final Supplier<? extends Capture<T>> supplier) {
        final var value = getOrNull();

        if (value == null) {
            if (supplier == null) {
                return Capture.empty();
            }
            else {
                final var lazy = supplier.get();
                return lazy == null ? Capture.empty() : lazy;
            }
        }
        else {
            return this;
        }
    }

    /**
     * Indicates whether some other object is "equal to" this {@link Capture}. The other object is
     * considered equal if:
     * <ul>
     * <li>it is also an {@link Capture} and;
     * <li>both instances have no value present or;
     * <li>the present values are "equal to" each other via {@code equals()}.
     * </ul>
     *
     * @param object an object to be tested for equality
     * @return {@code true} if the other object is "equal to" this object otherwise {@code false}
     */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        return object instanceof Capture<?> other
            && Objects.equals(getOrNull(), other.getOrNull());
    }

    /**
     * Returns a non-empty string representation of this {@link Capture} suitable for debugging.  The
     * exact presentation format is unspecified and may vary between implementations and versions.
     *
     * @return the string representation of this instance
     * result.  Empty and present {@code Optional}s must be unambiguously differentiable.
     */
    @Override
    public String toString() {
        return getOrNull() == null
            ? "Capture.empty"
            : "Capture[" + this.value + "]";
    }

    /**
     * Obtains an empty {@link Capture} value.
     *
     * @param <T> the type of {@link Capture} value
     * @return an empty {@link Capture} value
     */
    public static <T> Capture<T> empty() {
        return new Capture<>(null);
    }

    /**
     * Obtains a {@link Capture} representation of the {@code null}able value.
     *
     * @param <T>   the type of {@link Capture} value
     * @param value the type
     * @return a new {@link Capture} value
     */
    public static <T> Capture<T> ofNullable(final T value) {
        return new Capture<>(value);
    }

    /**
     * Obtains a {@link Capture} representation of the non-{@code null} value.
     *
     * @param <T>   the type of {@link Capture} value
     * @param value the type
     * @return a new {@link Capture} value
     */
    public static <T> Capture<T> of(final T value) {
        Objects.requireNonNull(value,
            "The Capture value must not be null.  Use Capture.ofNullable(T) instead?");

        return new Capture<>(value);
    }

    /**
     * Obtains a {@link Capture} representation of an {@link Optional} value.
     *
     * @param <T>      the type of {@link Capture} value
     * @param optional the {@link Optional} value
     * @return a new {@link Capture} value
     */
    public static <T> Capture<T> of(final Optional<T> optional) {
        return optional == null
            ? Capture.empty()
            : Capture.of(optional.orElse(null));
    }

    /**
     * Obtains a {@link Capture} that will be lazily initialized with a non-{@code null} value provided by the
     * {@link Supplier}.
     *
     * @param <T>      the type of {@link Capture} value
     * @param supplier the {@link Supplier} of a non-{@code null} value
     * @return a new {@link Capture} value
     */
    public static <T> Capture<T> of(final Supplier<? extends T> supplier) {
        return supplier == null
            ? Capture.empty()
            : new Capture<>(supplier);
    }
}
