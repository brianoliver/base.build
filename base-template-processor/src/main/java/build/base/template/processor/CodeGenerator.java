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

final class CodeGenerator {

    private CodeGenerator() {
    }

    static String generate(final ParsedTemplate template) {
        final StringBuilder sb = new StringBuilder();

        if (!template.packageName().isEmpty()) {
            sb.append("package ").append(template.packageName()).append(";\n\n");
        }

        for (final String imp : template.imports()) {
            sb.append(imp).append(";\n");
        }
        if (!template.imports().isEmpty()) {
            sb.append("\n");
        }

        // A qualified out type is imported as written; a simple one resolves against build.base.template
        sb.append("import ")
            .append(template.outType().contains(".") ? "" : "build.base.template.")
            .append(template.outType()).append(";\n");
        sb.append("import build.base.template.Template;\n\n");

        sb.append("public record ").append(template.className())
            .append("(").append(template.params()).append(")")
            .append(" implements Template<").append(template.outType()).append("> {\n\n");

        sb.append("    @Override\n");
        sb.append("    public void render(final ").append(template.outType()).append(" out) {\n");

        generateBody(template.body(), sb);

        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

    private static void generateBody(final List<BodyNode> body,
                                     final StringBuilder sb) {
        final StringBuilder raw = new StringBuilder();

        for (final BodyNode node : body) {
            switch (node) {
                case BodyNode.RawText(final String text) -> raw.append(text);
                case BodyNode.Expression(final String code) -> {
                    flushRaw(raw, sb);
                    sb.append("        out.write(").append(code).append(");\n");
                }
                case BodyNode.CodeLine(final String code) -> {
                    flushRaw(raw, sb);
                    sb.append("        ").append(code).append("\n");
                }
                case BodyNode.Include(final String expression) -> {
                    flushRaw(raw, sb);
                    sb.append("        ").append(expression).append(".render(out);\n");
                }
            }
        }

        flushRaw(raw, sb);
    }

    private static void flushRaw(final StringBuilder raw,
                                 final StringBuilder sb) {
        if (raw.isEmpty()) {
            return;
        }
        sb.append("        out.raw(\"").append(escapeJava(raw.toString())).append("\");\n");
        raw.setLength(0);
    }

    private static String escapeJava(final String s) {
        final StringBuilder result = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            switch (c) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> result.append(c);
            }
        }
        return result.toString();
    }
}
