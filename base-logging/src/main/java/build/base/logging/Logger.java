package build.base.logging;

/*-
 * #%L
 * base.build Logging
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

import build.base.foundation.Strings;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A facility to log information to an underlying {@code java.util.logging} {@link java.util.logging.Logger}.
 *
 * @author brian.oliver
 * @since Nov-2024
 */
public interface Logger {

    /**
     * Obtains the underlying {@link java.util.logging.Logger} that is used for logging.
     *
     * @return the underlying {@link java.util.logging.Logger}
     */
    java.util.logging.Logger logger();

    /**
     * Add a log {@link Handler} to receive logging messages.
     *
     * @param handler a logging {@link Handler}
     * @see java.util.logging.Logger#addHandler(Handler)
     */
    default void addHandler(final Handler handler)
        throws SecurityException {

        logger().addHandler(handler);
    }

    /**
     * Set the {@link Level} specifying which message levels will be logged by this {@link Logger}.  Message
     * {@link Level}s lower than this value will be discarded.  The level value {@link Level#OFF} can be used to turn
     * off logging.
     * <p>
     * If the new {@link Level} is {@code null}, it means that this {@link Logger} should inherit its level from its
     * nearest ancestor with a specific (non-null) level value.
     *
     * @param newLevel the new value for the log level (may be {@code null})
     */
    default void setLevel(final Level newLevel)
        throws SecurityException {

        logger().setLevel(newLevel);
    }

    /**
     * Logs a message at the {@link Level#SEVERE} level pertaining an <strong>unrecoverable error</strong>, especially
     * one that requires immediate action and will lead to the termination of an application.
     *
     * @param message the message
     */
    default void fatal(final String message) {
        logger().log(Level.SEVERE, message);
    }

    /**
     * Logs a message at the {@link Level#SEVERE} level pertaining an <strong>unrecoverable error</strong>, especially
     * one that requires immediate action and will lead to the termination of an application.
     *
     * @param message    the message
     * @param parameters the parameters for the message
     */
    default void fatal(final String message, final Object... parameters) {
        logger().log(Level.SEVERE, message, parameters);
    }

    /**
     * Logs a message at the {@link Level#SEVERE} level pertaining to an <strong>unrecoverable error</strong>,
     * especially one that requires immediate action and will lead to the termination of an application.
     *
     * @param messageSupplier the message {@link Supplier}
     * @param parameters      the parameters for the message
     */
    default void fatal(final Supplier<String> messageSupplier, final Object... parameters) {
        if (messageSupplier != null && logger().isLoggable(Level.SEVERE)) {
            final var message = messageSupplier.get();
            if (!Strings.isEmpty(message)) {
                logger().log(Level.SEVERE, message, parameters);
            }
        }
    }

    /**
     * Logs a {@link Throwable} at the {@link Level#SEVERE} level pertaining to an <strong>unrecoverable error</strong>,
     * especially one that requires immediate action and will lead to the termination of an application.
     *
     * @param throwable  the {@link Throwable}
     * @param message    the message {@link Supplier}
     * @param parameters the parameters for the message
     */
    default void fatal(final Throwable throwable, final String message, final Object... parameters) {
        final var logger = logger();

        if (throwable != null && logger.isLoggable(Level.SEVERE)) {
            final var logRecord = new LogRecord(Level.SEVERE, message);
            logRecord.setThrown(throwable);
            logRecord.setParameters(parameters);
            logger.log(logRecord);
        }
    }

    /**
     * Logs a message at the {@link Level#WARNING} level pertaining a <strong>potentially recoverable error</strong>,
     * especially one that requires immediate action, but <i>will not lead</i> to the termination of an application.
     *
     * @param message the message
     */
    default void error(final String message) {
        logger().log(Level.WARNING, message);
    }

    /**
     * Logs a message at the {@link Level#WARNING} level pertaining a <strong>potentially recoverable error</strong>,
     * especially one that requires immediate action, but <i>will not lead</i> to the termination of an application.
     *
     * @param message    the message
     * @param parameters the parameters for the message
     */
    default void error(final String message, final Object... parameters) {
        logger().log(Level.WARNING, message, parameters);
    }

    /**
     * Logs a message at the {@link Level#WARNING} level pertaining a <strong>potentially recoverable error</strong>,
     * especially one that requires immediate action, but <i>will not lead</i> to the termination of an application.
     *
     * @param messageSupplier the message {@link Supplier}
     * @param parameters      the parameters for the message
     */
    default void error(final Supplier<String> messageSupplier, final Object... parameters) {
        if (messageSupplier != null && logger().isLoggable(Level.WARNING)) {
            final var message = messageSupplier.get();
            if (!Strings.isEmpty(message)) {
                logger().log(Level.WARNING, message, parameters);
            }
        }
    }

