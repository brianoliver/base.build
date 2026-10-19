package build.base.foundation;

/*-
 * #%L
 * base.build Foundation
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

import build.base.foundation.memoizer.Memoizer;
import build.base.foundation.stream.Streamable;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilities to statically gather and provide access to runtime type information from {@link java.lang.reflect.Type}s
 * not readily available directly using Java Platform Reflection.
 * <p>
 * <a href="https://en.wikipedia.org/wiki/Type_introspection">Introspection</a> should not be confused with
 * <i>Reflection</i>, which goes a step further and provides the ability to manipulate the information at runtime.
 *
 * @author brian.oliver
 * @since Jul-2018
 */
public final class Introspection {

    /**
     * A mapping of primitive {@link Class}es to their wrapper {@link Class}es.
     */
    private static final Map<Class<?>, Class<?>> PRIMITIVE_OBJECT_MAPPINGS;

    /**
     * A mapping of wrapper {@link Class}es to primitive {@link Class}es.
     */
    private static final Map<Class<?>, Class<?>> OBJECT_PRIMITIVE_MAPPINGS;

    /**
     * A mapping of specific {@link Object}, primitive and wrapper {@link Class}es to their default {@link Object} values.
     */
    private static final Map<Class<?>, Object> DEFAULT_VALUES;

    /**
     * A mapping of the declared {@link Field}s by {@link Class}.
     */
    private static final Memoizer<Class<?>, Streamable<Field>> ALL_DECLARED_FIELDS_BY_CLASS;

    /**
     * A mapping of the declared {@link Method}s by {@link Class}.
     */
    private static final Memoizer<Class<?>, Streamable<Method>> ALL_DECLARED_METHODS_BY_CLASS;

    /**
     * A mapping of the declared {@link Annotation}s by {@link Class} for {@link Class}es.
     */
    private static final Memoizer<
        Class<?>,
        Memoizer<Class<? extends Annotation>, Streamable<? extends Annotation>>> ALL_DECLARED_ANNOTATIONS_BY_CLASS;

    static {
        final var primitiveObjectMappings = new HashMap<Class<?>, Class<?>>();
        primitiveObjectMappings.put(boolean.class, Boolean.class);
        primitiveObjectMappings.put(byte.class, Byte.class);
        primitiveObjectMappings.put(char.class, Character.class);
        primitiveObjectMappings.put(double.class, Double.class);
        primitiveObjectMappings.put(float.class, Float.class);
        primitiveObjectMappings.put(int.class, Integer.class);
        primitiveObjectMappings.put(long.class, Long.class);
        primitiveObjectMappings.put(short.class, Short.class);
        primitiveObjectMappings.put(void.class, Void.class);
        PRIMITIVE_OBJECT_MAPPINGS = java.util.Collections.unmodifiableMap(primitiveObjectMappings);

        final var objectPrimitiveMappings = new HashMap<Class<?>, Class<?>>();
        objectPrimitiveMappings.put(Boolean.class, boolean.class);
        objectPrimitiveMappings.put(Byte.class, byte.class);
        objectPrimitiveMappings.put(Character.class, char.class);
        objectPrimitiveMappings.put(Double.class, double.class);
        objectPrimitiveMappings.put(Float.class, float.class);
        objectPrimitiveMappings.put(Integer.class, int.class);
        objectPrimitiveMappings.put(Long.class, long.class);
        objectPrimitiveMappings.put(Short.class, short.class);
        objectPrimitiveMappings.put(Void.class, void.class);
        OBJECT_PRIMITIVE_MAPPINGS = java.util.Collections.unmodifiableMap(objectPrimitiveMappings);

        final var defaultValues = new HashMap<Class<?>, Object>();
        defaultValues.put(Boolean.class, false);
        defaultValues.put(boolean.class, false);
        defaultValues.put(Byte.class, (byte) 0);
        defaultValues.put(byte.class, (byte) 0);
        defaultValues.put(Character.class, (char) 0);
        defaultValues.put(char.class, (char) 0);
        defaultValues.put(Double.class, 0.0d);
        defaultValues.put(double.class, 0.0d);
        defaultValues.put(Float.class, 0.0f);
        defaultValues.put(float.class, 0.0f);
        defaultValues.put(Integer.class, 0);
        defaultValues.put(int.class, 0);
        defaultValues.put(Long.class, (long) 0);
        defaultValues.put(long.class, (long) 0);
        defaultValues.put(Short.class, (short) 0);
        defaultValues.put(short.class, (short) 0);

        defaultValues.put(Optional.class, Optional.empty());
        defaultValues.put(Stream.class, Stream.empty());
        defaultValues.put(String.class, "");
        DEFAULT_VALUES = java.util.Collections.unmodifiableMap(defaultValues);

        ALL_DECLARED_FIELDS_BY_CLASS = Memoizer.<Class<?>, Streamable<Field>>of(
            clazz -> extractAll(clazz, Class::getDeclaredFields)).concurrent().build();

        ALL_DECLARED_METHODS_BY_CLASS = Memoizer.<Class<?>, Streamable<Method>>of(
            clazz -> extractAll(clazz, Class::getDeclaredMethods)).concurrent().build();

        ALL_DECLARED_ANNOTATIONS_BY_CLASS = Memoizer.<Class<?>, Memoizer<Class<? extends Annotation>, Streamable<? extends Annotation>>>of(
            clazz -> Memoizer.<Class<? extends Annotation>, Streamable<? extends Annotation>>of(
                annotationClass -> extractAll(clazz, c -> c.getDeclaredAnnotationsByType(annotationClass))
            ).concurrent().build()
        ).concurrent().build();
    }

