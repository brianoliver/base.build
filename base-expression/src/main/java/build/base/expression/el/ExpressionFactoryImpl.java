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
import build.base.expression.eval.TypeCoercion;
import build.base.expression.parser.ELParser;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Jakarta EL 6.0 {@link ExpressionFactory} implementation.
 * <p>
 * Discovered via the JPMS {@code provides} declaration in {@code module-info.java}.
 * Obtain an instance via {@link ExpressionFactory#newInstance()}.
 * <p>
 * To evaluate expressions, pair this factory with a {@link jakarta.el.StandardELContext},
 * which provides the standard resolver chain (bean properties, maps, lists, arrays, static
 * fields, etc.) out of the box:
 * <pre>{@code
 * ExpressionFactory factory = ExpressionFactory.newInstance();
 * StandardELContext context = new StandardELContext(factory);
 * context.getELResolver(); // initialises the default resolver chain
 *
 * ValueExpression expr = factory.createValueExpression(context, "${name}", String.class);
 * }</pre>
 * Callers who construct a custom {@link jakarta.el.ELContext} are responsible for
 * supplying their own {@link jakarta.el.ELResolver} chain.
 */
public final class ExpressionFactoryImpl extends ExpressionFactory {

    /**
     * Constructs a new {@link ExpressionFactoryImpl}.
     */
    public ExpressionFactoryImpl() {}

    @Override
    public ValueExpression createValueExpression(final ELContext context,
                                                  final String expression,
                                                  final Class<?> expectedType) {
        final var ast = ELParser.parseFull(expression);
        return new ValueExpressionImpl(expression, ast, expectedType,
            context.getFunctionMapper(), context.getVariableMapper());
    }

    @Override
    public ValueExpression createValueExpression(final Object instance, final Class<?> expectedType) {
        return new ValueExpressionImpl(
            String.valueOf(instance), new LiteralNode(instance), expectedType, null, null);
    }

    @Override
    public MethodExpression createMethodExpression(final ELContext context,
                                                    final String expression,
                                                    final Class<?> expectedReturnType,
                                                    final Class<?>[] expectedParamTypes) {
        final var ast = ELParser.parseFull(expression);
        return new MethodExpressionImpl(expression, ast, expectedReturnType, expectedParamTypes,
            context.getFunctionMapper(), context.getVariableMapper());
    }

    @Override
    public <T> T coerceToType(final Object obj, final Class<T> targetType) throws ELException {
        return TypeCoercion.coerce(obj, targetType);
    }

    @Override
    public ELResolver getStreamELResolver() {
        return null;
    }

    @Override
    public Map<String, Method> getInitFunctionMap() {
        return Map.of();
    }
}
