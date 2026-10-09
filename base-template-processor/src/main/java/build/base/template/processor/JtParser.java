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

import build.base.io.LookaheadReader;
import build.base.parsing.AbstractParser;
import build.base.parsing.ParseException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author reed.vonredwitz
 * @since Apr-2026
 */
final class JtParser {

    private static final Pattern QUALIFIED_NAME =
        Pattern.compile("[a-zA-Z_$][\\w$]*(\\.[a-zA-Z_$][\\w$]*)*");

    // Matches the body of an import statement: optional "static", qualified name, optional ".*"
    private static final Pattern IMPORT_BODY =
        Pattern.compile("(static\\s+)?[a-zA-Z_$][\\w$]*(\\.[a-zA-Z_$][\\w$]*)*(\\.\\*)?");

    // A double-quoted option value; no escapes, so delimiters containing a backslash or quote are not expressible
    private static final Pattern STRING_LITERAL = Pattern.compile("\"[^\"\\n\\\\]*\"");

    // A whole-line Java call statement such as "out.raw(x);" (what the text after the prefix looks like when someone
    // wrote Java directly instead of using the java directive); annotations start in upper case and attributes
    // such as Alpine's @click.outside="..." have no call parentheses, so neither matches
    private static final Pattern LOOKS_LIKE_STATEMENT =
        Pattern.compile("[a-z_$][\\w$]*(\\.[\\w$]+)*\\(.*\\)\\s*;");

    // The argument of a fragment directive: a name and a parenthesised parameter list, as in a method declaration
    private static final Pattern FRAGMENT_DECLARATION = Pattern.compile("([a-zA-Z_$][\\w$]*)\\s*\\((.*)\\)");

    // The argument of an include with a body: the template type, its arguments and the brace that opens the body
    private static final Pattern INCLUDE_BLOCK =
        Pattern.compile("([a-zA-Z_$][\\w$]*(?:\\.[a-zA-Z_$][\\w$]*)*)\\s*\\((.*)\\)\\s*\\{");

    // A URL attribute whose value is just starting: an interpolation here could supply the scheme. A fixed prefix such
    // as href="/tasks/#{id}" cannot, so it does not match
    private static final Pattern URL_ATTRIBUTE_START = Pattern.compile(
        "(?i)(?:^|[\\s\"'])(href|src|action|formaction|poster|cite|data|srcset|ping|xlink:href"
            + "|hx-(?:get|post|put|patch|delete|push-url|replace-url))\\s*=\\s*[\"']?\\s*$");

    // An attribute whose value is JavaScript or JSON, with its value still open. A style="..." attribute (CSS) is
    // deliberately not covered: only the style element is
    private static final Pattern SCRIPT_ATTRIBUTE_OPEN = Pattern.compile(
        "(?i)(?:^|[\\s\"'])(on[a-z]+|hx-on[\\w:.\\-]*|hx-vals|hx-headers|x-[\\w:.\\-]+|@[\\w:.\\-]+|:[\\w\\-]+)"
            + "\\s*=\\s*(?:\"[^\"]*|'[^']*)$");

    // A complete opening or closing tag of an element whose content is not markup
    private static final Pattern RAW_TEXT_TAG = Pattern.compile("(?i)<(/?)(script|style)\\b[^>]*>");

    private JtParser() {
    }

    /**
     * The raw-text element ({@code script} or {@code style}) that the end of {@code text} is inside, or {@code null}.
     * <p>
     * Known limitation: HTML comments are not understood, so a tag inside {@code <!-- -->} still counts.
     *
     * @param text    markup, which may start or end the element
     * @param initial the element the text starts inside, or {@code null}
     */
    private static String elementAfter(final String text, final String initial) {
        String element = initial;
        final var tags = RAW_TEXT_TAG.matcher(text);
        while (tags.find()) {
            final boolean closing = !tags.group(1).isEmpty();
            final String name = tags.group(2).toLowerCase();
            if (element == null) {
                if (!closing && !tags.group().endsWith("/>")) {
                    element = name;
                }
            } else if (closing && name.equals(element)) {
                // Inside a raw-text element only its own end tag means anything: a "<style>" in a script is a string
                element = null;
            }
        }
        return element;
    }