    /**
     * Private constructor for {@link Introspection}.
     */
    private Introspection() {
        // prevent instantiation
    }

    /**
     * Obtains a {@link Stream} of all {@link Field}s, including those that are transitively
     * defined through inheritance for the specified {@link Class}.
     * <p>
     * This method is equivalent to invoking {@link #getAll(Class, Function)} with {@link Class#getDeclaredFields()}.
     *
     * @param clazz the {@link Class}
     * @return a {@link Stream} of {@link Field}s
     * @see Class#getDeclaredFields()
     */
    public static Stream<Field> getAllDeclaredFields(final Class<?> clazz) {
        return ALL_DECLARED_FIELDS_BY_CLASS.compute(clazz)
            .stream();
    }

    /**
     * Obtains a {@link Stream} of all {@link Method}s, including those that are transitively
     * defined either through inheritance or though implemented interfaces for the specified {@link Class}.
     * <p>
     * This method is equivalent to invoking {@link #getAll(Class, Function)} with {@link Class#getDeclaredMethods()}.
     *
     * @param clazz the {@link Class}
     * @return a {@link Stream} of {@link Method}s
     * @see Class#getDeclaredMethods()
     */
    public static Stream<Method> getAllDeclaredMethods(final Class<?> clazz) {
        return ALL_DECLARED_METHODS_BY_CLASS.compute(clazz)
            .stream();
    }

    /**
     * Obtains a {@link Stream} of all {@link Constructor}s, including those that are transitively
     * defined either through inheritance or though implemented interfaces for the specified {@link Class}.
     * <p>
     * This method is equivalent to invoking {@link #getAll(Class, Function)} with
     * {@link Class#getDeclaredConstructors()}.
     *
     * @param clazz the {@link Class}
     * @return a {@link Stream} of {@link Constructor}s
     * @see Class#getDeclaredConstructors()
     */
    public static Stream<Constructor<?>> getAllDeclaredConstructors(final Class<?> clazz) {
        return getAll(clazz, Class::getDeclaredConstructors);
    }

    /**
     * Obtains a {@link Stream} of extracted values, including those that are transitively
     * defined either through inheritance or through implemented interfaces for the specified {@link Class},
     * extracting values for each {@link Class} using the provided {@link Function}.
     *
     * @param <T>       the type of value extracted
     * @param clazz     the {@link Class}
     * @param extractor the {@link Function} to extract an array of values for a {@link Class}
     * @return a {@link Stream} of {@link Method}s
     * @see #extractAll(Class, Function)
     */
    public static <T> Stream<T> getAll(final Class<?> clazz,
                                       final Function<? super Class<?>, T[]> extractor) {

        return extractAll(clazz, extractor)
            .stream();
    }

