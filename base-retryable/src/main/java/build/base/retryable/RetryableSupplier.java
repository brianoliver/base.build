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

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Adapts a {@link Supplier} into a {@link Retryable}.
 *
 * @param <T> the type of the {@link Retryable}
 * @author brian.oliver
 * @since Dec-2017
 */
public class RetryableSupplier<T>
    implements Retryable<T> {

    /**
     * The {@link Supplier}
     */
    private Supplier<T> supplier;

    /**
     * Privately constructs a {@link RetryableSupplier} based on a specified {@link Supplier}.
     *
     * @param supplier the {@link Supplier}
     */
    private RetryableSupplier(final Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "The supplier can't be null");

        this.supplier = supplier;
    }

    /**
     * Constructs a {@link RetryableSupplier} based on a specified {@link Supplier}.
     *
     * @param <T>      the type of supplied value
     * @param supplier the {@link Supplier}
     * @return a {@link RetryableSupplier}
     */
    public static <T> RetryableSupplier<T> of(final Supplier<T> supplier) {
        return new RetryableSupplier<>(supplier);
    }

    @Override
    public T get()
        throws EphemeralFailureException,
        PermanentFailureException {

        return this.supplier.get();
    }

    @Override
    public String toString() {
        return "RetryableSupplier{" + this.supplier + "}";
    }
}
