package build.base.expression.parser;

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

import build.base.expression.ast.AssignmentNode;
import build.base.expression.ast.BinaryOpNode;
import build.base.expression.ast.CompositeNode;
import build.base.expression.ast.ConditionalNode;
import build.base.expression.ast.FunctionCallNode;
import build.base.expression.ast.IdentifierNode;
import build.base.expression.ast.LambdaNode;
import build.base.expression.ast.LiteralNode;
import build.base.expression.ast.MethodCallNode;
import build.base.expression.ast.Node;
import build.base.expression.ast.PropertyAccessNode;
import build.base.expression.ast.SemicolonNode;
import build.base.expression.ast.UnaryOpNode;
import build.base.parsing.AbstractParser;
import build.base.parsing.Evaluator;
import build.base.parsing.ParseException;
import build.base.parsing.Scanner;
import jakarta.el.ELException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Recursive-descent parser for Jakarta EL expressions.
 * <p>
 * Entry points:
 * <ul>
 *   <li>{@link #parseFull(String)} — parses a complete {@code ${...}} or {@code #{...}} expression string</li>
 *   <li>{@link #parseComposite(String)} — parses a string that may contain embedded expressions</li>
 *   <li>{@link #parseExpression(String)} — parses a raw expression (no {@code ${}} wrapper)</li>
 * </ul>
 */
public final class ELParser
    extends AbstractParser<Node> {

    // Evaluators for lexical token recognition
    private static final Evaluator<String> IDENTIFIER =
        Evaluator.create(Pattern.compile("[a-zA-Z_$][a-zA-Z0-9_$]*"), s -> s, "identifier");

    private static final Evaluator<Long> INTEGER =
        Evaluator.create(Pattern.compile("[0-9]+[lL]?"), s -> {
            final var trimmed = (s.endsWith("l") || s.endsWith("L")) ? s.substring(0, s.length() - 1) : s;
            return Long.parseLong(trimmed);
        }, "integer literal");

    private static final Evaluator<Double> FLOAT =
        Evaluator.create(
            Pattern.compile("[0-9]+\\.[0-9]*(?:[eE][+-]?[0-9]+)?[fFdD]?|[0-9]+[eE][+-]?[0-9]+[fFdD]?"),
            s -> Double.parseDouble(s.replaceAll("[fFdD]$", "")), "float literal");

    private static final Evaluator<String> STRING =
        Evaluator.create(Pattern.compile("'(?:[^'\\\\]|\\\\.)*'|\"(?:[^\"\\\\]|\\\\.)*\""),
            s -> unescape(s.substring(1, s.length() - 1), s.charAt(0)), "string literal");

    private enum Mode {EXPRESSION, FULL_WRAPPED}

    private final String originalInput;
    private final Mode mode;

    private ELParser(final String input, final Mode mode) {
        super(input);
        this.originalInput = input;
        this.mode = mode;
    }

    @Override
    protected Node parse() {
        return switch (mode) {
            case FULL_WRAPPED -> {
                scanner.consume(2);  // ${ or #{
                final Node node = parseSemicolon();
                expect("}");
                yield node;
            }
            default -> parseSemicolon();
        };
    }

    @Override
    protected RuntimeException translate(final ParseException cause) {
        return new ELException("Failed to parse expression: " + originalInput, cause);
    }

    // -------------------------------------------------------------------------
    // Public entry points
    // -------------------------------------------------------------------------

    /**
     * Parses a full EL expression string like {@code "${name}"} or {@code "#{name}"}.
     * Returns a {@link LiteralNode} when the input contains no expression delimiters.
     * Throws {@link ELException} if a wrapped expression is malformed.
     */
    public static Node parseFull(final String expression) {
        if (expression.startsWith("${") || expression.startsWith("#{")) {
            return new ELParser(expression, Mode.FULL_WRAPPED).run();
        }
        return new LiteralNode(expression);
    }

    /**
     * Parses a composite string that may contain embedded {@code ${}} or {@code #{}} expressions.
     * Returns a {@link CompositeNode} if mixed content, or a single node if pure.
     */
    public static Node parseComposite(final String template) {
        final var parts = new ArrayList<Node>();
        final var sb = new StringBuilder();
        try (var s = new Scanner(template)) {
            // No filters — preserve whitespace in literal text.
            while (s.hasNext()) {
                if (s.follows("${") || s.follows("#{")) {
                    if (sb.length() > 0) {
                        parts.add(new LiteralNode(sb.toString()));
                        sb.setLength(0);
                    }
                    s.consume(1);  // $ or #
                    parts.add(parseExpression(s.consumeBalanced('{', '}')));
                } else {
                    sb.append(s.consumeChar());
                }
            }
        } catch (final ELException e) {
            throw e;
        } catch (final Exception e) {
            throw new ELException("Unclosed expression in template: " + template, e);
        }
        if (sb.length() > 0) {
            parts.add(new LiteralNode(sb.toString()));
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        return new CompositeNode(List.copyOf(parts));
    }

    /**
     * Parses a raw expression string (without {@code ${}} wrapping).
     */
    public static Node parseExpression(final String expression) {
        return new ELParser(expression, Mode.EXPRESSION).run();
    }

    // -------------------------------------------------------------------------
    // Grammar rules (lowest to highest precedence)
    // -------------------------------------------------------------------------

    private Node parseSemicolon() {
        var left = parseAssignment();
        while (scanner.follows(";")) {
            scanner.consume(";");
            left = new SemicolonNode(left, parseAssignment());
        }
        return left;
    }

    private Node parseAssignment() {
        final var left = parseConditional();
        // assignment: = but not ==
        if (scanner.follows("=") && !scanner.follows("==")) {
            scanner.consume("=");
            return new AssignmentNode(left, parseAssignment());
        }
        return left;
    }

    private Node parseConditional() {
        final var condition = parseOr();
        if (scanner.follows("?")) {
            scanner.consume("?");
            final var thenExpr = parseAssignment();
            if (scanner.follows(":")) {
                scanner.consume(":");
            }
            final var elseExpr = parseAssignment();
            return new ConditionalNode(condition, thenExpr, elseExpr);
        }
        return condition;
    }

    private Node parseOr() {
        var left = parseAnd();
        while (scanner.follows("||") || followsKeyword("or")) {
            if (scanner.follows("||")) {
                scanner.consume("||");
            } else {
                consumeKeyword("or");
            }
            left = new BinaryOpNode(left, "||", parseAnd());
        }
        return left;
    }

    private Node parseAnd() {
        var left = parseNot();
        while (scanner.follows("&&") || followsKeyword("and")) {
            if (scanner.follows("&&")) {
                scanner.consume("&&");
            } else {
                consumeKeyword("and");
            }
            left = new BinaryOpNode(left, "&&", parseNot());
        }
        return left;
    }

    private Node parseNot() {
        // ! but not !=
        if (scanner.follows("!") && !scanner.follows("!=")) {
            scanner.consume("!");
            return new UnaryOpNode("!", parseNot());
        }
        if (followsKeyword("not")) {
            consumeKeyword("not");
            return new UnaryOpNode("!", parseNot());
        }
        return parseComparison();
    }

    private Node parseComparison() {
        final var left = parseAddSub();
        // Multi-char operators must be checked before single-char prefixes
        if (scanner.follows("==")) {
            scanner.consume("==");
            return new BinaryOpNode(left, "==", parseAddSub());
        }
        if (scanner.follows("!=")) {
            scanner.consume("!=");
            return new BinaryOpNode(left, "!=", parseAddSub());
        }
        if (scanner.follows("<=")) {
            scanner.consume("<=");
            return new BinaryOpNode(left, "<=", parseAddSub());
        }
        if (scanner.follows(">=")) {
            scanner.consume(">=");
            return new BinaryOpNode(left, ">=", parseAddSub());
        }
        if (scanner.follows("<")) {
            scanner.consume("<");
            return new BinaryOpNode(left, "<", parseAddSub());
        }
        if (scanner.follows(">")) {
            scanner.consume(">");
            return new BinaryOpNode(left, ">", parseAddSub());
        }
        if (followsKeyword("eq")) {
            consumeKeyword("eq");
            return new BinaryOpNode(left, "==", parseAddSub());
        }
        if (followsKeyword("ne")) {
            consumeKeyword("ne");
            return new BinaryOpNode(left, "!=", parseAddSub());
        }
        if (followsKeyword("le")) {
            consumeKeyword("le");
            return new BinaryOpNode(left, "<=", parseAddSub());
        }
        if (followsKeyword("ge")) {
            consumeKeyword("ge");
            return new BinaryOpNode(left, ">=", parseAddSub());
        }
        if (followsKeyword("lt")) {
            consumeKeyword("lt");
            return new BinaryOpNode(left, "<", parseAddSub());
        }
        if (followsKeyword("gt")) {
            consumeKeyword("gt");
            return new BinaryOpNode(left, ">", parseAddSub());
        }
        return left;
    }

    private Node parseAddSub() {
        var left = parseMulDiv();
        while (true) {
            if (scanner.follows("+=")) {
                // string concatenation operator
                scanner.consume("+=");
                left = new BinaryOpNode(left, "+=", parseMulDiv());
            } else if (scanner.follows("+")) {
                scanner.consume("+");
                left = new BinaryOpNode(left, "+", parseMulDiv());
            } else if (scanner.follows("-") && !scanner.follows("->")) {
                scanner.consume("-");
                left = new BinaryOpNode(left, "-", parseMulDiv());
            } else {
                break;
            }
        }
        return left;
    }

    private Node parseMulDiv() {
        var left = parseUnary();
        while (true) {
            if (scanner.follows("*")) {
                scanner.consume("*");
                left = new BinaryOpNode(left, "*", parseUnary());
            } else if (scanner.follows("/")) {
                scanner.consume("/");
                left = new BinaryOpNode(left, "/", parseUnary());
            } else if (followsKeyword("div")) {
                consumeKeyword("div");
                left = new BinaryOpNode(left, "/", parseUnary());
            } else if (scanner.follows("%")) {
                scanner.consume("%");
                left = new BinaryOpNode(left, "%", parseUnary());
            } else if (followsKeyword("mod")) {
                consumeKeyword("mod");
                left = new BinaryOpNode(left, "%", parseUnary());
            } else {
                break;
            }
        }
        return left;
    }

    private Node parseUnary() {
        if (scanner.follows("-") && !scanner.follows("->")) {
            scanner.consume("-");
            return new UnaryOpNode("-", parseUnary());
        }
        if (followsKeyword("empty")) {
            consumeKeyword("empty");
            return new UnaryOpNode("empty", parseUnary());
        }
        return parsePostfix();
    }

    private Node parsePostfix() {
        var node = parsePrimary();
        while (true) {
            if (scanner.follows(".") && !scanner.follows("..")) {
                scanner.consume(".");
                if (scanner.follows(IDENTIFIER)) {
                    final var prop = scanner.consume(IDENTIFIER);
                    if (scanner.follows("(")) {
                        scanner.consume("(");
                        final var args = parseArgList();
                        scanner.consume(")");
                        node = new MethodCallNode(node, prop, args);
                    } else {
                        node = new PropertyAccessNode(node, new LiteralNode(prop), false);
                    }
                } else {
                    throw new ELException("Expected identifier after '.'");
                }
            } else if (scanner.follows("[")) {
                scanner.consume("[");
                final var key = parseAssignment();
                scanner.consume("]");
                node = new PropertyAccessNode(node, key, true);
            } else {
                break;
            }
        }
        return node;
    }

    private Node parsePrimary() {
        // Parenthesised expression or lambda with no/multiple params
        if (scanner.follows("(")) {
            scanner.consume("(");
            if (scanner.follows(")")) {
                // Empty param list: () -> expr
                scanner.consume(")");
                if (scanner.follows("->")) {
                    scanner.consume("->");
                    return new LambdaNode(List.of(), parseAssignment());
                }
                throw new ELException("Unexpected '()' in expression");
            }
            // Multi-param lambda: (x, y, ...) -> body
            // In EL, (ident, is only ever valid as a lambda parameter list
            if (scanner.follows(Pattern.compile("[a-zA-Z_$][a-zA-Z0-9_$]*\\s*,"))) {
                final var params = new ArrayList<String>();
                params.add(scanner.consume(IDENTIFIER));
                while (scanner.follows(",")) {
                    scanner.consume(",");
                    if (!scanner.follows(IDENTIFIER)) {
                        throw new ELException("Expected identifier in lambda parameter list");
                    }
                    params.add(scanner.consume(IDENTIFIER));
                }
                scanner.consume(")");
                if (!scanner.follows("->")) {
                    throw new ELException("Expected '->' after lambda parameter list");
                }
                scanner.consume("->");
                return new LambdaNode(List.copyOf(params), parseAssignment());
            }
            final var inner = parseSemicolon();
            scanner.consume(")");
            // Check for single-param lambda: (x) -> expr
            if (inner instanceof IdentifierNode id && scanner.follows("->")) {
                scanner.consume("->");
                return new LambdaNode(List.of(id.name()), parseAssignment());
            }
            return inner;
        }

        // Identifier — covers keywords, bare names, unary lambda, function calls
        if (scanner.follows(IDENTIFIER)) {
            final var ident = scanner.consume(IDENTIFIER);

            // Single-param unparenthesised lambda: x -> expr
            if (scanner.follows("->")) {
                scanner.consume("->");
                return new LambdaNode(List.of(ident), parseAssignment());
            }

            // namespace:function(args) — namespace-qualified function call
            // Only matches if : is followed by identifier( (not ternary : which has no '(')
            if (scanner.follows(Pattern.compile(":[a-zA-Z_$][a-zA-Z0-9_$]*\\s*\\("))) {
                scanner.consume(":");
                final var local = scanner.consume(IDENTIFIER);
                scanner.consume("(");
                final var args = parseArgList();
                scanner.consume(")");
                return new FunctionCallNode(ident, local, args);
            }

            // Keyword literals
            return switch (ident) {
                case "true" -> new LiteralNode(Boolean.TRUE);
                case "false" -> new LiteralNode(Boolean.FALSE);
                case "null" -> new LiteralNode(null);
                default -> new IdentifierNode(ident);
            };
        }

        // Float before integer — both start with digits but float contains '.'
        if (scanner.follows(FLOAT)) {
            return new LiteralNode(scanner.consume(FLOAT));
        }
        if (scanner.follows(INTEGER)) {
            return new LiteralNode(scanner.consume(INTEGER));
        }

        // String literals
        if (scanner.follows(STRING)) {
            return new LiteralNode(scanner.consume(STRING));
        }

        throw new ELException("Unexpected token at: " + scanner.getLocation());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private List<Node> parseArgList() {
        final var args = new ArrayList<Node>();
        if (!scanner.follows(")")) {
            args.add(parseAssignment());
            while (scanner.follows(",")) {
                scanner.consume(",");
                args.add(parseAssignment());
            }
        }
        return List.copyOf(args);
    }

    private static String unescape(final String raw, final char quote) {
        // Per EL spec section 1.2, only \<quote> and \\ are defined escape sequences in
        // string literals. \n, \t, \r, etc. are NOT expanded — they are passed through
        // as the literal two-character sequences. This is intentional.
        return raw.replace("\\" + quote, String.valueOf(quote)).replace("\\\\", "\\");
    }
}
