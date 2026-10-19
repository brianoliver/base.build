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
import build.base.parsing.tokenparsers.TemplateAtomTokenParser;
import build.base.parsing.tokenparsers.TokenParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Parses a template string into an ordered list of nodes of type {@code N}.
 * <p>
 * Unlike {@link ExpressionParser}, template parsing is purely linear: whitespace is preserved,
 * there is no operator precedence, and every token is resolved independently into a node.
 * <p>
 * An optional {@code tryMerge} function can be supplied to consolidate adjacent nodes — for example,
 * combining two adjacent string-literal nodes into one. The function receives the previous node and
 * the current node and returns the merged result, or {@link Optional#empty()} if the two nodes
 * should not be merged.
 *
 * @param <N> the node type
 *
 * @author tim.berston
 * @since Mar-2025
 */
public class TemplateParser<N>
    implements Rule<List<N>> {

    private final List<TokenParser<N>> tokenParsers;
    private final BiFunction<N, N, Optional<N>> tryMerge;

    /**
     * Constructs a new {@link TemplateParser} with no adjacent-node merging.
     */
    public TemplateParser() {
        this(null);
    }

    /**
     * Constructs a new {@link TemplateParser}.
     *
     * @param tryMerge a function that attempts to merge two adjacent nodes into one, or
     *                 {@code null} to disable merging
     */
    public TemplateParser(final BiFunction<N, N, Optional<N>> tryMerge) {
        this.tokenParsers = new ArrayList<>();
        this.tryMerge = tryMerge;
    }

    /**
     * Defines an atom token parser for this template grammar.
     *
     * @param pattern        the regex pattern to match
     * @param implementation the function that creates a node from the matched token
     */
    public void defineAtom(final String pattern, final Function<Token<N>, N> implementation) {
        this.tokenParsers.add(new TemplateAtomTokenParser<>(pattern, 0, token -> new AtomNodeResolver<>(token, implementation)));
    }

    /**
     * Parses the template string and returns an ordered list of nodes.
     * Returns an empty list if the expression is {@code null} or blank.
     *
     * @param expression the template string to parse
     * @return the list of parsed nodes
     * @throws ExpressionParserException if the template contains an unrecognised token
     */
    public List<N> parse(final String expression) {
        if (expression == null || expression.isBlank()) {
            return List.of();
        }
        final var scanner = new Scanner(expression);
        final List<N> result = tryParse(scanner).orElse(List.of());
        if (scanner.hasNext()) {
            throw new ExpressionParserException(
                "The expression contains an unknown or unexpected token at " + scanner.getLocation());
        }
        return result;
    }

    /**
     * Parses as much of the template as possible from the current position of the {@link Scanner}, stopping
     * when no token matches rather than throwing.  Returns {@link Optional#empty()} if no tokens were recognised.
     * <p>
     * The scanner is used as-is — no filters are registered on it.
     *
     * @param scanner the scanner to read from
     * @return the parsed node list, or {@link Optional#empty()} if nothing was recognised
     * @throws ExpressionParserException if the template is malformed
     */
    @Override
    public Optional<List<N>> tryParse(final Scanner scanner) {
        final var tokens = new Tokenizer<>(this.tokenParsers, false).tokenize(scanner);
        if (tokens.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(resolveNodes(tokens));
        } catch (final ExpressionParserException e) {
            throw e;
        } catch (final RuntimeException e) {
            throw new ExpressionParserException("There was an error parsing the expression", e);
        }
    }

    private List<N> resolveNodes(final List<Token<N>> tokens) {
        final var nodes = new ArrayList<N>();
        for (final var token : tokens) {
            final var node = token.tokenParser().createNodeResolver(token).resolve();
            if (this.tryMerge != null && !nodes.isEmpty()) {
                final var merged = this.tryMerge.apply(nodes.getLast(), node);
                if (merged.isPresent()) {
                    nodes.set(nodes.size() - 1, merged.get());
                    continue;
                }
            }
            nodes.add(node);
        }
        return nodes;
    }
}
