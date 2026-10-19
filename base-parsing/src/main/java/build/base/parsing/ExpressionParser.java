package build.base.parsing;

/*-
 * #%L
 * base.build Parsing
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

import build.base.parsing.resolvers.AtomNodeResolver;
import build.base.parsing.resolvers.BinaryNodeResolver;
import build.base.parsing.resolvers.SectionBeginNodeResolver;
import build.base.parsing.resolvers.SectionEndNodeResolver;
import build.base.parsing.resolvers.UnaryNodeResolver;
import build.base.parsing.tokenparsers.AtomTokenParser;
import build.base.parsing.tokenparsers.BinaryTokenParser;
import build.base.parsing.tokenparsers.SectionBeginTokenParser;
import build.base.parsing.tokenparsers.SectionEndTokenParser;
import build.base.parsing.tokenparsers.TokenParser;
import build.base.parsing.tokenparsers.UnaryTokenParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Parses an expression string into a node of type {@code N} using a configured set of token parsers.
 *
 * @param <N> the node type
 *
 * @author tim.berston
 * @since Nov-2024
 */
public class ExpressionParser<N>
    implements Rule<N> {

    private final List<TokenParser<N>> tokenParsers;

    /**
     * Constructs a new {@link ExpressionParser}.
     */
    public ExpressionParser() {
        this.tokenParsers = new ArrayList<>();
    }

    /**
     * Defines an atom (leaf) token parser.
     *
     * @param pattern        the regex pattern to match
     * @param implementation the function that creates a node from the matched token
     */
    public void defineAtom(final String pattern, final Function<Token<N>, N> implementation) {
        this.tokenParsers.add(new AtomTokenParser<>(pattern, 0, token -> new AtomNodeResolver<>(token, implementation)));
    }

    /**
     * Defines a unary (prefix) token parser.
     *
     * @param template       the exact string to match
     * @param priority       operator precedence
     * @param implementation the function that creates a node given its operand
     */
    public void defineUnary(final String template, final int priority, final Function<N, N> implementation) {
        this.tokenParsers.add(new UnaryTokenParser<>(template, priority, token -> new UnaryNodeResolver<>(token, implementation)));
    }

    /**
     * Defines a binary (infix) token parser.
     *
     * @param template       the exact string to match
     * @param priority       operator precedence
     * @param implementation the function that creates a node given its two operands
     */
    public void defineBinary(final String template, final int priority, final BiFunction<N, N, N> implementation) {
        this.tokenParsers.add(new BinaryTokenParser<>(template, priority, token -> new BinaryNodeResolver<>(token, implementation)));
    }

    /**
     * Defines a paired section (e.g. parentheses).
     *
     * @param openTemplate  the opening delimiter string
     * @param closeTemplate the closing delimiter string
     */
    public void defineSection(final String openTemplate, final String closeTemplate) {
        this.tokenParsers.add(new SectionBeginTokenParser<>(openTemplate, 0, SectionBeginNodeResolver::new));
        this.tokenParsers.add(new SectionEndTokenParser<>(closeTemplate, 9999, SectionEndNodeResolver::new));
    }

    /**
     * Parses the expression string and returns the root node, or {@code null} if the expression is blank.
     *
     * @param expression the expression to parse
     * @return the parsed node, or {@code null} for a blank expression
     * @throws ExpressionParserException if the expression is malformed
     */
    public N parse(final String expression) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        final var scanner = new Scanner(expression);
        scanner.register(Filter.WHITESPACE);
        final N result = tryParse(scanner).orElse(null);
        if (scanner.hasNext()) {
            throw new ExpressionParserException(
                "The expression contains an unknown or unexpected token at " + scanner.getLocation());
        }
        return result;
    }

    /**
     * Parses as much of the expression as possible from the current position of the {@link Scanner}, stopping
     * when no token matches rather than throwing.  Returns {@link Optional#empty()} if no tokens were recognised.
     * <p>
     * The scanner is used as-is — filters (e.g. whitespace) must be registered by the caller before invoking.
     *
     * @param scanner the scanner to read from
     * @return the parsed node, or {@link Optional#empty()} if nothing was recognised
     * @throws ExpressionParserException if tokens were partially parsed but the expression is malformed
     */
    @Override
    public Optional<N> tryParse(final Scanner scanner) {
        final var tokens = new Tokenizer<>(this.tokenParsers).tokenize(scanner);
        if (tokens.isEmpty()) {
            return Optional.empty();
        }
        final var stackManager = new ExpressionStackManager<N>();
        try {
            for (final var token : tokens) {
                stackManager.push(token.tokenParser().createNodeResolver(token));
            }
            return Optional.of(stackManager.getExpressionRoot().resolve());
        } catch (final ExpressionParserException e) {
            throw e;
        } catch (final RuntimeException e) {
            throw new ExpressionParserException("There was an error parsing the expression", e);
        }
    }
}
