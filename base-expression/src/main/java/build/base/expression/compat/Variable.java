package build.base.expression.compat;

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

import build.base.configuration.MappedOption;
import build.base.configuration.Option;

import java.util.Objects;

/**
 * A variable representing a {@link Resolvable} {@link Object}.
 *
 * @author brian.oliver
 * @since Nov-2024
 * @deprecated Use the Jakarta EL API directly via {@link jakarta.el.ExpressionFactory}.
 */
@Deprecated
public class Variable
    implements Resolvable<Object>, Option, MappedOption<String> {

    private final String name;
    private final Object value;

    private Variable(final String name, final Object value) {
        this.name = Objects.requireNonNull(name, "Variable name must not be null");
        this.value = value;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public Object value() {
        return this.value;
    }

    @Override
    public String key() {
        return name();
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof final Variable that)) {
            return false;
        }
        return this.name.equals(that.name) && Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.value);
    }

    @Override
    public String toString() {
        return "Variable{" +
            "name='" + this.name + '\'' +
            ", value='" + this.value +
            "'}";
    }

    /**
     * Creates a {@link Variable} using the provided name and value.
     *
     * @param name  the {@link String} which names the variable
     * @param value the {@link Object} value which the variable holds
     * @return a new {@link Variable} containing the provided name and value
     */
    public static Variable of(final String name, final Object value) {
        return new Variable(name, value);
    }
}
