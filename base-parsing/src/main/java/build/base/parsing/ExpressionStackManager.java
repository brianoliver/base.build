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

import build.base.parsing.resolvers.AtomNodeResolver;
import build.base.parsing.resolvers.NodeResolver;
import build.base.parsing.resolvers.SectionBeginNodeResolver;
import build.base.parsing.resolvers.SectionEndNodeResolver;

import java.util.Optional;
import java.util.Stack;

/**
 * Manages the resolver stack during expression parsing, handling operator precedence and parenthesised sections.
 *
 * @param <N> the node type
 *
 * @author tim.berston
 * @since Nov-2024
 */
public class ExpressionStackManager<N> {

    private final Stack<Stack<NodeResolver<N>>> stackOfStacks;
    private Stack<NodeResolver<N>> activeResolverStack;

    /**
     * Constructs a new {@link ExpressionStackManager}.
     */
    public ExpressionStackManager() {
        this.stackOfStacks = new Stack<>();
        this.activeResolverStack = new Stack<>();
    }

    /**
     * Pushes a resolver onto the active stack, consolidating preceding operators where precedence requires it.
     *
     * @param nodeResolver the resolver to push
     */
    public void push(final NodeResolver<N> nodeResolver) {
        if (nodeResolver instanceof SectionBeginNodeResolver) {
            this.beginSection();
            return;
        }

        final var activeNodeResolver = nodeResolver instanceof SectionEndNodeResolver
            ? this.completeSection((SectionEndNodeResolver<N>) nodeResolver)
            : nodeResolver;

        if (!(activeNodeResolver instanceof AtomNodeResolver)) {
            var lastOperator = getLastRelevantOperator();
            while (lastOperator.isPresent()) {
                final var lastOperatorResolver = lastOperator.get();
                if (activeNodeResolver.getTokenParser().getPrecedence() >= lastOperatorResolver.getTokenParser().getPrecedence()) {
                    lastOperatorResolver.consolidate(this.activeResolverStack);
                    lastOperator = getLastRelevantOperator();
                } else {
                    break;
                }
            }
        }
        this.activeResolverStack.push(activeNodeResolver);
    }

    /**
     * Consolidates the entire stack and returns the root resolver.
     *
     * @return the root resolver
     * @throws ExpressionParserException if the expression is malformed
     */
    public NodeResolver<N> getExpressionRoot() {
        if (!this.stackOfStacks.isEmpty()) {
            throw new ExpressionParserException("The expression is malformed");
        }
        return this.getStackRoot();
    }

    private NodeResolver<N> getStackRoot() {
        while (this.activeResolverStack.size() > 1) {
            final var operator = this.getLastRelevantOperator();
            if (operator.isEmpty()) {
                throw new ExpressionParserException("The expression is malformed");
            }
            operator.get().consolidate(this.activeResolverStack);
        }

        final var root = this.activeResolverStack.pop();
        if (!root.isConsolidated()) {
            throw new ExpressionParserException("The expression is malformed");
        }
        return root;
    }

    private void beginSection() {
        this.stackOfStacks.push(this.activeResolverStack);
        this.activeResolverStack = new Stack<>();
    }

    private NodeResolver<N> completeSection(final SectionEndNodeResolver<N> nodeResolver) {
        if (this.stackOfStacks.isEmpty()) {
            throw new ExpressionParserException(
                "The expression contains an unknown or unexpected token at " + nodeResolver.getTokenLocation());
        }
        final var rootNodeResolver = this.getStackRoot();
        this.activeResolverStack = this.stackOfStacks.pop();
        return rootNodeResolver;
    }

    private Optional<NodeResolver<N>> getLastRelevantOperator() {
        final var stackSize = this.activeResolverStack.size();

        if (stackSize >= 2) {
            final var lastResolver = this.activeResolverStack.get(stackSize - 1);
            if (!lastResolver.isConsolidated()) {
                return Optional.empty();
            }

            final var resolver = this.activeResolverStack.get(stackSize - 2);
            if (resolver instanceof AtomNodeResolver || resolver.isConsolidated()) {
                return Optional.empty();
            }

            return Optional.of(resolver);
        }

        return Optional.empty();
    }
}
