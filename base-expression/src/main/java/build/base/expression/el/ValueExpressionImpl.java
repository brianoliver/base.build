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

import build.base.expression.ast.LiteralNode;
import build.base.expression.ast.Node;
import build.base.expression.eval.NodeEvaluator;
import build.base.expression.eval.TypeCoercion;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.FunctionMapper;
import jakarta.el.ValueExpression;
import jakarta.el.ValueReference;
import jakarta.el.VariableMapper;

import java.util.Objects;

/**
 * Jakarta EL {@link ValueExpression} implementation.
 * <p>
 * This class is module-internal: the {@code build.base.expression.el} package is not
 * exported. The {@code public} access modifier exists solely to permit construction from
 * sibling packages within the module (e.g. {@code build.base.expression.eval}).
 */
public final class ValueExpressionImpl extends ValueExpression {

    private final String expressionString;
    private final Node ast;
    private final Class<?> expectedType;
    private final FunctionMapper functionMapper;
    private final VariableMapper variableMapper;

    public ValueExpressionImpl(final String expressionString, final Node ast, final Class<?> expectedType,
                               final FunctionMapper functionMapper, final VariableMapper variableMapper) {
        this.expressionString = expressionString;
        this.ast = ast;
        this.expectedType = expectedType;
        this.functionMapper = functionMapper;
        this.variableMapper = variableMapper;
    }

    @Override
    public Object getValue(final ELContext context) throws ELException {
        final var raw = NodeEvaluator.evaluate(this.ast, context);
        if (this.expectedType == null || this.expectedType == Object.class) {
            return raw;
        }
        return TypeCoercion.coerce(raw, this.expectedType);
    }

    @Override
    public void setValue(final ELContext context, final Object value) throws ELException {
        NodeEvaluator.assign(this.ast, context, value);
    }

    @Override
    public boolean isReadOnly(final ELContext context) throws ELException {
        return this.ast instanceof LiteralNode;
    }

    @Override
    public Class<?> getType(final ELContext context) throws ELException {
        final var value = NodeEvaluator.evaluate(this.ast, context);
        return value == null ? null : value.getClass();
    }

    @Override
    public Class<?> getExpectedType() {
        return this.expectedType;
    }

    @Override
    public String getExpressionString() {
        return this.expressionString;
    }

    @Override
    public boolean isLiteralText() {
        return this.ast instanceof LiteralNode;
    }

    @Override
    public ValueReference getValueReference(final ELContext context) {
        return null;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ValueExpressionImpl that)) {
            return false;
        }
        return Objects.equals(this.expressionString, that.expressionString)
            && Objects.equals(this.expectedType, that.expectedType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.expressionString, this.expectedType);
    }
}
