package build.base.configuration;

/*-
 * #%L
 * base.build Configuration
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A <strong>strictly immutable</strong> value, often used to represent optional configuration information.
 *
 * @author brian.oliver
 * @see Configuration
 * @since Nov-2017
 */
public interface Option {

    /**
     * Determines the {@link Class} of {@link Option} in the class hierarchy for a specified {@link Class}
     * that is annotated with {@link OptionDiscriminator}.
     * <p>
     * When a class hierarchy for the specified {@link Class} (ie: itself and its parent interfaces/classes) are not
     * annotated with {@link OptionDiscriminator}, the most immediate non-abstract, non-anonymous, non-synthetic
     * {@link Class} of the specified {@link Class} is returned (which may be the said {@link Class} itself).
     * <p>
     * Otherwise, one of the {@link OptionDiscriminator} annotated classes in the specified {@link Class}
     * hierarchy is returned.
     *
     * @param classOfOption the class of the {@link Option}
     * @return the discriminator type of the {@link Option}
     */
    @SuppressWarnings("unchecked")
    static <T extends Option> Class<? extends Option> getDiscriminatorClass(final Class<T> classOfOption) {

        // we use a deque to perform class hierarchy searching (instead of using recursion)
        final Deque<Class<?>> hierarchy = new ArrayDeque<>();

        // initially we assume the discriminator type is the class of configuration itself.
        Class<?> discriminatorClass = classOfOption;

        // we also want to keep track of the first non-abstract, non-anonymous and non-lambda class
        // (ie: concrete configuration) that we find just in case we don't find a class annotated with @Discriminator
        Class<? extends Option> concreteClass = null;

        while (discriminatorClass != null) {

            // determine if the class is a concrete configuration (non-abstract, non-anonymous and non-synthetic)
            if (concreteClass == null && Option.class.isAssignableFrom(discriminatorClass)
                && (!Modifier.isAbstract(discriminatorClass.getModifiers()) || discriminatorClass.isInterface())
                && !discriminatorClass.isAnonymousClass() && !discriminatorClass.isSynthetic()) {

                concreteClass = (Class<Option>) discriminatorClass;
            }

            // determine if the class has a discriminator
            final OptionDiscriminator discriminator = discriminatorClass.getAnnotation(OptionDiscriminator.class);

            if (discriminator == null) {

                // push the super class (if there is one)
                final Class<?> superDiscriminatorClass = discriminatorClass.getSuperclass();

                if (superDiscriminatorClass != null && !superDiscriminatorClass.equals(Object.class)) {
                    hierarchy.push(superDiscriminatorClass);
                }

                // push the interfaces to search onto the deque
                for (final Class<?> interfaceClass : discriminatorClass.getInterfaces()) {
                    hierarchy.push(interfaceClass);
                }

            }
            else {
                // ensure that the discriminatorClass is an Option
                if (Option.class.isAssignableFrom(discriminatorClass)) {
                    return (Class<? extends Option>) discriminatorClass;
                }
                else {
                    // we ignore classes that use @Discriminator but are not Options
                }
            }

            // we couldn't determine the @Discriminator, so we try something else on the deque
            discriminatorClass = hierarchy.isEmpty() ? null : hierarchy.pop();
        }

        // when there's no @Discriminator, we return the concrete class or the specified class itself
        return concreteClass == null ? classOfOption : concreteClass;
    }

    /**
     * Retrieves the annotated {@link Default} {@link Option}
     * from a {@link Class} of {@link Option}.
     *
     * @param <T>           the type of {@link Option}
     * @param classOfOption the {@link Class} of {@link Option}
     * @return an {@link Option} or <code>null</code> if one can't be determined
     */
    @SuppressWarnings("unchecked")
    static <T extends Option> T getDefaultOption(final Class<T> classOfOption) {

        if (classOfOption == null) {
            return null;
        }
        else {
            if (!Modifier.isAbstract(classOfOption.getModifiers())) {
                // look for a no args constructor on the non-abstract class that is annotated with @Default
                for (final Constructor<?> constructor : classOfOption.getDeclaredConstructors()) {
                    if (constructor.getAnnotation(Default.class) != null && constructor.getParameterCount() == 0) {

                        try {
                            constructor.setAccessible(true);
                            return (T) constructor.newInstance();
                        }
                        catch (final Exception e) {
                            return null;
                        }
                    }
                }
            }

            // look for a static no args method that is annotated with @Default
            for (final Method method : classOfOption.getDeclaredMethods()) {
                if (method.getAnnotation(Default.class) != null
                    && method.getParameterCount() == 0
                    && method.getReturnType().isAssignableFrom(classOfOption)
                    && Modifier.isStatic(method.getModifiers())) {

                    try {
                        method.setAccessible(true);
                        return (T) method.invoke(null);
                    }
                    catch (final Exception e) {
                        return null;
                    }
                }
            }

            // look for a static field that is annotated with @Default
            for (final Field field : classOfOption.getDeclaredFields()) {
                if (field.getAnnotation(Default.class) != null && Modifier.isStatic(field.getModifiers())) {

                    try {
                        field.setAccessible(true);
                        return (T) field.get(null);
                    }
                    catch (final Exception e) {
                        return null;
                    }
                }
            }

            return null;
        }
    }
}
