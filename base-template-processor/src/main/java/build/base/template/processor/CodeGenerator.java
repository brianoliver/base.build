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

import java.nio.file.Path;
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

        generateBody(template, sb);

        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

    private static void generateBody(final ParsedTemplate template,
                                     final StringBuilder sb) {
        final List<BodyNode> body = template.body();
        final StringBuilder raw = new StringBuilder();
        // The source line of the first node of the pending raw text, and of the last line a comment was written for
        int rawLine = 0;
        int commented = 0;

        for (int i = 0; i < body.size(); i++) {
            final int line = i < template.lines().size() ? template.lines().get(i) : 0;
            final BodyNode node = body.get(i);
            if (node instanceof BodyNode.RawText) {
                if (raw.isEmpty()) {
                    rawLine = line;
                }
            } else {
                commented = flushRaw(raw, rawLine, commented, template, sb);
                commented = comment(line, commented, template, sb);
            }
            switch (node) {
                case BodyNode.RawText(final String text) -> raw.append(text);
                case BodyNode.Expression(final String code) ->
                    sb.append("        out.write(").append(code).append(");\n");
                case BodyNode.ContextExpression(final String method, final String code) ->
                    sb.append("        out.").append(method).append("(").append(code).append(");\n");
                case BodyNode.CodeLine(final String code) -> sb.append("        ").append(code).append("\n");
                case BodyNode.Include(final String expression) ->
                    sb.append("        ").append(expression).append(".render(out);\n");
            }
        }

        flushRaw(raw, rawLine, commented, template, sb);
    }

    /** Writes the pending raw text, returning the line last commented. */
    private static int flushRaw(final StringBuilder raw,
                                final int line,
                                final int commented,
                                final ParsedTemplate template,
                                final StringBuilder sb) {
        if (raw.isEmpty()) {
            return commented;
        }
        final int result = comment(line, commented, template, sb);
        sb.append("        out.raw(\"").append(escapeJava(raw.toString())).append("\");\n");
        raw.setLength(0);
        return result;
    }

    /**
     * Writes a comment naming the {@code .jt} line the next statement came from, unless it was just written for the
     * same line. Returns the line last commented.
     */
    private static int comment(final int line,
                               final int commented,
                               final ParsedTemplate template,
                               final StringBuilder sb) {
        if (line <= 0 || line == commented || template.sourceFile() == null) {
            return commented;
        }
        final Path name = Path.of(template.sourceFile()).getFileName();
        sb.append("        // ").append(name == null ? template.sourceFile() : name).append(':').append(line)
            .append('\n');
        return line;
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
