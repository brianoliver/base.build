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
import build.base.parsing.tokenparsers.TokenParser;

import java.util.Stack;

/**
 * Resolves a parsed node of type {@code N} from a token in the expression stack.
 *
 * @param <N> the node type
 *
 * @author tim.berston
 * @since Nov-2024
 */
public interface NodeResolver<N> {

    /**
     * Returns the {@link TokenParser} that produced the token behind this resolver.
     */
    TokenParser<N> getTokenParser();

    /**
     * Returns the source location of the token behind this resolver.
     */
    LookaheadReader.Location getTokenLocation();

    /**
     * Returns {@code true} once this resolver has been consolidated (i.e. its operands have been
     * popped from the stack and attached).
     */
    boolean isConsolidated();

    /**
     * Pops operand resolvers from the stack and attaches them to this resolver.
     *
     * @param stack the active resolver stack
     */
    void consolidate(Stack<NodeResolver<N>> stack);

    /**
     * Produces the node value for this resolver.
     *
     * @return the resolved node
     */
    N resolve();
}
