package build.base.parsing.resolvers;

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

import build.base.io.LookaheadReader;
import build.base.parsing.Token;
import build.base.parsing.tokenparsers.TokenParser;

import java.util.Stack;

/**
 * Base implementation of {@link NodeResolver}.
 *
 * @param <N> the node type
 *
 * @author tim.berston
 * @since Nov-2024
 */
public abstract class AbstractNodeResolver<N>
    implements NodeResolver<N> {

    protected final Token<N> token;
    protected boolean isConsolidated;

    /**
     * Constructs a new {@link AbstractNodeResolver}.
     *
     * @param token the token to resolve
     */
    protected AbstractNodeResolver(final Token<N> token) {
        this.token = token;
        this.isConsolidated = false;
    }

    @Override
    public TokenParser<N> getTokenParser() {
        return this.token.tokenParser();
    }

    @Override
    public LookaheadReader.Location getTokenLocation() {
        return this.token.location();
    }

    @Override
    public boolean isConsolidated() {
        return this.isConsolidated;
    }

    @Override
    public void consolidate(final Stack<NodeResolver<N>> stack) {
        this.isConsolidated = true;
    }
}
