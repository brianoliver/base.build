package build.base.template.processor;

/*-
 * #%L
 * base.build Template Processor
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
 * A part of the body of a parsed template, in the order it appears in the {@code .jt} file.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
sealed interface BodyNode {
    record RawText(String text) implements BodyNode {
    }

    record Expression(String code) implements BodyNode {
    }

    /**
     * A write to a named output context, for example <code>#url&#123;expr&#125;</code>, which calls the method of the
     * out type that implements the context.
     *
     * @param method the name of the method to call on the out type
     * @param code   the expression to write
     */
    record ContextExpression(String method, String code) implements BodyNode {
    }

    record CodeLine(String code) implements BodyNode {
    }

    record Include(String expression) implements BodyNode {
    }

    /**
     * The start of an include with a body, <code>@include Layout(title) &#123;</code>: the nodes up to the matching
     * {@link IncludeEnd} are a template that is passed to the included template as its last argument, for it to render
     * with a slot.
     *
     * @param type the name of the included template type
     * @param args the arguments for the included template, before the body; may be empty
     */
    record IncludeStart(String type, String args) implements BodyNode {
    }

    /**
     * The end of an include with a body: the closing brace line that matches an {@link IncludeStart}.
     */
    record IncludeEnd() implements BodyNode {
    }

    /**
     * Renders a part of the body of an include, from the {@code Slots} parameter of the template.
     *
     * @param name the name of the part, or {@code null} for the content that is in no fill
     */
    record Slot(String name) implements BodyNode {
    }

    /**
     * The start of a named part of the body of an include, <code>@fill head</code>: the nodes up to the matching
     * {@link FillEnd} are given to the included template under the name, and are not a part of the rest of the body.
     *
     * @param name the name of the part, which the included template renders with a slot of that name
     */
    record FillStart(String name) implements BodyNode {
    }

    /**
     * The end of a named part of the body of an include, <code>@endfill</code>, which closes the {@link FillStart}
     * before it.
     */
    record FillEnd() implements BodyNode {
    }

    /**
     * The start of a fragment: the nodes up to the matching {@link FragmentEnd} are a part of the template that is
     * rendered in place and is also a template of its own.
     *
     * @param name   the name of the fragment, as written
     * @param params the parameters of the fragment, as in a method declaration; the fragment is rendered in place with
     *               the variables of the same names
     */
    record FragmentStart(String name, String params) implements BodyNode {

        /**
         * The name of the type generated for the fragment: its name with the first letter upper-cased.
         */
        String typeName() {
            return Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
    }

    record FragmentEnd() implements BodyNode {
    }
}
