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

import build.base.foundation.Arrays;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * A builder of {@link Processor}s.
 *
 * @author brian.oliver
 * @since Dec-2024
 * @deprecated Use the Jakarta EL API directly via {@link jakarta.el.ExpressionFactory}.
 */
@Deprecated
public final class ProcessorBuilder {

    private final LinkedHashSet<Resolvable<?>> resolvables;
    private final LinkedHashMap<String, PropertyResolver<?>> propertyResolvers;

    ProcessorBuilder() {
        this.resolvables = new LinkedHashSet<>();
        this.propertyResolvers = new LinkedHashMap<>();
    }

    /**
     * Includes the provided {@link Resolvable} in the evaluation context.
     *
     * @param resolvable the {@link Resolvable} to include
     * @return this {@link ProcessorBuilder}
     */
    public ProcessorBuilder include(final Resolvable<?> resolvable) {
        if (resolvable != null) {
            this.resolvables.add(resolvable);
        }
        return this;
    }

    /**
     * Includes the specified {@link Variable} in the evaluation context.
     *
     * @param variable the {@link Variable} to define
     * @return this {@link ProcessorBuilder}
     */
    public ProcessorBuilder define(final Variable variable) {
        return include(variable);
    }

    /**
     * Adds a {@link PropertyResolver} for a named bean.
     *
     * @param beanName the bean name
     * @param resolver the {@link PropertyResolver}
     * @return this {@link ProcessorBuilder}
     */
    public ProcessorBuilder addPropertyResolver(final String beanName, final PropertyResolver<?> resolver) {
        if (beanName != null && !beanName.isEmpty() && resolver != null) {
            this.propertyResolvers.put(beanName, resolver);
        }
        return this;
    }

    /**
     * Builds a new immutable {@link Processor}.
     *
     * @return a new immutable {@link Processor}
     */
    public Processor build() {
        final var processor = new DefaultProcessor();
        this.resolvables.stream()
            .sorted(Comparator.comparing(Resolvable::name))
            .forEach(processor::include);
        this.propertyResolvers.forEach(processor::addPropertyResolver);
        return processor;
    }

    /**
     * Creates a {@link ProcessorBuilder} with the provided {@link Resolvable}s.
     *
     * @param resolvables the {@link Resolvable}s to include
     * @return a new {@link ProcessorBuilder}
     */
    public static ProcessorBuilder create(final Resolvable<?>... resolvables) {
        final var builder = new ProcessorBuilder();
        Arrays.stream(resolvables)
            .filter(Objects::nonNull)
            .forEach(builder::include);
        return builder;
    }
}
