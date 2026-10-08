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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

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

    private JtParser() {
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
                                      final Consumer<String> warnings) {
        final String trimmed = line.trim();
        final int column = line.indexOf(syntax.prefix()) + 1;

        // A doubled prefix is an escape: the line is text with one prefix removed
        if (trimmed.startsWith(syntax.escapedPrefix())) {
            final String unescaped = line.substring(0, column - 1) + line.substring(column - 1 + syntax.prefix().length());
            body.addAll(new TextLineParser(unescaped + "\n", sourceFile, lineNumber, syntax,
                                           syntax.prefix().length()).run());
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
                        body.add(new BodyNode.Include(argument));
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
        body.addAll(new TextLineParser(line + "\n", sourceFile, lineNumber, syntax, 0).run());
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
        body.addAll(new TextLineParser(line, null, 1, syntax, 0).run());
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
                try {
                    parseBodyLine(line, body, sourceFile, lineNumber, syntax, warnings);
                } catch (final JtParseException e) {
                    reportTranslated(e);
                    // A directive that may have opened or closed a block was dropped, so what is open is now unknown
                    if (line.trim().startsWith(syntax.prefix()) && (line.indexOf('{') >= 0 || line.indexOf('}') >= 0)) {
                        blocksKnown = false;
                    }
                }
                while (lines.size() < body.size()) {
                    lines.add(lineNumber);
                }
                if (blocksKnown) {
                    trackBlocks(body.subList(first, body.size()), open, lineNumber,
                        line.indexOf(syntax.prefix()) + 1, syntax);
                }
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
         * Matches the braces of the code lines among {@code nodes} against the blocks still {@code open}, reporting a
         * closing brace that has nothing to close.
         */
        private void trackBlocks(final List<BodyNode> nodes,
                                 final Deque<int[]> open,
                                 final int lineNumber,
                                 final int column,
                                 final Syntax syntax) {
            for (final BodyNode node : nodes) {
                if (!(node instanceof BodyNode.CodeLine(final String code))) {
                    continue;
                }
                for (final char brace : bracesOf(code)) {
                    if (brace == '{') {
                        open.push(new int[]{lineNumber, column});
                    } else if (open.isEmpty()) {
                        reportTranslated(directiveError(sourceFile, lineNumber, column,
                            "unmatched '}': there is no open block to close"));
                    } else {
                        open.pop();
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

        /**
         * @param sourceFile   the file the line came from, or {@code null} when parsing a bare line
         * @param lineNumber   the one-based line number of the line within the file
         * @param syntax       the delimiters of the template
         * @param columnOffset the number of characters removed from the line before it reached this parser (an
         *                     escaped prefix), added to reported columns so they match the source file
         */
        TextLineParser(final String input,
                       final String sourceFile,
                       final int lineNumber,
                       final Syntax syntax,
                       final int columnOffset) {
            super(input);
            this.sourceFile = sourceFile;
            this.lineNumber = lineNumber;
            this.syntax = syntax;
            this.columnOffset = columnOffset;
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
                    if (!text.isEmpty()) {
                        nodes.add(new BodyNode.RawText(text.toString()));
                        text.setLength(0);
                    }
                    final var start = scanner.getLocation();
                    scanner.consume(syntax.interpolationLead());
                    nodes.add(new BodyNode.Expression(consumeExpression(start)));
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
