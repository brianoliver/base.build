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

import build.base.foundation.Introspection;
import build.base.foundation.stream.Streams;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static build.base.configuration.Option.getDefaultOption;
import static build.base.configuration.Option.getDiscriminatorClass;

/**
 * A builder of {@link Configuration}s.
 *
 * @author brian.oliver
 * @since Oct-2024
 */
public class ConfigurationBuilder {

    /**
     * The {@link Option}s organized by discriminator class.
     */
    private final LinkedHashMap<Class<? extends Option>, Option> options;

    /**
     * The {@link MappedOption}s organized by discriminator class.
     */
    private final LinkedHashMap<Class<? extends Option>, LinkedHashMap<Object, MappedOption<?>>> mappedOptions;

    /**
     * The {@link CollectedOption}s organized by discriminator class.
     */
    private final LinkedHashMap<Class<? extends Option>, Collection<CollectedOption<?>>> collectedOptions;

    /**
     * Constructs an empty {@link ConfigurationBuilder}.
     */
    ConfigurationBuilder() {
        this.options = new LinkedHashMap<>();
        this.mappedOptions = new LinkedHashMap<>();
        this.collectedOptions = new LinkedHashMap<>();
    }

    /**
     * Determines if there are no {@link Option}s defined in the {@link ConfigurationBuilder}.
     *
     * @return {@code true} if the {@link ConfigurationBuilder} contains no {@link Option}s
     */
    public boolean isEmpty() {
        return this.options.isEmpty() && this.mappedOptions.isEmpty() && this.collectedOptions.isEmpty();
    }

    /**
     * Obtains a {@link Stream} of {@link Option}s defined by the {@link ConfigurationBuilder}.
     *
     * @return a {@link Stream} of {@link Option}s
     */
    public Stream<Option> stream() {
        return Streams.concat(
            this.options.values().stream(),
            this.mappedOptions.values().stream()
                .flatMap(mappedOptions -> mappedOptions.values().stream()),
            this.collectedOptions.values().stream()
                .flatMap(Collection::stream));
    }

    /**
     * Obtains a {@link Stream} of {@link Option}s defined by the {@link ConfigurationBuilder} that are
     * assignable to the specified {@link Class}.
     *
     * @return a {@link Stream} of {@link Option}s assignable to the specified {@link Class}
     */
    public <T> Stream<T> stream(final Class<T> requiredClass) {
        return requiredClass == null
            ? Stream.empty()
            : stream()
            .filter(requiredClass::isInstance)
            .map(requiredClass::cast);
    }

