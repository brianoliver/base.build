package build.base.marshalling;

/*-
 * #%L
 * base.build Marshalling
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
 * An <strong>immutable</strong> once set, <strong>non-thread-safe</strong> {@link Optional}-inspired {@code null}able
 * value, that is produced as a result of <a href="https://en.wikipedia.org/wiki/Marshalling_(computer_science)">Marshalling</a>.
 * <p>
 * Attempts to read the value of an {@link Out}, when it has not been set, will result in an
 * {@link IllegalStateException} being thrown.  Attempts to write the value of an {@link Out}, when it has previously
 * been set, will result in an {@link IllegalStateException} being thrown.
 *
 * @param <T> the type of value
 * @author brian.oliver
 * @since Apr-2025
 */
public final class Out<T>
    implements Supplier<T> {

    /**
     * An constant representing a {@code null} {@link Out} value.
     */
    private static final Out<?> NULL = Out.empty().set(null);

    /**
     * The {@code null}able value.
     */
    private T value;

    /**
     * Determines if the value has been set.
     */
    private boolean set;

    /**
     * Constructs a {@link Out} with an unset value.
     */
    private Out() {
        this.value = null;
        this.set = false;
    }

    /**
     * Obtains the value of the {@link Out}, or {@code null} if unset.
     *
     * @return the {@link Out} value or {@code null} if not set
     */
    private T getOrNull() {
        return this.set ? this.value : null;
    }

    /**
     * Determines if the {@link Out} value has been set, indicating that the {@link Out} value is immutable.
     *
     * @return {@code true} if a {@link Out} value has been set, {@code false} otherwise
     * @see #isEmpty()
     */
    public boolean isPresent() {
        return this.set;
    }

    /**
     * Determines if the {@link Out} value is unset, indicating that the {@link Out} value is mutable.
     *
     * @return {@code true} if a {@link Out} value is not set, {@code false} otherwise
     * @see #isPresent()
     */
    public boolean isEmpty() {
        return !isPresent();
    }

    /**
     * Attempts to set the {@link Out} to the specified value, after which it {@link Out} is immutable.
     *
     * @param value the value
     * @return this {@link Out} to permit fluent-method calls
     * @throws IllegalStateException should {@link Out#isPresent()}
     */
    public Out<T> set(final T value) {
        if (isPresent()) {
            final var stack = Arrays.stream(Thread.currentThread().getStackTrace())
                .sequential()
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n"));

            throw new IllegalStateException("The Out<T> is already set to [" + this.value + "]\n" + stack);
        }

        this.set = true;
        this.value = value;

        return this;
    }

    /**
     * Sets the {@link Out} to the specified value, if and only if there {@link Out#isEmpty()}.
     *
     * @param value the value
     * @return this {@link Out} to permit fluent-method calls
     * @see #isEmpty()
     */
    public Out<T> setIfAbsent(final T value) {
        if (!isPresent()) {
            set(value);
        }
        return this;
    }

    /**
     * Obtains the value of the {@link Out} when {@link Out#isPresent()}.
     *
     * @return the value
     * @throws NoSuchElementException when the {@link Out#isEmpty()}
     * @see #isPresent()
     */
    @Override
    public T get()
        throws NoSuchElementException {

        if (!isPresent()) {
            final var stack = Arrays.stream(Thread.currentThread().getStackTrace())
                .sequential()
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n"));

            throw new NoSuchElementException("The Out<T> has not been set\n" + stack);
        }
        else {
            return this.value;
        }
    }

    /**
     * Obtains an {@link Optional} of the {@link Out} value.  Should {@link Out#isEmpty()},
     * an {@link Optional#empty()} will be returned.
     *
     * @return an {@link Optional#empty()} should {@link Out#isEmpty()}, otherwise an {@link Optional#ofNullable(Object)}
     * of the {@link Out} value
     */
    public Optional<T> optional() {
        return Optional.ofNullable(getOrNull());
    }

    /**
     * Applies the specified {@link Function} to map the {@link Out} value to produce a new {@link Out} value.
     *
     * @param function the {@link Function}
     * @param <U>      the type of the new {@link Out} value
     * @return the result of the {@link Function} applied to the {@link Out} value
     */
    public <U> Out<U> map(final Function<? super T, ? extends U> function) {
        return function == null || isEmpty()
            ? Out.empty()
            : Out.of(function.apply(get()));
    }

    /**
     * Attempts to cast the currently set value to the specified {@link Class}, returning a {@link Out}
     * of the converted value.
     *
     * @param requiredClass the required {@link Class}
     * @param <U>           the type of the new {@link Out} value
     * @return the result of casting the currently captured value to the specified {@link Class}
     */
    @SuppressWarnings("unchecked")
    public <U> Out<U> map(final Class<U> requiredClass) {
        return requiredClass == null || isEmpty() || !requiredClass.isInstance(this.value)
            ? Out.empty()
            : (Out<U>) this;
    }

    /**
     * Returns this {@link Out} if and only if the set value satisfies the specified {@link Predicate}.
     *
     * @param predicate the {@link Predicate}
     * @return this {@link Out} when the {@link Predicate} is satisfied, otherwise {@link Out#empty()}
     */
    public Out<T> filter(final Predicate<? super T> predicate) {
        return predicate == null || isEmpty() || !predicate.test(this.value)
            ? Out.empty()
            : this;
    }

    /**
     * If {@link Out#isPresent()}, performs the given action with the value, otherwise does nothing.
     *
     * @param action the action to be performed, if {@link Out#isPresent()}
     * @throws NullPointerException if value is present and the given action is {@code null}
     */
    public void ifPresent(final Consumer<? super T> action) {
        if (isPresent()) {
            action.accept(this.value);
        }
    }

    /**
     * Executes the specified {@link Runnable} when the {@link Out#isEmpty()}.
     *
     * @param runnable the {@link Runnable} to execute
     */
    public void ifEmpty(final Runnable runnable) {
        if (runnable != null && !isEmpty()) {
            runnable.run();
        }
    }

    /**
     * If {@link Out#isPresent()}, performs the given action with the value, otherwise performs the given
     * empty-based action.
     *
     * @param action      the action to be performed, if {@link Out#isPresent()}
     * @param emptyAction the empty-based action to be performed, if no value is present
     */
    public void ifPresentOrElse(final Consumer<? super T> action, final Runnable emptyAction) {
        if (action != null && isPresent()) {
            action.accept(this.value);
        }
        else if (emptyAction != null) {
            emptyAction.run();
        }
    }

    /**
     * If {@link Out#isPresent()}, returns a sequential {@link Stream} containing only the {@link Out#get()} value,
     * otherwise returns an empty {@code Stream}. This method can be used to transform a {@code Stream} of {@link Out}
     * elements to a {@code Stream} of present value elements:
     * <pre>{@code
     *     Stream<Out<T>> os = ..
     *     Stream<T> s = os.flatMap(Out::stream)
     * }</pre>
     *
     * @return the value as a {@code Stream}
     */
    public Stream<T> stream() {
        return isEmpty()
            ? Stream.empty()
            : Stream.ofNullable(this.value);
    }

    /**
     * If {@link Out#isPresent()}, returns the value, otherwise returns {@code other}.
     *
     * @param other the value to be returned, if no value is present. May be {@code null}.
     * @return the value, if present, otherwise {@code other}
     */
    public T orElse(final T other) {
        return isPresent()
            ? this.value
            : other;
    }

    /**
     * If {@link Out#isPresent()}, returns the value, otherwise returns the result produced by the
     * supplying function.
     *
     * @param supplier the supplying function that produces a value to be returned
     * @return the value, if present, otherwise the result produced by the supplying function
     * @throws NullPointerException if no value is present and the supplying function is {@code null}
     */
    public T orElseGet(final Supplier<? extends T> supplier) {
        return isPresent()
            ? this.value
            : supplier.get();
    }

    /**
     * If {@link Out#isPresent()}, returns the value, otherwise throws {@code NoSuchElementException}.
     *
     * @return the {@link Out} value
     * @throws NoSuchElementException if {@link Out#isEmpty()}
     * @see #get()
     */
    public T orElseThrow()
        throws NoSuchElementException {

        return get();
    }

    /**
     * If {@link Out#isPresent()}, returns the value, otherwise throws a {@link Throwable} produced by the
     * {@link Supplier}.
     *
     * @param <X>               Type of the exception to be thrown
     * @param exceptionSupplier the {@link Supplier} that produces a {@link Throwable} to be thrown
     * @return the value, if {@link Out#isPresent()}
     * @throws X                    if {@link Out#isEmpty()}
     * @throws NullPointerException if {@link Out#isEmpty()} and the exception supplying function is
     *                              {@code null}
     */
    public <X extends Throwable> T orElseThrow(final Supplier<? extends X> exceptionSupplier)
        throws X {

        if (isPresent()) {
            return this.value;
        }

        throw exceptionSupplier.get();
    }

    /**
     * Indicates whether some other object is "equal to" this {@link Out}. The other object is
     * considered equal if:
     * <ul>
     * <li>it is also an {@link Out} and;
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

        return object instanceof Out<?> other
            && this.set == other.set
            && Objects.equals(this.value, other.value);
    }

    /**
     * Returns a non-empty string representation of this {@link Out} suitable for debugging.  The
     * exact presentation format is unspecified and may vary between implementations and versions.
     *
     * @return the string representation of this instance
     * result.  Empty and present {@code Optional}s must be unambiguously differentiable.
     */
    @Override
    public String toString() {
        return isPresent()
            ? "Out[" + this.value + "]"
            : "Out.empty()";
    }

    /**
     * Obtains a mutable unset {@link Out} value.
     *
     * @param <T> the type of {@link Out} value
     * @return a mutable unset {@link Out} value
     */
    public static <T> Out<T> empty() {
        return new Out<>();
    }

    /**
     * Obtains an immutable {@link Out} {@code null} value.
     *
     * @param <T> the type of {@link Out} value
     * @return an immutable {@link Out} {@code null} value
     */
    @SuppressWarnings("unchecked")
    public static <T> Out<T> nullValue() {
        return (Out<T>) NULL;
    }

    /**
     * Obtains an immutable {@link Out} of the specified value.
     *
     * @param <T>   the type of {@link Out} value
     * @param value the type
     * @return a new immutable {@link Out} value
     */
    public static <T> Out<T> of(final T value) {
        return value == null
            ? nullValue()
            : Out.<T>empty()
                .set(value);
    }

    /**
     * Obtains an immutable {@link Out} representation of an {@link Optional} value.
     *
     * @param <T>      the type of {@link Out} value
     * @param optional the {@link Optional} value
     * @return a new {@link Out} value
     */
    public static <T> Out<T> of(final Optional<T> optional) {
        return optional == null || optional.isEmpty()
            ? nullValue()
            : Out.of(optional.orElseThrow());
    }
}
