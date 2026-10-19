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
 * Thrown by {@link Retryable#get()} implementations, indicating a {@link Retryable} currently failed to produce a
 * value, but may do so in future if retried.
 *
 * @author brian.oliver
 * @since Dec-2017
 */
public class EphemeralFailureException
    extends AbstractRetryException {

    /**
     * Constructs a {@link EphemeralFailureException} based on a causing exception.
     *
     * @param cause the optional cause for the failure
     */
    public EphemeralFailureException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a {@link EphemeralFailureException} based on a rationale for the failure.
     *
     * @param message the rationale for the failure
     */
    public EphemeralFailureException(final String message) {
        super(message);
    }
}