    /**
     * Adds the specified {@link Option} replacing any previous {@link Option} of the same discriminated type
     * in the {@link ConfigurationBuilder}.  Should the provided {@link Option} be {@code null}, nothing is added.
     *
     * @param option the {@link Option} to add
     * @return this {@link ConfigurationBuilder} to permit fluent-style method invocation
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ConfigurationBuilder add(final Option option) {

        switch (option) {
            case null -> {
                return this;
            }
            case MappedOption mapped -> {
                // determine the discriminator class of the mapped configuration
                final var discriminatorClass = getDiscriminatorClass(mapped.getClass());

                // determine the map maintaining mapped options of the discriminator type
                final var mappedOptions = this.mappedOptions
                    .compute(discriminatorClass, (_, v) -> v == null ? new LinkedHashMap<>() : v);

                // add / override the existing mapped configuration
                mappedOptions.put(mapped.key(), mapped);
            }
            case CollectedOption collected -> {
                // determine the discriminator class of the mapped configuration
                final var discriminatorClass = getDiscriminatorClass(collected.getClass());

                // determine the collection maintaining collected options of the discriminator type
                final var collection = this.collectedOptions
                    .compute(discriminatorClass, (_, v) -> v == null ? collected.createCollection() : v);

                collection.add(collected);
            }
            case ComposedOption _ -> {
                // determine the discriminator class of the composed
                var discriminatorClass = getDiscriminatorClass(option.getClass());

                // ensure that the discriminator class is still composed!
                if (!ComposedOption.class.isAssignableFrom(discriminatorClass)) {
                    discriminatorClass = option.getClass();
                }

                // attempt to find the existing composed
                final var composed = (ComposedOption) this.options.get(discriminatorClass);

                // establish the newly composed configuration
                final ComposedOption newlyComposed = composed == null
                    ? (ComposedOption) option                 // use the provided composed if one doesn't already exist
                    : composed.compose((ComposedOption) option);// compose the existing with the provided

                // store the newly composed
                this.options.put(discriminatorClass, newlyComposed);
            }
            default -> {
                final var discriminatorClass = getDiscriminatorClass(option.getClass());
                this.options.put(discriminatorClass, option);
            }
        }

        return this;
    }

    /**
     * Removes the specified {@link Option}.
     *
     * @param option the {@link Option} to remove
     * @return this {@link ConfigurationBuilder} to permit fluent-style method invocation
     */
    @SuppressWarnings({"rawtypes"})
    public ConfigurationBuilder remove(final Option option) {

        switch (option) {
            case null -> {
                return this;
            }
            case MappedOption mapped -> {
                // determine the discriminator class of the mapped configuration
                final var discriminatorClass = getDiscriminatorClass(mapped.getClass());

                // determine the map maintaining mapped options of the discriminator type
                final var mappedOptions = this.mappedOptions
                    .compute(discriminatorClass, (_, values) -> {
                        if (values == null) {
                            return null;
                        }

                        values.remove(mapped.key());

                        return values.isEmpty() ? null : values;
                    });
            }
            case CollectedOption collected -> {
                // determine the discriminator class of the mapped configuration
                final var discriminatorClass = getDiscriminatorClass(collected.getClass());

                // determine the collection maintaining collected options of the discriminator type
                final var collection = this.collectedOptions
                    .compute(discriminatorClass, (_, values) -> {
                        if (values == null) {
                            return null;
                        }

                        values.remove(collected);

                        return values.isEmpty() ? null : values;
                    });
            }
            case ComposedOption _ -> {
                // determine the discriminator class of the composed
                var discriminatorClass = getDiscriminatorClass(option.getClass());

                // ensure that the discriminator class is still composed!
                if (!ComposedOption.class.isAssignableFrom(discriminatorClass)) {
                    discriminatorClass = option.getClass();
                }

                // store the newly composed
                this.options.remove(discriminatorClass);
            }
            default -> {
                final var discriminatorClass = getDiscriminatorClass(option.getClass());
                this.options.remove(discriminatorClass);
            }
        }

        return this;
    }

    /**
     * Includes the specified {@link Option}s replacing any previous {@link Option}s of the same discriminated type
     * in the {@link ConfigurationBuilder}.
     *
     * @param stream the {@link Stream} of {@link Option}s to add
     * @return this {@link ConfigurationBuilder} to permit fluent-style method invocation
     */
    public ConfigurationBuilder include(final Stream<? extends Option> stream) {

        if (stream != null) {
            stream.forEach(this::add);
        }

        return this;
    }

    /**
     * Includes the specified {@link Option}s replacing any previous {@link Option}s of the same discriminated type
     * in the {@link ConfigurationBuilder}.
     *
     * @param options the {@link Option}s to add
     * @return this {@link ConfigurationBuilder} to permit fluent-style method invocation
     */
    public ConfigurationBuilder include(final Option... options) {
        return include(Streams.of(options));
    }

    /**
     * Includes the {@link Option}s in the specified {@link Configuration} into this {@link ConfigurationBuilder}.
     *
     * @param configuration the {@link Option}s to add
     * @return this {@link ConfigurationBuilder} to permit fluent-style method invocation
     */
    public ConfigurationBuilder include(final Configuration configuration) {
        return include(configuration.stream());
    }

    /**
     * Includes the {@link Option}s in the specified {@link ConfigurationBuilder} into this {@link ConfigurationBuilder}.
     *
     * @param builder the {@link ConfigurationBuilder} containing {@link Option}s to add
     * @return this {@link ConfigurationBuilder} to permit fluent-style method invocation
     */
    public ConfigurationBuilder include(final ConfigurationBuilder builder) {
        return include(builder.stream());
    }

