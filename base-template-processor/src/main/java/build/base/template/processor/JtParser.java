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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

final class JtParser {

    private static final Pattern QUALIFIED_NAME =
        Pattern.compile("[a-zA-Z_$][\\w$]*(\\.[a-zA-Z_$][\\w$]*)*");

    // Matches the body of an import statement: optional "static", qualified name, optional ".*"
    private static final Pattern IMPORT_BODY =
        Pattern.compile("(static\\s+)?[a-zA-Z_$][\\w$]*(\\.[a-zA-Z_$][\\w$]*)*(\\.\\*)?");

    private JtParser() {
    }

    static ParsedTemplate parse(final String content, final String sourceFile) {
        return new JtFileParser(content, sourceFile).run();
    }

    private static void parseBodyLine(final String line,
                                      final List<BodyNode> body,
                                      final String sourceFile,
                                      final int lineNumber) {
        final String trimmed = line.trim();
        if (trimmed.equals("@include") || trimmed.startsWith("@include ")) {
            final String expression = trimmed.substring("@include".length()).trim();
            if (expression.isEmpty()) {
                throw new JtParseException(
                    sourceFile + ": @include requires an expression at line " + lineNumber + ", column 1",
                    sourceFile, lineNumber, 1);
            }
            body.add(new BodyNode.Include(expression));
            return;
        }
        if (trimmed.startsWith("@")) {
            body.add(new BodyNode.CodeLine(trimmed.substring(1).trim()));
            return;
        }
        body.addAll(new TextLineParser(line + "\n", sourceFile, lineNumber).run());
    }

    /** Test-facing entry point: parse a single text line, appending the resulting nodes to {@code body}. */
    static void parseTextLine(final String line, final List<BodyNode> body) {
        body.addAll(new TextLineParser(line, null, 1).run());
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

        JtFileParser(final String content, final String sourceFile) {
            super(content);
            this.sourceFile = sourceFile;
        }

        @Override
        protected void registerFilters(final build.base.parsing.Scanner s) {
            // No filters — whitespace is significant in the body.
        }

        @Override
        protected ParsedTemplate parse() {
            String packageName = "";
            final List<String> imports = new ArrayList<>();

            skip();
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

            if (!followsKeyword("template")) {
                throw new JtParseException(sourceFile + ": missing template declaration");
            }
            consumeKeyword("template");
            skip();
            final String outType = scanner.consume(QUALIFIED_NAME);
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
            boolean ended = false;
            while (scanner.hasNext()) {
                final int lineNumber = scanner.getLocation().getLine();
                final String line = scanner.consumeUntil("\n");
                if (scanner.follows('\n')) {
                    scanner.consumeChar();
                }
                if (line.trim().equals("@end")) {
                    ended = true;
                    break;
                }
                parseBodyLine(line, body, sourceFile, lineNumber);
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

            return new ParsedTemplate(packageName, imports, outType, className, params, body);
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

        /**
         * @param sourceFile the file the line came from, or {@code null} when parsing a bare line
         * @param lineNumber the one-based line number of the line within the file
         */
        TextLineParser(final String input, final String sourceFile, final int lineNumber) {
            super(input);
            this.sourceFile = sourceFile;
            this.lineNumber = lineNumber;
        }

        @Override
        protected void registerFilters(final build.base.parsing.Scanner s) {
            // No filters — whitespace and other characters in literal template text must be preserved verbatim.
        }

        @Override
        protected List<BodyNode> parse() {
            final List<BodyNode> nodes = new ArrayList<>();
            while (scanner.hasNext()) {
                if (scanner.follows("#{")) {
                    final var start = scanner.getLocation();
                    scanner.consume("#");
                    nodes.add(new BodyNode.Expression(consumeExpression(start)));
                } else {
                    final String raw = scanner.consumeUntil("#{");
                    if (!raw.isEmpty()) {
                        nodes.add(new BodyNode.RawText(raw));
                    }
                }
            }
            return nodes;
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
            final int column = cause.getLocation().map(LookaheadReader.Location::getColumn).orElse(1);
            return new JtParseException(
                (sourceFile == null ? "" : sourceFile + ": ")
                + "unclosed '#{' expression at line " + lineNumber + ", column " + column,
                sourceFile, lineNumber, column);
        }
    }
}