    /**
     * Logs a {@link Throwable} at the {@link Level#WARNING} level pertaining a <strong>potentially recoverable error</strong>,
     * especially one that requires immediate action, but <i>will not lead</i> to the termination of an application.
     *
     * @param throwable  the {@link Throwable}
     * @param message    the message {@link Supplier}
     * @param parameters the parameters for the message
     */
    default void error(final Throwable throwable, final String message, final Object... parameters) {
        final var logger = logger();

        if (logger.isLoggable(Level.WARNING)) {
            final var logRecord = new LogRecord(Level.WARNING, message);
            logRecord.setThrown(throwable);
            logRecord.setParameters(parameters);
            logger.log(logRecord);
        }
    }

    /**
     * Logs a message at the {@link Level#WARNING} level pertaining a <strong>problem</strong>,
     * especially one that is automatically recoverable or can be ignored.
     *
     * @param message the message
     */
    default void warn(final String message) {
        logger().log(Level.WARNING, message);
    }

    /**
     * Logs a message at the {@link Level#WARNING} level pertaining a <strong>problem</strong>,
     * especially one that is automatically recoverable or can be ignored.
     *
     * @param message    the message
     * @param parameters the parameters for the message
     */
    default void warn(final String message, final Object... parameters) {
        logger().log(Level.WARNING, message, parameters);
    }

    /**
     * Logs a message at the {@link Level#WARNING} level pertaining a <strong>problem</strong>,
     * especially one that is automatically recoverable or can be ignored.
     *
     * @param messageSupplier the message {@link Supplier}
     * @param parameters      the parameters for the message
     */
    default void warn(final Supplier<String> messageSupplier, final Object... parameters) {
        if (messageSupplier != null && logger().isLoggable(Level.WARNING)) {
            final var message = messageSupplier.get();
            if (!Strings.isEmpty(message)) {
                logger().log(Level.WARNING, message, parameters);
            }
        }
    }

    /**
     * Logs a {@link Throwable} at the {@link Level#WARNING} level pertaining a <strong>problem</strong>,
     * especially one that is automatically recoverable or can be ignored.
     *
     * @param throwable  the {@link Throwable}
     * @param message    the message {@link Supplier}
     * @param parameters the parameters for the message
     */
    default void warn(final Throwable throwable, final String message, final Object... parameters) {
        final var logger = logger();

        if (logger.isLoggable(Level.WARNING)) {
            final var logRecord = new LogRecord(Level.WARNING, message);
            logRecord.setThrown(throwable);
            logRecord.setParameters(parameters);
            logger.log(logRecord);
        }
    }

    /**
     * Logs a message at the {@link Level#INFO} level pertaining to end-user or application
     * <strong>functional use</strong>, but not problems or errors.
     *
     * @param message the message
     */
    default void info(final String message) {
        logger().log(Level.INFO, message);
    }

    /**
     * Logs a message at the {@link Level#INFO} level pertaining to end-user or application
     * <strong>functional use</strong>, but not problems or errors.
     *
     * @param message    the message
     * @param parameters the parameters for the message
     */
    default void info(final String message, final Object... parameters) {
        logger().log(Level.INFO, message, parameters);
    }

    /**
     * Logs a message at the {@link Level#INFO} level pertaining to end-user or application
     * <strong>functional use</strong>, but not problems or errors.
     *
     * @param messageSupplier the message {@link Supplier}
     * @param parameters      the parameters for the message
     */
    default void info(final Supplier<String> messageSupplier, final Object... parameters) {
        if (messageSupplier != null && logger().isLoggable(Level.INFO)) {
            final var message = messageSupplier.get();
            if (!Strings.isEmpty(message)) {
                logger().log(Level.INFO, message, parameters);
            }
        }
    }

    /**
     * Logs a {@link Throwable} at the {@link Level#INFO} level pertaining to end-user or application
     * <strong>functional use</strong>, but not problems or errors.
     *
     * @param throwable  the {@link Throwable}
     * @param message    the message {@link Supplier}
     * @param parameters the parameters for the message
     */
    default void info(final Throwable throwable, final String message, final Object... parameters) {
        final var logger = logger();

        if (logger.isLoggable(Level.INFO)) {
            final var logRecord = new LogRecord(Level.INFO, message);
            logRecord.setThrown(throwable);
            logRecord.setParameters(parameters);
            logger.log(logRecord);
        }
    }

    /**
     * Logs a message at the {@link Level#CONFIG} level pertaining to end-user or application
     * <strong>configuration</strong> and not functional use, problems or errors.
     *
     * @param message the message
     */
    default void config(final String message) {
        logger().log(Level.CONFIG, message);
    }

    /**
     * Logs a message at the {@link Level#CONFIG} level pertaining to end-user or application
     * <strong>configuration</strong> and not functional use, problems or errors.
     *
     * @param message    the message
     * @param parameters the parameters for the message
     */
    default void config(final String message, final Object... parameters) {
        logger().log(Level.CONFIG, message, parameters);
    }