    /**
     * Obtains a {@link Streamable} of the extracted values, including those that are transitively
     * defined either through inheritance or through implemented interfaces for the specified {@link Class},
     * extracting values for each {@link Class} using the provided {@link Function}.
     *
     * @param <T>       the type of value extracted
     * @param clazz     the {@link Class}
     * @param extractor the {@link Function} to extract an array of values for a {@link Class}
     * @return a {@link Streamable} of extracted values
     */
    public static <T> Streamable<T> extractAll(final Class<?> clazz,
                                               final Function<? super Class<?>, T[]> extractor) {

        Objects.requireNonNull(clazz, "The class must not be null");

        // assume no results (but we want to keep them ordered)
        final var values = new LinkedHashSet<T>();

        // we use a queue to avoid recursion
        final Queue<Class<?>> queue = new LinkedList<>();

        queue.offer(clazz);

        while (!queue.isEmpty()) {

            final var c = queue.poll();

            // extract the values for this class
            for (final T value : extractor.apply(c)) {
                values.add(value);
            }

            // include the interfaces in the search
            for (final Class<?> i : c.getInterfaces()) {
                queue.offer(i);
            }

            // include the parent class in the search
            if (c.getSuperclass() != null) {
                queue.offer(c.getSuperclass());
            }
        }

        return values.isEmpty()
            ? Streamable.empty()
            : Streamable.of(values);
    }

    /**
     * Obtains a {@link Stream} of all {@link Annotation}s by type, including those that are transitively
     * defined either through inheritance or through implemented interfaces, and those that are
     * annotated as {@link Repeatable} for the specified {@link Class}.
     *
     * @param <T>             the type of {@link Annotation}
     * @param clazz           the {@link Class}
     * @param annotationClass the {@link Annotation} {@link Class}
     * @return a {@link Stream} of {@link Annotation}s
     * @see Class#getDeclaredAnnotationsByType(Class)
     */
    public static <T extends Annotation> Stream<T> getAllDeclaredAnnotationsByType(final Class<?> clazz,
                                                                                   final Class<? extends T> annotationClass) {

        return ALL_DECLARED_ANNOTATIONS_BY_CLASS.compute(clazz)
            .compute(annotationClass)
            .stream()
            .map(annotationClass::cast);
    }

    /**
     * Obtains a {@link Stream} of all {@link Annotation}s, including those that are transitively
     * defined either through inheritance or through implemented interfaces, and those that are
     * annotated as {@link Repeatable} for the specified {@link Class}.
     *
     * @param clazz the {@link Class}
     * @return a {@link Stream} of {@link Annotation}s
     * @see Class#getDeclaredAnnotations()
     */
    public static Stream<Annotation> getAllDeclaredAnnotations(final Class<?> clazz) {
        return getAll(clazz, Class::getDeclaredAnnotations);
    }

    /**
     * Indicates whether a given class is annotated with the specified annotation, either on the class itself or
     * transitively define either through inheritance or through implemented interfaces, and those that are
     * annotated as {@link Repeatable} for the specified {@link Class}.
     *
     * @param clazz           the {@link Class}
     * @param annotationClass the {@link Annotation} {@link Class}
     * @return true if the class includes the annotation, false otherwise
     */
    public static boolean hasDeclaredAnnotation(final Class<?> clazz,
                                                final Class<? extends Annotation> annotationClass) {

        return !ALL_DECLARED_ANNOTATIONS_BY_CLASS.compute(clazz)
            .compute(annotationClass)
            .isEmpty();
    }

    /**
     * Obtains a {@link Stream} of all interfaces of the given class, including those that are transitively defined
     * through inheritance or implemented interfaces.
     *
     * @param clazz the {@link Class} for which to obtain interfaces
     * @return a {@link Stream} of interfaces
     * @see Class#getInterfaces()
     */
    public static Stream<Class<?>> getAllInterfaces(final Class<?> clazz) {
        return getAll(clazz, Class::getInterfaces);
    }

