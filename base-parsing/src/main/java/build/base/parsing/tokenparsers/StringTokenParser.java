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

import java.util.function.Function;

/**
 * A {@link TokenParser} that matches via an exact string template.
 *
 * @param <N> the node type
 *
 * @author tim.berston
 * @since Nov-2024
 */
public abstract class StringTokenParser<N>
    extends AbstractTokenParser<N> {

    private final String template;

    /**
     * Constructs a new {@link StringTokenParser}.
     *
     * @param template   the string to match
     * @param precedence the precedence
     * @param runnable   the resolver factory
     */
    protected StringTokenParser(final String template,
                                final int precedence,
                                final Function<Token<N>, NodeResolver<N>> runnable) {
        super(precedence, runnable);
        this.template = template;
    }

    @Override
    protected boolean isNextToken(final Scanner scanner) {
        return scanner.follows(this.template);
    }

    @Override
    protected String extractToken(final Scanner scanner) {
        return scanner.consume(this.template);
    }
}
