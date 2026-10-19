package build.base.expression.eval;

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

import build.base.expression.ast.AssignmentNode;
import build.base.expression.ast.BinaryOpNode;
import build.base.expression.ast.CompositeNode;
import build.base.expression.ast.ConditionalNode;
import build.base.expression.ast.FunctionCallNode;
import build.base.expression.ast.IdentifierNode;
import build.base.expression.ast.LambdaNode;
import build.base.expression.ast.LiteralNode;
import build.base.expression.ast.MethodCallNode;
import build.base.expression.ast.Node;
import build.base.expression.ast.PropertyAccessNode;
import build.base.expression.ast.SemicolonNode;
import build.base.expression.ast.UnaryOpNode;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.LambdaExpression;
import jakarta.el.PropertyNotFoundException;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;

/**
 * Evaluates a parsed EL {@link Node} against an {@link ELContext}.
 */
public final class NodeEvaluator {

    private NodeEvaluator() {}

    /**
     * Evaluates the node and returns the result.
     *
     * @param node    the AST node to evaluate
     * @param context the EL evaluation context
     * @return the evaluated value (may be null)
     * @throws ELException on evaluation failure
     */
    public static Object evaluate(final Node node, final ELContext context) {
        return switch (node) {
            case LiteralNode n        -> n.value();
            case CompositeNode n      -> evaluateComposite(n, context);
            case IdentifierNode n     -> evaluateIdentifier(n, context);
            case BinaryOpNode n       -> evaluateBinary(n, context);
            case UnaryOpNode n        -> evaluateUnary(n, context);
            case PropertyAccessNode n -> evaluatePropertyAccess(n, context);
            case MethodCallNode n     -> evaluateMethodCall(n, context);
            case FunctionCallNode n   -> evaluateFunctionCall(n, context);
            case LambdaNode n         -> new LambdaExpression(n.params(),
                new build.base.expression.el.ValueExpressionImpl("<lambda>", n.body(), Object.class,
                    context.getFunctionMapper(), context.getVariableMapper()));
            case AssignmentNode n     -> evaluateAssignment(n, context);
            case ConditionalNode n    -> evaluateConditional(n, context);
            case SemicolonNode n      -> {
                evaluate(n.left(), context);
                yield evaluate(n.right(), context);
            }
        };
    }

    /**
     * Assigns a value to the target node's location in the ELContext.
     */
    public static void assign(final Node node, final ELContext context, final Object value) {
        switch (node) {
            case IdentifierNode n -> context.getELResolver().setValue(context, null, n.name(), value);
            case PropertyAccessNode n -> {
                final var base = evaluate(n.base(), context);
                final var key  = evaluate(n.key(), context);
                context.getELResolver().setValue(context, base, key, value);
            }
            default -> throw new ELException("Cannot assign to: " + node);
        }
    }

    // -------------------------------------------------------------------------

    private static String evaluateComposite(final CompositeNode n, final ELContext ctx) {
        final var sb = new StringBuilder();
        for (final var part : n.parts()) {
            final var val = evaluate(part, ctx);
            sb.append(val == null ? "" : val.toString());
        }
        return sb.toString();
    }

    private static Object evaluateIdentifier(final IdentifierNode n, final ELContext ctx) {
        final var varMapper = ctx.getVariableMapper();
        if (varMapper != null) {
            final var expr = varMapper.resolveVariable(n.name());
            if (expr != null) {
                return expr.getValue(ctx);
            }
        }
        ctx.setPropertyResolved(false);
        final var value = ctx.getELResolver().getValue(ctx, null, n.name());
        if (ctx.isPropertyResolved()) {
            return value;
        }
        return null;
    }

