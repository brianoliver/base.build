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

import java.util.Stack;
import java.util.function.Function;

/**
 * A {@link NodeResolver} that resolves a unary operation into a node.
 *
 * @param <N> the node type
 *
 * @author tim.berston
 * @since Nov-2024
 */
public class UnaryNodeResolver<N>
    extends AbstractNodeResolver<N> {

    private final Function<N, N> createNodeRunnable;

    private NodeResolver<N> operand;

    /**
     * Constructs a new {@link UnaryNodeResolver}.
     *
     * @param token              the token to resolve
     * @param createNodeRunnable the function that creates the node from the operand
     */
    public UnaryNodeResolver(final Token<N> token, final Function<N, N> createNodeRunnable) {
        super(token);
        this.createNodeRunnable = createNodeRunnable;
    }

    @Override
    public void consolidate(final Stack<NodeResolver<N>> stack) {
        if (this != stack.get(stack.size() - 2)) {
            throw new IllegalStateException("This UnaryNodeResolver should be the second to last element in the stack.");
        }
        if (this.isConsolidated) {
            throw new IllegalStateException("This UnaryNodeResolver has already been consolidated.");
        }
        if (stack.size() < 2) {
            throw new IllegalStateException("There should be at least 2 elements in the stack.");
        }

        this.operand = stack.pop();

        super.consolidate(stack);
    }

    @Override
    public N resolve() {
        if (!this.isConsolidated) {
            throw new IllegalStateException("The NodeResolver has not been consolidated.");
        }
        return this.createNodeRunnable.apply(this.operand.resolve());
    }
}
