package build.base.query;

/*-
 * #%L
 * base.build Query
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.function.Function;

/**
 * Specifies that a {@link Class} or specific {@code public} {@code static} {@code final}ly {@link Function}s
 * {@link Field} defined by a {@link Class} may be <i>indexed</i> and/or queried by an {@link Index}.
 *
 * @author brian.oliver
 * @since Jun-2025
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Indexable {

    /**
     * When {@code true}, the function is expected to return a {@link java.util.stream.Stream},
     * {@link java.util.Collection}, or {@link Iterable}, and each element of that container is indexed
     * individually as a separate key.  This enables O(1) {@link Condition#contains} lookups.
     * <p>
     * When {@code false} (the default), the return value of the function is treated as a single scalar key.
     *
     * @return {@code true} if each element should be indexed separately
     */
    boolean each() default false;
}
