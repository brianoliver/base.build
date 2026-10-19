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
 * Specifies that the key produced by an {@link Indexable} {@code public} {@code static} {@code final} {@link Function}
 * {@link Field} is unique within the {@link Index} — i.e. no two indexed objects will ever produce the same key for
 * that function.
 *
 * <p>When present alongside {@link Indexable} on a {@link Function} field, the index stores a direct
 * {@code key → object} mapping instead of {@code key → Set<object>}, making {@code isEqualTo} lookups O(1) and
 * enforcing uniqueness at index time.
 *
 * @author brian.oliver
 * @since Jun-2025
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Unique {

}
