package build.base.marshalling;

/*-
 * #%L
 * base.build Marshalling
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

/**
 * Indicates that an {@link Marshal} or {@link Unmarshal} argument is a <i>bound</i> {@link Parameter}, the value of
 * which is defined as a binding using {@link Marshaller#bind(Class)}.
 *
 * @author brian.oliver
 * @see Schema#parameters()
 * @see Schema#dependencies()
 * @since Nov-2024
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Bound {

}