    /**
     * Includes the {@link Option} defined by public static no-arg methods that returns an {@link Option} defined by
     * the specified {@link Class} into the {@link ConfigurationBuilder}.
     *
     * @param fromClass the class on which the methods are invoked
     * @return this {@link ConfigurationBuilder} to permit fluent-style method invocation
     */
    public ConfigurationBuilder include(final Class<?> fromClass) {

        Introspection.getVisibleMethods(fromClass, method ->
                Modifier.isPublic(method.getModifiers()) &&
                    Modifier.isStatic(method.getModifiers()) &&
                    Option.class.isAssignableFrom(method.getReturnType()) &&
                    method.getParameterCount() == 0)
            .map(method -> {
                try {
                    return method.invoke(null);
                } catch (final IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalArgumentException("Exception thrown by invocation of method: " + method, e);
                }
            })
            .map(Option.class::cast)
            .forEach(this::add);

        return this;
    }

    /**
     * Determines the {@link Option} from the {@link ConfigurationBuilder} with the specified {@link Class}, without
     * attempting to determine a {@link Default} {@link Option}.  Should an {@link Option} of the specified
     * {@link Class} not exist, {@code null} is returned.
     *
     * @param <T>           the type of the {@link Option}
     * @param classOfOption the {@link Class} of {@link Option}
     * @return the {@link Option} or {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T extends Option> T getWithoutDefault(final Class<T> classOfOption) {

        return classOfOption == null
            || MappedOption.class.isAssignableFrom(classOfOption)
            || CollectedOption.class.isAssignableFrom(classOfOption)
            ? null
            : (T) this.options.get(getDiscriminatorClass(classOfOption));
    }

    /**
     * Determines the {@link Option} from the {@link ConfigurationBuilder} with the specified {@link Class}.  Should an
     * {@link Option} of the specified {@link Class} not exist, an attempt will be made to determine a {@link Default}
     * {@link Option}, returning the default value if successful.  Should it not be possible to determine a default,
     * {@code null} is returned.
     *
     * @param <T>           the type of the {@link Option}
     * @param classOfOption the {@link Class} of {@link Option}
     * @return the {@link Option} or {@code null}
     */
    public <T extends Option> T get(final Class<T> classOfOption) {

        if (classOfOption == null
            || MappedOption.class.isAssignableFrom(classOfOption)
            || CollectedOption.class.isAssignableFrom(classOfOption)) {

            return null;
        } else {
            var option = getWithoutDefault(classOfOption);

            if (option == null) {
                option = getDefaultOption(classOfOption);
            }

            return option;
        }
    }

    /**
     * Determines if the {@link Option} for the provided {@link Class} is defined for the {@link ConfigurationBuilder}.
     *
     * @param classOfOption the {@link Class} of {@link Option}
     * @return {@code true} if the {@link Option} is defined for the {@link ConfigurationBuilder}, otherwise {@code false}
     */
    public boolean isPresent(final Class<? extends Option> classOfOption) {
        return getWithoutDefault(classOfOption) != null;
    }

    /**
     * Determines the {@link Option} from the {@link Configuration} with the specified {@link Class}.  Should an
     * {@link Option} of the specified {@link Class} not exist in the {@link Configuration}, the specified
     * {@link Supplier} is consulted to obtain a default.
     *
     * @param <T>           the type of the {@link Option}
     * @param classOfOption the {@link Class} of {@link Option}
     * @param supplier      the {@link Supplier} of the default {@link Option} to return
     * @return an {@link Option}
     */
    @SuppressWarnings("unchecked")
    public <T extends Option> T getOrDefault(final Class<T> classOfOption,
                                             final Supplier<T> supplier) {

        if (classOfOption == null
            || MappedOption.class.isAssignableFrom(classOfOption)
            || CollectedOption.class.isAssignableFrom(classOfOption)) {

            return null;
        } else {
            var option = (T) this.options.get(getDiscriminatorClass(classOfOption));

            if (option == null && supplier != null) {
                option = supplier.get();
            }

            return option;
        }
    }

