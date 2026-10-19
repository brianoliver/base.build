package build.base.retryable;

/*-
 * #%L
 * base.build Retryable
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

/**
 * An {@link Exception}s thrown by {@link Retryable#get()} implementations, indicating the type of failure and
 * thus allowing retry or fail-fast of {@link Retryable#get()} invocations.
 *
 * @author brian.oliver
 * @since Dec-2017
 */
public abstract class AbstractRetryException
    extends RuntimeException {

    /**
     * Constructs a {@link AbstractRetryException} via a causing exception.
     *
     * @param cause the cause of the failure
     */
    public AbstractRetryException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a {@link AbstractRetryException} via a rationale for failure.
     *
     * @param message the rationale for the failure
     */
    public AbstractRetryException(final String message) {
        super(message);
    }

    /**
     * Constructs a {@link AbstractRetryException} based on a rationale for the failure and its cause.
     *
     * @param message the rationale for the failure
     * @param cause   the optional cause for the failure
     */
    public AbstractRetryException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
