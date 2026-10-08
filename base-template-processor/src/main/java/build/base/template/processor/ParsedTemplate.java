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

import java.util.List;

/**
 * @param sourceFile the {@code .jt} file the template was parsed from, or {@code null} when unknown
 * @param lines      the one-based line of the {@code .jt} file that each node of {@code body} came from, in the same
 *                   order, or empty when unknown
 */
record ParsedTemplate(String packageName,
                      List<String> imports,
                      String outType,
                      String className,
                      String params,
                      List<BodyNode> body,
                      String sourceFile,
                      List<Integer> lines
) {
    ParsedTemplate(final String packageName,
                   final List<String> imports,
                   final String outType,
                   final String className,
                   final String params,
                   final List<BodyNode> body) {
        this(packageName, imports, outType, className, params, body, null, List.of());
    }

    ParsedTemplate withPackageName(final String packageName) {
        return new ParsedTemplate(packageName, imports, outType, className, params, body, sourceFile, lines);
    }

    String qualifiedClassName() {
        return packageName.isEmpty() ? className : packageName + "." + className;
    }
}