    /**
     * Computes an {@link Option} of the specified {@link Class} for the {@link ConfigurationBuilder} using the
     * provided {@link Function}.  Should an {@link Option} of the specified {@link Class} already exist,
     * the {@link Function} is applied to the existing {@link Option}, otherwise the {@link Function} is applied to
     * {@code null}.  Should the {@link Function} be or return {@code null}, nothing is added and the existing
     * {@link Option} is removed if it exists, otherwise the returned {@link Option} is recorded by the
     * {@link ConfigurationBuilder}.
     *
     * @param classOfOption the {@link Class} of {@link Option}
     * @param function      the {@link Function} to compute and return an {@link Option} of the specified {@link Class}
     * @return this {@link ConfigurationBuilder} to permit fluent-style method invocation
     */
    public <T extends Option> ConfigurationBuilder compute(final Class<T> classOfOption,
                                                           final Function<T, T> function) {

        final var existing = getWithoutDefault(classOfOption);

        final var option = function == null
            ? null
            : function.apply(existing);

        if (existing != null) {
            remove(existing);
        }

        if (option != null) {
            add(option);
        }

        return this;
    }

    /**
     * Computes an {@link Option} of the specified {@link Class} if one is not already present in the
     * {@link ConfigurationBuilder} using the provided {@link Supplier}.  Should the {@link Supplier} be or return
     * {@code null}, nothing is added.
     *
     * @param classOfOption the {@link Class} of {@link Option}
     * @param supplier      the {@link Supplier} of an {@link Option}
     * @return this {@link ConfigurationBuilder} to permit fluent-style method invocation
     */
    public <T extends Option> ConfigurationBuilder computeIfNotPresent(final Class<T> classOfOption,
                                                                       final Supplier<? extends T> supplier) {

        final var existing = getWithoutDefault(classOfOption);

        if (existing == null) {
            if (supplier != null) {
                final var option = supplier.get();
                if (option != null) {
                    add(option);
                }
            }
        }

        return this;
    }

    /**
     * Adds an {@link Option} of the specified {@link Class} if one is not already present in the
     * {@link ConfigurationBuilder}.  Should the {@link Option} be {@code null}, nothing is added.
     *
     * @param classOfOption the {@link Class} of {@link Option}
     * @param option        the {@link Option}
     * @return this {@link ConfigurationBuilder} to permit fluent-style method invocation
     */
    public <T extends Option> ConfigurationBuilder addIfNotPresent(final Class<T> classOfOption,
                                                                   final T option) {

        final var existing = getWithoutDefault(classOfOption);

        if (existing == null && option != null) {
            add(option);
        }

        return this;
    }

    /**
     * Builds a new {@link Configuration} given the {@link Option}s defined in the {@link ConfigurationBuilder}.
     *
     * @return {@link Configuration}
     */
    public Configuration build() {
        return isEmpty()
            ? Configuration.EMPTY
            : new InternalConfiguration(this.options, this.mappedOptions, this.collectedOptions);
    }

    /**
     * Constructs a new empty {@link ConfigurationBuilder}.
     *
     * @return a new empty {@link ConfigurationBuilder}
     */
    public static ConfigurationBuilder create() {
        return new ConfigurationBuilder();
    }

    /**
     * Creates an {@link ConfigurationBuilder} based on the specified {@link Option}s.
     *
     * @param options the {@link Option}s to add to the {@link ConfigurationBuilder}
     * @return an {@link ConfigurationBuilder} containing the specified {@link Option}s.
     */
    public static ConfigurationBuilder create(final Option... options) {

        final ConfigurationBuilder builder = ConfigurationBuilder.create();

        if (options != null) {
            for (final Option option : options) {
                if (option != null) {
                    builder.add(option);
                }
            }
        }

        return builder;
    }

    /**
     * Creates an {@link ConfigurationBuilder} based on the specified {@link Option}s.
     *
     * @param stream the {@link Stream} of {@link Option}s to add to the {@link ConfigurationBuilder}
     * @return an {@link ConfigurationBuilder} containing the specified {@link Option}s.
     */
    static ConfigurationBuilder create(final Stream<? extends Option> stream) {

        return stream == null
            ? ConfigurationBuilder.create()
            : stream.collect(ConfigurationBuilder.collector());
    }

