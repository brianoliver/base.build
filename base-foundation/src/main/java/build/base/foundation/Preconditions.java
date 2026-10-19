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
import java.util.function.Predicate;

/**
 * {@link Preconditions} defines methods to help a constructor or method validate that the arguments passed to it are
 * allowable.
 * <p>
 * If an argument is invalid, each {@link Preconditions} method will throw an unchecked exception of a specified type,
 * which helps the method in which the exception was thrown communicate that its caller has made a mistake. This allows
 * assertions such as:
 * <pre>
 * {@code
 * public static int myMethod(final int value) {
 *     if (value &lt; 1) {
 *         throw new IllegalArgumentException("The value argument must be positive);
 *     }
 *
 *     // do something with the value argument
 * }
 * }
 * </pre>
 * to be replaced by:
 * <pre>
 * {@code
 * public static int myMethod(final int value) {
 *     verify(value, value &gt; 0, "The value argument must be positive");
 *
 *     // do something with the value argument
 * }
 * }
 * </pre>
 * Each {@link Preconditions} method is fluid in that it returns the supplied argument if it passes the precondition.
 * This is quite useful in constructors. For example:
 * <pre>
 * {@code
 * public class MyClass {
 *     private final int value;
 *
 *     public MyClass(final int value) {
 *         this.value = verify(value, value &gt; 0, "The value argument must be positive");
 *     }
 * }
 * }
 * </pre>
 * In addition to passing a static error message, there are also variants of each {@link Preconditions} method that take
 * a string template and one or more template arguments. The error message is formatted using the
 * {@link String#format(String, Object...)} method. For example:
 * <pre>
 * public static int myMethod(final int value) {
 *     verify(value, value &gt; 0, "The value argument must be positive. The value passed was %1$d.", value);
 *
 *     // do something with the value argument
 * }
 * </pre>
 * If either of the error message or error message template is {@code null} or empty, a default error message will be
 * used to construct the unchecked exception.
 * <p>
 * In addition to testing the result of a boolean expression, there are also variants of the {@link Preconditions}
 * methods that accept a predicate to be supplied that will be evaluated to determine whether an unchecked exception
 * is thrown. For example:
 * <pre>
 * {@code
 * public static int myMethod(final String value) {
 *     verify(value, s -> Objects.equals(s, "Hello world!"));
 *
 *     // do something with the value argument
 * }
 * }
 * </pre>
 * <p>
 * Finally, there are also variants of each {@link Preconditions} method that take primitive error message template
 * arguments. These exist to avoid autoboxing in performance-critical constructors or methods.
 *
 * @author jason.howes
 * @since Feb-2019
 */
public final class Preconditions {

    /**
     * The error message used if one cannot be constructed.
     */
    private static final String ERROR_MESSAGE_DEFAULT = "Verification error";

    /**
     * The error message to use if a supplied {@link Predicate} is {@code null}.
     */
    private static final String ERROR_MESSAGE_NULL_PREDICATE = "The Predicate cannot be null";

    /**
     * Private constructor to prevent instantiation.
     */
    private Preconditions() {
        // empty constructor
    }

    /**
     * Validate that the given precondition is {@code true}, and if so, return the supplied argument.
     *
     * @param argument the argument to return if the precondition is {@code true}
     * @param check    the precondition to check
     * @param <T>      the argument type
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */

    public static <T> T require(final T argument, final boolean check) {
        if (check) {
            return argument;
        }
        throw new IllegalArgumentException(ERROR_MESSAGE_DEFAULT);
    }

    /**
     * Validate that the given precondition is {@code true}, and if so, return the supplied argument.
     *
     * @param argument     the argument to return if the precondition is {@code true}
     * @param check        the precondition to check
     * @param errorMessage the message used to construct the unchecked exception if the precondition is {@code false}
     * @param <T>          the argument type
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */

    public static <T> T require(final T argument, final boolean check, final String errorMessage) {
        if (check) {
            return argument;
        }
        throw new IllegalArgumentException(formatErrorMessage(errorMessage));
    }

