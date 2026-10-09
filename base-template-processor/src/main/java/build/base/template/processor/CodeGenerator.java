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
import java.util.ArrayList;
import java.util.List;

/**
 * Generates the Java source of the record for a {@link ParsedTemplate}, with a nested record for each of its
 * fragments.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
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

        final List<int[]> fragments = new ArrayList<>();
        generateBody(template, 0, template.body().size(), "        ", fragments, sb);

        sb.append("    }\n");

        // Each fragment is also a template of its own, nested in the template it is part of
        for (final int[] range : fragments) {
            final BodyNode.FragmentStart start = (BodyNode.FragmentStart) template.body().get(range[0]);
            sb.append("\n    public record ").append(start.typeName())
                .append("(").append(start.params()).append(")")
                .append(" implements Template<").append(template.outType()).append("> {\n\n");
            sb.append("        @Override\n");
            sb.append("        public void render(final ").append(template.outType()).append(" out) {\n");
            generateBody(template, range[0] + 1, range[1], "            ", new ArrayList<>(), sb);
            sb.append("        }\n");
            sb.append("    }\n");
        }

        sb.append("}\n");

        return sb.toString();
    }

    /** The names of the parameters in a parameter list: the last identifier of each top-level comma-separated part. */
    static List<String> parameterNames(final String params) {
        final List<String> names = new ArrayList<>();
        int depth = 0;
        int from = 0;
        for (int i = 0; i <= params.length(); i++) {
            final char c = i < params.length() ? params.charAt(i) : ',';
            if (c == '<' || c == '(' || c == '[') {
                depth++;
            } else if (c == '>' || c == ')' || c == ']') {
                depth--;
            } else if (c == ',' && depth == 0) {
                final String part = params.substring(from, i).trim();
                if (!part.isEmpty()) {
                    int start = part.length();
                    while (start > 0 && Character.isJavaIdentifierPart(part.charAt(start - 1))) {
                        start--;
                    }
                    names.add(part.substring(start));
                }
                from = i + 1;
            }
        }
        return names;
    }

    /**
     * Generates the statements for the nodes {@code from} (inclusive) to {@code to} (exclusive) of the body. A fragment
     * in that range is rendered in place, by creating its type with the variables named by its parameters, and its
     * range is added to {@code fragments} for the caller to generate the type from.
     */
    private static void generateBody(final ParsedTemplate template,
                                     final int from,
                                     final int to,
                                     final String indent,
                                     final List<int[]> fragments,
                                     final StringBuilder sb) {
        final List<BodyNode> body = template.body();
        final StringBuilder raw = new StringBuilder();
        // The source line of the first node of the pending raw text, and of the last line a comment was written for
        int rawLine = 0;
        int commented = 0;

        for (int i = from; i < to; i++) {
            final int line = i < template.lines().size() ? template.lines().get(i) : 0;
            final BodyNode node = body.get(i);
            if (node instanceof BodyNode.RawText) {
                if (raw.isEmpty()) {
                    rawLine = line;
                }
            } else {
                commented = flushRaw(raw, rawLine, commented, template, indent, sb);
                commented = comment(line, commented, template, indent, sb);
            }
            switch (node) {
                case BodyNode.RawText(final String text) -> raw.append(text);
                case BodyNode.Expression(final String code) ->
                    sb.append(indent).append("out.write(").append(code).append(");\n");
                case BodyNode.ContextExpression(final String method, final String code) ->
                    sb.append(indent).append("out.").append(method).append("(").append(code).append(");\n");
                case BodyNode.CodeLine(final String code) -> sb.append(indent).append(code).append("\n");
                case BodyNode.Include(final String expression) ->
                    sb.append(indent).append(expression).append(".render(out);\n");
                case BodyNode.FragmentStart start -> {
                    // The parser guarantees that every start has an end and that fragments do not nest
                    int end = i + 1;
                    while (!(body.get(end) instanceof BodyNode.FragmentEnd)) {
                        end++;
                    }
                    fragments.add(new int[]{i, end});
                    sb.append(indent).append("new ").append(start.typeName())
                        .append("(").append(String.join(", ", parameterNames(start.params())))
                        .append(").render(out);\n");
                    i = end;
                }
                case BodyNode.FragmentEnd() -> {
                    // Not reached for the end of a fragment in range, which the start skips; only for exhaustiveness
                }
            }
        }

        flushRaw(raw, rawLine, commented, template, indent, sb);
    }

    /** Writes the pending raw text, returning the line last commented. */
    private static int flushRaw(final StringBuilder raw,
                                final int line,
                                final int commented,
                                final ParsedTemplate template,
                                final String indent,
                                final StringBuilder sb) {
        if (raw.isEmpty()) {
            return commented;
        }
        final int result = comment(line, commented, template, indent, sb);
        sb.append(indent).append("out.raw(\"").append(escapeJava(raw.toString())).append("\");\n");
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
                               final String indent,
                               final StringBuilder sb) {
        if (line <= 0 || line == commented || template.sourceFile() == null) {
            return commented;
        }
        final Path name = Path.of(template.sourceFile()).getFileName();
        sb.append(indent).append("// ").append(name == null ? template.sourceFile() : name).append(':').append(line)
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
