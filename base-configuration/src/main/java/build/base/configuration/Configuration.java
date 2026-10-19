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

import java.lang.reflect.Array;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static build.base.configuration.Option.getDefaultOption;

/**
 * An <strong>immutable</strong> {@link java.util.Map}-like structure for managing zero or more configuration
 * {@link Option}s, discriminating (keying) them by the concrete {@link Class} of the said {@link Option}s.
 * <p>
 * The concrete {@link Class} of an {@link Option} is defined as the most immediate {@link Class} in the
 * class-hierarchy, including the {@link Option} itself, that is neither abstract, anonymous, synthetic, nor a lambda
 * expression.
 * <p>
 * Much like {@link java.util.Map}s which only permit a single value to be associated with a particular
 * key, {@link Configuration}s only permit a single {@link Option} instance to be associated with a particular
 * {@link Class} of {@link Option}.
 * <p>
 * By default, the {@link Class} used to discriminate individual {@link Option}s is simply its concrete {@link Class},
 * acquired by calling {@link Object#getClass()}.  To override this behavior an {@link Option} {@link Class} or one
 * of its super {@link Class}es may be annotated with an {@link OptionDiscriminator}.
 * <p>
 * For example:  Given an interface Animal that extends {@link Option} and concrete classes Cat and Dog that implement
 * the Animal interface, a {@link Configuration} by default will naturally discriminate and independently store
 * <strong>both</strong> Cat and Dog instances as their concrete {@link Class}es are different.   However, by
 * annotating the Animal interface as an {@link OptionDiscriminator}, a {@link Configuration} will then only store
 * one instance of an Animal, ie: either a Cat or a Dog, <strong>but not both</strong>.
 *
 * @author brian.oliver
 * @since Nov-2017
 */
public interface Configuration {

    /**
     * An empty {@link Configuration}.
     */
    Configuration EMPTY = new Configuration() {
        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public <T extends Option> T get(final Class<T> classOfOption) {

            return classOfOption == null
                || MappedOption.class.isAssignableFrom(classOfOption)
                || CollectedOption.class.isAssignableFrom(classOfOption)
                ? null
                : getDefaultOption(classOfOption);
        }

        @Override
        public <T extends Option> T getOrDefault(final Class<T> classOfOption, final Supplier<T> supplier) {
            return supplier == null ? null : supplier.get();
        }

        @Override
        public <K, T extends MappedOption<K>> T get(final Class<T> classOfOption, final K key) {
            return null;
        }

        @Override
        public <I> Stream<I> stream(final Class<I> requiredClass) {
            return Stream.empty();
        }

        @Override
        public Stream<Option> stream() {
            return Stream.empty();
        }
    };

    /**
     * Determines if there are no {@link Option}s defined in the {@link Configuration}.
     *
     * @return {@code true} if the {@link Configuration} contains no {@link Option}s
     */
    boolean isEmpty();

    /**
     * Determines the {@link Option} from the {@link Configuration} with the specified {@link Class}.  Should an
     * {@link Option} of the specified {@link Class} not exist, an attempt will be made to determine a {@link Default}
     * {@link Option}, returning the default value if successful.  Should it not be possible to determine a default,
     * {@code null} is returned.
     *
     * @param <T>           the type of the {@link Option}
     * @param classOfOption the {@link Class} of {@link Option}
     * @return the {@link Option} or {@code null}
     */
    <T extends Option> T get(Class<T> classOfOption);

    /**
     * Determines the {@link Optional} {@link Option} from the {@link Configuration} with the specified {@link Class}.
     * Should an {@link Option} of the specified {@link Class} not exist, an attempt will be made to determine a
     * {@link Default} {@link Option}, returning an {@link Optional} of the default value if successful.  Should it not
     * be possible to determine a default, an {@link Optional#empty()} is returned.
     *
     * @param <T>           the type of the {@link Option}
     * @param classOfOption the {@link Class} of {@link Option}
     * @return an {@link Optional} {@link Option} or {@link Optional#empty()}
     */
    default <T extends Option> Optional<T> getOptional(final Class<T> classOfOption) {
        return Optional.ofNullable(get(classOfOption));
    }