    /**
     * Validate that the given precondition is {@code true}, and if so, return the supplied argument.
     *
     * @param argument             the argument to return if the precondition is {@code true}
     * @param check                the precondition to check
     * @param errorMessageTemplate the message template used to construct the unchecked exception message if the
     *                             precondition is {@code false}
     * @param errorMessageArgs     arguments passed to {@link String#format(String, Object...)} along with the error message
     *                             template; the resulting error message will be used to construct the unchecked exception
     *                             message if the precondition is {@code false}
     * @param <T>                  the argument type
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */

    public static <T> T require(final T argument,
                                final boolean check,
                                final String errorMessageTemplate,
                                final Object... errorMessageArgs) {
        if (check) {
            return argument;
        }
        throw new IllegalArgumentException(formatErrorMessage(errorMessageTemplate, errorMessageArgs));
    }

    /**
     * Validate that the given precondition is {@code true}, and if so, return the supplied argument.
     *
     * @param argument             the argument to return if the precondition is {@code true}
     * @param check                the precondition to check
     * @param errorMessageTemplate the message template used to construct the unchecked exception message if the
     *                             precondition is {@code false}
     * @param errorMessageArg      argument passed to {@link String#format(String, Object...)} along with the error message
     *                             template; the resulting error message will be used to construct the unchecked exception
     *                             message if the precondition is {@code false}
     * @param <T>                  the argument type
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */

    public static <T> T require(final T argument,
                                final boolean check,
                                final String errorMessageTemplate,
                                final boolean errorMessageArg) {
        if (check) {
            return argument;
        }
        throw new IllegalArgumentException(formatErrorMessage(errorMessageTemplate, errorMessageArg));
    }

    /**
     * Validate that the given precondition is {@code true}, and if so, return the supplied argument.
     *
     * @param argument             the argument to return if the precondition is {@code true}
     * @param check                the precondition to check
     * @param errorMessageTemplate the message template used to construct the unchecked exception message if the
     *                             precondition is {@code false}
     * @param errorMessageArg      argument passed to {@link String#format(String, Object...)} along with the error message
     *                             template; the resulting error message will be used to construct the unchecked exception
     *                             message if the precondition is {@code false}
     * @param <T>                  the argument type
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */

    public static <T> T require(final T argument,
                                final boolean check,
                                final String errorMessageTemplate,
                                final char errorMessageArg) {
        if (check) {
            return argument;
        }
        throw new IllegalArgumentException(formatErrorMessage(errorMessageTemplate, errorMessageArg));
    }

    /**
     * Validate that the given precondition is {@code true}, and if so, return the supplied argument.
     *
     * @param argument             the argument to return if the precondition is {@code true}
     * @param check                the precondition to check
     * @param errorMessageTemplate the message template used to construct the unchecked exception message if the
     *                             precondition is {@code false}
     * @param errorMessageArg      argument passed to {@link String#format(String, Object...)} along with the error message
     *                             template; the resulting error message will be used to construct the unchecked exception
     *                             message if the precondition is {@code false}
     * @param <T>                  the argument type
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */

    public static <T> T require(final T argument,
                                final boolean check,
                                final String errorMessageTemplate,
                                final short errorMessageArg) {
        if (check) {
            return argument;
        }
        throw new IllegalArgumentException(formatErrorMessage(errorMessageTemplate, errorMessageArg));
    }

    /**
     * Validate that the given precondition is {@code true}, and if so, return the supplied argument.
     *
     * @param argument             the argument to return if the precondition is {@code true}
     * @param check                the precondition to check
     * @param errorMessageTemplate the message template used to construct the unchecked exception message if the
     *                             precondition is {@code false}
     * @param errorMessageArg      argument passed to {@link String#format(String, Object...)} along with the error message
     *                             template; the resulting error message will be used to construct the unchecked exception
     *                             message if the precondition is {@code false}
     * @param <T>                  the argument type
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */

    public static <T> T require(final T argument,
                                final boolean check,
                                final String errorMessageTemplate,
                                final int errorMessageArg) {
        if (check) {
            return argument;
        }
        throw new IllegalArgumentException(formatErrorMessage(errorMessageTemplate, errorMessageArg));
    }