    /**
     * Logs a message at the {@link Level#CONFIG} level pertaining to end-user or application
     * <strong>configuration</strong> and not functional use, problems or errors.
     *
     * @param messageSupplier the message {@link Supplier}
     * @param parameters      the parameters for the message
     */
    default void config(final Supplier<String> messageSupplier, final Object... parameters) {
        if (messageSupplier != null && logger().isLoggable(Level.CONFIG)) {
            final var message = messageSupplier.get();
            if (!Strings.isEmpty(message)) {
                logger().log(Level.CONFIG, message, parameters);
            }
        }
    }

    /**
     * Logs a {@link Throwable} at the {@link Level#CONFIG} level pertaining to end-user or application
     * <strong>configuration</strong> and not functional use, problems or errors.
     *
     * @param throwable  the {@link Throwable}
     * @param message    the message {@link Supplier}
     * @param parameters the parameters for the message
     */
    default void config(final Throwable throwable, final String message, final Object... parameters) {
        final var logger = logger();

        if (logger.isLoggable(Level.CONFIG)) {
            final var logRecord = new LogRecord(Level.CONFIG, message);
            logRecord.setThrown(throwable);
            logRecord.setParameters(parameters);
            logger.log(logRecord);
        }
    }

    /**
     * Logs a message at the {@link Level#FINE} level pertaining internal application state, events and
     * state transition information for developers.
     *
     * @param message the message
     */
    default void debug(final String message) {
        logger().log(Level.FINE, message);
    }

    /**
     * Logs a message at the {@link Level#FINE} level pertaining internal application state, events and
     * state transition information for developers.
     *
     * @param message    the message
     * @param parameters the parameters for the message
     */
    default void debug(final String message, final Object... parameters) {
        logger().log(Level.FINE, message, parameters);
    }

    /**
     * Logs a message at the {@link Level#FINE} level pertaining internal application state, events and
     * state transition information for developers.
     *
     * @param messageSupplier the message {@link Supplier}
     * @param parameters      the parameters for the message
     */
    default void debug(final Supplier<String> messageSupplier, final Object... parameters) {
        if (messageSupplier != null && logger().isLoggable(Level.FINE)) {
            final var message = messageSupplier.get();
            if (!Strings.isEmpty(message)) {
                logger().log(Level.FINE, message, parameters);
            }
        }
    }

    /**
     * Logs a {@link Throwable} at the {@link Level#FINE} level pertaining internal application state, events and
     * state transition information for developers.
     *
     * @param throwable  the {@link Throwable}
     * @param message    the message {@link Supplier}
     * @param parameters the parameters for the message
     */
    default void debug(final Throwable throwable, final String message, final Object... parameters) {
        final var logger = logger();
        if (logger.isLoggable(Level.FINE)) {
            final var logRecord = new LogRecord(Level.FINE, message);
            logRecord.setThrown(throwable);
            logRecord.setParameters(parameters);
            logger.log(logRecord);
        }
    }

    /**
     * Logs a message at the {@link Level#FINEST} level pertaining method and algorithm trace information for
     * application developers.
     *
     * @param message the message
     */
    default void trace(final String message) {
        logger().log(Level.FINEST, message);
    }

    /**
     * Logs a message at the {@link Level#FINEST} level pertaining method and algorithm trace information for
     * application developers.
     *
     * @param message    the message
     * @param parameters the parameters for the message
     */
    default void trace(final String message, final Object... parameters) {
        logger().log(Level.FINEST, message, parameters);
    }

    /**
     * Logs a message at the {@link Level#FINEST} level pertaining method and algorithm trace information for
     * application developers.
     *
     * @param messageSupplier the message {@link Supplier}
     * @param parameters      the parameters for the message
     */
    default void trace(final Supplier<String> messageSupplier, final Object... parameters) {
        if (messageSupplier != null && logger().isLoggable(Level.FINEST)) {
            final var message = messageSupplier.get();
            if (!Strings.isEmpty(message)) {
                logger().log(Level.FINEST, message, parameters);
            }
        }
    }

    /**
     * Logs a {@link Throwable} at the {@link Level#FINEST} level pertaining method and algorithm trace information for
     * application developers.
     *
     * @param throwable  the {@link Throwable}
     * @param message    the message {@link Supplier}
     * @param parameters the parameters for the message
     */
    default void trace(final Throwable throwable, final String message, final Object... parameters) {
        final var logger = logger();

        if (logger.isLoggable(Level.FINEST)) {
            final var logRecord = new LogRecord(Level.FINEST, message);
            logRecord.setThrown(throwable);
            logRecord.setParameters(parameters);
            logger.log(logRecord);
        }
    }

    /**
     * Obtains a {@link Logger} for the specified {@link Class} using an underlying {@code java.util.logging.Logger}.
     *
     * @param clazz the {@link Class} for which to obtain a {@link Logger}
     * @return a {@link Logger} for the specified {@link Class}
     */
    static Logger get(final Class<?> clazz) {
        Objects.requireNonNull(clazz, "The class must not be null");

        final var logger = java.util.logging.Logger.getLogger(clazz.getName());

        return () -> logger;
    }
}
