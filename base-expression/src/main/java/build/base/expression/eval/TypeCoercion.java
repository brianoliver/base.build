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
 * Type coercion rules per Jakarta EL 6.0 specification section 1.23.
 */
public final class TypeCoercion {

    private TypeCoercion() {}

    /**
     * Coerces {@code value} to {@code targetType} following EL spec rules.
     *
     * @param value      the value to coerce (may be null)
     * @param targetType the desired type
     * @param <T>        the target type
     * @return the coerced value
     * @throws ELException if coercion is not possible
     */
    @SuppressWarnings("unchecked")
    public static <T> T coerce(final Object value, final Class<T> targetType) {
        if (targetType == null || targetType == Object.class) {
            return (T) value;
        }
        if (value == null) {
            return coerceNull(targetType);
        }
        if (targetType.isInstance(value)) {
            return (T) value;
        }

        if (targetType == String.class) {
            return (T) value.toString();
        }

        if (targetType == Boolean.class || targetType == boolean.class) {
            return (T) coerceToBoolean(value);
        }

        if (targetType == Integer.class  || targetType == int.class) {
            return (T) coerceToInteger(value);
        }
        if (targetType == Long.class || targetType == long.class) {
            return (T) coerceToLong(value);
        }
        if (targetType == Double.class || targetType == double.class) {
            return (T) coerceToDouble(value);
        }
        if (targetType == Float.class || targetType == float.class) {
            return (T) coerceToFloat(value);
        }
        if (targetType == Short.class || targetType == short.class) {
            return (T) coerceToShort(value);
        }
        if (targetType == Byte.class || targetType == byte.class) {
            return (T) coerceToByte(value);
        }
        if (targetType == BigInteger.class) {
            return (T) coerceToBigInteger(value);
        }
        if (targetType == BigDecimal.class) {
            return (T) coerceToBigDecimal(value);
        }
        if (targetType == Character.class || targetType == char.class) {
            return (T) coerceToCharacter(value);
        }

        if (targetType.isEnum()) {
            @SuppressWarnings("rawtypes")
            final var enumType = (Class<? extends Enum>) targetType;
            return (T) coerceToEnum(value, enumType);
        }

        throw new ELException("Cannot coerce value of type " + value.getClass().getName()
            + " to " + targetType.getName());
    }

    @SuppressWarnings("unchecked")
    private static <T> T coerceNull(final Class<T> targetType) {
        if (targetType.isPrimitive()) {
            if (targetType == boolean.class) {
                return (T) Boolean.FALSE;
            }
            if (targetType == char.class) {
                return (T) Character.valueOf('\0');
            }
            if (targetType == int.class) {
                return (T) Integer.valueOf(0);
            }
            if (targetType == long.class) {
                return (T) Long.valueOf(0L);
            }
            if (targetType == double.class) {
                return (T) Double.valueOf(0.0);
            }
            if (targetType == float.class) {
                return (T) Float.valueOf(0.0f);
            }
            if (targetType == short.class) {
                return (T) Short.valueOf((short) 0);
            }
            if (targetType == byte.class) {
                return (T) Byte.valueOf((byte) 0);
            }
        }
        if (targetType == String.class) {
            return (T) "";
        }
        if (targetType == Boolean.class) {
            return (T) Boolean.FALSE;
        }
        return null;
    }

    private static Boolean coerceToBoolean(final Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            return s.isEmpty() ? Boolean.FALSE : Boolean.parseBoolean(s);
        }
        throw new ELException("Cannot coerce " + value.getClass().getName() + " to Boolean");
    }

    private static Integer coerceToInteger(final Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            return s.isEmpty() ? 0 : Integer.parseInt(s);
        }
        if (value instanceof Character c) {
            return (int) c.charValue();
        }
        throw new ELException("Cannot coerce to Integer: " + value);
    }

    private static Long coerceToLong(final Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s) {
            return s.isEmpty() ? 0L : Long.parseLong(s);
        }
        if (value instanceof Character c) {
            return (long) c.charValue();
        }
        throw new ELException("Cannot coerce to Long: " + value);
    }

    private static Double coerceToDouble(final Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof String s) {
            return s.isEmpty() ? 0.0 : Double.parseDouble(s);
        }
        throw new ELException("Cannot coerce to Double: " + value);
    }

    private static Float coerceToFloat(final Object value) {
        if (value instanceof Number n) {
            return n.floatValue();
        }
        if (value instanceof String s) {
            return s.isEmpty() ? 0.0f : Float.parseFloat(s);
        }
        throw new ELException("Cannot coerce to Float: " + value);
    }

    private static Short coerceToShort(final Object value) {
        if (value instanceof Number n) {
            return n.shortValue();
        }
        if (value instanceof String s) {
            return s.isEmpty() ? (short) 0 : Short.parseShort(s);
        }
        throw new ELException("Cannot coerce to Short: " + value);
    }

    private static Byte coerceToByte(final Object value) {
        if (value instanceof Number n) {
            return n.byteValue();
        }
        if (value instanceof String s) {
            return s.isEmpty() ? (byte) 0 : Byte.parseByte(s);
        }
        throw new ELException("Cannot coerce to Byte: " + value);
    }

    private static BigInteger coerceToBigInteger(final Object value) {
        if (value instanceof BigInteger bi) {
            return bi;
        }
        if (value instanceof BigDecimal bd) {
            return bd.toBigInteger();
        }
        if (value instanceof Number n) {
            return BigInteger.valueOf(n.longValue());
        }
        if (value instanceof String s) {
            return s.isEmpty() ? BigInteger.ZERO : new BigInteger(s);
        }
        throw new ELException("Cannot coerce to BigInteger: " + value);
    }

    private static BigDecimal coerceToBigDecimal(final Object value) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        if (value instanceof String s) {
            return s.isEmpty() ? BigDecimal.ZERO : new BigDecimal(s);
        }
        throw new ELException("Cannot coerce to BigDecimal: " + value);
    }

    private static Character coerceToCharacter(final Object value) {
        if (value instanceof Character c) {
            return c;
        }
        if (value instanceof Number n) {
            return (char) n.intValue();
        }
        if (value instanceof String s) {
            return s.isEmpty() ? '\0' : s.charAt(0);
        }
        throw new ELException("Cannot coerce to Character: " + value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <E extends Enum<E>> E coerceToEnum(final Object value, final Class<E> enumType) {
        if (value instanceof String s) {
            try {
                return Enum.valueOf(enumType, s);
            } catch (final IllegalArgumentException e) {
                throw new ELException("Cannot coerce to enum " + enumType.getName() + ": " + s, e);
            }
        }
        throw new ELException("Cannot coerce to enum " + enumType.getName() + ": " + value);
    }
}
