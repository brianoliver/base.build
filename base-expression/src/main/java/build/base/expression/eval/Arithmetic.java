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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

/**
 * Arithmetic operations per Jakarta EL 6.0 specification sections 1.7–1.9.
 * <p>
 * Promotion rules:
 * <ul>
 *   <li>If either operand is {@link BigDecimal}, promote both and use {@link BigDecimal} arithmetic.</li>
 *   <li>If either operand is floating-point ({@link Float}, {@link Double}, or a {@link String}
 *       containing {@code .}, {@code e}, or {@code E}), promote both and use {@link Double} arithmetic.</li>
 *   <li>If either operand is {@link BigInteger}, promote both and use {@link BigInteger} arithmetic.</li>
 *   <li>Otherwise promote both to {@link Long}.</li>
 * </ul>
 */
public final class Arithmetic {

    private Arithmetic() {}

    /** Adds two values per EL spec. */
    public static Object add(final Object left, final Object right) {
        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            return toBigDecimal(left).add(toBigDecimal(right));
        }
        if (isFloating(left) || isFloating(right)) {
            return toDouble(left) + toDouble(right);
        }
        if (left instanceof BigInteger || right instanceof BigInteger) {
            return toBigInteger(left).add(toBigInteger(right));
        }
        return toLong(left) + toLong(right);
    }

    /** Subtracts two values per EL spec. */
    public static Object subtract(final Object left, final Object right) {
        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            return toBigDecimal(left).subtract(toBigDecimal(right));
        }
        if (isFloating(left) || isFloating(right)) {
            return toDouble(left) - toDouble(right);
        }
        if (left instanceof BigInteger || right instanceof BigInteger) {
            return toBigInteger(left).subtract(toBigInteger(right));
        }
        return toLong(left) - toLong(right);
    }

    /** Multiplies two values per EL spec. */
    public static Object multiply(final Object left, final Object right) {
        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            return toBigDecimal(left).multiply(toBigDecimal(right));
        }
        if (isFloating(left) || isFloating(right)) {
            return toDouble(left) * toDouble(right);
        }
        if (left instanceof BigInteger || right instanceof BigInteger) {
            return toBigInteger(left).multiply(toBigInteger(right));
        }
        return toLong(left) * toLong(right);
    }

    /** Divides two values per EL spec. Division always produces Double or BigDecimal. */
    public static Object divide(final Object left, final Object right) {
        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            return toBigDecimal(left).divide(toBigDecimal(right), MathContext.DECIMAL128);
        }
        return toDouble(left) / toDouble(right);
    }

    /** Computes modulo per EL spec. */
    public static Object mod(final Object left, final Object right) {
        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            return toBigDecimal(left).remainder(toBigDecimal(right));
        }
        if (isFloating(left) || isFloating(right)) {
            return toDouble(left) % toDouble(right);
        }
        if (left instanceof BigInteger || right instanceof BigInteger) {
            return toBigInteger(left).remainder(toBigInteger(right));
        }
        return toLong(left) % toLong(right);
    }

    /** Negates a value per EL spec. */
    public static Object negate(final Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof BigDecimal bd) {
            return bd.negate();
        }
        if (value instanceof BigInteger bi) {
            return bi.negate();
        }
        if (isFloating(value)) {
            return -toDouble(value);
        }
        return -toLong(value);
    }

    private static boolean isFloating(final Object v) {
        if (v instanceof Float || v instanceof Double) {
            return true;
        }
        if (v instanceof String s) {
            return s.contains(".") || s.contains("e") || s.contains("E");
        }
        return false;
    }

    private static double toDouble(final Object v) {
        return TypeCoercion.coerce(v, Double.class);
    }

    private static long toLong(final Object v) {
        return TypeCoercion.coerce(v, Long.class);
    }

    private static BigDecimal toBigDecimal(final Object v) {
        return TypeCoercion.coerce(v, BigDecimal.class);
    }

    private static BigInteger toBigInteger(final Object v) {
        return TypeCoercion.coerce(v, BigInteger.class);
    }
}