    /**
     * Validate that the given precondition is {@code true}, and if so, return the supplied argument.
     *
     * @param argument             the argument to return if the precondition is {@code true}
     * @param check                the precondition to check
     * @param errorMessageTemplate the message template used to construct the unchecked exception message if the
     *                             precondition is {@code false}
     * @param errorMessageArg      argument passed to {@link String#format(String, Object...)} along with the error message
     *                             template; the resulting error message will be used to construct the unchecked exception
     *                             message if the precondition is {@code false}
     * @param <T>                  the argument type
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */

    public static <T> T require(final T argument,
                                final boolean check,
                                final String errorMessageTemplate,
                                final long errorMessageArg) {
        if (check) {
            return argument;
        }
        throw new IllegalArgumentException(formatErrorMessage(errorMessageTemplate, errorMessageArg));
    }

    /**
     * Validate that the given precondition is {@code true}, and if so, return the supplied argument.
     *
     * @param argument             the argument to return if the precondition is {@code true}
     * @param check                the precondition to check
     * @param errorMessageTemplate the message template used to construct the unchecked exception message if the
     *                             precondition is {@code false}
     * @param errorMessageArg      argument passed to {@link String#format(String, Object...)} along with the error message
     *                             template; the resulting error message will be used to construct the unchecked exception
     *                             message if the precondition is {@code false}
     * @param <T>                  the argument type
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */

    public static <T> T require(final T argument,
                                final boolean check,
                                final String errorMessageTemplate,
                                final float errorMessageArg) {
        if (check) {
            return argument;
        }
        throw new IllegalArgumentException(formatErrorMessage(errorMessageTemplate, errorMessageArg));
    }

    /**
     * Validate that the given precondition is {@code true}, and if so, return the supplied argument.
     *
     * @param argument             the argument to return if the precondition is {@code true}
     * @param check                the precondition to check
     * @param errorMessageTemplate the message template used to construct the unchecked exception message if the
     *                             precondition is {@code false}
     * @param errorMessageArg      argument passed to {@link String#format(String, Object...)} along with the error message
     *                             template; the resulting error message will be used to construct the unchecked exception
     *                             message if the precondition is {@code false}
     * @param <T>                  the argument type
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */

    public static <T> T require(final T argument,
                                final boolean check,
                                final String errorMessageTemplate,
                                final double errorMessageArg) {

        if (check) {
            return argument;
        }
        throw new IllegalArgumentException(formatErrorMessage(errorMessageTemplate, errorMessageArg));
    }

    /**
     * Validate that the given precondition evaluates to {@code true}, and if so, return the supplied argument.
     *
     * @param argument  the argument to return if the precondition is {@code true}
     * @param predicate the precondition to check
     * @param <T>       the argument type
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */

    public static <T> T require(final T argument,
                                final Predicate<? super T> predicate) {

        Objects.requireNonNull(predicate, ERROR_MESSAGE_NULL_PREDICATE);

        if (predicate.test(argument)) {
            return argument;
        }
        throw new IllegalArgumentException(ERROR_MESSAGE_DEFAULT);
    }

    /**
     * Validate that the given precondition evaluates to {@code true}, and if so, return the supplied argument.
     *
     * @param argument     the argument to return if the precondition is {@code true}
     * @param predicate    the precondition to check
     * @param errorMessage the message used to construct the unchecked exception if the precondition is {@code false}
     * @param <T>          the argument type
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */

    public static <T> T require(final T argument,
                                final Predicate<? super T> predicate,
                                final String errorMessage) {

        Objects.requireNonNull(predicate, ERROR_MESSAGE_NULL_PREDICATE);

        if (predicate.test(argument)) {
            return argument;
        }
        throw new IllegalArgumentException(formatErrorMessage(errorMessage));
    }

