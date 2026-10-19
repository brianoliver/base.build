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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        final Set<String> slots = fillableSlots(template);
        if (slots != null) {
            sb.append("/**\n * A layout: an include of it with a body can give the content that is in no fill, and a part for ")
                .append(slots.isEmpty() ? "no named slot" : "each of its named slots (" + String.join(", ", slots) + ")")
                .append(".\n */\n");
        }
        sb.append("public record ").append(template.className())
            .append("(").append(template.params()).append(")")
            .append(" implements Template<").append(template.outType()).append("> {\n\n");

        if (slots != null) {
            // Read by the processor from the compiled class, to check the fills of an include of this template that
            // is in another build
            sb.append("    /** The names of the named slots, separated by commas. */\n");
            sb.append("    public static final String ").append(SLOTS_CONSTANT).append(" = \"")
                .append(String.join(",", slots)).append("\";\n\n");
        }

        sb.append("    @Override\n");
        sb.append("    public void render(final ").append(template.outType()).append(" out) {\n");

        final List<int[]> fragments = new ArrayList<>();
        generateBody(template, 0, template.body().size(), template.params(), "        ", fragments, sb);

        sb.append("    }\n");

        // Each fragment is also a template of its own, nested in the template it is part of
        for (final int[] range : fragments) {
            final BodyNode.FragmentStart start = (BodyNode.FragmentStart) template.body().get(range[0]);
            sb.append("\n    public record ").append(start.typeName())
                .append("(").append(start.params()).append(")")
                .append(" implements Template<").append(template.outType()).append("> {\n\n");
            sb.append("        @Override\n");
            sb.append("        public void render(final ").append(template.outType()).append(" out) {\n");
            generateBody(template, range[0] + 1, range[1], start.params(), "            ", new ArrayList<>(), sb);
            sb.append("        }\n");
            sb.append("    }\n");
        }

        sb.append("}\n");

        return sb.toString();
    }

    /**
     * The name of the part of the body of an include that is in no fill: the same as {@code Slots.BODY}, which this
     * module cannot see (a test checks that they agree).
     */
    static final String BODY_SLOT = "body";

    /**
     * The name of the constant of a generated template that lists its named slots, for the fills of an include of the
     * template to be checked when it is not a template of the build.
     */
    static final String SLOTS_CONSTANT = "SLOTS";

    // The type that carries the fills of an include
    private static final String SLOTS_TYPE = "build.base.template.Slots";

    // A parameter of that type: written in full, or as Slots, which is checked against the imports
    private static final Pattern SLOTS_PARAMETER =
        Pattern.compile("(?:final\\s+)?(build\\.base\\.template\\.Slots|Slots)(?:\\s*<.*>\\s*|\\s+)([\\w$]+)",
                        Pattern.DOTALL);

    /**
     * The name of the first parameter in a parameter list that is of the type {@code build.base.template.Slots}, which
     * a slot renders a part of, or {@code null}. A type written as {@code Slots} is that type only if the
     * template imports it, so a type of that name from elsewhere is not mistaken for it.
     *
     * @param imports the imports of the template, each as {@code import ...}
     */
    static String slotsParameter(final String params, final List<String> imports) {
        for (final String part : parameterDeclarations(params)) {
            final Matcher slots = SLOTS_PARAMETER.matcher(part);
            if (slots.matches() && (slots.group(1).equals(SLOTS_TYPE) || importsSlots(imports))) {
                return slots.group(2);
            }
        }
        return null;
    }

    private static boolean importsSlots(final List<String> imports) {
        return imports.stream()
            .map(declaration -> declaration.substring("import ".length()).trim())
            .anyMatch(imported -> imported.equals(SLOTS_TYPE) || imported.equals("build.base.template.*"));
    }

    /**
     * The names of the slots a template can be filled for, or {@code null} if it has no {@code Slots} parameter: those
     * it declares with {@code slots(...)}, or else those its body renders from that parameter. The slots of its
     * fragments are of their own types and are not included.
     */
    static Set<String> fillableSlots(final ParsedTemplate template) {
        if (slotsParameter(template.params(), template.imports()) == null) {
            return null;
        }
        final Set<String> names = new TreeSet<>();
        if (template.declaredSlots() != null) {
            // What the template declares is what it can be filled for, whatever its body renders
            names.addAll(template.declaredSlots());
            return names;
        }
        boolean inFragment = false;
        for (final BodyNode node : template.body()) {
            if (node instanceof BodyNode.FragmentStart) {
                inFragment = true;
            } else if (node instanceof BodyNode.FragmentEnd) {
                inFragment = false;
            } else if (!inFragment && node instanceof BodyNode.Slot(final String name) && name != null) {
                names.add(name);
            }
        }
        return names;
    }

    /** The names of the parameters in a parameter list: the last identifier of each top-level comma-separated part. */
    static List<String> parameterNames(final String params) {
        final List<String> names = new ArrayList<>();
        for (final String part : parameterDeclarations(params)) {
            int start = part.length();
            while (start > 0 && Character.isJavaIdentifierPart(part.charAt(start - 1))) {
                start--;
            }
            names.add(part.substring(start));
        }
        return names;
    }

    /** The parameters in a parameter list, each as written: the top-level comma-separated parts that are not blank. */
    private static List<String> parameterDeclarations(final String params) {
        final List<String> declarations = new ArrayList<>();
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
                    declarations.add(part);
                }
                from = i + 1;
            }
        }
        return declarations;
    }

    /**
     * Generates the statements for the nodes {@code from} (inclusive) to {@code to} (exclusive) of the body. A fragment
     * in that range is rendered in place, by creating its type with the variables named by its parameters, and its
     * range is added to {@code fragments} for the caller to generate the type from. The body of an include is
     * generated as a template of its own, in place, which sees what is around it.
     *
     * @param scopeParams the parameters of the template or fragment the nodes are in, one of which is the
     *                    {@code Slots} that a slot renders from
     */
    private static void generateBody(final ParsedTemplate template,
                                     final int from,
                                     final int to,
                                     final String scopeParams,
                                     final String indent,
                                     final List<int[]> fragments,
                                     final StringBuilder sb) {
        generateBody(template, List.of(new int[]{from, to}), scopeParams, indent, fragments, sb);
    }

    /**
     * Generates the statements for the nodes in the {@code ranges}, each from (inclusive) to (exclusive) of the body, as
     * if they were next to each other.
     */
    private static void generateBody(final ParsedTemplate template,
                                     final List<int[]> ranges,
                                     final String scopeParams,
                                     final String indent,
                                     final List<int[]> fragments,
                                     final StringBuilder sb) {
        final List<BodyNode> body = template.body();
        final StringBuilder raw = new StringBuilder();
        // The source line of the first node of the pending raw text, and of the last line a comment was written for
        int rawLine = 0;
        int commented = 0;

        for (final int[] range : ranges) {
            for (int i = range[0]; i < range[1]; i++) {
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
                    case BodyNode.IncludeStart(final String type, final String args) ->
                        i = generateInclude(template, i, type, args, scopeParams, indent, fragments, sb);
                    case BodyNode.IncludeEnd() -> {
                        // Not reached for the end of an include in range, which the start skips
                    }
                    case BodyNode.FillStart(final String name) -> {
                        // Not reached for a fill in range, which the include it is in takes out of its body
                    }
                    case BodyNode.FillEnd() -> {
                        // Not reached for the end of a fill in range, which the start takes out with it
                    }
                    case BodyNode.Slot(final String name) ->
                        // The parser guarantees that what a slot is in has a Slots parameter
                        sb.append(indent).append(slotsParameter(scopeParams, template.imports()))
                            .append(".render(\"").append(name != null ? name : BODY_SLOT).append("\", out);\n");
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
                        // Not reached for the end of a fragment in range, which the start skips
                    }
                }
            }
        }

        flushRaw(raw, rawLine, commented, template, indent, sb);
    }

    /**
     * Generates the include with a body that starts at {@code start}, and returns the index of the node that ends it.
     * <p>
     * The body is given as a template of its own, an anonymous class rather than a lambda so that it can keep calling
     * its parameter {@code out}. The fills and the rest of the body are always given together as {@code Slots}, by name
     * (a body without fills is just the {@code body}, which is also what the {@code Slots} renders as a template).
     * Blank lines next to a fill, or at the ends of the body, are left out of the rest of the body whether or not there
     * are fills, and the others are kept.
     */
    private static int generateInclude(final ParsedTemplate template,
                                       final int start,
                                       final String type,
                                       final String args,
                                       final String scopeParams,
                                       final String indent,
                                       final List<int[]> fragments,
                                       final StringBuilder sb) {
        final List<BodyNode> body = template.body();
        // The parser guarantees that every start has an end, and ends match starts
        int depth = 1;
        int end = start + 1;
        for (; end < body.size(); end++) {
            if (body.get(end) instanceof BodyNode.IncludeStart) {
                depth++;
            } else if (body.get(end) instanceof BodyNode.IncludeEnd && --depth == 0) {
                break;
            }
        }

        // The fills of this include, and the ranges of the rest of its body, leaving out what is in a nested include
        final List<int[]> fills = new ArrayList<>();
        final List<int[]> rest = new ArrayList<>();
        int nested = 0;
        int restFrom = start + 1;
        for (int i = start + 1; i < end; i++) {
            final BodyNode node = body.get(i);
            if (node instanceof BodyNode.IncludeStart) {
                nested++;
            } else if (node instanceof BodyNode.IncludeEnd) {
                nested--;
            } else if (nested == 0 && node instanceof BodyNode.FillStart) {
                int fillDepth = 1;
                int fillEnd = i + 1;
                for (; fillEnd < end; fillEnd++) {
                    if (body.get(fillEnd) instanceof BodyNode.FillStart) {
                        fillDepth++;
                    } else if (body.get(fillEnd) instanceof BodyNode.FillEnd && --fillDepth == 0) {
                        break;
                    }
                }
                addWithoutBlankLines(template, rest, restFrom, i);
                fills.add(new int[]{i, fillEnd});
                restFrom = fillEnd + 1;
                i = fillEnd;
            }
        }
        addWithoutBlankLines(template, rest, restFrom, end);

        final String out = template.outType();
        sb.append(indent).append("new ").append(type).append("(").append(args.isEmpty() ? "" : args + ", ")
            .append("new build.base.template.Slots<").append(out)
            .append(">(java.util.Map.<String, Template<").append(out).append(">>ofEntries(\n");
        for (final int[] fill : fills) {
            final String name = ((BodyNode.FillStart) body.get(fill[0])).name();
            generatePart(template, List.of(new int[]{fill[0] + 1, fill[1]}), scopeParams,
                         "java.util.Map.entry(\"" + name + "\", ", indent + "    ", fragments, sb);
            sb.append("),\n");
        }
        generatePart(template, rest, scopeParams, "java.util.Map.entry(\"" + BODY_SLOT + "\", ",
                     indent + "    ", fragments, sb);
        sb.append(")))).render(out);\n");
        return end;
    }

    /**
     * Adds the nodes from (inclusive) to (exclusive) to the {@code ranges}, less the lines of blank text at either end:
     * they are next to a fill, or the end of the body, and are not a part of what is given.
     */
    private static void addWithoutBlankLines(final ParsedTemplate template,
                                             final List<int[]> ranges,
                                             final int from,
                                             final int to) {
        int first = from;
        int last = to;
        while (first < last && isBlankLine(template, first)) {
            first++;
        }
        while (last > first && isBlankLine(template, last - 1)) {
            last--;
        }
        if (first < last) {
            ranges.add(new int[]{first, last});
        }
    }

    /**
     * Whether the node is the text of a line of its own that is blank: not the end of a line that has an expression on it,
     * whose newline is a part of that line.
     */
    private static boolean isBlankLine(final ParsedTemplate template, final int index) {
        if (!(template.body().get(index) instanceof BodyNode.RawText(final String text))
            || !text.isBlank() || !text.endsWith("\n")) {
            return false;
        }
        final List<Integer> lines = template.lines();
        return index == 0 || index >= lines.size() || !lines.get(index - 1).equals(lines.get(index));
    }

    /**
     * Generates a template of its own, an anonymous class, for the nodes in the {@code ranges}, with the text
     * {@code lead} before it on its first line. The last line is not ended.
     */
    private static void generatePart(final ParsedTemplate template,
                                     final List<int[]> ranges,
                                     final String scopeParams,
                                     final String lead,
                                     final String indent,
                                     final List<int[]> fragments,
                                     final StringBuilder sb) {
        sb.append(indent).append(lead).append("new Template<").append(template.outType())
            .append(">() {\n");
        sb.append(indent).append("    @Override\n");
        sb.append(indent).append("    public void render(final ").append(template.outType()).append(" out) {\n");
        generateBody(template, ranges, scopeParams, indent + "        ", fragments, sb);
        sb.append(indent).append("    }\n");
        sb.append(indent).append("}");
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
