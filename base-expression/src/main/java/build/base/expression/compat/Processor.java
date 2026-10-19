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

import build.base.configuration.Option;
import build.base.configuration.ValueOption;
import build.base.expression.option.ResolvableOption;
import build.base.foundation.Introspection;
import jakarta.el.ELException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Objects;

/**
 * A processor which evaluates {@link jakarta.el.ValueExpression}s using provided {@link Resolvable} values.
 *
 * @author graeme.campbell
 * @author brian.oliver
 * @see <a href="https://docs.oracle.com/javaee/7/tutorial/jsf-el.htm">Java Expression Language Documentation</a>
 * @since Jan-2019
 * @deprecated Use the Jakarta EL API directly via {@link jakarta.el.ExpressionFactory}.
 */
@Deprecated
public interface Processor {

    /**
     * Evaluates and replaces the {@link jakarta.el.ValueExpression}s enclosed in <code>${</code> and
     * <code>}</code> markup embedded in the provided {@link String} expression returning the resulting {@link String}.
     *
     * @param expression the {@link String} containing {@link jakarta.el.ValueExpression}s to be evaluated and replaced
     * @return the {@link String} containing the evaluated and replaced {@link jakarta.el.ValueExpression}s
     */
    String replace(String expression);

    /**
     * Evaluates the specified {@link jakarta.el.ValueExpression} and attempts to return it as the expected {@link Class}.
     *
     * @param <T>         the type of result
     * @param expression  the {@link String} containing {@link jakarta.el.ValueExpression}s to be evaluated
     * @param resultClass the {@link Class} of result
     * @return the result
     * @throws ELException should parsing or evaluating the expression fail
     */
    <T> T evaluate(String expression, Class<T> resultClass) throws ELException;

    /**
     * Attempts to resolve {@link ResolvableOption}s or {@link jakarta.el.ValueExpression}s defined in
     * {@link String}-based {@link ValueOption}s to produce a resolved {@link Option} of the same {@link Class}.
     *
     * @param option the {@link Option} to resolve
     * @param <T>    the type of {@link Option}
     * @return a new resolved {@link Option} or the provided {@link Option} if not resolvable
     */
    @SuppressWarnings("unchecked")
    default <T extends Option> T resolve(final T option) {

        if (option instanceof ResolvableOption<?> resolvableOption) {
            return (T) resolvableOption.resolve(this);
        }
        else if (option instanceof ValueOption<?> valueOption
            && valueOption.get() instanceof final String unresolvedValue) {

            final var resolvedValue = replace(unresolvedValue);

            if (Objects.equals(resolvedValue, unresolvedValue)) {
                return option;
            }

            final var optionClass = valueOption.getClass();

            final var stringBasedConstructor = Introspection.getAllDeclaredConstructors(optionClass)
                .filter(constructor -> Modifier.isPublic(constructor.getModifiers()))
                .filter(constructor -> constructor.getParameterCount() == 1
                    && constructor.getParameterTypes()[0].equals(String.class))
                .findFirst();

            if (stringBasedConstructor.isPresent()) {
                final var constructor = stringBasedConstructor.orElseThrow();
                constructor.setAccessible(true);
                try {
                    return (T) constructor.newInstance(resolvedValue);
                }
                catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    // fall-through
                }
            }

            final var stringBasedMethod = Introspection.getAllDeclaredMethods(optionClass)
                .filter(method -> Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers()))
                .filter(method -> optionClass.isAssignableFrom(method.getReturnType()))
                .filter(method -> method.getParameterCount() == 1 && method.getParameterTypes()[0].equals(String.class))
                .findFirst();

            if (stringBasedMethod.isPresent()) {
                final var method = stringBasedMethod.orElseThrow();
                method.setAccessible(true);
                try {
                    return (T) method.invoke(null, resolvedValue);
                }
                catch (InvocationTargetException | IllegalAccessException e) {
                    return option;
                }
            }

            return option;
        }
        else {
            return option;
        }
    }

    /**
     * Creates a {@link Processor} with the provided {@link Resolvable}s in the context.
     *
     * @param resolvables the {@link Resolvable}s to be included in the context
     * @return a new {@link Processor} with the provided {@link Resolvable}s included
     */
    static Processor create(final Resolvable<?>... resolvables) {
        return new DefaultProcessor(resolvables);
    }
}