    /**
     * Validate that the given precondition evaluates to {@code true}, and if so, return the supplied argument.
     *
     * @param argument             the argument to return if the precondition is {@code true}
     * @param predicate            the precondition to check
     * @param errorMessageTemplate the message template used to construct the unchecked exception message if the
     *                             precondition is {@code false}
     * @param errorMessageArgs     arguments passed to {@link String#format(String, Object...)} along with the error message
     *                             template; the resulting error message will be used to construct the unchecked exception
     *                             message if the precondition is {@code false}
     * @param <T>                  the argument type
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */

    public static <T> T require(final T argument,
                                final Predicate<? super T> predicate,
                                final String errorMessageTemplate,
                                final Object... errorMessageArgs) {

        Objects.requireNonNull(predicate, ERROR_MESSAGE_NULL_PREDICATE);

        if (predicate.test(argument)) {
            return argument;
        }
        throw new IllegalArgumentException(formatErrorMessage(errorMessageTemplate, errorMessageArgs));
    }

    /**
     * Validate that the given precondition evaluates to {@code true}, and if so, return the supplied argument.
     *
     * @param argument             the argument to return if the precondition is {@code true}
     * @param predicate            the precondition to check
     * @param errorMessageTemplate the message template used to construct the unchecked exception message if the
     *                             precondition is {@code false}
     * @param errorMessageArg      argument passed to {@link String#format(String, Object...)} along with the error message
     *                             template; the resulting error message will be used to construct the unchecked exception
     *                             message if the precondition is {@code false}
     * @param <T>                  the argument type
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */

    public static <T> T require(final T argument,
                                final Predicate<? super T> predicate,
                                final String errorMessageTemplate,
                                final boolean errorMessageArg) {

        Objects.requireNonNull(predicate, ERROR_MESSAGE_NULL_PREDICATE);

        if (predicate.test(argument)) {
            return argument;
        }
        throw new IllegalArgumentException(formatErrorMessage(errorMessageTemplate, errorMessageArg));
    }

    /**
     * Validate that the given precondition evaluates to {@code true}, and if so, return the supplied argument.
     *
     * @param argument             the argument to return if the precondition is {@code true}
     * @param predicate            the precondition to check
     * @param errorMessageTemplate the message template used to construct the unchecked exception message if the
     *                             precondition is {@code false}
     * @param errorMessageArg      argument passed to {@link String#format(String, Object...)} along with the error message
     *                             template; the resulting error message will be used to construct the unchecked exception
     *                             message if the precondition is {@code false}
     * @param <T>                  the argument type
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */

    public static <T> T require(final T argument,
                                final Predicate<? super T> predicate,
                                final String errorMessageTemplate,
                                final char errorMessageArg) {

        Objects.requireNonNull(predicate, ERROR_MESSAGE_NULL_PREDICATE);

        if (predicate.test(argument)) {
            return argument;
        }
        throw new IllegalArgumentException(formatErrorMessage(errorMessageTemplate, errorMessageArg));
    }

    /**
     * Validate that the given precondition evaluates to {@code true}, and if so, return the supplied argument.
     *
     * @param argument             the argument to return if the precondition is {@code true}
     * @param predicate            the precondition to check
     * @param errorMessageTemplate the message template used to construct the unchecked exception message if the
     *                             precondition is {@code false}
     * @param errorMessageArg      argument passed to {@link String#format(String, Object...)} along with the error message
     *                             template; the resulting error message will be used to construct the unchecked exception
     *                             message if the precondition is {@code false}
     * @param <T>                  the argument type
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */

    public static <T> T require(final T argument,
                                final Predicate<? super T> predicate,
                                final String errorMessageTemplate,
                                final short errorMessageArg) {

        Objects.requireNonNull(predicate, ERROR_MESSAGE_NULL_PREDICATE);

        if (predicate.test(argument)) {
            return argument;
        }
        throw new IllegalArgumentException(formatErrorMessage(errorMessageTemplate, errorMessageArg));
    }

