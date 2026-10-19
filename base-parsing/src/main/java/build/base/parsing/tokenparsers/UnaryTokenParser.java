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

import build.base.parsing.Token;
import build.base.parsing.resolvers.NodeResolver;

import java.util.List;
import java.util.function.Function;

/**
 * A {@link TokenParser} for unary (prefix) operator tokens matched by an exact string.
 *
 * @param <N> the node type
 *
 * @author tim.berston
 * @since Nov-2024
 */
public class UnaryTokenParser<N>
    extends StringTokenParser<N> {

    /**
     * Constructs a new {@link UnaryTokenParser}.
     *
     * @param template   the operator string
     * @param precedence the precedence
     * @param runnable   the resolver factory
     */
    public UnaryTokenParser(final String template,
                            final int precedence,
                            final Function<Token<N>, NodeResolver<N>> runnable) {
        super(template, precedence, runnable);
    }

    @Override
    protected boolean isAllowedAfter(final List<Token<N>> tokens) {
        if (tokens.isEmpty()) {
            return true;
        }
        final var last = tokens.getLast().tokenParser();
        return !(last instanceof AtomTokenParser) && !(last instanceof SectionEndTokenParser);
    }
}
