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
import java.util.function.Function;

/**
 * Helper methods for working with exceptions.
 *
 * @author mark.falco
 * @since Nov-2018
 */
public class Exceptions {

    /**
     * Private constructor to prevent instantiation.
     */
    private Exceptions() {
    }

    /**
     * Consume (i.e. ignore) the supplied exception if it is a checked exception, rethrow if unchecked.
     * <p>
     * If the exception represents an interrupt the calling thread will be interrupted but no exception will be thrown.
     *
     * @param exception the exception to consume
     */
    public static void consumeChecked(final Throwable exception) {
        if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        }
        else if (exception instanceof Error) {
            throw (Error) exception;
        }

        // consume the checked exception
        reassertOnInterrupted(exception);
    }

    /**
     * Rethrow the specified exception as an unchecked exception, wrapping with a {@link RuntimeException} as necessary.
     * <p>
     * Note, if this specified exception represents an interrupt the calling thread will be re-interrupted prior to
     * rethrowing the wrapper. This allows the caller to both not need to bother with re-interrupting and also ensures
     * that the re-interrupt occurs even if the caller is unaware that the checked exception they're handling represented
     * an interrupt.
     * <p>
     * This method declares that it returns a {@link RuntimeException} but since it always throws it will never actually
     * return anything as it always throws. In declaring that it returns a {@link RuntimeException} it allows the caller
     * to indicate to the compiler that the calling function will not return either simply by prefixing the call to this
     * method with {@code throws}.
     * <pre>{@code
     * int someValueReturningFunction() {
     *     try {
     *         return getValueFromSomewhereThatCanThrow();
     *     }
     *     catch (Exception ex) {
     *         throw Exceptions.rethrowUnchecked(ex); // would fail to compile with the "throws"
     *     }
     * }
     * }</pre>
     *
     * @param throwable the {@link Throwable} to rethrow
     * @return never
     */
    public static RuntimeException rethrowUnchecked(final Throwable throwable) {
        return rethrowUnchecked(throwable, RuntimeException::new);
    }

    /**
     * A variant of {@link #rethrowUnchecked(Throwable)} which allows the caller to specify the type of wrapper to
     * rethrow.
     *
     * @param throwable       the {@link Throwable} to rethrow
     * @param exceptionalizer a {@link Function} producing a {@link RuntimeException} derivative for producing
     *                        wrapper exceptions
     * @return never
     */
    public static RuntimeException rethrowUnchecked(final Throwable throwable,
                                                    final Function<? super Throwable, ? extends RuntimeException> exceptionalizer) {

        if (reassertOnInterrupted(throwable)) {
            Thread.currentThread().interrupt();
            throw exceptionalizer.apply(throwable);
        }
        else if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        }
        else if (throwable instanceof Error) {
            throw (Error) throwable;
        }
        else {
            throw exceptionalizer.apply(throwable);
        }
    }

    /**
     * Helper method which always rethrows the exception (potentially after wrapping it), but never returns a result.
     *
     * @param throwable the {@link Throwable} to rethrow as unchecked
     * @param <T>       the result type
     * @return no value, always throws
     */
    /*package*/
    static <T> T rethrowVoidUnchecked(final Throwable throwable) {
        throw rethrowUnchecked(throwable);
    }

    /**
     * Rethrow an {@link Exception}'s {@link Throwable#getCause() causual exception} if it is of the specified
     * type otherwise return the supplied {@link Exception}.
     * <p>
     * If the rethrown exception is an {@link InterruptedException} or {@link InterruptedIOException} the calling
     * thread's interrupt status will automatically be cleared.
     *
     * @param exception    the {@link RuntimeException}
     * @param clzException the {@link Class} of the type to unwrap to if possible
     * @param <E>          the supplied and returned exception type
     * @param <W>          the type to unwrap to if possible
     * @return the original {@link Exception} if it did not wrap an exception for the specified type
     * @throws W if the supplied exception wraps the specified type
     */
    @SuppressWarnings("unchecked")
    public static <E extends Exception, W extends Exception> E rethrowUnwrapped(final E exception,
                                                                                final Class<? extends W> clzException)
        throws W {

        final Throwable e = exception.getCause();
        if (clzException.isInstance(e)) {
            clearOnInterrupted(e);

            throw (W) e;
        }

        return exception;
    }

    /**
     * Internal helper method for clearning the calling thread's interrupt if it is going to throw an exception which
     * represents an interrupt.
     *
     * @param throwable the exception to check
     */
    private static void clearOnInterrupted(final Throwable throwable) {

        if (throwable instanceof InterruptedException || throwable instanceof InterruptedIOException) {
            // we're going to rethrow an interrupted exception and can clear the interrupt flag
            Thread.interrupted();
        }
    }

    /**
     * Return {@code true} iff the supplied exception represents an interrupt, and if so {@link Thread#interrupt()}
     * the calling thread.
     *
     * @param throwable the exception to check
     * @return {@code true} iff the supplied exception represents an interrupt
     */
    public static boolean reassertOnInterrupted(final Throwable throwable) {

        if (throwable instanceof InterruptedException || throwable instanceof InterruptedIOException) {
            // restore the interrupt as we're not going to rethrow this checked exception
            Thread.currentThread().interrupt();
            return true;
        }

        return false;
    }
}
