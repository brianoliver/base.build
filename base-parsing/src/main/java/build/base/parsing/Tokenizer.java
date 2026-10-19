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

import build.base.parsing.tokenparsers.TokenParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts an expression string into a list of {@link Token}s using a configured set of {@link TokenParser}s.
 *
 * @param <N> the node type
 *
 * @author tim.berston
 * @since Nov-2024
 */
public class Tokenizer<N> {

    private final List<TokenParser<N>> tokenParsers;
    private final boolean ignoreWhitespace;

    /**
     * Constructs a {@link Tokenizer} that ignores whitespace.
     *
     * @param tokenParsers the token parsers to apply
     */
    public Tokenizer(final List<TokenParser<N>> tokenParsers) {
        this(tokenParsers, true);
    }

    /**
     * Constructs a {@link Tokenizer}.
     *
     * @param tokenParsers     the token parsers to apply
     * @param ignoreWhitespace whether to skip whitespace between tokens
     */
    public Tokenizer(final List<TokenParser<N>> tokenParsers, final boolean ignoreWhitespace) {
        this.tokenParsers = tokenParsers;
        this.ignoreWhitespace = ignoreWhitespace;
    }

    /**
     * Tokenizes the expression string.
     *
     * @param expression the expression to tokenize
     * @return the list of tokens
     * @throws ExpressionParserException if an unknown token is encountered
     */
    public List<Token<N>> tokenize(final String expression) {
        final var scanner = new Scanner(expression);
        if (this.ignoreWhitespace) {
            scanner.register(Filter.WHITESPACE);
        }
        return tokenize(scanner, true);
    }

    /**
     * Tokenizes from an already-positioned {@link Scanner}, stopping when no token parser matches rather than
     * throwing.  The scanner is used as-is — no filters are registered on it.
     *
     * @param scanner the scanner to read from
     * @return the list of tokens parsed before the first unrecognised position
     */
    public List<Token<N>> tokenize(final Scanner scanner) {
        return tokenize(scanner, false);
    }

    private List<Token<N>> tokenize(final Scanner scanner, final boolean throwOnUnknown) {
        final var tokens = new ArrayList<Token<N>>();

        while (scanner.hasNext()) {
            var tokenProcessed = false;
            for (final var tokenParser : this.tokenParsers) {
                final var token = tokenParser.attemptExtractToken(scanner, tokens);
                if (token.isPresent()) {
                    tokens.add(token.get());
                    tokenProcessed = true;
                    break;
                }
            }
            if (!tokenProcessed) {
                if (throwOnUnknown) {
                    throw new ExpressionParserException(
                        "The expression contains an unknown or unexpected token at " + scanner.getLocation());
                }
                break;
            }
        }

        return tokens;
    }
}