    /**
     * The line with each interpolation, plain or to a named context, replaced by {@code ?}: what is inside the braces
     * is Java, whose quotes and angle brackets say nothing about the markup around it. An interpolation that is never
     * closed takes the rest of the line.
     */
    private static String withoutExpressions(final String line, final Syntax syntax) {
        // Only the contexts of the out type, as the text parser treats any other name (such as CSS "#nav{") as text
        final var names = syntax.contexts().keySet().stream().map(Pattern::quote).toList();
        final var opener = Pattern.compile(Pattern.quote(syntax.interpolationLead())
                + (names.isEmpty() ? "" : "(?:" + String.join("|", names) + ")?") + "\\{")
            .matcher(line);
        final var result = new StringBuilder();
        int from = 0;
        while (opener.find(from)) {
            result.append(line, from, opener.start()).append('?');
            int depth = 1;
            int i = opener.end();
            while (i < line.length() && depth > 0) {
                final char c = line.charAt(i);
                if (c == '"' || c == '\'') {
                    for (i++; i < line.length() && line.charAt(i) != c; i++) {
                        if (line.charAt(i) == '\\') {
                            i++;
                        }
                    }
                } else if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                }
                i++;
            }
            from = Math.min(i, line.length());
        }
        return result.append(line, from, line.length()).toString();
    }

    /**
     * The start of the tag that the end of {@code text} is inside, from its {@code <}, or {@code null} when the text
     * ends outside any tag. Quoted attribute values may contain {@code >}.
     *
     * @param text    markup, which may close or open tags
     * @param element the raw-text element the text starts inside, or {@code null}; its content is not markup, so it is
     *                skipped
     */
    private static String openTagAfter(final String text, final String element) {
        String markup = text;
        if (element != null) {
            final var end = Pattern.compile("(?i)</" + element + "\\b[^>]*>").matcher(text);
            if (!end.find()) {
                return null;
            }
            markup = text.substring(end.end());
        }
        int start = -1;
        char quote = 0;
        for (int i = 0; i < markup.length(); i++) {
            final char c = markup.charAt(i);
            if (start < 0) {
                if (c == '<' && i + 1 < markup.length() && Character.isLetter(markup.charAt(i + 1))) {
                    start = i;
                }
            } else if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '>') {
                start = -1;
            }
        }
        return start < 0 ? null : markup.substring(start);
    }

    static ParsedTemplate parse(final String content, final String sourceFile) {
        return parse(content, sourceFile, outType -> Syntax.DEFAULT);
    }

    /**
     * @param outSyntax resolves the default syntax of an out type, as written in the {@code out} declaration; the
     *                  template's {@code option} lines override it
     */
    static ParsedTemplate parse(final String content,
                                final String sourceFile,
                                final Function<String, Syntax> outSyntax) {
        return parse(content, sourceFile, outSyntax, warning -> { });
    }

    /**
     * @param warnings receives a message for each construct that is accepted but probably not what the author meant
     */
    static ParsedTemplate parse(final String content,
                                final String sourceFile,
                                final Function<String, Syntax> outSyntax,
                                final Consumer<String> warnings) {
        return new JtFileParser(content, sourceFile, outSyntax, warnings).run();
    }

    /**
     * Parses a template like {@link #parse(String, String, Function, Consumer)}, but reports every error instead of
     * the first. A malformed body line is reported and parsing continues with the next line; a malformed header, a
     * missing end or content after it ends the parse, after the errors found before it.
     *
     * @return the outcome, whose errors are all {@link JtParseException}s
     */
    static AbstractParser.Outcome<ParsedTemplate> parseAll(final String content,
                                                           final String sourceFile,
                                                           final Function<String, Syntax> outSyntax,
                                                           final Consumer<String> warnings) {
        return new JtFileParser(content, sourceFile, outSyntax, warnings).runRecovering();
    }

    private static void parseBodyLine(final String line,
                                      final List<BodyNode> body,
                                      final String sourceFile,
                                      final int lineNumber,
                                      final Syntax syntax,
                                      final Consumer<String> warnings,
                                      final String element,
                                      final String openTag) {
        final String trimmed = line.trim();
        final int column = line.indexOf(syntax.prefix()) + 1;

        // A doubled prefix is an escape: the line is text with one prefix removed
        if (trimmed.startsWith(syntax.escapedPrefix())) {
            final String unescaped = line.substring(0, column - 1) + line.substring(column - 1 + syntax.prefix().length());
            body.addAll(new TextLineParser(unescaped + "\n", sourceFile, lineNumber, syntax,
                                           syntax.prefix().length(), warnings, element, openTag).run());
            return;
        }

        if (trimmed.startsWith(syntax.prefix())) {
            final String rest = trimmed.substring(syntax.prefix().length());
            if (rest.startsWith("}")) {
                body.add(new BodyNode.CodeLine(rest.trim()));
                return;
            }

            int end = 0;
            while (end < rest.length() && Character.isJavaIdentifierPart(rest.charAt(end))) {
                end++;
            }
            final String directive = rest.substring(0, end);
            final String argument = rest.substring(end).trim();

            if (Syntax.DIRECTIVES.contains(directive)) {
                if (Syntax.RESERVED.contains(directive)) {
                    throw directiveError(sourceFile, lineNumber, column,
                                         "directive " + syntax.prefix() + directive + " is not supported yet");
                }
                switch (directive) {
                    case "end" -> throw directiveError(sourceFile, lineNumber, column,
                                                       "unexpected content after " + syntax.prefix() + "end");
                    case "include" -> {
                        if (argument.isEmpty()) {
                            throw directiveError(sourceFile, lineNumber, column,
                                                 syntax.prefix() + "include requires an expression");
                        }
                        if (argument.endsWith("{")) {
                            final Matcher block = INCLUDE_BLOCK.matcher(argument);
                            if (!block.matches()) {
                                throw directiveError(sourceFile, lineNumber, column,
                                                     syntax.prefix() + "include with a body requires a template and"
                                                     + " arguments, as in '" + syntax.prefix()
                                                     + "include Layout(title) {'");
                            }
                            body.add(new BodyNode.IncludeStart(block.group(1), block.group(2).trim()));
                        } else {
                            body.add(new BodyNode.Include(argument));
                        }
                    }
                    case "fragment" -> {
                        final Matcher declaration = FRAGMENT_DECLARATION.matcher(argument);
                        if (!declaration.matches()) {
                            throw directiveError(sourceFile, lineNumber, column,
                                                 syntax.prefix() + "fragment requires a name and parameters, as in '"
                                                 + syntax.prefix() + "fragment row(Task task)'");
                        }
                        body.add(new BodyNode.FragmentStart(declaration.group(1), declaration.group(2).trim()));
                    }
                    case "endfragment" -> {
                        if (!argument.isEmpty()) {
                            throw directiveError(sourceFile, lineNumber, column,
                                                 "unexpected content after " + syntax.prefix() + "endfragment");
                        }
                        body.add(new BodyNode.FragmentEnd());
                    }
                    case "java" -> {
                        if (argument.isEmpty()) {
                            throw directiveError(sourceFile, lineNumber, column,
                                                 syntax.prefix() + "java requires a statement");
                        }
                        body.add(new BodyNode.CodeLine(argument));
                    }
                    default -> body.add(new BodyNode.CodeLine(rest.trim()));
                }
                return;
            }
            // Not a directive (for example a Java annotation or a CSS at-rule): the line is text
            if (LOOKS_LIKE_STATEMENT.matcher(rest.trim()).matches()) {
                warnings.accept(sourceFile + ": line " + lineNumber + ": '" + trimmed + "' looks like Java but '"
                                + syntax.prefix() + directive + "' is not a directive, so it is emitted as text; use '"
                                + syntax.prefix() + "java ' to run it or '" + syntax.escapedPrefix()
                                + "' to silence this warning");
            }
        }
        body.addAll(new TextLineParser(line + "\n", sourceFile, lineNumber, syntax, 0, warnings, element, openTag).run());
    }

    /**
     * The braces of a line of Java, in order, leaving out those in string and character literals and comments.
     */
    private static List<Character> bracesOf(final String code) {
        final List<Character> braces = new ArrayList<>();
        for (int i = 0; i < code.length(); i++) {
            final char c = code.charAt(i);
            if (c == '"' || c == '\'') {
                for (i++; i < code.length() && code.charAt(i) != c; i++) {
                    if (code.charAt(i) == '\\') {
                        i++;
                    }
                }
            } else if (code.startsWith("//", i)) {
                break;
            } else if (code.startsWith("/*", i)) {
                final int end = code.indexOf("*/", i + 2);
                if (end < 0) {
                    break;
                }
                i = end + 1;
            } else if (c == '{' || c == '}') {
                braces.add(c);
            }
        }
        return braces;
    }

    private static JtParseException directiveError(final String sourceFile,
                                                   final int lineNumber,
                                                   final int column,
                                                   final String message) {
        return new JtParseException(
            sourceFile + ": " + message + " at line " + lineNumber + ", column " + column,
            sourceFile, lineNumber, column);
    }

    /** Test-facing entry point: parse a single text line, appending the resulting nodes to {@code body}. */
    static void parseTextLine(final String line, final List<BodyNode> body) {
        parseTextLine(line, body, Syntax.DEFAULT);
    }

    /** Test-facing entry point: parse a single text line with the given syntax. */
    static void parseTextLine(final String line, final List<BodyNode> body, final Syntax syntax) {
        body.addAll(new TextLineParser(line, null, 1, syntax, 0, warning -> { }, null, null).run());
    }

    /**
     * Parses the full content of a {@code .jt} file.
     * <p>
     * No whitespace filter is registered — whitespace is significant in the body and is skipped manually
     * between header tokens.
     */
    private static final class JtFileParser
        extends AbstractParser<ParsedTemplate> {

        private final String sourceFile;
        private final Function<String, Syntax> outSyntax;
        private final Consumer<String> warnings;

        /**
         * A fragment whose end has not been seen.
         */
        private record OpenFragment(String name, int line, int column, int depth, int[] block) {
        }

        private OpenFragment openFragment;

        /**
         * The number of fragments started inside the open fragment, whose ends are to be ignored.
         */
        private int nestedStarts;

        /**
         * Whether every fragment directive so far was parsed, so that a fragment that is open or has nothing to close is
         * real rather than a consequence of a directive that failed.
         */
        private boolean fragmentsKnown = true;

        /**
         * The names of the types generated for the fragments so far.
         */
        private final Set<String> fragmentTypes = new HashSet<>();

        JtFileParser(final String content,
                     final String sourceFile,
                     final Function<String, Syntax> outSyntax,
                     final Consumer<String> warnings) {
            super(content);
            this.sourceFile = sourceFile;
            this.outSyntax = outSyntax;
            this.warnings = warnings;
        }

        @Override
        protected void registerFilters(final build.base.parsing.Scanner s) {
            // No filters — whitespace is significant in the body.
        }

        @Override
        protected ParsedTemplate parse() {
            try {
                return parseTemplate();
            } catch (final JtParseException e) {
                // Whatever follows an error that cannot be recovered from is not worth reporting on
                reportTranslated(e);
                scanner.skipWhile(c -> true);
                return null;
            }
        }

        private ParsedTemplate parseTemplate() {
            String packageName = "";
            final List<String> imports = new ArrayList<>();

            // The header is read in a fixed order: the output type, the options, then the Java declarations
            skip();
            if (!followsKeyword("out")) {
                throw error("missing 'out' declaration (a template must begin with 'out <Type>;')",
                            scanner.getLocation());
            }
            consumeKeyword("out");
            skip();
            final var outLocation = scanner.getLocation();
            final String outType = scanner.consume(QUALIFIED_NAME);
            skip();
            expect(";");
            skip();

            final Syntax defaults;
            try {
                defaults = outSyntax.apply(outType);
            } catch (final IllegalArgumentException e) {
                throw error("invalid syntax declared by " + outType + ": " + e.getMessage(), outLocation);
            }
            final Syntax syntax = parseOptions(defaults);

            if (followsKeyword("package")) {
                consumeKeyword("package");
                skip();
                packageName = scanner.consume(QUALIFIED_NAME);
                skip();
                expect(";");
            }

            skip();
            while (followsKeyword("import")) {
                consumeKeyword("import");
                skip();
                imports.add("import " + scanner.consume(IMPORT_BODY));
                skip();
                expect(";");
                skip();
            }

            if (followsKeyword("option")) {
                throw error("'option' must come directly after the 'out' declaration, before 'package' and 'import'",
                            scanner.getLocation());
            }
            if (!followsKeyword("template")) {
                throw new JtParseException(sourceFile + ": missing template declaration");
            }
            consumeKeyword("template");
            skip();
            final String className = scanner.consume(QUALIFIED_NAME);
            skip();
            final String params = scanner.consumeBalanced('(', ')');

            // The declaration must end with "{", and nothing else may follow it on that line
            scanner.skipWhile(c -> c == ' ' || c == '\t');
            expect("{");
            if (scanner.hasNext()) {
                final var location = scanner.getLocation();
                final String trailing = scanner.consumeUntil("\n");
                if (!trailing.isBlank()) {
                    throw error("unexpected content after '{' in template declaration", location);
                }
                if (scanner.follows('\n')) {
                    scanner.consumeChar();
                }
            }

            // Parse body lines until @end
            final List<BodyNode> body = new ArrayList<>();
            final List<Integer> lines = new ArrayList<>();
            // The blocks opened by directive lines and not yet closed, as {line, column}, innermost first
            final Deque<int[]> open = new ArrayDeque<>();
            // The script or style element the next line starts inside, if any
            String element = null;
            // The tag the next line starts inside, if any: an attribute value may continue on a following line
            String openTag = null;
            // Whether every brace of the body so far was seen, so that block mismatches are real rather than a
            // consequence of a line that failed to parse
            boolean blocksKnown = true;
            boolean ended = false;
            while (scanner.hasNext()) {
                final int lineNumber = scanner.getLocation().getLine();
                final String line = scanner.consumeUntil("\n");
                if (scanner.follows('\n')) {
                    scanner.consumeChar();
                }
                if (line.trim().equals(syntax.prefix() + "end")) {
                    ended = true;
                    break;
                }
                final int first = body.size();
                boolean failed = false;
                try {
                    parseBodyLine(line, body, sourceFile, lineNumber, syntax, warnings, element, openTag);
                } catch (final JtParseException e) {
                    failed = true;
                    reportTranslated(e);
                    // A directive that may have opened or closed a block was dropped, so what is open is now unknown
                    if (line.trim().startsWith(syntax.prefix()) && (line.indexOf('{') >= 0 || line.indexOf('}') >= 0)) {
                        blocksKnown = false;
                    }
                    // Likewise a fragment directive that was dropped leaves it unknown which fragments are open
                    if (line.trim().startsWith(syntax.prefix() + "fragment")
                        || line.trim().startsWith(syntax.prefix() + "endfragment")) {
                        fragmentsKnown = false;
                    }
                }
                // Only text is markup: a directive line is Java, whose strings may well mention tags
                final var added = body.subList(first, body.size());
                final boolean directive = failed
                    ? line.trim().startsWith(syntax.prefix()) && !line.trim().startsWith(syntax.escapedPrefix())
                    : !added.isEmpty() && added.stream()
                    .allMatch(n -> n instanceof BodyNode.CodeLine || n instanceof BodyNode.Include
                        || n instanceof BodyNode.FragmentStart || n instanceof BodyNode.FragmentEnd
                        || n instanceof BodyNode.IncludeStart);
                if (!directive) {
                    final String markup = (openTag == null ? "" : openTag + "\n") + withoutExpressions(line, syntax);
                    element = elementAfter(markup, element);
                    openTag = openTagAfter(markup, element);
                }
                while (lines.size() < body.size()) {
                    lines.add(lineNumber);
                }
                trackFragments(body.subList(first, body.size()), open, blocksKnown, lineNumber,
                    line.indexOf(syntax.prefix()) + 1, className, params, imports);
                if (blocksKnown) {
                    trackBlocks(body.subList(first, body.size()), open, lineNumber,
                        line.indexOf(syntax.prefix()) + 1, syntax);
                }
            }
            if (fragmentsKnown && openFragment != null) {
                reportTranslated(directiveError(sourceFile, openFragment.line(), openFragment.column(),
                    "fragment " + openFragment.name() + " is never closed (missing '" + syntax.prefix()
                    + "endfragment')"));
            }
            // Javac would report these in the generated code, far from the template line that caused them
            final var unclosed = open.descendingIterator();
            while (blocksKnown && unclosed.hasNext()) {
                final int[] opened = unclosed.next();
                reportTranslated(directiveError(sourceFile, opened[0], opened[1],
                    "this block is never closed (missing '" + syntax.prefix() + "}')"));
            }
            if (!ended) {
                throw error("missing @end for template " + className, scanner.getLocation());
            }

            // A file holds exactly one template, so only whitespace may follow @end
            skip();
            if (scanner.hasNext()) {
                throw error("unexpected content after @end (a file may contain only one template)",
                            scanner.getLocation());
            }

            return new ParsedTemplate(packageName, imports, outType, className, params, body, sourceFile, lines);
        }

        /**
         * Checks the fragment directives among {@code nodes}, which came from one line: fragments do not nest, are
         * closed, have names that give distinct nested types, and leave the blocks of the code around them balanced.
         *
         * @param open        the blocks open before the line
         * @param blocksKnown whether {@code open} can be relied on
         * @param params      the parameters of the template
         * @param imports     the imports of the template
         */
        private void trackFragments(final List<BodyNode> nodes,
                                    final Deque<int[]> open,
                                    final boolean blocksKnown,
                                    final int lineNumber,
                                    final int column,
                                    final String className,
                                    final String params,
                                    final List<String> imports) {
            if (!fragmentsKnown) {
                return;
            }
            for (final BodyNode node : nodes) {
                if (node instanceof BodyNode.FragmentStart start) {
                    final String name = start.name();
                    final String type = start.typeName();
                    if (openFragment != null) {
                        reportTranslated(directiveError(sourceFile, lineNumber, column,
                            "fragments cannot be nested (fragment " + openFragment.name() + " is still open)"));
                        // Its end is not a stray end, and it does not close the fragment that is open
                        nestedStarts++;
                        continue;
                    } else if (type.equals(className)) {
                        reportTranslated(directiveError(sourceFile, lineNumber, column,
                            "fragment " + name + " would generate a type with the name of the template"));
                    } else if (hidesAType(type, imports, params + "," + start.params())) {
                        reportTranslated(directiveError(sourceFile, lineNumber, column,
                            "fragment " + name + " would generate the type " + type + ", which hides the type of"
                            + " that name that the template or the fragment uses (imported, from java.lang or"
                            + " named in a parameter list)"));
                    } else if (!fragmentTypes.add(type)) {
                        reportTranslated(directiveError(sourceFile, lineNumber, column,
                            "fragment " + name + " is already declared (fragment names are compared by the type"
                            + " they generate, " + type + ")"));
                    }
                    openFragment = new OpenFragment(name, lineNumber, column, open.size(), open.peek());
                } else if (node instanceof BodyNode.FragmentEnd) {
                    if (nestedStarts > 0) {
                        nestedStarts--;
                    } else if (openFragment == null) {
                        reportTranslated(directiveError(sourceFile, lineNumber, column,
                            "endfragment has no fragment to close"));
                    } else {
                        // A block that was closed and another opened in its place, as in '@} else {', has a different
                        // innermost block even though as many are open
                        if (blocksKnown && (open.size() != openFragment.depth() || open.peek() != openFragment.block())) {
                            reportTranslated(directiveError(sourceFile, openFragment.line(), openFragment.column(),
                                "fragment " + openFragment.name() + " must contain whole blocks: a block opened or"
                                + " closed inside it is not closed or opened inside it"));
                        }
                        openFragment = null;
                    }
                }
            }
        }

        /**
         * Whether a type nested in the template with the name {@code type} would hide a type the template can use by
         * its simple name: one it imports (by name, or on demand from a package of the JDK), one from
         * {@code java.lang}, or one named in the parameter lists {@code params}. A type of the template's own package
         * cannot be seen from here.
         */
        private static boolean hidesAType(final String type, final List<String> imports, final String params) {
            for (final Matcher word = Pattern.compile("[\\w$]+").matcher(params); word.find();) {
                if (word.group().equals(type)) {
                    return true;
                }
            }
            for (final String declaration : imports) {
                final String imported = declaration.substring("import ".length()).trim();
                if (imported.startsWith("static ")) {
                    continue;
                }
                if (imported.endsWith("." + type)) {
                    return true;
                }
                if (imported.endsWith(".*") && existsInJdk(imported.substring(0, imported.length() - 1) + type)) {
                    return true;
                }
            }
            return existsInJdk("java.lang." + type);
        }

        private static boolean existsInJdk(final String name) {
            try {
                Class.forName(name, false, null);
                return true;
            } catch (final ClassNotFoundException e) {
                return false;
            }
        }

        /**
         * The block opened by the body of an include: {@code {line, column}} like any block, with a third element that
         * marks it.
         */
        private static int[] includeBody(final int line, final int column) {
            return new int[]{line, column, 1};
        }

        private static boolean isIncludeBody(final int[] block) {
            return block.length == 3;
        }

        /**
         * Matches the braces of the code lines among {@code nodes} against the blocks still {@code open}, reporting a
         * closing brace that has nothing to close.
         */
        private void trackBlocks(final List<BodyNode> nodes,
                                 final Deque<int[]> open,
                                 final int lineNumber,
                                 final int column,
                                 final Syntax syntax) {
            for (int i = 0; i < nodes.size(); i++) {
                final BodyNode node = nodes.get(i);
                if (node instanceof BodyNode.IncludeStart) {
                    open.push(includeBody(lineNumber, column));
                    continue;
                }
                if (!(node instanceof BodyNode.CodeLine(final String code))) {
                    continue;
                }
                for (final char brace : bracesOf(code)) {
                    if (brace == '{') {
                        open.push(new int[]{lineNumber, column});
                    } else if (open.isEmpty()) {
                        reportTranslated(directiveError(sourceFile, lineNumber, column,
                            "unmatched '}': there is no open block to close"));
                    } else if (isIncludeBody(open.pop())) {
                        // The generator finds the end of the body by this node, not by counting braces
                        if (code.equals("}")) {
                            nodes.set(i, new BodyNode.IncludeEnd());
                        } else {
                            reportTranslated(directiveError(sourceFile, lineNumber, column,
                                "the body of an include must be closed by '" + syntax.prefix() + "}' on a line of"
                                + " its own"));
                        }
                    }
                }
            }
        }

        /**
         * Parses {@code option <name> = "<value>";} declarations, which follow the {@code out} declaration, and returns the resulting syntax.
         */
        private Syntax parseOptions(final Syntax defaults) {
            String prefix = defaults.prefix();
            String interpolation = defaults.interpolation();
            Syntax syntax = defaults;
            final Set<String> seen = new HashSet<>();

            while (followsKeyword("option")) {
                consumeKeyword("option");
                skip();
                final var location = scanner.getLocation();
                final String name = scanner.consume(QUALIFIED_NAME);
                skip();
                expect("=");
                skip();
                final String quoted = scanner.consume(STRING_LITERAL);
                final String value = quoted.substring(1, quoted.length() - 1);
                skip();
                expect(";");
                skip();

                if (!seen.add(name)) {
                    throw error("duplicate option '" + name + "'", location);
                }
                switch (name) {
                    case "prefix" -> prefix = value;
                    case "interpolation" -> interpolation = value;
                    default -> throw error("unknown option '" + name + "' (expected prefix or interpolation)",
                                           location);
                }

                // Validate as each option is read, so an error points at the option that caused it
                try {
                    syntax = defaults.withDelimiters(prefix, interpolation);
                } catch (final IllegalArgumentException e) {
                    throw error("invalid option '" + name + "': " + e.getMessage(), location);
                }
            }

            return syntax;
        }

        private JtParseException error(final String message, final LookaheadReader.Location location) {
            return new JtParseException(
                sourceFile + ": " + message + " at line " + location.getLine() + ", column " + location.getColumn(),
                sourceFile, location.getLine(), location.getColumn());
        }

        /** Skips whitespace (including newlines) between header tokens. */
        private void skip() {
            scanner.skipWhile(c -> Character.isWhitespace((char) c));
        }

        @Override
        protected RuntimeException translate(final ParseException cause) {
            final int line = cause.getLocation().map(LookaheadReader.Location::getLine).orElse(0);
            final int column = cause.getLocation().map(LookaheadReader.Location::getColumn).orElse(0);
            return new JtParseException(sourceFile + ": " + cause.getMessage(), sourceFile, line, column);
        }
    }

    /**
     * Parses a single line of the template body, alternating literal text with {@code #{...}} expression
     * interpolations.
     */
    private static final class TextLineParser
        extends AbstractParser<List<BodyNode>> {

        private final String sourceFile;
        private final int lineNumber;
        private final Syntax syntax;
        private final int columnOffset;
        private final Consumer<String> warnings;

        /**
         * The script or style element the line starts inside, or {@code null}.
         */
        private final String startElement;

        /**
         * The text of the tag the line starts inside, from its {@code <}, or {@code null}. A tag may span lines.
         */
        private final String startTag;

        /**
         * An opener that is not a context: the interpolation lead, a name and a left brace.
         */
        private final Pattern unknownOpener;

        /**
         * @param sourceFile   the file the line came from, or {@code null} when parsing a bare line
         * @param lineNumber   the one-based line number of the line within the file
         * @param syntax       the delimiters of the template
         * @param columnOffset the number of characters removed from the line before it reached this parser (an
         *                     escaped prefix), added to reported columns so they match the source file
         * @param warnings     receives a message for each construct that is accepted but probably not what the author
         *                     meant
         * @param startElement the {@code script} or {@code style} element the line starts inside, or {@code null}
         * @param startTag     the text of the tag the line starts inside, or {@code null}
         */
        TextLineParser(final String input,
                       final String sourceFile,
                       final int lineNumber,
                       final Syntax syntax,
                       final int columnOffset,
                       final Consumer<String> warnings,
                       final String startElement,
                       final String startTag) {
            super(input);
            this.sourceFile = sourceFile;
            this.lineNumber = lineNumber;
            this.syntax = syntax;
            this.columnOffset = columnOffset;
            this.warnings = warnings;
            this.startElement = startElement;
            this.startTag = startTag;
            this.unknownOpener = Pattern.compile(Pattern.quote(syntax.interpolationLead()) + "[a-zA-Z_][\\w]*\\{");
        }

        @Override
        protected void registerFilters(final build.base.parsing.Scanner s) {
            // No filters — whitespace and other characters in literal template text must be preserved verbatim.
        }

        @Override
        protected List<BodyNode> parse() {
            final List<BodyNode> nodes = new ArrayList<>();
            final StringBuilder text = new StringBuilder();
            while (scanner.hasNext()) {
                // The escape is checked first: it contains the opener
                final String escapedContext = followedByEscapedContext();
                final String context = escapedContext == null ? followedByContext() : null;
                if (scanner.follows(syntax.escapedInterpolation())) {
                    scanner.consume(syntax.escapedInterpolation());
                    text.append(syntax.interpolation());
                } else if (escapedContext != null) {
                    scanner.consume(syntax.escapedContextOpener(escapedContext));
                    text.append(syntax.contextOpener(escapedContext));
                } else if (context != null) {
                    if (!text.isEmpty()) {
                        nodes.add(new BodyNode.RawText(text.toString()));
                        text.setLength(0);
                    }
                    final var start = scanner.getLocation();
                    scanner.consume(syntax.interpolationLead() + context);
                    nodes.add(new BodyNode.ContextExpression(syntax.contexts().get(context), consumeExpression(start)));
                } else if (scanner.follows(syntax.interpolation())) {
                    warnIfUnsafeContext(nodes, text);
                    if (!text.isEmpty()) {
                        nodes.add(new BodyNode.RawText(text.toString()));
                        text.setLength(0);
                    }
                    final var start = scanner.getLocation();
                    scanner.consume(syntax.interpolationLead());
                    nodes.add(new BodyNode.Expression(consumeExpression(start)));
                } else if (scanner.follows(unknownOpener)) {
                    // Not a context, so text: CSS such as "#nav{" is legitimate, which is why this is only a warning,
                    // and not one at all in a style element, where such selectors are expected
                    final var start = scanner.getLocation();
                    final String opener = scanner.consume(unknownOpener);
                    if (!"style".equals(elementAfter(markupBefore(nodes, text), startElement))) {
                        warnIfMistyped(opener, start);
                    }
                    text.append(opener);
                } else {
                    text.append(scanner.consumeChar());
                }
            }
            if (!text.isEmpty()) {
                nodes.add(new BodyNode.RawText(text.toString()));
            }
            return nodes;
        }

        /**
         * Warns when a plain interpolation, which only HTML-escapes, is where that cannot make the value safe: the
         * start of a URL attribute (a {@code javascript:} URL gets through), JavaScript or JSON in an attribute, and
         * the body of a script or style element. The advice names a context only if the out type declares it, so out
         * types for other languages are never warned. Writing to any named context silences the warning.
         */
        private void warnIfUnsafeContext(final List<BodyNode> nodes, final StringBuilder text) {
            final String markup = markupBefore(nodes, text);

            final String element = elementAfter(markup, startElement);
            final String tag = openTagAfter(markup, element);
            final String problem;
            final String context;
            final Matcher script;
            final Matcher url;
            if ("script".equals(element)) {
                problem = "inside a <script> element, where HTML escaping does not make it safe";
                context = "js";
            } else if ("style".equals(element)) {
                problem = "inside a <style> element, where HTML escaping does not make it safe";
                context = "css";
            } else if (tag == null) {
                // Not inside a tag, so what looks like an attribute is only text
                return;
            } else if ((script = SCRIPT_ATTRIBUTE_OPEN.matcher(tag)).find()) {
                problem = "inside the " + script.group(1) + " attribute, which holds JavaScript or JSON, where HTML"
                    + " escaping cannot make it safe";
                context = "json";
            } else if ((url = URL_ATTRIBUTE_START.matcher(tag)).find()) {
                problem = "at the start of the " + url.group(1) + " attribute, where HTML escaping does not stop a"
                    + " 'javascript:' URL";
                context = "url";
            } else {
                return;
            }
            if (!syntax.contexts().containsKey(context)) {
                return;
            }
            final var location = scanner.getLocation();
            // JSON and JavaScript values write their own quotes around a string
            final String quotes = context.equals("json") || context.equals("js")
                ? ", and drop any quotes around it, as it writes its own"
                : "";
            warnings.accept(where(location) + "'" + syntax.interpolation() + "' is "
                + problem + "; use '" + syntax.contextOpener(context) + "'" + quotes + " (writing to any named"
                + " context silences this warning)");
        }

        /**
         * The start of a warning: the file, line and column of the location, with a trailing separator.
         */
        private String where(final LookaheadReader.Location location) {
            return (sourceFile == null ? "" : sourceFile + ": ") + "line " + lineNumber + ", column "
                + (location.getColumn() + columnOffset) + ": ";
        }

        /**
         * The markup of the line before the scanner, with each interpolation as {@code ?}, preceded by the tag the
         * line starts inside.
         */
        private String markupBefore(final List<BodyNode> nodes, final StringBuilder text) {
            final StringBuilder before = new StringBuilder();
            if (startTag != null) {
                before.append(startTag).append('\n');
            }
            for (final BodyNode node : nodes) {
                before.append(node instanceof BodyNode.RawText(final String raw) ? raw : "?");
            }
            return before.append(text).toString();
        }

        /**
         * Warns when the opener of a named write is one edit away from a context of the out type, as that is far more
         * likely to be a typo than text that happens to look like an opener.
         */
        private void warnIfMistyped(final String opener, final LookaheadReader.Location location) {
            final String name = opener.substring(syntax.interpolationLead().length(), opener.length() - 1);
            for (final String context : syntax.contexts().keySet()) {
                if (context.length() >= 3 && oneEditApart(name, context)) {
                    warnings.accept(where(location) + "'" + opener
                        + "' is not an output context of this out type, so it is emitted as text; did you"
                        + " mean '" + syntax.contextOpener(context) + "'? The contexts are "
                        + String.join(", ", new TreeSet<>(syntax.contexts().keySet())));
                    return;
                }
            }
        }

        /**
         * Whether the names differ by one inserted, removed or changed character, or two adjacent characters swapped.
         */
        private static boolean oneEditApart(final String a, final String b) {
            if (a.equals(b) || Math.abs(a.length() - b.length()) > 1) {
                return false;
            }
            int i = 0;
            while (i < Math.min(a.length(), b.length()) && a.charAt(i) == b.charAt(i)) {
                i++;
            }
            if (a.length() != b.length()) {
                final String longer = a.length() > b.length() ? a : b;
                final String shorter = a.length() > b.length() ? b : a;
                return longer.substring(i + 1).equals(shorter.substring(i));
            }
            return a.substring(i + 1).equals(b.substring(i + 1))
                || (i + 1 < a.length() && a.charAt(i) == b.charAt(i + 1) && a.charAt(i + 1) == b.charAt(i)
                && a.substring(i + 2).equals(b.substring(i + 2)));
        }

        /**
         * The context whose opener (<code>#url&#123;</code>) is next in the input, or {@code null}.
         */
        private String followedByContext() {
            for (final String context : syntax.contexts().keySet()) {
                if (scanner.follows(syntax.contextOpener(context))) {
                    return context;
                }
            }
            return null;
        }

        /**
         * The context whose escaped opener (<code>##url&#123;</code>) is next in the input, or {@code null}.
         */
        private String followedByEscapedContext() {
            for (final String context : syntax.contexts().keySet()) {
                if (scanner.follows(syntax.escapedContextOpener(context))) {
                    return context;
                }
            }
            return null;
        }

        /**
         * Consumes a {@code {...}} expression and returns its content, honouring Java string and character
         * literals and comments so that a {@code }} inside them does not end the expression.
         */
        private String consumeExpression(final LookaheadReader.Location start) {
            scanner.consume("{");
            final StringBuilder expression = new StringBuilder();
            int depth = 1;
            while (scanner.hasNext()) {
                final char c = scanner.consumeChar();
                if (c == '"' || c == '\'') {
                    expression.append(c);
                    while (scanner.hasNext()) {
                        final char literal = scanner.consumeChar();
                        expression.append(literal);
                        if (literal == '\\' && scanner.hasNext()) {
                            expression.append(scanner.consumeChar());
                        } else if (literal == c) {
                            break;
                        }
                    }
                } else if (c == '/' && scanner.follows('*')) {
                    expression.append(c).append(scanner.consumeChar());
                    while (scanner.hasNext() && !scanner.follows("*/")) {
                        expression.append(scanner.consumeChar());
                    }
                    if (scanner.hasNext()) {
                        expression.append(scanner.consume("*/"));
                    }
                } else if (c == '/' && scanner.follows('/')) {
                    expression.append(c);
                    while (scanner.hasNext() && !scanner.follows('\n')) {
                        expression.append(scanner.consumeChar());
                    }
                } else if (c == '{') {
                    depth++;
                    expression.append(c);
                } else if (c == '}' && --depth == 0) {
                    return expression.toString();
                } else {
                    expression.append(c);
                }
            }
            throw new ParseException(start, "}", "<end of line>");
        }

        @Override
        protected RuntimeException translate(final ParseException cause) {
            // The scanner's location is relative to this single line, so only its column is meaningful
            final int column = cause.getLocation().map(LookaheadReader.Location::getColumn).orElse(1) + columnOffset;
            return new JtParseException(
                (sourceFile == null ? "" : sourceFile + ": ")
                + "unclosed '" + syntax.interpolation() + "' expression at line " + lineNumber + ", column " + column,
                sourceFile, lineNumber, column);
        }
    }
}
