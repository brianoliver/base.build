package build.base.marshalling;

/*-
 * #%L
 * base.build Marshalling
 * %%
 * Copyright (C) 2025 Workday Inc
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
import build.base.foundation.Introspection;
import build.base.foundation.Lazy;
import build.base.foundation.Preconditions;
import build.base.foundation.stream.Streamable;
import build.base.foundation.stream.Streams;
import build.base.foundation.tuple.Pair;

import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A concurrent implementation of a {@link SchemaFactory}.
 *
 * @author brian.oliver
 * @since Nov-2024
 */
public class ConcurrentSchemaFactory
    implements SchemaFactory {

    /**
     * The {@link MarshallingSchema}s that may be used for {@link Marshal}ling by a <i>marshallable</i> {@link Class}.
     */
    private final ConcurrentHashMap<Class<?>, MarshallingSchema<?>> marshallingSchemasByClass;

    /**
     * The {@link UnmarshallingSchema}s that may be used for {@link Unmarshal}ling by a <i>marshallable</i> {@link Class}.
     */
    private final ConcurrentHashMap<Class<?>, LinkedHashSet<UnmarshallingSchema<?>>> unmarshallingSchemasByClass;

    /**
     * The {@link Class}es that are known to be unmarshallable (for some reason).
     */
    private final CopyOnWriteArraySet<Class<?>> unmarshallableClasses;

    /**
     * The {@link Schema}s for explicitly registered enum {@link Class}es.
     * Enums cannot declare {@link Marshal}/{@link Unmarshal} methods, so they are registered
     * separately via {@link #registerEnum(Class)} and serialized by their {@link Enum#name()}.
     */
    private final ConcurrentHashMap<Class<?>, Schema<?>> enumSchemasByClass;

    /**
     * Constructs the {@link ConcurrentSchemaFactory}.
     */
    public ConcurrentSchemaFactory() {
        this.marshallingSchemasByClass = new ConcurrentHashMap<>();
        this.unmarshallingSchemasByClass = new ConcurrentHashMap<>();
        this.unmarshallableClasses = new CopyOnWriteArraySet<>();
        this.enumSchemasByClass = new ConcurrentHashMap<>();
    }

    /**
     * Ensure the specified {@link Class} has been loaded, and thus the {@code static} initializations have
     * been performed.
     *
     * @param initializableClass the {@link Class} to initialize
     */
    private void ensureInitialized(final Class<?> initializableClass) {

        // ensure the class has been loaded and thus initialized
        // (it may have static initializers that we need for registration)
        if (initializableClass != null
            && !this.marshallingSchemasByClass.containsKey(initializableClass)
            && !this.unmarshallableClasses.contains(initializableClass)) {

            try {
                Class.forName(initializableClass.getName());
            } catch (final ClassNotFoundException e) {
                this.unmarshallableClasses.add(initializableClass);
                throw new IllegalArgumentException("Failed to load " + initializableClass, e);
            }
        }
    }

    @Override
    public boolean isMarshallable(final Class<?> marshallableClass) {
        if (marshallableClass == null || this.unmarshallableClasses.contains(marshallableClass)) {
            return false;
        } else if (this.enumSchemasByClass.containsKey(marshallableClass)) {
            return true;
        } else {
            ensureInitialized(marshallableClass);
            if (this.unmarshallableClasses.contains(marshallableClass)) {
                return false;
            } else if (this.marshallingSchemasByClass.containsKey(marshallableClass)) {
                return true;
            } else {
                try {
                    register(marshallableClass);
                    return this.marshallingSchemasByClass.containsKey(marshallableClass);
                } catch (final RuntimeException e) {
                    this.unmarshallableClasses.add(marshallableClass);
                    return false;
                }
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void register(final Class<T> marshallableClass,
                             final MethodHandles.Lookup lookup) {

        Objects.requireNonNull(marshallableClass, "The marshallable class must not be null");
        Objects.requireNonNull(lookup, "The MethodHandles.Lookup for the marshallable class must not be null");

        Preconditions.require(marshallableClass, marshallableClass == lookup.lookupClass(),
            "The MethodHandles.Lookup must be for the marshallable class");

        ensureInitialized(marshallableClass);

        // -----------
        // discover the @Marshal annotated Methods (there should be only one)
        final var marshalMethods = Stream.of(marshallableClass.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(Marshal.class))
            .toList();

        if (marshalMethods.isEmpty()) {
            this.unmarshallableClasses.add(marshallableClass);
            throw new IllegalArgumentException("The class " + marshallableClass.getName()
                + " is not marshallable as it has no @Marshal annotated method");
        }

        if (marshalMethods.size() > 1) {
            this.unmarshallableClasses.add(marshallableClass);
            throw new IllegalArgumentException(
                "The class " + marshallableClass.getName() + " has more than one @Marshal method");
        }

        // establish the Marshalling Schema for the @Marshal method
        final var marshalMethod = marshalMethods.getFirst();

        // ensure the @Marshal method is accessible (so we can call it)
        marshalMethod.setAccessible(true);

        // for now ensure the @Marshall method is non-static
        if (Modifier.isStatic(marshalMethod.getModifiers())) {
            this.unmarshallableClasses.add(marshallableClass);
            throw new IllegalArgumentException(
                "The class " + marshallableClass.getName() + " @Marshal method must not be static");
        }

        final var marshalDependencies = new LinkedList<Dependency>();

        // obtain the @Marshal Out parameters using the method parameter names
        final var marshalParameters = Streams.zip(
                Arrays.stream(marshalMethod.getParameters())
                    .map(java.lang.reflect.Parameter::getName),
                Arrays.stream(marshalMethod.getGenericParameterTypes()))

            .peek(pair -> {
                // determine the @Bound / Marshaller dependency used as a Parameter
                if (pair.second().getClass().isAnnotationPresent(Bound.class)
                    || Introspection.getClassFromType(pair.second())

                    .map(parameterClass -> parameterClass.equals(Marshaller.class))
                    .orElse(false)) {
                    marshalDependencies.add(new Dependency() {
                        @Override
                        public String name() {
                            return pair.first();
                        }

                        @Override
                        public Type type() {
                            return pair.second();
                        }
                    });
                }
            })

            // only use the Out<T> parameters
            .filter(pair -> pair.second() instanceof ParameterizedType parameterizedType
                && parameterizedType.getRawType().equals(Out.class))
            .map(pair -> Pair.of(
                pair.first(),
                Introspection.getParameterType(pair.second()).orElse(Object.class)))
            .map(pair -> new Parameter() {
                @Override
                public String name() {
                    return pair.first();
                }

                @Override
                public Type type() {
                    return pair.second();
                }
            })
            .map(parameter -> (Parameter) parameter)
            .toList();

        this.marshallingSchemasByClass.putIfAbsent(marshallableClass, new MarshallingSchema<>(
            marshallableClass,
            marshalMethod,
            marshalDependencies.stream(),
            marshalParameters.stream()));

        // -----------
        // fail fast if @Unmarshal is mistakenly placed on a method (only constructors are supported)
        final var unmarshalMethods = Stream.of(marshallableClass.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(Unmarshal.class))
            .toList();

        if (!unmarshalMethods.isEmpty()) {
            this.unmarshallableClasses.add(marshallableClass);
            throw new IllegalArgumentException("The class " + marshallableClass.getName()
                + " has @Unmarshal on method(s) " + unmarshalMethods.stream()
                .map(Method::getName).toList()
                + " — @Unmarshal is only supported on constructors");
        }

        // -----------
        // discover the @Unmarshal annotated Constructors (there may be many)
        final var unmarshallConstructors = Stream.of(marshallableClass.getDeclaredConstructors())
            .filter(constructor -> constructor.isAnnotationPresent(Unmarshal.class))
            .toList();

        if (unmarshallConstructors.isEmpty()) {
            this.unmarshallableClasses.add(marshallableClass);
            throw new IllegalArgumentException("The class " + marshallableClass.getName()
                + " is not marshallable as it has no @Unmarshal annotated constructors");
        }

        // establish an unmarshalling schema for each of the @Unmarshal constructors
        this.unmarshallingSchemasByClass.compute(marshallableClass, (_, existing) -> {

            final var schemas = existing == null
                ? new LinkedHashSet<UnmarshallingSchema<?>>()
                : existing;

            if (schemas.isEmpty()) {
                unmarshallConstructors.stream()
                    .map(constructor -> (Constructor<T>) constructor)
                    .forEach(constructor -> {
                        // obtain the @Unmarshal parameters using the method parameter names and classes
                        final var unmarshalParameters = new LinkedHashMap<String, Parameter>();
                        final var unmarshalDependencies = new LinkedList<Dependency>();

                        Streams.zip(
                                Arrays.stream(constructor.getParameters())
                                    .map(java.lang.reflect.Parameter::getName),
                                Arrays.stream(constructor.getGenericParameterTypes()),
                                Arrays.stream(constructor.getParameterAnnotations()))
                            .filter(triple -> {
                                final var parameterClass = Introspection.getClassFromType(triple.second())
                                    .orElse(Object.class);

                                // obtain the @Bound / Marshaller dependency as a Parameter
                                if (parameterClass.equals(Marshaller.class)
                                    || Arrays.stream(triple.third()).anyMatch(Bound.class::isInstance)) {

                                    unmarshalDependencies.add(new Dependency() {
                                        @Override
                                        public String name() {
                                            return triple.first();
                                        }

                                        @Override
                                        public Type type() {
                                            return triple.second();
                                        }
                                    });

                                    return false;
                                } else {
                                    return true;
                                }
                            })

                            .map(triple -> Pair.of(
                                triple.first(),
                                triple.second() instanceof Class<?> classType
                                    ? Introspection.getBoxedClass(classType)
                                    : triple.second()))
                            .forEach(pair -> unmarshalParameters.put(pair.first(), new Parameter() {
                                @Override
                                public String name() {
                                    return pair.first();
                                }

                                @Override
                                public Type type() {
                                    return pair.second();
                                }
                            }));

                        schemas.addLast(new UnmarshallingSchema<>(
                            marshallableClass,
                            constructor,
                            unmarshalDependencies.stream(),
                            unmarshalParameters.values().stream()));
                    });
            }

            return schemas;
        });

        // TODO: ensure that there's no contradictions in parameter types with the same name.  The types must be
        //  exactly the same!

        // TODO: ensure that at least one of the unmarshalling schemas is compatible with the marshalling schema
        // (this is to ensure that we can unmarshal something we can marshal!)
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Enum<E>> void registerEnum(final Class<E> enumClass) {

        Objects.requireNonNull(enumClass, "The enum class must not be null");

        if (!enumClass.isEnum()) {
            throw new IllegalArgumentException("Class " + enumClass.getName() + " is not an enum");
        }

        final Parameter nameParam = new Parameter() {
            @Override
            public String name() {
                return "name";
            }

            @Override
            public Type type() {
                return String.class;
            }
        };

        final Schema<E> schema = new Schema<>() {
            @Override
            public Class<E> owner() {
                return enumClass;
            }

            @Override
            public Streamable<Parameter> parameters() {
                return Streamable.of(Stream.of(nameParam));
            }

            @Override
            public Optional<Parameter> getParameter(final String n) {
                return "name".equals(n) ? Optional.of(nameParam) : Optional.empty();
            }

            @Override
            public Streamable<Dependency> dependencies() {
                return Streamable.of(Stream.empty());
            }
        };

        this.enumSchemasByClass.putIfAbsent(enumClass, schema);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<Schema<T>> getMarshallingSchema(final Class<T> marshallableClass) {

        if (marshallableClass == null) {
            return Optional.empty();
        }

        final var enumSchema = (Schema<T>) this.enumSchemasByClass.get(marshallableClass);
        if (enumSchema != null) {
            return Optional.of(enumSchema);
        }

        ensureInitialized(marshallableClass);

        return Optional.ofNullable((Schema<T>) this.marshallingSchemasByClass.get(marshallableClass));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Stream<Schema<T>> getUnmarshallingSchemas(final Class<T> marshallableClass) {

        if (marshallableClass == null) {
            return Stream.empty();
        }

        final var enumSchema = (Schema<T>) this.enumSchemasByClass.get(marshallableClass);
        if (enumSchema != null) {
            return Stream.of(enumSchema);
        }

        ensureInitialized(marshallableClass);

        final var schemas = this.unmarshallingSchemasByClass.get(marshallableClass);

        return schemas == null
            ? Stream.empty()
            : schemas.stream()
            .map(schema -> (Schema<T>) schema);
    }

    @Override
    public Marshaller newMarshaller() {
        return new HierarchicalMarshaller();
    }

    /**
     * A {@link Schema} implementation that supports marshalling.
     *
     * @param <T> the type to be marshalled
     */
    private static class MarshallingSchema<T>
        implements Schema<T> {

        /**
         * The marshallable {@link Class}.
         */
        private final Class<T> marshallableClass;

        /**
         * The {@link Method} to destruct a {@code T} into parameter values for a {@link Marshalled}.
         */
        private final Method destructor;

        /**
         * The {@link Bound} {@link Dependency}s.
         */
        private final LinkedHashMap<String, Dependency> dependencies;

        /**
         * The ordered {@link Parameter}s by name.
         */
        private final LinkedHashMap<String, Parameter> parameters;

        /**
         * Constructs a {@link MarshallingSchema}.
         *
         * @param marshallableClass the {@link Class} to be marshalled
         * @param destructor        the {@link Method} of the destructor
         * @param dependencies      the {@link Dependency}s in order of use
         * @param parameters        the <i>marshallable</i> {@link Parameter}s in order of use
         */
        MarshallingSchema(final Class<T> marshallableClass,
                          final Method destructor,
                          final Stream<Dependency> dependencies,
                          final Stream<Parameter> parameters) {

            this.marshallableClass = Objects.requireNonNull(marshallableClass,
                "The marshallable Class must not be null");
            this.destructor = Objects.requireNonNull(destructor, "The destructor Method must not be null");

            this.dependencies = new LinkedHashMap<>();
            if (dependencies != null) {
                dependencies.forEach(dependency -> this.dependencies.put(dependency.name(), dependency));
            }

            this.parameters = new LinkedHashMap<>();
            if (parameters != null) {
                parameters.forEach(parameter -> this.parameters.put(parameter.name(), parameter));
            }
        }

        @Override
        public Class<T> owner() {
            return this.marshallableClass;
        }

        @Override
        public Streamable<Parameter> parameters() {
            return Streamable.of(this.parameters.values());
        }

        @Override
        public Optional<Parameter> getParameter(final String name) {
            return Optional.of(this.parameters.get(name));
        }

        @Override
        public Streamable<Dependency> dependencies() {
            return Streamable.of(this.dependencies.values());
        }

        /**
         * Marshals the specified {@link Object} using this {@link Schema}.
         *
         * @param object     the {@link Object} to marshal
         * @param marshaller the {@link HierarchicalMarshaller} to use for marshalling
         * @return a {@link Marshalled} representation of the {@link Object}
         */
        public Marshalled<T> marshal(final T object,
                                     final HierarchicalMarshaller marshaller) {
            Objects.requireNonNull(object, "The object to marshal must not be null");

            // establish the arguments for the destructor method
            final var arguments = new Object[this.destructor.getParameterCount()];
            final var index = new AtomicInteger(0);

            // include the dependencies as arguments
            this.dependencies.forEach((name, dependency) -> arguments[index.getAndIncrement()] = marshaller
                .getValue(dependency)
                .orElseThrow(() -> new IllegalArgumentException("Unsatisfied dependency [" + dependency.name() + "]"
                    + " for [" + owner().getName() + "]")));

            // create Outs for the parameters as arguments
            final var outParameters = new LinkedHashMap<String, Out<?>>();
            this.parameters.forEach((name, _) -> {
                final var outParameter = Out.empty();
                arguments[index.getAndIncrement()] = outParameter;
                outParameters.put(name, outParameter);
            });

            try {
                // invoke the destructor allow the Out parameters to be marshalled
                this.destructor.invoke(object, arguments);

                // create a new Marshalled for the Object
                return new Marshalled<>() {
                    @Override
                    public Schema<T> schema() {
                        return MarshallingSchema.this;
                    }

                    @Override
                    public Streamable<Object> values() {
                        return Streamable.of(outParameters.values()
                            .stream()
                            .map(out -> out.orElse(null)));
                    }
                };
            } catch (final IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to marshal " + object, e);
            }
        }
    }

    /**
     * A {@link Schema} implementation that supports unmarshalling.
     *
     * @param <T> the type to be unmarshalled
     */
    private static class UnmarshallingSchema<T>
        implements Schema<T> {

        /**
         * The marshallable {@link Class}.
         */
        private final Class<T> marshallableClass;

        /**
         * The {@link Constructor} to construct a {@code T} with parameter values from a {@link Marshalled}.
         */
        private final Constructor<T> constructor;

        /**
         * The {@link Bound} {@link Parameter} dependencies.
         */
        private final LinkedHashMap<String, Dependency> dependencies;

        /**
         * The ordered {@link Parameter}s by name.
         */
        private final LinkedHashMap<String, Parameter> parameters;

        /**
         * Constructs a {@link UnmarshallingSchema}.
         *
         * @param marshallableClass the {@link Class} to be marshalled
         * @param constructor       the {@link Constructor}
         * @param dependencies      the {@link Dependency}s in order of use
         * @param parameters        the <i>marshallable</i> {@link Parameter}s in order of use
         */
        UnmarshallingSchema(final Class<T> marshallableClass,
                            final Constructor<T> constructor,
                            final Stream<Dependency> dependencies,
                            final Stream<Parameter> parameters) {

            this.marshallableClass = Objects.requireNonNull(marshallableClass,
                "The marshallable Class must not be null");
            this.constructor = Objects.requireNonNull(constructor, "The Constructor must not be null");

            this.dependencies = new LinkedHashMap<>();
            if (dependencies != null) {
                dependencies.forEach(dependency -> this.dependencies.put(dependency.name(), dependency));
            }

            this.parameters = new LinkedHashMap<>();
            if (parameters != null) {
                parameters.forEach(parameter -> this.parameters.put(parameter.name(), parameter));
            }
        }

        @Override
        public Class<T> owner() {
            return this.marshallableClass;
        }

        @Override
        public Streamable<Parameter> parameters() {
            return Streamable.of(this.parameters.values());
        }

        @Override
        public Optional<Parameter> getParameter(final String name) {
            return Optional.of(this.parameters.get(name));
        }

        @Override
        public Streamable<Dependency> dependencies() {
            return Streamable.of(this.dependencies.values());
        }

        /**
         * Unmarshal the specified {@link Marshalled} into an {@link Object} using this {@link Schema}.
         *
         * @param marshalled             the {@link Marshalled} to unmarshal
         * @param hierarchicalMarshaller the parent {@link HierarchicalMarshaller} for further unmarshalling
         * @return an {@link Object} representation of the {@link Marshalled}
         */
        public T unmarshal(final Marshalled<T> marshalled,
                           final HierarchicalMarshaller hierarchicalMarshaller) {

            Objects.requireNonNull(marshalled, "The Marshalled must not be null");
            Objects.requireNonNull(hierarchicalMarshaller, "The Marshaller must not be null");

            // establish a new Marshaller to perform unmarshalling
            final var marshaller = hierarchicalMarshaller.newMarshaller();

            // establish the arguments for the constructor
            final var arguments = new Object[this.constructor.getParameterCount()];
            final var index = new AtomicInteger(0);

            // include the dependencies defined by the unmarshalling schema (as arguments)
            this.dependencies.values()
                .forEach(dependency -> arguments[index.getAndIncrement()] = marshaller
                    .getValue(dependency)
                    .orElseThrow(() -> new IllegalArgumentException("Unsatisfied dependency [" + dependency.name() + "]"
                        + " for marshalled [" + marshalled.schema().owner().getName() + "]")));

            // include the parameter values (as arguments) - a parameter declared as Lazy<X> is willing to
            // receive its value once it becomes available rather than up front, so a value that has only
            // been decoded as far as a not-yet-unmarshalled Marshalled<X> (see JsonTransport's handling of
            // a Lazy<T> parameter) is wrapped to defer that unmarshal() until first access, using this
            // unmarshal() call's own Marshaller - the one actually doing the constructing - rather than
            // whichever Marshaller originally decoded the value. This is what allows a genuine cycle to be
            // reconstructed: by the time something calls Lazy.get(), the object this Lazy participates in -
            // including, for a self-reference, the very object under construction here - has finished
            // constructing and is resolvable.
            Streams.zip(this.parameters.values().stream(), marshalled.values().stream())
                .forEach(pair -> arguments[index.getAndIncrement()] =
                    deferLazyValue(pair.first().type(), pair.second(), marshaller));

            try {
                // invoke the constructor to perform unmarshalling
                return this.constructor.newInstance(arguments);
            } catch (final IllegalAccessException | InstantiationException | InvocationTargetException e) {
                throw new RuntimeException("Failed to unmarshal " + marshalled.schema().owner().getCanonicalName(), e);
            }
        }

        /**
         * Defers unmarshalling a value destined for a {@link Lazy}-typed constructor parameter - {@code Lazy<X>}
         * directly, or nested one level inside {@code Optional<Lazy<X>>}, {@code Stream<Lazy<X>>}, or
         * {@code Streamable<Lazy<X>>} - so a not-yet-unmarshalled {@link Marshalled} value is wrapped in a
         * {@link Lazy} that unmarshal()s it (using {@code marshaller}) on first access, rather than now. Any
         * other parameter shape is passed through unchanged.
         *
         * @param parameterType the constructor parameter's declared {@link Type}
         * @param value         the value decoded for it
         * @param marshaller    the {@link Marshaller} to use once the {@link Lazy} is eventually resolved
         * @return the value to use as the constructor argument
         */
        private static Object deferLazyValue(final Type parameterType,
                                             final Object value,
                                             final Marshaller marshaller) {

            final var parameterClass = Introspection.getClassFromType(parameterType).orElse(null);

            if (Lazy.class.equals(parameterClass) && value instanceof Marshalled<?> nestedMarshalled) {
                return Lazy.of(() -> marshaller.unmarshal(nestedMarshalled));
            }

            if (Optional.class.equals(parameterClass) && value instanceof Optional<?> optionalValue) {
                final var elementType = Introspection.getParameterType(parameterType).orElse(null);
                if (elementType != null) {
                    return optionalValue.map(element -> deferLazyValue(elementType, element, marshaller));
                }
            }

            if (Stream.class.equals(parameterClass) && value instanceof Stream<?> streamValue) {
                final var elementType = Introspection.getParameterType(parameterType).orElse(null);
                if (elementType != null) {
                    return streamValue.map(element -> deferLazyValue(elementType, element, marshaller));
                }
            }

            if (Streamable.class.equals(parameterClass) && value instanceof Streamable<?> streamableValue) {
                final var elementType = Introspection.getParameterType(parameterType).orElse(null);
                if (elementType != null) {
                    return Streamable.of(streamableValue.stream()
                        .map(element -> deferLazyValue(elementType, element, marshaller)));
                }
            }

            return value;
        }
    }

    /**
     * A {@link Marshaller} implementation permitting the creation of sub-{@link Marshaller}s and the hierarchical
     * resolution of {@link Dependency}s during <i>marshalling</i> and <i>unmarshalling</i>.
     */
    private class HierarchicalMarshaller
        implements Marshaller {

        /**
         * The {@link Optional} parent {@link HierarchicalMarshaller}.
         */
        private final Optional<HierarchicalMarshaller> parent;

        /**
         * The {@link Binding}s defined for {@code this} {@link HierarchicalMarshaller} by {@link Type}.
         */
        private final ConcurrentHashMap<Type, Binding<?>> bindings;

        /**
         * The identity {@link Set} of {@link Object}s currently being marshalled (i.e. still on the call stack)
         * by {@code this} {@link HierarchicalMarshaller}. Used to detect a genuine cycle - an {@link Object}
         * that is its own ancestor - which is reported via {@link CyclicMarshallingException}.
         */
        private final Set<Object> inProgress;

        /**
         * The identity {@link Map} of {@link Object}s already marshalled by {@code this}
         * {@link HierarchicalMarshaller} to their {@link Marshalled} representation. Ensures an {@link Object}
         * reached more than once (whether or not it is genuinely cyclic) is only ever destructed once, and is
         * represented by the same {@link Marshalled} instance every time thereafter.
         */
        private final Map<Object, Marshalled<?>> completed;

        /**
         * The {@link Object} unmarshalled from each identified {@link Marshalled} - only ever populated and
         * consulted on the root {@link HierarchicalMarshaller} (see {@link #root()}), since
         * {@link #newMarshaller()} otherwise creates a fresh child for every nested unmarshal.
         * <p>
         * Keyed by the {@link Marshalled} instance itself (relying on {@link Marshalled} implementations not
         * overriding {@code equals}/{@code hashCode}, as {@link WeakHashMap} compares keys with them),
         * <em>not</em> by its {@link Marshalled#id()}: an id is only unique within the one document it was
         * assigned in, whereas this {@link HierarchicalMarshaller} is typically reused across many documents.
         * Both weakly keyed and weakly valued, so neither a {@link Marshalled} (and its document) nor the
         * {@link Object} unmarshalled from it is retained solely by this map. The value must be weak too: a
         * self-referential object holds a deferred reference back to the {@link Marshalled} it was unmarshalled
         * from, so a strong value would strongly reach its own key and the entry would never be collected. Once
         * nothing else holds the {@link Object}, nothing can observe its identity, so it may safely be forgotten.
         */
        private final Map<Marshalled<?>, WeakReference<Object>> unmarshalled;

        /**
         * The identified {@link Marshalled}s currently being unmarshalled (i.e. still on the call stack) - like
         * {@link #unmarshalled}, only ever populated and consulted on the root. A reference to one of these
         * is a genuine synchronous cycle, which cannot be resolved on demand.
         */
        private final Set<Marshalled<?>> unmarshalling;

        /**
         * Guards {@link #inProgress}, {@link #completed}, and (on the root {@link HierarchicalMarshaller} only -
         * see {@link #root()}) {@link #unmarshalled}, none of which are otherwise safe for concurrent access.
         * <p>
         * A {@link HierarchicalMarshaller} is ordinarily created once (see {@link SchemaFactory#newMarshaller()})
         * and reused for many {@link #marshal(Object)}/{@link #unmarshal(Marshalled)} calls over its lifetime -
         * including calls made well after an earlier one returned (for example, a {@code Transport} discovering
         * and marshalling a raw, previously-unmarshalled field while walking an already-produced
         * {@link Marshalled} - see {@code JsonTransport.encode}) - so this state is deliberately never cleared;
         * it is what allows such a later call to recognize an {@link Object} it has already seen. A
         * {@link ReentrantLock}, rather than a concurrent collection, is used because the invariant that must
         * hold is compound (e.g. "not in progress, and not already completed, therefore mark in progress and
         * proceed to marshal") and must be checked-and-acted-upon atomically; {@link ReentrantLock} additionally
         * allows the same thread to safely re-acquire it while recursing (directly, for {@link #marshal(Object)};
         * via {@link #root()}, for {@link #unmarshal(Marshalled)}) into further {@link #marshal(Object)}/
         * {@link #unmarshal(Marshalled)} calls of its own.
         */
        private final ReentrantLock lock;

        /**
         * Constructs a {@link HierarchicalMarshaller} with the specified {@code null}able parent.
         *
         * @param parent the {@code null}able parent
         */
        HierarchicalMarshaller(final HierarchicalMarshaller parent) {
            this.parent = Optional.ofNullable(parent);
            this.bindings = new ConcurrentHashMap<>();
            this.inProgress = Collections.newSetFromMap(new IdentityHashMap<>());
            this.completed = new IdentityHashMap<>();
            this.unmarshalled = new WeakHashMap<>();
            this.unmarshalling = Collections.newSetFromMap(new IdentityHashMap<>());
            this.lock = new ReentrantLock();
        }

        /**
         * Obtains the root {@link HierarchicalMarshaller} - the ancestor with no {@link #parent()} - of
         * {@code this} {@link HierarchicalMarshaller}.
         *
         * @return the root {@link HierarchicalMarshaller}
         */
        private HierarchicalMarshaller root() {
            return this.parent
                .map(HierarchicalMarshaller::root)
                .orElse(this);
        }

        /**
         * Constructs a {@link HierarchicalMarshaller} without a parent.
         */
        HierarchicalMarshaller() {
            this(null);
        }

        @Override
        public boolean isMarshallable(final Class<?> marshallableClass) {
            return ConcurrentSchemaFactory.this.isMarshallable(marshallableClass);
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public <T> Marshalled<T> marshal(final T object) {
            Objects.requireNonNull(object, "The object to marshal must not be null");

            // obtain the Schema for the Object
            final var marshallableClass = (Class<T>) object.getClass();

            // Check for explicitly registered enums (serialized by name)
            final var enumSchema = (Schema<T>) ConcurrentSchemaFactory.this.enumSchemasByClass.get(marshallableClass);
            if (enumSchema != null) {
                final var enumName = ((Enum) object).name();
                return new Marshalled<>() {
                    @Override
                    public Schema<T> schema() {
                        return enumSchema;
                    }

                    @Override
                    public Streamable<Object> values() {
                        return Streamable.of(Stream.of(enumName));
                    }
                };
            }

            final var schema = (MarshallingSchema<T>) ConcurrentSchemaFactory.this
                .marshallingSchemasByClass.get(marshallableClass);

            if (schema == null) {
                throw new UnsupportedOperationException(
                    "The marshalling schema for " + object.getClass() + " is unavailable.");
            }

            this.lock.lock();
            try {
                // an Object that is its own ancestor (i.e. still being marshalled, synchronously, further up
                // this very call stack) is a genuine cycle - there is no Marshalled yet for it to be
                // referenced by
                if (this.inProgress.contains(object)) {
                    throw new CyclicMarshallingException(object);
                }

                // an Object already marshalled (whether shared or - once discovered lazily - cyclic) is
                // represented by the very same Marshalled every time thereafter, rather than being destructed
                // again
                @SuppressWarnings("unchecked") final var alreadyMarshalled = (Marshalled<T>) this.completed.get(object);
                if (alreadyMarshalled != null) {
                    return alreadyMarshalled;
                }

                this.inProgress.add(object);
                try {
                    // use the MarshallingSchema to marshal
                    final var marshalled = schema.marshal(object, this);
                    this.completed.put(object, marshalled);
                    return marshalled;
                } finally {
                    this.inProgress.remove(object);
                }
            } finally {
                this.lock.unlock();
            }
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public <T> T unmarshal(final Marshalled<T> marshalled) {
            Objects.requireNonNull(marshalled, "The Marshalled must not be null");

            // unmarshalledById is only ever populated/consulted on the root - see #root() - so it, and the
            // reference/id resolution built on it, are guarded by the root's lock rather than this one
            final var root = root();
            root.lock.lock();
            try {
                // a reference Marshalled has no schema/values of its own - resolve it to the Object unmarshalled
                // from the Marshalled it refers to, rather than attempting to construct anything
                final var reference = marshalled.reference();
                if (reference.isPresent()) {
                    final var referent = marshalled.referent().orElseThrow(() -> new IllegalStateException(
                        "Cannot resolve reference [" + reference.orElseThrow() + "] - the Marshalled it refers to "
                            + "is unknown"));
                    if (root.unmarshalling.contains(referent)) {
                        throw new IllegalStateException("Cannot resolve reference [" + reference.orElseThrow()
                            + "] - it refers to an Object that has not finished unmarshalling yet (a genuine "
                            + "cycle), which this marshalling framework does not support reconstructing");
                    }
                    // already unmarshalled, or (defined within a deferred value nothing has forced yet) on demand
                    return unmarshal(referent);
                }

                // an Object already unmarshalled from this very Marshalled (for example, on demand, via a
                // reference reached before this definition was) is the Object, rather than being constructed
                // a second time
                final var identified = marshalled.id().isPresent();
                if (identified) {
                    final var existing = root.unmarshalled.get(marshalled);
                    final var existingObject = existing == null ? null : existing.get();
                    if (existingObject != null) {
                        return (T) existingObject;
                    }
                }

                // Check for explicitly registered enums (deserialized by name via Enum.valueOf)
                if (ConcurrentSchemaFactory.this.enumSchemasByClass.containsKey(marshalled.schema().owner())) {
                    final var enumName = (String) marshalled.values().stream().findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                            "No name value present for enum " + marshalled.schema().owner()));
                    final var enumResult = (T) Enum.valueOf((Class<Enum>) marshalled.schema().owner(), enumName);
                    if (identified) {
                        root.unmarshalled.put(marshalled, new WeakReference<>(enumResult));
                    }
                    return enumResult;
                }

                // obtain the UnmarshallingSchema for the owner of the Marshalled
                final var schemas = ConcurrentSchemaFactory.this
                    .unmarshallingSchemasByClass.get(marshalled.schema().owner());

                if (schemas == null) {
                    throw new IllegalArgumentException(
                        "No schemas are available to unmarshall " + marshalled.schema().owner());
                }

                // attempt to find a Schema that matches
                final var schema = schemas.stream()
                    .filter(someSchema -> someSchema.parameters().count() == marshalled.values().count())
                    .filter(someSchema -> Streams.zip(
                            marshalled.values().stream(),
                            someSchema.parameters().stream()
                                .map(Parameter::type)
                                .map(type -> Introspection.getClassFromType(type).orElse(Object.class)))
                        .allMatch(pair -> pair.second().isInstance(pair.first()) ||
                            ((!pair.second().isPrimitive()) && pair.first() == null) ||
                            // a Lazy<X> parameter is matched by a not-yet-unmarshalled Marshalled<X> value -
                            // UnmarshallingSchema.unmarshal() is what actually wraps it in a deferred Lazy
                            (Lazy.class.equals(pair.second()) && pair.first() instanceof Marshalled<?>)))
                    .findFirst()
                    .map(unmarshallingSchema -> (UnmarshallingSchema<T>) unmarshallingSchema)
                    .orElseThrow(() -> new IllegalArgumentException(
                        "No schemas are available to unmarshall " + marshalled.schema().owner()));

                if (identified) {
                    root.unmarshalling.add(marshalled);
                }
                final T result;
                try {
                    result = schema.unmarshal(marshalled, this);
                }
                finally {
                    root.unmarshalling.remove(marshalled);
                }

                // register the result so a reference elsewhere to the same Marshalled can resolve to it
                if (identified) {
                    root.unmarshalled.put(marshalled, new WeakReference<>(result));
                }

                return result;
            } finally {
                root.lock.unlock();
            }
        }

        /**
         * Obtains a new {@link HierarchicalMarshaller} based on {@code this} {@link HierarchicalMarshaller}.
         * <p>
         * Any bindings created in the new {@link HierarchicalMarshaller} for {@link Dependency}s
         * <strong>will not be visible</strong> in {@code this} {@link HierarchicalMarshaller}.
         *
         * @return a new {@link HierarchicalMarshaller}
         */
        public HierarchicalMarshaller newMarshaller() {
            return new HierarchicalMarshaller(this);
        }

        /**
         * Obtains the {@link Optional} parent {@link HierarchicalMarshaller} that established {@code this}
         * {@link HierarchicalMarshaller}.
         *
         * @return the {@link Optional} parent {@link HierarchicalMarshaller}, or {@link Optional#empty()} if no parent
         * is present
         */
        public Optional<HierarchicalMarshaller> parent() {
            return this.parent;
        }

        /**
         * Obtains the value of the specified {@link Dependency} bound to {@code this} {@link Marshaller}, failing that,
         * consults the parent {@link Marshaller}.
         *
         * @param dependency the {@link Dependency}
         * @return the {@link Optional} value for the {@link Dependency}, {@link Optional#empty()} otherwise
         */
        public Optional<?> getValue(final Dependency dependency) {
            if (dependency == null) {
                return Optional.empty();
            }

            final var type = dependency.type();

            if (Marshaller.class.isAssignableFrom(Introspection
                .getClassFromType(type)
                .orElse(Object.class))) {

                return Optional.of(this);
            }

            final var binding = this.bindings.get(type);

            if (binding != null) {
                return Optional.ofNullable(binding.value());
            }

            return this.parent()
                .flatMap(parent -> parent.getValue(dependency));
        }

        @Override
        public <T> BindingBuilder<T> bind(final Class<T> bindingClass) {
            return new BindingBuilder<>() {
                @Override
                public Binding<T> to(final T value) {
                    final var binding = new Binding<T>() {
                        @Override
                        public T value() {
                            return value;
                        }
                    };

                    HierarchicalMarshaller.this.bindings.putIfAbsent(bindingClass, binding);
                    return binding;
                }

                @Override
                public Binding<T> to(final Supplier<T> supplier) {
                    final var binding = new Binding<T>() {
                        @Override
                        public T value() {
                            return supplier == null ? null : supplier.get();
                        }
                    };

                    HierarchicalMarshaller.this.bindings.putIfAbsent(bindingClass, binding);
                    return binding;
                }
            };
        }
    }
}
