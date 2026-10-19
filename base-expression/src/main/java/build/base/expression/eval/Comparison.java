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

import jakarta.el.ELException;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Relational comparison operations per Jakarta EL 6.0 specification section 1.8.
 */
public final class Comparison {

    private Comparison() {}

    /** Returns {@code true} if {@code left == right} per EL spec. */
    public static Boolean equals(final Object left, final Object right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        if (left instanceof Number ln && right instanceof Number rn) {
            if (left instanceof BigDecimal || right instanceof BigDecimal) {
                final var l = (left instanceof BigDecimal bd) ? bd : new BigDecimal(ln.toString());
                final var r = (right instanceof BigDecimal bd) ? bd : new BigDecimal(rn.toString());
                return l.compareTo(r) == 0;
            }
            if (left instanceof Float || left instanceof Double ||
                right instanceof Float || right instanceof Double) {
                return Double.compare(ln.doubleValue(), rn.doubleValue()) == 0;
            }
            if (left instanceof BigInteger || right instanceof BigInteger) {
                final var l = (left instanceof BigInteger bi) ? bi : BigInteger.valueOf(ln.longValue());
                final var r = (right instanceof BigInteger bi) ? bi : BigInteger.valueOf(rn.longValue());
                return l.compareTo(r) == 0;
            }
            return Long.compare(ln.longValue(), rn.longValue()) == 0;
        }
        return left.equals(right);
    }

    /** Returns {@code true} if {@code left < right} per EL spec. */
    @SuppressWarnings("unchecked")
    public static Boolean lessThan(final Object left, final Object right) {
        if (left == null || right == null) {
            return false;
        }
        if (left instanceof Number ln && right instanceof Number rn) {
            return Double.compare(ln.doubleValue(), rn.doubleValue()) < 0;
        }
        if (left instanceof Comparable c) {
            try {
                return c.compareTo(right) < 0;
            } catch (final ClassCastException e) {
                throw new ELException("Cannot compare: " + left + " < " + right, e);
            }
        }
        throw new ELException("Cannot compare: " + left + " < " + right);
    }

    /** Returns {@code true} if {@code left > right} per EL spec. */
    public static Boolean greaterThan(final Object left, final Object right) {
        return lessThan(right, left);
    }

    /** Returns {@code true} if {@code left <= right} per EL spec. */
    public static Boolean lessThanOrEqual(final Object left, final Object right) {
        return equals(left, right) || lessThan(left, right);
    }

    /** Returns {@code true} if {@code left >= right} per EL spec. */
    public static Boolean greaterThanOrEqual(final Object left, final Object right) {
        return equals(left, right) || greaterThan(left, right);
    }
}