    /**
     * Obtains a {@link Stream} of all generic interfaces of the given class, including those that are transitively
     * defined through inheritance or implemented interfaces.
     *
     * @param clazz the {@link Class} for which to obtain generic interfaces
     * @return a {@link Stream} of generic interfaces
     * @see Class#getGenericInterfaces()
     */
    public static Stream<Type> getAllGenericInterfaces(final Class<?> clazz) {
        return getAll(clazz, Class::getGenericInterfaces);
    }

    /**
     * Obtains a description of the specified {@link Class}, suitable for displaying to application developers.
     *
     * @param clazz the {@link Class}
     * @return a {@link String}
     */
    public static String describe(final Class<?> clazz) {
        if (clazz == null) {
            return "null";
        }

        // attempt to obtain the canonical name (it may not be available due to ClassLoader restrictions)
        final String name = clazz.isArray() ? clazz.getComponentType().getName() + "[]" : clazz.getName();

        // optimize the class name
        if (name.startsWith("java.lang")) {
            return name.substring(name.lastIndexOf(".") + 1);
        }

        return name;
    }

    /**
     * Obtains a description of the specified {@link Executable}, suitable for displaying to application developers.
     *
     * @param executable the {@link Executable}
     * @return a {@link String}
     */
    public static String describe(final Executable executable) {
        if (executable == null) {
            return "null";
        }

        final StringBuilder builder = new StringBuilder();

        if (executable instanceof Method method) {
            if (method.getReturnType().equals(void.class)) {
                builder.append("void");
            } else {
                builder.append(describe(method.getReturnType()));
            }

            builder.append(" ");
            builder.append(executable.getName());

        } else if (executable instanceof Constructor<?> constructor) {
            builder.append(describe(constructor.getDeclaringClass()));

        } else {
            // just in case we ever end up with an unknown type of Executable!
            builder.append(executable.getName());
        }

        builder.append("(");
        builder.append(Arrays.stream(executable.getParameters()).map(Introspection::describe).collect(
            Collectors.joining(", ")));
        builder.append(")");

        return builder.toString();
    }

    /**
     * Obtains a description of the specified {@link Parameter}, suitable for displaying to application developers.
     *
     * @param parameter the {@link Parameter}
     * @return a {@link String}
     */
    public static String describe(final Parameter parameter) {
        if (parameter == null) {
            return "null";
        }
        return describe(parameter.getParameterizedType()) + " " + parameter.getName();
    }

    /**
     * Obtains a description of the specified {@link Type}, suitable for displaying to application developers.
     *
     * @param type the {@link Type}
     * @return a {@link String}
     */
    public static String describe(final Type type) {
        if (type == null) {
            return "null";
        }

        if (type instanceof Class) {
            return describe((Class<?>) type);
        }

        // optimize the type name by removing "java.lang."
        return type.toString().replaceFirst("java\\.lang\\.", "");
    }

    /**
     * Obtains a description of the specified {@link Field}, suitable for displaying to application developers.
     *
     * @param field the {@link Field}
     * @return a {@link String}
     */
    public static String describe(final Field field) {
        if (field == null) {
            return "null";
        }

        return describe(field.getGenericType()) + " " + field.getName();
    }

    /**
     * Obtains the {@link Stream} of {@link Method}s on the specified {@link Class} that both satisfy the provided
     * {@link Predicate} and are visible for invocation.
     * <p>
     * A visible {@link Method} is one that is neither private nor has been overridden by a sub-{@link Class} where by
     * the visibility has been made private.
     *
     * @param inspectionClass the {@link Class} to inspect
     * @param predicate       the {@link Predicate}, {@code null} indicates all {@link Method}s
     * @return a {@link Stream} of {@link Method}s
     */
    public static Stream<Method> getVisibleMethods(final Class<?> inspectionClass,
                                                   final Predicate<? super Method> predicate) {

        // the methods by their formal signature
        final var methods = new LinkedHashMap<String, Method>();

        Introspection.getAllDeclaredMethods(inspectionClass)
            .forEach(method -> {
                if (predicate == null || predicate.test(method)) {
                    final String signature = getFormalMethodSignature(method);
                    methods.putIfAbsent(signature, method);
                }
            });

        return methods.values().stream();
    }

