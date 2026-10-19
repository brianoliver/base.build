package build.base.foundation.predicate;

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
 * A {@link Predicate} that tests if an {@link Object} is an instance of a specified {@link Class}.
 *
 * @param <T> the type of {@link Class}
 * @author brian.oliver
 * @since Sep-2025
 */
public class InstanceOfPredicate<T>
    implements Predicate<T> {

    /**
     * The required {@link Class} to which to test instances against.
     */
    private final Class<T> requiredClass;

    /**
     * Constructs a new {@link InstanceOfPredicate} with the specified assignable {@link Class}.
     *
     * @param requiredClass the assignable {@link Class}
     */
    protected InstanceOfPredicate(final Class<T> requiredClass) {
        this.requiredClass = Objects.requireNonNull(requiredClass, "The Assignable Class must not be null");
    }

    /**
     * Obtains the required {@link Class} to which to test instances against.
     *
     * @return the required {@link Class}
     */
    public Class<T> requiredClass() {
        return this.requiredClass;
    }

    @Override
    public boolean test(final T t) {
        return this.requiredClass.isInstance(t);
    }

    @Override
    public String toString() {
        return this.requiredClass.getName() + "::instanceOf";
    }

    /**
     * Creates a new {@link InstanceOfPredicate} that tests whether instances are assignable to the specified
     * {@link Class}.
     *
     * @param <T>             the type of assignable {@link Class}
     * @param assignableClass the assignable {@link Class}
     * @return a new {@link InstanceOfPredicate}
     */
    public static <T> InstanceOfPredicate<T> of(final Class<T> assignableClass) {
        return new InstanceOfPredicate<>(assignableClass);
    }
}
