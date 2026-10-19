package build.base.network;

/*-
 * #%L
 * base.build Network
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
 * Thrown by {@link Network} methods indicating that a networking issue has occurred and prevented correct execution.
 *
 * @author graeme.campbell
 * @since Mar-2019
 */
public class NetworkException
    extends RuntimeException {

    /**
     * Creates a {@link NetworkException} based on a causing {@link Throwable}.
     *
     * @param cause the cause of the failure
     */
    public NetworkException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates a {@link NetworkException} based on a rationale for the failure.
     *
     * @param message the cause of the failure
     */
    public NetworkException(final String message) {
        super(message);
    }
}
