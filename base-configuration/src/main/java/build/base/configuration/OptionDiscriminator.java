package build.base.configuration;

/*-
 * #%L
 * base.build Configuration
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies that a type is to be used for discriminating {@link Option}s instances with {@link Configuration}s,
 * instead of using the concrete type of {@link Option}.
 * <p>
 * When an {@link Option} type or super type is annotated as a {@link OptionDiscriminator}, that annotated type is
 * used for discriminating between different types of {@link Option}s with {@link Configuration}s, instead of the
 * default behavior of using the concrete type of said {@link Option}s.
 *
 * @author brian.oliver
 * @since Nov-2017
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OptionDiscriminator {

}
