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

import java.io.InterruptedIOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * An {@link Optional}-like immutable container for capturing either non-{@code null} values or {@link Exception}s.
 * <p>
 * If a value is present, {@link #isPresent()} returns {@code true}. If an exception is present {@link #isException()}
 * returns {@code true}. If neither is present {@link #isEmpty()} returns {@code true}.
 * <p>
 * Additional methods that depend on the presence or absence of a contained value are provided, such as orElse()
 * (returns a default value if no value is present) and ifPresent() (performs an action if a value is present).
 * <p>
 * This is a value-based class; programmers should treat instances that are equal as interchangeable and should not use
 * instances for synchronization, or unpredictable behavior may occur. For example, in a future release,
 * synchronization may fail.
 * <p>
 * API Note:
 * {@link Exceptional} is primarily intended for use as a method return type where there is a clear need to represent
 * "no result" or "an exception instead of a result" and where using {@code null}, or throwing exceptions is likely to
 * cause errors. A variable whose type is {@link Exceptional} should never itself be {@code null}; it should always
 * point to an {@link Exceptional} instance.
 * <p>
 * Note that {@link Exceptional} handles {@link Exception}s including unchecked {@link RuntimeException}s, but
 * intentionally does not handle {@link Error}s as those should not be obscured.
 *
 * @param <T> the type of the value
 * @author brian.oliver
 * @author mark.falco
 * @since Nov-2021
 */
public final class Exceptional<T>
    implements Iterable<T> {

    /**
     * The constant for an empty {@link Exceptional}.
     */
    private static final Exceptional<?> EMPTY = new Exceptional<>();

    /**
     * The value for the {@link Exceptional}.
     * <p>
     * A value of {@code null} means no value is present, however a {@link Exception} may be present.
     */
    private final T value;

    /**
     * The {@link Exception} for the {@link Exceptional}.
     * <p>
     * A {@code null} means no {@link Exception} is present.
     */
    private final Exception exception;

    /**
     * Constructs an empty {@link Exceptional}.
     */
    private Exceptional() {
        this.value = null;
        this.exception = null;
    }

    /**
     * Constructs an {@link Exceptional} given the specified {@link Exception}.
     *
     * @param exception the {@link Exception}
     */
    private Exceptional(final T value, final Exception exception) {
        this.value = value;
        this.exception = exception;

        // ensure InterruptedExceptions carry forward the interruption
        if (this.exception instanceof InterruptedException || this.exception instanceof InterruptedIOException) {
            // we're consuming the exception and thus need to restore the interrupt
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Determines if a value is present, irrespective of a {@link Exception} being present.
     *
     * @return {@code true} if a value is present, {@code false} otherwise
     */
    public boolean isPresent() {
        return this.value != null;
    }

    /**
     * Determines if the {@link Exceptional} contains a {@link Exception}.
     *
     * @return {@code true} if when the {@link Exceptional} contains a {@link Exception}, {@code false} otherwise
     */
    public boolean isException() {
        return this.exception != null;
    }

    /**
     * Determines if neither a value nor exception is present.
     *
     * @return {@code true} if a value is not present, {@code false} otherwise
     */
    public boolean isEmpty() {
        return this.value == null && this.exception == null;
    }


    /**
     * If a value is present, performs the given action with the value,
     * otherwise does nothing.
     *
     * @param action the action to be performed, if a value is present
     * @return an {@link Exceptional} containing any exception thrown by the action if run, otherwise {@link #empty()}
     */
    public Exceptional<?> ifPresent(final Consumer<? super T> action) {
        return isPresent()
            ? consume(action::accept, this.value)
            : empty();
    }

    /**
     * If a value is present, performs the given action with the value,
     * otherwise performs the given empty-based action.
     *
     * @param action      the action to be performed, if a value is present
     * @param emptyAction the empty-based action to be performed, if no value is
     *                    present
     * @return an {@link Exceptional} containing any exception thrown by the action, otherwise {@link #empty()}
     */
    public Exceptional<?> ifPresentOrElse(final Consumer<? super T> action, final Runnable emptyAction) {
        return isPresent()
            ? consume(action::accept, this.value)
            : run(emptyAction::run);
    }

    /**
     * Returns {@code this} if {@link #isPresent()} and the {@link #orElseThrow()} () value} satisfies the filter, or if
     * {@link #isException()}, otherwise returns an empty {@link Exceptional}.
     *
     * @param predicate the predicate to apply to a value, if present
     * @return {@code this} if {@link #isPresent()} and the {@link #orElseThrow()} () value} satisfies the filter, or if
     * * {@link #isException()}, otherwise returns an empty {@link Exceptional}
     */
    public Exceptional<T> filter(final Predicate<? super T> predicate) {
        try {
            return isException() || (isPresent() && predicate.test(this.value)) ? this : empty();
        } catch (final Exception e) {
            return ofException(e);
        }
    }

    /**
     * If a value is present, returns an {@link Exceptional} describing (as if by {@link #ofNullable}) the result of
     * applying the given mapping function to the value, otherwise if {@link #isException()} returns an {@link Exceptional}
     * describing the exception, otherwise returns an empty {@link Exceptional}.
     * <p>
     * If the mapping function returns a {@code null} result then this method returns an empty {@link Exceptional}.
     *
     * @param mapper the mapping function to apply to a value, if present
     * @param <U>    The type of the value returned from the mapping function
     * @return an {@link Exceptional} describing the result of applying a mapping function to the value of this
     * {@link Exceptional}, if a value is present
     */
    public <U> Exceptional<U> map(final Function<? super T, ? extends U> mapper) {
        return isPresent()
            ? apply(mapper::apply, this.value)
            : isException()
            ? Exceptional.ofException(this.exception)
            : empty();
    }

    /**
     * If a value is present, provides the value to specified {@link Consumer}.
     *
     * @param consumer the {@link Consumer} to consume the value, if present
     * @return this {@link Exceptional} unless the consumer throws an exception in which case an {@link Exceptional}
     * describing the exception is returned.
     */
    public Exceptional<T> peek(final Consumer<? super T> consumer) {
        if (isPresent()) {
            try {
                consumer.accept(this.value);
            } catch (final Exception e) {
                return ofException(e);
            }
        }

        return this;
    }

    /**
     * If a value is present, returns the result of applying the given {@link Exceptional}-bearing mapping function
     * to the value, otherwise returns an empty {@link Exceptional}.
     * <p>
     * This method is similar to {@link #map(Function)}, but the mapping function is one whose result is already an
     * {@link Exceptional}, and if invoked, {@code flatMap} does not wrap it within an additional
     * {@link Exceptional}.
     *
     * @param <U>    The type of value of the {@link Exceptional} returned by the mapping function
     * @param mapper the mapping function to apply to a value, if present
     * @return the result of applying an {@link Exceptional}-bearing mapping
     * function to the value of this {@link Exceptional}, if a value is
     * present, otherwise an empty {@link Exceptional}
     */
    @SuppressWarnings("unchecked")
    public <U> Exceptional<U> flatMap(final Function<? super T, ? extends Exceptional<U>> mapper) {
        if (!isPresent()) {
            return (Exceptional<U>) exception()
                .map(Exceptional::ofException)
                .orElse(empty());
        } else {
            try {
                return Objects.requireNonNull(mapper.apply(this.value));
            } catch (final Exception e) {
                return ofException(e);
            }
        }
    }

    /**
     * Obtains an {@link Optional} representing the value.
     *
     * @return an {@link Optional} value
     */
    public Optional<T> optional() {
        return Optional.ofNullable(this.value);
    }

    /**
     * Obtains an {@link Optional} representing the {@link Exception}.
     *
     * @return an {@link Optional} {@link Exception}
     */
    public Optional<Exception> exception() {
        return Optional.ofNullable(this.exception);
    }

    /**
     * Obtains a completed {@link CompletableFuture} representing the {@link Exceptional}.
     *
     * @return an {@link CompletableFuture}
     */
    public CompletableFuture<T> future() {
        return isPresent()
            ? CompletableFuture.completedFuture(this.value)
            : isException()
            ? CompletableFuture.failedFuture(this.exception)
            : CompletableFuture.completedFuture(null);
    }

    /**
     * If a value is present, returns a sequential {@link Stream} containing only that value, otherwise returns an
     * empty {@link Stream}.
     * <p>
     * This method can be used to transform a {@code Stream} of Exceptional elements to a {@link Stream} of present
     * value elements:
     * <pre>{@code
     *     Stream<Exceptional<T, ?>> os = ..
     *     Stream<T> s = os.flatMap(Exceptional::stream)
     * }</pre>
     *
     * @return the {@link Exceptional} value as a {@link Stream}
     */
    public Stream<T> stream() {
        return isPresent() ? Stream.of(this.value) : Stream.empty();
    }

    @Override
    public Iterator<T> iterator() {
        if (isPresent()) {
            return new Iterator<T>() {
                T next = orElseThrow();

                @Override
                public boolean hasNext() {
                    return this.next != null;
                }

                @Override
                public T next() {
                    final T next = this.next;
                    if (next == null) {
                        throw new NoSuchElementException();
                    }

                    this.next = null;
                    return next;
                }
            };
        }

        return java.util.Collections.emptyIterator();
    }

    /**
     * If a value is present, returns the value, otherwise returns {@code other}, including if {@link #isException()}.
     *
     * @param other the value to be returned, if no value is present. May be {@code null}.
     * @return the value, if present, otherwise {@code other}
     */
    public T orElse(final T other) {
        return this.value == null
            ? other
            : this.value;
    }

    /**
     * If a value is present, returns the value, otherwise returns the result produced by the supplying function,
     * including if {@link #isException()}.
     *
     * @param supplier the supplying function that produces a value to be returned
     * @return the value, if present, otherwise the result produced by the
     * supplying function
     * @throws NullPointerException if no value is present and the supplying
     *                              function is {@code null}
     */
    public T orElseGet(final Supplier<? extends T> supplier) {
        return this.value == null
            ? supplier.get()
            : this.value;
    }

    /**
     * Obtains the current value of the {@link Exceptional}, or throws a {@link NoSuchElementException}
     * with the {@link Throwable#getCause()} being the underlying {@link Exception}, should there be an
     * {@link Exception} causing the value not to be present.
     *
     * @return the non-{@code null} value described by this {@link Exceptional}
     * @throws NoSuchElementException if no value is present
     */
    public T orElseThrow()
        throws NoSuchElementException {

        if (this.value == null) {
            final NoSuchElementException exception = new NoSuchElementException(this.exception == null
                ? "No value present"
                : "No value present (failed exceptionally)");
            exception.initCause(this.exception);
            throw exception;
        }
        return this.value;
    }

    /**
     * If a value is present, return this {@link Exceptional}, otherwise use the {@link Supplier} to obtain
     * an {@link Exceptional} value.
     *
     * @param supplier the {@link Supplier}
     * @return an {@link Exceptional} value
     */
    public Exceptional<T> otherwise(final Supplier<Exceptional<T>> supplier) {
        return isPresent()
            ? this
            : supplier.get();
    }

    /**
     * If a value is present, returns the value, otherwise throws an exception produced by the exception supplying
     * function.
     * <p>
     * A method reference to the exception constructor with an empty argument list can be used as the supplier. For
     * example, {@code IllegalStateException::new}
     *
     * @param <X>               Type of the exception to be thrown
     * @param exceptionSupplier the supplying function that produces an
     *                          exception to be thrown
     * @return the value, if present
     * @throws X                    if no value is present
     * @throws NullPointerException if no value is present and the exception
     *                              supplying function is {@code null}
     */
    public <X extends Throwable> T orElseThrow(final Supplier<? extends X> exceptionSupplier)
        throws X {

        if (this.value != null) {
            return this.value;
        } else {
            throw exceptionSupplier.get();
        }
    }

    /**
     * If a value is present, returns the value, otherwise attempts to throw an exception produced by the exception
     * supplying {@link Function}.
     *
     * @param <X>               Type of the exception to be thrown
     * @param exceptionFunction the supplying {@link Function} that produces an exception to be thrown,
     *                          based on the current {@link Exception} (which may be {@code null})
     * @return the value, if present
     * @throws X                    if no value is present
     * @throws NullPointerException if no value is present and the exception supplying function is {@code null}
     */
    public <X extends Throwable> T orElseRethrow(final Function<? super Exception, ? extends X> exceptionFunction)
        throws X {

        if (this.value != null) {
            return this.value;
        } else {
            throw exceptionFunction.apply(this.exception);
        }
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Exceptional<?> that)) {
            return false;
        }

        return Objects.equals(this.value, that.value)
            && Objects.equals(this.exception, that.exception);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.value, this.exception);
    }

    @Override
    public String toString() {
        if (this.value != null) {
            return "Exceptional[" + this.value + "]";
        } else if (this.exception != null) {
            return "Exceptional[" + this.exception + "]";
        } else {
            return "Exceptional.empty";
        }
    }

    /**
     * Obtains the empty {@link Exceptional}, without a value and a {@link Exception}.
     *
     * @param <T> the type of the value
     * @return the empty {@link Exceptional}
     */
    @SuppressWarnings("unchecked")
    public static <T> Exceptional<T> empty() {
        return (Exceptional<T>) EMPTY;
    }

    /**
     * Creates an {@link Exceptional} for the specified value.
     *
     * @param value the non-{@code null} value
     * @param <T>   the type of the value
     * @return an {@link Exceptional} of the specified value
     */
    public static <T> Exceptional<T> of(final T value) {
        return new Exceptional<>(Objects.requireNonNull(value), null);
    }

    /**
     * Creates an {@link Exceptional} for the specified nullable-value.
     *
     * @param value the nullable value
     * @param <T>   the type of the value
     * @return an {@link Exceptional} of the specified value
     */
    public static <T> Exceptional<T> ofNullable(final T value) {
        return value == null
            ? empty()
            : of(value);
    }

    /**
     * Creates an {@link Exceptional} for the specified {@link Exception}.
     *
     * @param exception the non-{@code null} {@link Exception}
     * @param <T>       the type of the value
     * @return an {@link Exceptional} of the specified {@link Exception}
     */
    public static <T> Exceptional<T> ofException(final Exception exception) {
        return new Exceptional<>(null, exception == null ? new NullPointerException() : exception);
    }

    /**
     * Creates an {@link Exceptional} for the specified {@link Optional} value.
     *
     * @param optional the {@link Optional} value
     * @param <T>      the type of the value
     * @return an {@link Exceptional} of the specified {@link Optional} value
     */
    public static <T> Exceptional<T> ofOptional(final Optional<T> optional) {
        return optional == null
            ? empty()
            : optional.map(Exceptional::of)
            .orElse(empty());
    }

    /**
     * Creates an {@link Exceptional} for the specified {@link Future} value (once complete).
     *
     * @param future the {@link Future} value
     * @param <T>    the type of the value
     * @return an {@link Exceptional} of the specified {@link Optional} value
     */
    public static <T> Exceptional<T> ofFuture(final Future<T> future) {
        try {
            return ofNullable(future.get());
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            return cause instanceof Exception
                ? ofException((Exception) cause)
                : ofException(e);
        } catch (final Exception e) {
            return ofException(e);
        }
    }

    /**
     * Attempts to invoke the specified {@link Callable}, returning the result of the invocation.  Should
     * the {@link Callable} throw an exception, the {@link Exceptional} will be completed exceptionally with
     * the {@link Exception}.
     *
     * @param <T>      the type of value returned by the {@link Callable}
     * @param callable the {@link Callable}
     * @return an {@link Exceptional} containing the result or exception of the {@link Callable}
     */
    public static <T> Exceptional<T> call(final Callable<T> callable) {
        try {
            return ofNullable(callable.call());
        } catch (final Exception exception) {
            return ofException(exception);
        }
    }


    /**
     * Attempts to invoke the specified {@link Runnable}.  Should the {@link Runnable} throw an exception, the
     * {@link Exceptional} will be completed exceptionally with the {@link Exception}.
     *
     * @param <E>      the exception type throwable by the supplied runnable
     * @param runnable the {@link Runnable}
     * @return an {@link Exceptional} containing the exception if any thrown by the {@link Runnable}, or {@link Exceptional#empty()}
     */
    public static <E extends Exception> Exceptional<?> run(final CheckedRunnable<E> runnable) {
        try {
            runnable.run();
            return empty();
        } catch (final Exception exception) {
            return ofException(exception);
        }
    }

    /**
     * Attempts to invoke the specified {@link Consumer}.  Should the {@link Consumer} throw an exception, the
     * {@link Exceptional} will be completed exceptionally with the {@link Exception}.
     *
     * @param <T>      the type of value consumed by the {@link Consumer}
     * @param <E>      the exception type throwable by the supplied consumer
     * @param consumer the {@link Consumer}
     * @param value    the value to consume
     * @return an {@link Exceptional} containing the exception if any thrown by the {@link Consumer}, or {@link Exceptional#empty()}
     */
    public static <T, E extends Exception> Exceptional<?> consume(final CheckedConsumer<? super T, E> consumer,
                                                                  final T value) {
        try {
            consumer.accept(value);
            return empty();
        } catch (final Exception exception) {
            return ofException(exception);
        }
    }

    /**
     * Attempts to invoke the specified {@link Supplier}.  Should the {@link Supplier} throw an exception, the
     * {@link Exceptional} will be completed exceptionally with the {@link Exception}.
     *
     * @param <T>      the type of value returned by the {@link Supplier}
     * @param <E>      the exception type throwable by the supplier
     * @param supplier the {@link Supplier}
     * @return an {@link Exceptional} containing the result of exception if any thrown by the {@link Supplier}
     */
    public static <T, E extends Exception> Exceptional<T> supply(final CheckedSupplier<? extends T, E> supplier) {
        try {
            return ofNullable(supplier.get());
        } catch (final Exception exception) {
            return ofException(exception);
        }
    }

    /**
     * Attempts to invoke the specified {@link Function} and return its value.  Should the {@link Function} throw an
     * exception, the {@link Exceptional} will be completed exceptionally with the {@link Exception}.
     *
     * @param <T>      the type consumed by the function
     * @param <R>      the type of value returned by the function
     * @param <E>      the exception type throwable by the supplied function
     * @param function the {@link Function}
     * @param value    the value to consume
     * @return an {@link Exceptional} containing the exception if any thrown by the {@link Consumer}, or {@link Exceptional#empty()}
     */
    public static <T, R, E extends Exception> Exceptional<R> apply(final CheckedFunction<? super T, R, E> function,
                                                                   final T value) {
        try {
            return ofNullable(function.apply(value));
        } catch (final Exception exception) {
            return ofException(exception);
        }
    }

    /**
     * Return a {@link Callable} which returns an {@link Exceptional}.
     *
     * @param <T>      the result type
     * @param callable the {@link Callable}
     * @return the exceptional callable
     */
    public static <T> Callable<Exceptional<T>> callable(final Callable<T> callable) {
        return () -> call(callable);
    }

    /**
     * Return a {@link Runnable} which returns an {@link Exceptional}.
     *
     * @param runnable the {@link Runnable}
     * @return the exceptional runnable
     */
    public static Supplier<Exceptional<?>> runnable(final Runnable runnable) {
        return () -> run(runnable::run);
    }

    /**
     * Return a {@link Function} invokes the specified {@link Consumer} and returns an {@link Exceptional}.
     *
     * @param <T>      the consumed type
     * @param <E>      the exception type throwable by the supplied consumer
     * @param consumer the {@link Consumer}
     * @return the exceptional based consumer function
     */
    public static <T, E extends Exception> Function<T, Exceptional<?>> consumer(final CheckedConsumer<T, E> consumer) {
        return v -> consume(consumer, v);
    }

    /**
     * Return a {@link Function} invokes the specified {@link Consumer} and returns an {@link Exceptional}.
     *
     * @param <T>      the consumed type
     * @param <E>      the exception type throwable by the supplier
     * @param supplier the {@link Supplier}
     * @return the exceptional based consumer function
     */
    public static <T, E extends Exception> Supplier<Exceptional<T>> supplier(final CheckedSupplier<? extends T, E> supplier) {
        return () -> supply(supplier);
    }

    /**
     * Return a {@link Function} invokes the specified {@link Function} and returns an {@link Exceptional}.
     *
     * @param <T>      the consumed type
     * @param <R>      the result type
     * @param <E>      the exception type throwable by the supplied function
     * @param function the {@link Function}
     * @return the exceptional based consumer function
     */
    public static <T, R, E extends Exception> Function<T, Exceptional<R>> function(final CheckedFunction<T, R, E> function) {
        return v -> apply(function, v);
    }
}