    private static Object evaluateBinary(final BinaryOpNode n, final ELContext ctx) {
        // Short-circuit logical operators
        if ("&&".equals(n.op())) {
            final var left = TypeCoercion.coerce(evaluate(n.left(), ctx), Boolean.class);
            if (Boolean.FALSE.equals(left)) {
                return false;
            }
            return TypeCoercion.coerce(evaluate(n.right(), ctx), Boolean.class);
        }
        if ("||".equals(n.op())) {
            final var left = TypeCoercion.coerce(evaluate(n.left(), ctx), Boolean.class);
            if (Boolean.TRUE.equals(left)) {
                return true;
            }
            return TypeCoercion.coerce(evaluate(n.right(), ctx), Boolean.class);
        }

        final var left  = evaluate(n.left(), ctx);
        final var right = evaluate(n.right(), ctx);

        return switch (n.op()) {
            case "+"  -> Arithmetic.add(left, right);
            case "-"  -> Arithmetic.subtract(left, right);
            case "*"  -> Arithmetic.multiply(left, right);
            case "/"  -> Arithmetic.divide(left, right);
            case "%"  -> Arithmetic.mod(left, right);
            case "==" -> Comparison.equals(left, right);
            case "!=" -> !Comparison.equals(left, right);
            case "<"  -> Comparison.lessThan(left, right);
            case ">"  -> Comparison.greaterThan(left, right);
            case "<=" -> Comparison.lessThanOrEqual(left, right);
            case ">=" -> Comparison.greaterThanOrEqual(left, right);
            case "+=" -> String.valueOf(left) + right; // EL string concatenation
            default   -> throw new ELException("Unknown operator: " + n.op());
        };
    }

    private static Object evaluateUnary(final UnaryOpNode n, final ELContext ctx) {
        final var val = evaluate(n.operand(), ctx);
        return switch (n.op()) {
            case "!"     -> !TypeCoercion.coerce(val, Boolean.class);
            case "-"     -> Arithmetic.negate(val);
            case "empty" -> isEmpty(val);
            default      -> throw new ELException("Unknown unary operator: " + n.op());
        };
    }

    private static Object evaluatePropertyAccess(final PropertyAccessNode n, final ELContext ctx) {
        final var base = evaluate(n.base(), ctx);
        final var key  = evaluate(n.key(), ctx);
        if (base == null) {
            throw new PropertyNotFoundException("Base object is null for property: " + key);
        }
        ctx.setPropertyResolved(false);
        final var value = ctx.getELResolver().getValue(ctx, base, key);
        if (!ctx.isPropertyResolved()) {
            throw new PropertyNotFoundException(
                "Property '" + key + "' not found on " + base.getClass().getName());
        }
        return value;
    }

    private static Object evaluateMethodCall(final MethodCallNode n, final ELContext ctx) {
        final var base = evaluate(n.base(), ctx);
        final var args = n.args().stream().map(a -> evaluate(a, ctx)).toList();
        ctx.setPropertyResolved(false);
        return ctx.getELResolver().invoke(ctx, base, n.methodName(),
            args.stream().map(a -> a == null ? null : a.getClass()).toArray(Class<?>[]::new),
            args.toArray());
    }

    private static Object evaluateFunctionCall(final FunctionCallNode n, final ELContext ctx) {
        final var mapper = ctx.getFunctionMapper();
        if (mapper == null) {
            throw new ELException("No FunctionMapper in context for " + n.namespace() + ":" + n.localName());
        }
        final var method = mapper.resolveFunction(n.namespace(), n.localName());
        if (method == null) {
            throw new ELException("Function not found: " + n.namespace() + ":" + n.localName());
        }
        final var args = n.args().stream().map(a -> evaluate(a, ctx)).toArray();
        try {
            return method.invoke(null, args);
        } catch (final IllegalAccessException | InvocationTargetException e) {
            throw new ELException("Failed to invoke function: " + n.localName(), e);
        }
    }

    private static Object evaluateAssignment(final AssignmentNode n, final ELContext ctx) {
        final var value = evaluate(n.value(), ctx);
        assign(n.target(), ctx, value);
        return value;
    }

    private static Object evaluateConditional(final ConditionalNode n, final ELContext ctx) {
        final var cond = TypeCoercion.coerce(evaluate(n.condition(), ctx), Boolean.class);
        return Boolean.TRUE.equals(cond) ? evaluate(n.thenExpr(), ctx) : evaluate(n.elseExpr(), ctx);
    }

    private static boolean isEmpty(final Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String s) {
            return s.isEmpty();
        }
        if (value instanceof Collection<?> c) {
            return c.isEmpty();
        }
        if (value instanceof Map<?, ?> m) {
            return m.isEmpty();
        }
        if (value.getClass().isArray()) {
            return Array.getLength(value) == 0;
        }
        return false;
    }
}
