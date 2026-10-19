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
 * Adapts a {@link Runnable} into a {@link Retryable}.
 *
 * @author patrick.peralta
 * @since Jul-2018
 */
public class RetryableRunnable
    implements Retryable<Void> {

    /**
     * The {@link Runnable} to execute.
     */
    private final Runnable runnable;

    /**
     * Private constructor for {@link RetryableRunnable} based on a {@link Runnable}.
     *
     * @param runnable the {@link Runnable} to execute
     */
    private RetryableRunnable(final Runnable runnable) {
        this.runnable = runnable;
    }

    /**
     * Create a {@link RetryableRunnable} based on a {@link Runnable}.
     *
     * @param runnable the {@link Runnable} to execute
     * @return new instance of {@code RetryableRunnable}
     */
    public static RetryableRunnable of(final Runnable runnable) {
        return new RetryableRunnable(runnable);
    }

    @Override
    public Void get()
        throws EphemeralFailureException, PermanentFailureException {
        this.runnable.run();
        return null;
    }

    @Override
    public String toString() {
        return "RetryableRunnable{" + this.runnable + '}';
    }
}