    /**
     * Validate that the given precondition evaluates to {@code true}, and if so, return the supplied argument.
     *
     * @param argument             the argument to return if the precondition is {@code true}
     * @param predicate            the precondition to check
     * @param errorMessageTemplate the message template used to construct the unchecked exception message if the
     *                             precondition is {@code false}
     * @param errorMessageArg      argument passed to {@link String#format(String, Object...)} along with the error message
     *                             template; the resulting error message will be used to construct the unchecked exception
     *                             message if the precondition is {@code false}
     * @param <T>                  the argument type
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */

    public static <T> T require(final T argument,
                                final Predicate<? super T> predicate,
                                final String errorMessageTemplate,
                                final int errorMessageArg) {

        Objects.requireNonNull(predicate, ERROR_MESSAGE_NULL_PREDICATE);

        if (predicate.test(argument)) {
            return argument;
        }
        throw new IllegalArgumentException(formatErrorMessage(errorMessageTemplate, errorMessageArg));
    }

    /**
     * Validate that the given precondition evaluates to {@code true}, and if so, return the supplied argument.
     *
     * @param argument             the argument to return if the precondition is {@code true}
     * @param predicate            the precondition to check
     * @param errorMessageTemplate the message template used to construct the unchecked exception message if the
     *                             precondition is {@code false}
     * @param errorMessageArg      argument passed to {@link String#format(String, Object...)} along with the error message
     *                             template; the resulting error message will be used to construct the unchecked exception
     *                             message if the precondition is {@code false}
     * @param <T>                  the argument type
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */

    public static <T> T require(final T argument,
                                final Predicate<? super T> predicate,
                                final String errorMessageTemplate,
                                final long errorMessageArg) {

        Objects.requireNonNull(predicate, ERROR_MESSAGE_NULL_PREDICATE);

        if (predicate.test(argument)) {
            return argument;
        }
        throw new IllegalArgumentException(formatErrorMessage(errorMessageTemplate, errorMessageArg));
    }

    /**
     * Validate that the given precondition evaluates to {@code true}, and if so, return the supplied argument.
     *
     * @param argument             the argument to return if the precondition is {@code true}
     * @param predicate            the precondition to check
     * @param errorMessageTemplate the message template used to construct the unchecked exception message if the
     *                             precondition is {@code false}
     * @param errorMessageArg      argument passed to {@link String#format(String, Object...)} along with the error message
     *                             template; the resulting error message will be used to construct the unchecked exception
     *                             message if the precondition is {@code false}
     * @param <T>                  the argument type
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */

    public static <T> T require(final T argument,
                                final Predicate<? super T> predicate,
                                final String errorMessageTemplate,
                                final float errorMessageArg) {

        Objects.requireNonNull(predicate, ERROR_MESSAGE_NULL_PREDICATE);

        if (predicate.test(argument)) {
            return argument;
        }
        throw new IllegalArgumentException(formatErrorMessage(errorMessageTemplate, errorMessageArg));
    }

    /**
     * Validate that the given precondition evaluates to {@code true}, and if so, return the supplied argument.
     *
     * @param argument             the argument to return if the precondition is {@code true}
     * @param predicate            the precondition to check
     * @param errorMessageTemplate the message template used to construct the unchecked exception message if the
     *                             precondition is {@code false}
     * @param errorMessageArg      argument passed to {@link String#format(String, Object...)} along with the error message
     *                             template; the resulting error message will be used to construct the unchecked exception
     *                             message if the precondition is {@code false}
     * @param <T>                  the argument type
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */

    public static <T> T require(final T argument,
                                final Predicate<? super T> predicate,
                                final String errorMessageTemplate,
                                final double errorMessageArg) {

        Objects.requireNonNull(predicate, ERROR_MESSAGE_NULL_PREDICATE);

        if (predicate.test(argument)) {
            return argument;
        }
        throw new IllegalArgumentException(formatErrorMessage(errorMessageTemplate, errorMessageArg));
    }

    /**
     * Validate that the given precondition is {@code true}, and if so, return the supplied byte argument, this is to
     * avoid the autoboxing when using primitives.
     *
     * @param argument the argument to return if the precondition is {@code true}
     * @param check    the precondition to check
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */
    public static byte require(final byte argument, final boolean check) {
        if (check) {
            return argument;
        }
        throw new IllegalArgumentException(ERROR_MESSAGE_DEFAULT);
    }