    /**
     * Obtains the formal signature of a {@link Method}, including its access modifiers, package of declaration and
     * return and formal parameters.
     *
     * @param method the {@link Method}
     * @return the method name
     */
    public static String getFormalMethodSignature(final Method method) {
        final int modifiers = method.getModifiers();

        // start with the generic name
        final StringBuilder builder = new StringBuilder();

        // include the package when the method has package visibility
        if (!Modifier.isPublic(modifiers)
            && !Modifier.isPrivate(modifiers)
            && !Modifier.isProtected(modifiers)) {

            builder.append(method.getDeclaringClass().getPackage());
            builder.append(' ');
        }

        // include the declaring class name when the method is private
        if (Modifier.isPrivate(modifiers)) {
            builder.append(method.getDeclaringClass().getName());
            builder.append('.');
        }

        // include the return type
        builder.append(method.getReturnType());
        builder.append(' ');

        // include the method name
        builder.append(method.getName());

        // include the method parameter types
        builder.append('(');
        if (method.getParameterCount() > 0) {
            builder.append(Arrays.toString(method.getParameterTypes()));
        }
        builder.append(')');

        return builder.toString();
    }

    /**
     * Converts a primitive class into its boxed class. If the class provided is null or not a primitive, this method
     * returns the class without conversion.
     *
     * @param aClass the class to convert
     * @return the boxed class
     */
    public static Class<?> getBoxedClass(final Class<?> aClass) {
        return PRIMITIVE_OBJECT_MAPPINGS.getOrDefault(aClass, aClass);
    }

    /**
     * Converts a primitive class into its boxed class. If the class provided is null or not a primitive, this method
     * returns an empty Optional
     *
     * @param aClass the class to convert
     * @return the boxed class
     */
    public static Optional<Class<?>> getBoxedClassIfPrimitive(final Class<?> aClass) {
        return Optional.ofNullable(PRIMITIVE_OBJECT_MAPPINGS.get(aClass));
    }

    /**
     * Converts a non-primitive class into its primitive class. If the class provided is null or not a boxed primitive,
     * this method returns the class without conversion.
     *
     * @param aClass the class to convert
     * @return the unboxed class
     */
    public static Class<?> getUnboxedClass(final Class<?> aClass) {
        return OBJECT_PRIMITIVE_MAPPINGS.getOrDefault(aClass, aClass);
    }

    /**
     * Converts a non-primitive class into its primitive class. If the class provided is null or not a boxed primitive,
     * this method returns an empty Optional
     *
     * @param aClass the class to convert
     * @return the unboxed class
     */
    public static Optional<Class<?>> getUnboxedClassIfBoxed(final Class<?> aClass) {
        return Optional.ofNullable(OBJECT_PRIMITIVE_MAPPINGS.get(aClass));
    }

    /**
     * For a {@link Type}, returns an {@link Optional} of the {@link Class} of the type in the following manner:
     * <ul>
     *     <li>For a {@link Class}, returns an {@link Optional} of the class</li>
     *     <li>For a {@link ParameterizedType}, returns an {@link Optional} of the raw type</li>
     *     <li>For a {@link WildcardType}, returns an {@link Optional} of the upper bounds if itself is an instanceof
     *         Class</li>
     *     <li>Otherwise, returns {@link Optional#empty()}</li>
     * </ul>
     *
     * @param type the {@link Type} for which to get the class
     * @return an {@link Optional} of the {@link Class} of the {@link Type}
     */
    public static Optional<Class<?>> getClassFromType(final Type type) {
        if (type instanceof Class) {
            return Optional.of((Class<?>) type);
        }
        if (type instanceof ParameterizedType) {
            return Optional.of((Class<?>) ((ParameterizedType) type).getRawType());
        }
        if (type instanceof WildcardType
            && ((WildcardType) type).getUpperBounds().length == 1
            && ((WildcardType) type).getUpperBounds()[0] instanceof Class) {
            return Optional.of((Class<?>) ((WildcardType) type).getUpperBounds()[0]);
        }
        return Optional.empty();
    }

