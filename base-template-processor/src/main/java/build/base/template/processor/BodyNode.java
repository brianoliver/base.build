package build.base.template.processor;

/*-
 * #%L
 * base.build Template Processor
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
}
