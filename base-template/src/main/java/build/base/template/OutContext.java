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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a method of an {@link Out} type as an output context, so a template can write to it with a named
 * interpolation. A context is a place in the output language (an attribute, a URL, a string literal, a comment) where
 * a value must be escaped or checked in its own way; the method is what does that for the place it names. For
 * example, with the default delimiters <code>#url&#123;expr&#125;</code> compiles to {@code out.writeUrl(expr)} when
 * {@code writeUrl} is annotated {@code @OutContext("url")}.
 * <p>
 * The method must be public, non-static, and take the value to write as its single parameter. Only declared contexts
 * are recognised in templates, so ordinary text such as a CSS selector <code>#nav&#123;</code> is never mistaken for
 * one. The names {@code raw}, {@code write} and {@code include} are reserved and cannot be declared:
 * <code>#raw&#123;expr&#125;</code> always compiles to {@code out.raw(expr)}.
 * <p>
 * The annotation is read by {@code base-template-processor} at compile time, so it must survive in class files.
 *
 * @author reed.vonredwitz
 * @since Oct-2026
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface OutContext {

    /**
     * The name used in templates, which must be a Java identifier. Defaults to the name of the method.
     */
    String value() default "";
}
