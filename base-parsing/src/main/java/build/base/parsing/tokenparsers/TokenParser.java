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

/**
 * Attempts to extract a {@link Token} from a {@link Scanner} and produce the corresponding {@link NodeResolver}.
 *
 * @param <N> the node type
 *
 * @author tim.berston
 * @since Nov-2024
 */
public interface TokenParser<N> {

    /**
     * Returns the precedence of this token parser. Higher values mean lower precedence (evaluated later).
     */
    int getPrecedence();

    /**
     * Attempts to extract the next token from the scanner.
     *
     * @param scanner      the scanner positioned at the current parse location
     * @param parsedTokens the tokens extracted so far (used for context-sensitive matching)
     * @return the extracted token, or empty if this parser does not match
     */
    Optional<Token<N>> attemptExtractToken(Scanner scanner, List<Token<N>> parsedTokens);

    /**
     * Creates a {@link NodeResolver} for the given token.
     *
     * @param token the extracted token
     * @return the resolver
     */
    NodeResolver<N> createNodeResolver(Token<N> token);
}