    /**
     * Validate that the given precondition is {@code true}, and if so, return the supplied short argument, this is to
     * avoid the autoboxing when using primitives.
     *
     * @param argument the argument to return if the precondition is {@code true}
     * @param check    the precondition to check
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */
    public static short require(final short argument, final boolean check) {
        if (check) {
            return argument;
        }
        throw new IllegalArgumentException(ERROR_MESSAGE_DEFAULT);
    }

    /**
     * Validate that the given precondition is {@code true}, and if so, return the supplied int argument, this is to
     * avoid the autoboxing when using primitives.
     *
     * @param argument the argument to return if the precondition is {@code true}
     * @param check    the precondition to check
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */
    public static int require(final int argument, final boolean check) {
        if (check) {
            return argument;
        }
        throw new IllegalArgumentException(ERROR_MESSAGE_DEFAULT);
    }

    /**
     * Validate that the given precondition is {@code true}, and if so, return the supplied float argument, this is to
     * avoid the autoboxing when using primitives.
     *
     * @param argument the argument to return if the precondition is {@code true}
     * @param check    the precondition to check
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */
    public static float require(final float argument, final boolean check) {
        if (check) {
            return argument;
        }
        throw new IllegalArgumentException(ERROR_MESSAGE_DEFAULT);
    }

    /**
     * Validate that the given precondition is {@code true}, and if so, return the supplied long argument, this is to
     * avoid the autoboxing when using primitives.
     *
     * @param argument the argument to return if the precondition is {@code true}
     * @param check    the precondition to check
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */
    public static long require(final long argument, final boolean check) {
        if (check) {
            return argument;
        }
        throw new IllegalArgumentException(ERROR_MESSAGE_DEFAULT);
    }

    /**
     * Validate that the given precondition is {@code true}, and if so, return the supplied double argument, this is to
     * avoid the autoboxing when using primitives.
     *
     * @param argument the argument to return if the precondition is {@code true}
     * @param check    the precondition to check
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */
    public static double require(final double argument, final boolean check) {
        if (check) {
            return argument;
        }
        throw new IllegalArgumentException(ERROR_MESSAGE_DEFAULT);
    }

    /**
     * Validate that the given precondition is {@code true}, and if so, return the supplied boolean argument, this is to
     * avoid the autoboxing when using primitives.
     *
     * @param argument the argument to return if the precondition is {@code true}
     * @param check    the precondition to check
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */
    public static boolean require(final boolean argument, final boolean check) {
        if (check) {
            return argument;
        }
        throw new IllegalArgumentException(ERROR_MESSAGE_DEFAULT);
    }

    /**
     * Validate that the given precondition is {@code true}, and if so, return the supplied char argument, this is to
     * avoid the autoboxing when using primitives.
     *
     * @param argument the argument to return if the precondition is {@code true}
     * @param check    the precondition to check
     * @return the argument if the precondition is {@code true}
     * @throws IllegalArgumentException if the precondition is {@code false}
     */
    public static char require(final char argument, final boolean check) {
        if (check) {
            return argument;
        }
        throw new IllegalArgumentException(ERROR_MESSAGE_DEFAULT);
    }

    /**
     * Return a message that is created by formatting the supplied template with the given arguments using the
     * {@link String#format(String, Object...)} method. If the supplied template is {@code null} or empty, a default
     * message will be returned.
     *
     * @param errorMessageTemplate the message template
     * @param errorMessageArgs     arguments passed to {@link String#format(String, Object...)} along with the error message
     *                             template to construct the messageg
     * @return the formatted message
     */
    private static String formatErrorMessage(final String errorMessageTemplate, final Object... errorMessageArgs) {
        if (Strings.isEmpty(errorMessageTemplate)) {
            return ERROR_MESSAGE_DEFAULT;
        }
        try {
            return String.format(errorMessageTemplate, errorMessageArgs);
        }
        catch (final RuntimeException e) {
            return ERROR_MESSAGE_DEFAULT;
        }
    }
}
