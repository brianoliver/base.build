package build.base.template;

/*-
 * #%L
 * base.build Template
 * %%
 * Copyright (C) 2026 Workday, Inc.
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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the default template syntax for templates that write to the annotated {@link Out} type.
 * <p>
 * A template can still override either setting with an {@code option} line. The annotation is read by
 * {@code base-template-processor} at compile time, so it must survive in class files, and it is inherited by
 * subclasses of the annotated type.
 *
 * @author reed.vonredwitz
 * @since Oct-2026
 */
@Documented
@Inherited
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface OutSyntax {

    /**
     * The prefix that introduces a directive at the start of a line. It must be non-empty, contain no whitespace
     * and not start with an identifier character.
     */
    String prefix() default "@";

    /**
     * The opener of an interpolation, which is closed by a right brace. It must end with a left brace, have at least
     * one other character and contain no whitespace.
     */
    String interpolation() default "#{";
}
