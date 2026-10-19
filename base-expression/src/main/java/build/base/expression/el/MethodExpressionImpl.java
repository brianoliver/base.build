package build.base.expression.el;

/*-
 * #%L
 * base.build Expression
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

import build.base.expression.ast.MethodCallNode;
import build.base.expression.ast.Node;
import build.base.expression.ast.PropertyAccessNode;
import build.base.expression.eval.NodeEvaluator;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.FunctionMapper;
import jakarta.el.MethodExpression;
import jakarta.el.MethodInfo;
import jakarta.el.VariableMapper;

import java.util.Objects;

/**
 * Jakarta EL {@link MethodExpression} implementation.
 */
public final class MethodExpressionImpl extends MethodExpression {

    private final String expressionString;
    private final Node ast;
    private final Class<?> expectedReturnType;
    private final Class<?>[] expectedParamTypes;
    private final FunctionMapper functionMapper;
    private final VariableMapper variableMapper;

    MethodExpressionImpl(final String expressionString, final Node ast,
                         final Class<?> expectedReturnType, final Class<?>[] expectedParamTypes,
                         final FunctionMapper functionMapper, final VariableMapper variableMapper) {
        this.expressionString = expressionString;
        this.ast = ast;
        this.expectedReturnType = expectedReturnType;
        this.expectedParamTypes = expectedParamTypes != null ? expectedParamTypes.clone() : new Class<?>[0];
        this.functionMapper = functionMapper;
        this.variableMapper = variableMapper;
    }

    @Override
    public MethodInfo getMethodInfo(final ELContext context) throws ELException {
        if (this.ast instanceof MethodCallNode mcn) {
            return new MethodInfo(mcn.methodName(), this.expectedReturnType, this.expectedParamTypes);
        }
        throw new ELException("Expression does not represent a method: " + this.expressionString);
    }

    @Override
    public Object invoke(final ELContext context, final Object[] params) throws ELException {
        // Method reference form ${bean.method} — resolve the base and invoke with supplied params
        if (this.ast instanceof PropertyAccessNode pan) {
            final var base = NodeEvaluator.evaluate(pan.base(), context);
            final var methodName = NodeEvaluator.evaluate(pan.key(), context);
            context.setPropertyResolved(false);
            return context.getELResolver().invoke(context, base, methodName,
                this.expectedParamTypes, params != null ? params : new Object[0]);
        }
        // Expression with embedded args ${bean.method(arg)} — evaluate normally
        return NodeEvaluator.evaluate(this.ast, context);
    }

    @Override
    public String getExpressionString() {
        return this.expressionString;
    }

    @Override
    public boolean isLiteralText() {
        return false;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MethodExpressionImpl that)) {
            return false;
        }
        return Objects.equals(this.expressionString, that.expressionString);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.expressionString);
    }
}
