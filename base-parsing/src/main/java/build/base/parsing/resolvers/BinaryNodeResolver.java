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
import java.util.function.BiFunction;

/**
 * A {@link NodeResolver} that resolves a binary operation into a node.
 *
 * @param <N> the node type
 *
 * @author tim.berston
 * @since Nov-2024
 */
public class BinaryNodeResolver<N>
    extends AbstractNodeResolver<N> {

    private final BiFunction<N, N, N> createNodeRunnable;

    private NodeResolver<N> left;
    private NodeResolver<N> right;

    /**
     * Constructs a new {@link BinaryNodeResolver}.
     *
     * @param token              the token to resolve
     * @param createNodeRunnable the function that creates the node from the left and right operands
     */
    public BinaryNodeResolver(final Token<N> token, final BiFunction<N, N, N> createNodeRunnable) {
        super(token);
        this.createNodeRunnable = createNodeRunnable;
    }

    @Override
    public void consolidate(final Stack<NodeResolver<N>> stack) {
        if (this != stack.get(stack.size() - 2)) {
            throw new IllegalStateException("This BinaryNodeResolver should be the second to last element in the stack.");
        }
        if (this.isConsolidated) {
            throw new IllegalStateException("This BinaryNodeResolver has already been consolidated.");
        }
        if (stack.size() < 3) {
            throw new IllegalStateException("There should be at least 3 elements in the stack.");
        }

        this.right = stack.pop();
        stack.pop(); // self
        this.left = stack.pop();

        stack.push(this);

        super.consolidate(stack);
    }

    @Override
    public N resolve() {
        if (!this.isConsolidated) {
            throw new IllegalStateException("The NodeResolver has not been consolidated.");
        }
        return this.createNodeRunnable.apply(this.left.resolve(), this.right.resolve());
    }
}