    /**
     * Obtains an {@link Option} {@link Collector} that collects {@link Option}s into a new {@link ConfigurationBuilder}.
     *
     * @return a new {@link Option} {@link Collector}
     */
    static Collector<Option, ConfigurationBuilder, ConfigurationBuilder> collector() {
        return new Collector<>() {
            @Override
            public Supplier<ConfigurationBuilder> supplier() {
                return ConfigurationBuilder::create;
            }

            @Override
            public BiConsumer<ConfigurationBuilder, Option> accumulator() {
                return ConfigurationBuilder::add;
            }

            @Override
            public BinaryOperator<ConfigurationBuilder> combiner() {
                return ConfigurationBuilder::include;
            }

            @Override
            public Function<ConfigurationBuilder, ConfigurationBuilder> finisher() {
                return Function.identity();
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Collections.singleton(Characteristics.IDENTITY_FINISH);
            }
        };
    }

    /**
     * An internal {@link Configuration} implementation.
     */
    static class InternalConfiguration
        implements Configuration {

        /**
         * The {@link Option}s organized by discriminator {@link Class}.
         */
        private final LinkedHashMap<Class<? extends Option>, Option> options;

        /**
         * The {@link MappedOption}s organized by discriminator {@link Class}.
         */
        private final LinkedHashMap<Class<? extends Option>, LinkedHashMap<Object, MappedOption<?>>> mappedOptions;

        /**
         * The {@link CollectedOption}s organized by discriminator {@link Class}.
         */
        private final LinkedHashMap<Class<? extends Option>, Collection<CollectedOption<?>>> collectedOptions;

        /**
         * An internal {@link Configuration} implementation.
         */
        InternalConfiguration(final LinkedHashMap<Class<? extends Option>, Option> options,
                              final LinkedHashMap<Class<? extends Option>, LinkedHashMap<Object, MappedOption<?>>> mappedOptions,
                              final LinkedHashMap<Class<? extends Option>, Collection<CollectedOption<?>>> collectedOptions) {

            this.options = new LinkedHashMap<>(options);
            this.mappedOptions = new LinkedHashMap<>(mappedOptions);
            this.collectedOptions = new LinkedHashMap<>(collectedOptions);
        }

        @Override
        public boolean isEmpty() {
            return this.options.isEmpty() && this.mappedOptions.isEmpty() && this.collectedOptions.isEmpty();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Option> T get(final Class<T> classOfOption) {

            if (classOfOption == null
                || MappedOption.class.isAssignableFrom(classOfOption)
                || CollectedOption.class.isAssignableFrom(classOfOption)) {

                return null;
            } else {
                var option = (T) this.options.get(getDiscriminatorClass(classOfOption));

                if (option == null) {
                    option = getDefaultOption(classOfOption);
                }

                return option;
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Option> T getOrDefault(final Class<T> classOfOption, final Supplier<T> supplier) {

            if (classOfOption == null
                || MappedOption.class.isAssignableFrom(classOfOption)
                || CollectedOption.class.isAssignableFrom(classOfOption)) {

                return null;
            } else {
                var option = (T) this.options.get(getDiscriminatorClass(classOfOption));

                if (option == null && supplier != null) {
                    option = supplier.get();
                }

                return option;
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <K, T extends MappedOption<K>> T get(final Class<T> classOfOption, final K key) {

            if (classOfOption == null
                || key == null
                || !MappedOption.class.isAssignableFrom(classOfOption)) {

                return null;
            } else {
                final var mappedOptions = this.mappedOptions.get(getDiscriminatorClass(classOfOption));

                return mappedOptions == null
                    ? null
                    : (T) mappedOptions.get(key);
            }
        }

        @Override
        public Stream<Option> stream() {
            return Streams.concat(
                this.options.values().stream(),
                this.mappedOptions.values().stream()
                    .flatMap(mappedOptions -> mappedOptions.values().stream()),
                this.collectedOptions.values().stream()
                    .flatMap(Collection::stream));
        }
    }
}
