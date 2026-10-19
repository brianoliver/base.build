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
 * A {@link Retryable} that always returns the same value.
 *
 * @param <T> the type of the value
 * @author brian.oliver
 * @since Dec-2017
 */
public class RetryableConstant<T>
    implements Retryable<T> {

    /**
     * The value to return.
     */
    private T value;

    /**
     * Privately constructs a {@link RetryableConstant} for a specified value.
     *
     * @param value the value
     */
    private RetryableConstant(final T value) {
        this.value = value;
    }

    /**
     * Obtains a {@link RetryableConstant} for a specified value.
     *
     * @param <T>   the type of the value
     * @param value the value
     * @return a {@link RetryableConstant} for a value
     */
    public static <T> RetryableConstant<T> of(final T value) {
        return new RetryableConstant<>(value);
    }

    @Override
    public T get() {
        return this.value;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public String toString() {
        return "RetryableConstant{" + this.value + '}';
    }
}