    /**
     * Determines the {@link Optional} {@link Option} from the {@link Configuration} with the specified {@link Class}.
     * Should an {@link Option} of the specified {@link Class} not exist, {@link Optional#empty()} is returned.
     *
     * @param <T>           the type of the {@link Option}
     * @param classOfOption the {@link Class} of {@link Option}
     * @return an {@link Optional} {@link Option} or {@link Optional#empty()}
     */
    default <T extends Option> Optional<T> getOptionalWithoutDefault(final Class<T> classOfOption) {
        return Optional.ofNullable(getOrDefault(classOfOption, null));
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
    <T extends Option> T getOrDefault(Class<T> classOfOption, Supplier<T> supplier);

    /**
     * Determines the {@link MappedOption} from the {@link Configuration} with the specified {@link Class} and key.
     * Should a {@link MappedOption} of the specified {@link Class} and key not exist, {@code null} is returned.
     *
     * @param <K>           the type of the {@link MappedOption} key
     * @param <T>           the type of the {@link MappedOption} value
     * @param classOfOption the {@link Class} of {@link MappedOption}
     * @param key           the key
     * @return an {@link MappedOption} or {@code null}
     */
    <K, T extends MappedOption<K>> T get(Class<T> classOfOption, K key);

    /**
     * Determines the {@link Optional} {@link MappedOption} from the {@link Configuration} with the specified
     * {@link Class} and key.  Should a {@link MappedOption} of the specified {@link Class} and key not exist,
     * {@link Optional#empty()} is returned.
     *
     * @param <K>           the type of the {@link MappedOption} key
     * @param <T>           the type of the {@link MappedOption} value
     * @param classOfOption the {@link Class} of {@link MappedOption}
     * @param key           the key
     * @return the {@link Optional} {@link MappedOption} or {@link Optional#empty()}
     */
    default <K, T extends MappedOption<K>> Optional<T> getOptional(final Class<T> classOfOption,
                                                                   final K key) {

        return Optional.ofNullable(get(classOfOption, key));
    }

    /**
     * Determines the value of the {@link ValueOption} from the {@link Configuration} with the specified {@link Class}.
     * Should an {@link Option} of the specified type not exist, an attempt will be made to determine a {@link Default}
     * {@link Option} from the {@link Configuration}, returning the default value if successful.  Should it not be
     * possible to determine a default, {@code null} is returned.
     *
     * @param <T>                the type of the {@link ValueOption}
     * @param <V>                the type of the value of the {@link ValueOption}
     * @param classOfValueOption the {@link Class} of {@link Option}
     * @return a value or <code>null</code>
     */
    default <V, T extends ValueOption<V>> V getValue(final Class<T> classOfValueOption) {
        final T valueOption = get(classOfValueOption);
        return valueOption == null ? null : valueOption.get();
    }

    /**
     * Determines an {@link Optional} value of the {@link ValueOption} from the {@link Configuration} with the specified
     * {@link Class}. Should an {@link Option} of the specified type not exist, an attempt will be made to determine a
     * {@link Default} {@link Option} from the {@link Configuration}, returning the default value if successful.
     * Should it not be possible to determine a default, {@link Optional#empty()} is returned.
     *
     * @param <T>                the type of the {@link ValueOption}
     * @param <V>                the type of the value of the {@link ValueOption}
     * @param classOfValueOption the {@link Class} of {@link Option}
     * @return a value or <code>null</code>
     */
    default <V, T extends ValueOption<V>> Optional<V> getOptionalValue(final Class<T> classOfValueOption) {
        return Optional.ofNullable(getValue(classOfValueOption));
    }

    /**
     * Determines if the {@link Option} for the provided {@link Class} is present in the {@link Configuration}.
     *
     * @param <T>           the type of the {@link Option}
     * @param classOfOption the {@link Class} of {@link Option}
     * @return {@code true} if the {@link Option} exists in the {@link Configuration}, otherwise {@code false}
     */
    default <T extends Option> boolean isPresent(final Class<T> classOfOption) {
        return getOptional(classOfOption).isPresent();
    }

    /**
     * If the {@link Class} of {@link Option} is present in the {@link Configuration}, invoke the specified consumer
     * with the value, otherwise do nothing.
     *
     * @param <T>           the type of the {@link Option}
     * @param classOfOption the {@link Class} of {@link Option}
     * @param consumer      block to be executed if a value is present
     * @throws NullPointerException if value is present and {@code consumer} is null
     */
    default <T extends Option> void ifPresent(final Class<T> classOfOption,
                                              final Consumer<? super T> consumer) {

        getOptional(classOfOption).ifPresent(consumer);
    }

    /**
     * Obtains a {@link Stream} of {@link Option}s that are assignable to the specified {@link Class}
     * (ie: are instances of the specified class)
     *
     * @param <I>           the type of the required class
     * @param requiredClass the required class
     * @return a {@link Stream} of {@link Option}s implementing the specified type
     */
    @SuppressWarnings("unchecked")
    default <I> Stream<I> stream(final Class<I> requiredClass) {

        return stream()
            .filter(requiredClass::isInstance)
            .map(o -> (I) o);
    }

    /**
     * Obtains a {@link Stream} of {@link Option}s contained in the {@link Configuration}.
     *
     * @return a {@link Stream} of {@link Option}s
     */
    Stream<Option> stream();

    /**
     * Obtains an array representation of the {@link Option}s contained in the {@link Configuration}.
     *
     * @return an array of {@link Option}s
     */
    default Option[] toArray() {
        return stream().toArray(Option[]::new);
    }

    /**
     * Obtains an array of the {@link Option}s contained in the {@link Configuration} implementing
     * the specified type.
     *
     * @param <I>           the required type
     * @param requiredClass the required {@link Class}
     * @return an array of {@link Option}s implementing the required type
     */
    @SuppressWarnings("unchecked")
    default <I> I[] toArrayOf(final Class<I> requiredClass) {
        return stream(requiredClass).toArray(n -> (I[]) Array.newInstance(requiredClass, n));
    }

    /**
     * Obtains an empty {@link Configuration}.
     *
     * @return an empty {@link Configuration}
     */
    static Configuration empty() {
        return EMPTY;
    }

    /**
     * Creates a {@link Configuration} with the specified {@link Option}s.
     *
     * @param options the {@link Option}s
     * @return a new {@link Configuration}
     * @see ConfigurationBuilder#create(Option...)
     */
    static Configuration of(final Option... options) {
        return ConfigurationBuilder.create(options).build();
    }
}
