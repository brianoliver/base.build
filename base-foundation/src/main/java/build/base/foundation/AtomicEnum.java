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

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A non-{@code null} {@link Enum} value that may be operated on atomically.
 * <p>
 * Unlike the methods defined by {@link java.util.concurrent.atomic.AtomicReference}, specifically {@code compute}
 * methods, the {@link Consumer} and {@link Function} methods used with an {@link AtomicEnum} <i>may</i> safely cause
 * side effects, as they are guaranteed to be executed once and once only.
 *
 * @param <T> the type of the {@link Enum}
 * @author brian.oliver
 * @since Oct-2020
 */
public final class AtomicEnum<T extends Enum<T>> {

    /**
     * The current {@link Enum} value.
     */
    private volatile T value;

    /**
     * Constructs an {@link AtomicEnum}.
     *
     * @param value the initial value of the {@link AtomicEnum}
     */
    private AtomicEnum(final T value) {
        this.value = Objects.requireNonNull(value, "The enum value must not be null");
    }

    /**
     * Determines if the {@link AtomicEnum} is equal to the specified value.
     *
     * @param expected the expected value
     * @return {@code true} if the {@link AtomicEnum} is equal to the specified value, {@code false} otherwise
     */
    public boolean is(final T expected) {
        return this.value == expected;
    }

    /**
     * Obtains the current value of the {@link AtomicEnum}.
     *
     * @return the current value
     */
    public T get() {
        return this.value;
    }

    /**
     * Sets the current value of the {@link AtomicEnum}.
     *
     * @param value the value
     * @return the previous value of the {@link AtomicEnum}
     */
    public T set(final T value) {
        synchronized (this) {
            Objects.requireNonNull(value, "The enum value must not be null");

            final T current = this.value;
            this.value = value;
            return current;
        }
    }

    /**
     * Atomically performs compare-and-set operation on the {@link AtomicEnum}.
     * <p>
     * Should the current value of the {@link AtomicEnum} be equal to the specified expected value, the
     * {@link AtomicEnum} is atomically updated to the specified update value and {@code true} is returned.  Should the
     * current value not be equal to expected value, no update is performed and {@code false} is returned.
     *
     * @param expected the expected value
     * @param update   the update value
     * @return {@code true} if an update was performed, {@code false} otherwise
     */
    public boolean compareAndSet(final T expected, final T update) {
        synchronized (this) {
            final T current = this.value;
            if (current == expected) {
                set(update);
                return true;
            }
        }
        return false;
    }

    /**
     * Atomically computes a new value for the {@link AtomicEnum} using current value and the specified
     * {@link Function}.
     *
     * @param function the {@link Function}
     * @return the previous value
     */
    public T compute(final Function<? super T, T> function) {
        if (function == null) {
            return this.value;
        }

        synchronized (this) {
            final T current = this.value;
            set(function.apply(current));
            return current;
        }
    }

    /**
     * Atomically consumes the current value of the {@link AtomicEnum} if it satisfies the specified {@link Predicate}.
     *
     * @param predicate the {@link Predicate}
     * @param consumer  the {@link Consumer}
     */
    public void consumeIf(final Predicate<? super T> predicate,
                          final Consumer<? super T> consumer) {

        if (predicate != null && consumer != null) {
            synchronized (this) {
                final T current = this.value;
                if (predicate.test(current)) {
                    consumer.accept(current);
                }
            }
        }
    }

    /**
     * Atomically consumes the current value of the {@link AtomicEnum} if it equals the specified value.
     *
     * @param value    the value
     * @param consumer the {@link Consumer}
     */
    public void consumeIf(final T value,
                          final Consumer<? super T> consumer) {

        if (consumer != null) {
            synchronized (this) {
                final T current = this.value;
                if (current == value) {
                    consumer.accept(current);
                }
            }
        }
    }

    /**
     * Atomically consumes the current value of the {@link AtomicEnum} if it equals the specified value with a
     * {@link Consumer}.  Should the value not be equal the "otherwise" {@link Consumer} is used to consume the
     * non-equal value.
     *
     * @param value     the value
     * @param consumer  the {@link Consumer}
     * @param otherwise the otherwise {@link Consumer}
     */
    public void consumeIf(final T value,
                          final Consumer<? super T> consumer,
                          final Consumer<? super T> otherwise) {

        if (consumer != null) {
            synchronized (this) {
                final T current = this.value;
                if (current == value) {
                    consumer.accept(current);
                }
                else if (otherwise != null) {
                    otherwise.accept(current);
                }
            }
        }
    }

    /**
     * Atomically consumes the current value for the {@link AtomicEnum} using the specified {@link Consumer}.
     *
     * @param consumer the {@link Consumer}
     */
    public void consume(final Consumer<? super T> consumer) {
        if (consumer != null) {
            consumer.accept(this.value);
        }
    }

    /**
     * Atomically maps the current value for the {@link AtomicEnum} using the specified {@link Function}.
     *
     * @param <R>      the type of the mapping {@link Function} result
     * @param function the {@link Function}
     * @return the mapped value
     */
    public <R> R map(final Function<? super T, R> function) {
        Objects.requireNonNull(function, "The function must not be null");
        return function.apply(this.value);
    }

    /**
     * Atomically applies the currently {@link Capture}d value for the {@link AtomicEnum} using the specified
     * {@link Function}, allowing both simultaneous mapping of the said value to another value and update of the
     * {@link AtomicEnum} value.
     * <p>
     * The input parameter to the {@link Function} is a {@link Capture} representing the current value of
     * {@link AtomicEnum}.  Should the value of the {@link Capture} be updated, the {@link AtomicEnum} will
     * correspondingly be updated to the said value atomically as part of executing the {@link Function}.  Should the
     * {@link Capture} be cleared, or left unchanged, the {@link AtomicEnum} value will remain unchanged.
     *
     * @param <R>      the type of the mapping {@link Function} result
     * @param function the {@link Function}
     * @return the mapped value
     */
    public <R> R apply(final Function<? super Capture<T>, R> function) {
        Objects.requireNonNull(function, "The function must not be null");

        synchronized (this) {
            final Capture<T> capture = Capture.of(this.value);
            final R result = function.apply(capture);
            capture.ifPresent(v -> this.value = v);
            return result;
        }
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

    /**
     * Creates an {@link AtomicEnum} with the specified non-{@code null} value.
     *
     * @param value the initial non-{@code null} value of the {@link AtomicEnum}
     */
    public static <T extends Enum<T>> AtomicEnum<T> of(final T value) {
        return new AtomicEnum<T>(value);
    }
}
