package build.base.parsing.tokenparsers;

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

import build.base.parsing.Scanner;
import build.base.parsing.Token;
import build.base.parsing.resolvers.NodeResolver;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Base implementation of {@link TokenParser}.
 *
 * @param <N> the node type
 *
 * @author tim.berston
 * @since Nov-2024
 */
public abstract class AbstractTokenParser<N>
    implements TokenParser<N> {

    private final int precedence;
    private final Function<Token<N>, NodeResolver<N>> resolverFactory;

    /**
     * Constructs a new {@link AbstractTokenParser}.
     *
     * @param precedence the precedence (higher = lower operator precedence)
     * @param resolverFactory   the function that creates the {@link NodeResolver} for a matched token
     */
    protected AbstractTokenParser(final int precedence, final Function<Token<N>, NodeResolver<N>> resolverFactory) {
        this.precedence = precedence;
        this.resolverFactory = resolverFactory;
    }

    @Override
    public int getPrecedence() {
        return this.precedence;
    }

    @Override
    public Optional<Token<N>> attemptExtractToken(final Scanner scanner, final List<Token<N>> parsedTokens) {
        if (isNextToken(scanner) && isAllowedAfter(parsedTokens)) {
            final var token = new Token<N>(this, scanner.getLocation(), extractToken(scanner));
            return Optional.of(token);
        }
        return Optional.empty();
    }

    @Override
    public NodeResolver<N> createNodeResolver(final Token<N> token) {
        return this.resolverFactory.apply(token);
    }

    /**
     * Returns {@code true} if the scanner's current position matches this token.
     */
    protected abstract boolean isNextToken(Scanner scanner);

    /**
     * Extracts and consumes the token text from the scanner.
     */
    protected abstract String extractToken(Scanner scanner);

    /**
     * Returns {@code true} if this token is valid given the tokens parsed so far.
     */
    protected abstract boolean isAllowedAfter(List<Token<N>> tokens);
}
