/*-
 * #%L
 * base.build Expression
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
 * Provides <i>Jakarta Expression Language</i> parsing and evaluation support.
 *
 * @author brian.oliver
 * @since Nov-2024
 */
module build.base.expression {
    requires build.base.foundation;
    requires build.base.configuration;
    requires build.base.parsing;
    requires jakarta.el;

    provides jakarta.el.ExpressionFactory
        with build.base.expression.el.ExpressionFactoryImpl;

    exports build.base.expression.ast;
    exports build.base.expression.compat;
    exports build.base.expression.eval;
    exports build.base.expression.option;
    exports build.base.expression.parser;
}