    /**
     * For a {@link Type}, returns an {@link Optional} of the first parameter {@link Type}. If the {@link Type} is a
     * {@link Class}, it will return the first encountered parameterized type of any parameterized classes in its
     * superclass or interfaces. If a targeted parameterized class is desired, use
     * {@link #getParameterType(Class, Class)}.
     *
     * @param type the {@link Type} for which to get the parameter type
     * @return an {@link Optional} of the first parameter {@link Type}
     */
    public static Optional<Type> getParameterType(final Type type) {
        return parameterTypes(type).findFirst();
    }

    /**
     * For a {@link Class}, returns an {@link Optional} of the first parameter {@link Type} for the given parameterized
     * class.
     *
     * @param targetClass        the class for which to extract the parameterized type
     * @param parameterizedClass the class to filter against when selecting the parameterized type to return
     * @return an {@link Optional} of the {@link Type} of the parameterized {@link Class} for the target {@link Class}
     */
    public static Optional<Type> getParameterType(final Class<?> targetClass, final Class<?> parameterizedClass) {
        return parameterTypes(targetClass, parameterizedClass).findFirst();
    }

    /**
     * For a {@link Type}, returns an {@link Stream} of the parameter {@link Type}s. If the type is a {@link Class} and
     * the parameter types of a targeted parameterized class is desired, use {@link #parameterTypes(Class, Class)}.
     *
     * @param type the {@link Type} for which to get the parameter type
     * @return a {@link Stream} of the parameter {@link Type}s
     */
    public static Stream<Type> parameterTypes(final Type type) {
        return Stream.of(type)
            .filter(ParameterizedType.class::isInstance)
            .map(ParameterizedType.class::cast)
            .map(ParameterizedType::getActualTypeArguments)
            .filter(Objects::nonNull)
            .flatMap(Stream::of);
    }

    /**
     * For a {@link Class}, returns a {@link Stream} of the parameter {@link Type}s for the given parameterized class.
     *
     * @param targetClass        the class for which to extract the parameterized type
     * @param parameterizedClass the class to filter against when selecting the parameterized type to return
     * @return a {@link Stream} of the {@link Type}s of the parameterized {@link Class} for the target {@link Class}
     */
    public static Stream<Type> parameterTypes(final Class<?> targetClass, final Class<?> parameterizedClass) {
        return parameterTypes(targetClass,
            parameterizedType -> parameterizedType.getRawType().equals(parameterizedClass));
    }

    /**
     * For a {@link Class}, returns a {@link Stream} of the parameter {@link Type}s for the given parameterized
     * class filter.
     *
     * @param targetClass              the class for which to extract the parameterized type
     * @param parameterizedClassFilter the class to filter against when selecting the parameterized type to return
     * @return a {@link Stream} of the {@link Type}s of the parameterized {@link Class} for the target {@link Class}
     */
    private static Stream<Type> parameterTypes(final Class<?> targetClass,
                                               final Predicate<ParameterizedType> parameterizedClassFilter) {
        return Stream.concat(Stream.of(targetClass.getGenericSuperclass()),
                Arrays.stream(targetClass.getGenericInterfaces()))
            .filter(ParameterizedType.class::isInstance)
            .map(ParameterizedType.class::cast)
            .filter(parameterizedClassFilter)
            .map(ParameterizedType::getActualTypeArguments)
            .filter(Objects::nonNull)
            .flatMap(Stream::of);
    }

    /**
     * Recursively determines whether an {@link AnnotatedElement} has any {@link Annotation}s on it that are
     * annotated with the provided annotation class.
     *
     * @param annotatedElement the {@link AnnotatedElement}
     * @param annotationClass  the {@link Annotation} class to look for
     * @return true if the {@link AnnotatedElement} has the {@link Annotation}
     */
    public static boolean hasMetaAnnotation(final AnnotatedElement annotatedElement,
                                            final Class<? extends Annotation> annotationClass) {
        final Set<Class<? extends Annotation>> annotationTypes = new HashSet<>();
        collectHierarchicalAnnotations(annotatedElement, annotationTypes);
        return annotationTypes.stream()
            .anyMatch(c -> c.isAnnotationPresent(annotationClass));
    }

