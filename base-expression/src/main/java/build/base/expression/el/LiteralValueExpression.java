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

import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ValueExpression;
import jakarta.el.ValueReference;

import java.util.Objects;

/**
 * A {@link ValueExpression} that wraps a fixed literal value.
 * Used internally for lambda parameter binding.
 */
public final class LiteralValueExpression extends ValueExpression {

    private final Object value;

    LiteralValueExpression(final Object value) {
        this.value = value;
    }

    @Override
    public Object getValue(final ELContext context) {
        return this.value;
    }

    @Override
    public void setValue(final ELContext context, final Object value) throws ELException {
        throw new ELException("Cannot set value on a literal expression");
    }

    @Override
    public boolean isReadOnly(final ELContext context) {
        return true;
    }

    @Override
    public Class<?> getType(final ELContext context) {
        return this.value == null ? null : this.value.getClass();
    }

    @Override
    public Class<?> getExpectedType() {
        return Object.class;
    }

    @Override
    public String getExpressionString() {
        return this.value == null ? "null" : this.value.toString();
    }

    @Override
    public boolean isLiteralText() {
        return true;
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
        if (!(other instanceof LiteralValueExpression that)) {
            return false;
        }
        return Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.value);
    }
}
