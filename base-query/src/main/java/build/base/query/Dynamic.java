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
 * Marks an {@link Indexable} {@code public} {@code static} {@code final} {@link Function} {@link Field} as
 * <i>dynamic</i> — its value for a given object may change after the object is constructed.
 *
 * <p>When present alongside {@link Indexable} on a {@link Function} field, calls to
 * {@link Index#reindexDynamic(Object)} will update only the index entries for {@link Dynamic} fields, leaving
 * stable (non-{@link Dynamic}) entries untouched.
 *
 * @author brian.oliver
 * @since Jun-2025
 * @see Indexable
 * @see Index#reindexDynamic(Object)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Dynamic {

}