    /**
     * For a given {@link AnnotatedElement}, collects all annotation classes and adds them to the provided set. If the
     * added annotation class has not yet been processed, it will recursively collect its annotation classes.
     *
     * @param annotatedElement the {@link AnnotatedElement}
     * @param set              the set in which to collect the annotation classes
     */
    private static void collectHierarchicalAnnotations(final AnnotatedElement annotatedElement,
                                                       final Set<Class<? extends Annotation>> set) {
        Stream.of(annotatedElement.getAnnotations())
            .map(Annotation::annotationType)
            .forEach(c -> {
                if (set.add(c)) {
                    collectHierarchicalAnnotations(c, set);
                }
            });
    }

    /**
     * Determine whether the {@link Class} or any of its {@link Annotation}s are annotated with the provided {@link Annotation}.
     *
     * @param clazz           the {@link Class}
     * @param annotationClass the {@link Annotation} class to look for
     * @return true if the {@link AnnotatedElement} has the {@link Annotation}
     */
    public static boolean hasHierarchyAnnotation(final Class<?> clazz,
                                                 final Class<? extends Annotation> annotationClass) {
        return hasDeclaredAnnotation(clazz, annotationClass) || hasMetaAnnotation(clazz, annotationClass);
    }

    /**
     * Return the direct or meta annotation if present on the given class
     *
     * @param clazz           the {@link Class}
     * @param annotationClass the {@link Annotation} class to look for
     * @param <A>             the annotation type
     * @return an Optional containing the annotation; never null but potentially empty
     */
    public static <A extends Annotation> Optional<A> findAnnotation(final Class<?> clazz,
                                                                    final Class<A> annotationClass) {
        Objects.requireNonNull(annotationClass, "annotationClass must not be null");
        if (hasDeclaredAnnotation(clazz, annotationClass)) {
            return getAllDeclaredAnnotationsByType(clazz, annotationClass).findFirst();
        } else if (hasMetaAnnotation(clazz, annotationClass)) {
            final Set<Class<? extends Annotation>> metaAnnotations = new HashSet<>();
            collectHierarchicalAnnotations(clazz, metaAnnotations);
            final Class<? extends Annotation> targetAnnotation = metaAnnotations.stream()
                .filter(c -> c.isAnnotationPresent(annotationClass))
                .findFirst().get();
            return Optional.of(targetAnnotation.getAnnotation(annotationClass));
        }
        return Optional.empty();
    }

    /**
     * Obtains the default value for the specified {@link Class}.  For primitive types or boxed types,
     * the default value is non-{@code null}.  For all other {@link Object} types, the default value is {@code null}.
     *
     * @param aClass the {@link Class} for which to obtain the default value
     * @param <T>    the type of {@link Class}
     * @return the default value
     */
    @SuppressWarnings("unchecked")
    public static <T> T getDefaultValue(final Class<T> aClass) {
        return aClass == null
            ? null
            : (T) DEFAULT_VALUES.getOrDefault(aClass, null);
    }

    /**
     * Obtains a {@link Stream} of primitive {@link Class}es.
     *
     * @return a {@link Stream} of primitive {@link Class}es
     */
    public static Stream<Class<?>> primitives() {
        return PRIMITIVE_OBJECT_MAPPINGS.keySet()
            .stream()
            .sorted(Comparator.comparing(Class::getName));
    }

    /**
     * Obtains a {@link Stream} of boxed {@link Class}es.
     *
     * @return a {@link Stream} of boxed {@link Class}es
     */
    public static Stream<Class<?>> boxed() {
        return OBJECT_PRIMITIVE_MAPPINGS.keySet()
            .stream()
            .sorted(Comparator.comparing(Class::getName));
    }
}
