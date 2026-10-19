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

import build.base.parsing.Token;

import java.util.function.Function;

/**
 * A {@link NodeResolver} that resolves a leaf (atom) token into a node.
 *
 * @param <N> the node type
 *
 * @author tim.berston
 * @since Nov-2024
 */
public class AtomNodeResolver<N>
    extends AbstractNodeResolver<N> {

    private final Function<Token<N>, N> createNodeRunnable;

    /**
     * Constructs a new {@link AtomNodeResolver}.
     *
     * @param token              the token to resolve
     * @param createNodeRunnable the function that creates the node from the token
     */
    public AtomNodeResolver(final Token<N> token, final Function<Token<N>, N> createNodeRunnable) {
        super(token);
        this.createNodeRunnable = createNodeRunnable;
        this.isConsolidated = true;
    }

    @Override
    public N resolve() {
        return this.createNodeRunnable.apply(this.token);
    }
}
