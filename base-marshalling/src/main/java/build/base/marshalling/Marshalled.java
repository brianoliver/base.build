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

import build.base.foundation.stream.Streamable;

import java.util.Optional;

/**
 * Provides access to information concerning and including the values of an {@link Object} that has been
 * <a href="https://en.wikipedia.org/wiki/Marshalling_(computer_science)">Marshalled</a>.
 * <p>
 * {@link Marshalled}s are produced and consumed by {@link Marshaller}s, which themselves are responsible for
 * deconstructing {@link Object}s into their constituent {@link Parameter} <i>values</i> and when required,
 * reconstructing said {@link Object} from said {@link Parameter} <i>values</i>.
 *
 * @param <T> the type of {@link Object} that has been marshalled
 * @author brian.oliver
 * @see Marshaller
 * @since Nov-2024
 */
public interface Marshalled<T> {

    /**
     * Obtains the {@link Schema} for {@link Marshalled} instance.
     *
     * @return the {@link Schema}
     */
    Schema<T> schema();

    /**
     * Provides access to the <i>marshalled</i> {@link Parameter} values of an {@link Object}, in-order of definition
     * in the {@link Schema}.
     *
     * @return a {@link Streamable} of {@link Object} values
     */
    Streamable<Object> values();

    /**
     * Obtains the identity assigned to {@code this} {@link Marshalled}, if it was reached more than once while
     * marshalling (directly, or transitively via a shared or cyclic reference) and therefore needs to be
     * referenceable by {@link #reference()} elsewhere.
     * <p>
     * Absent for the vast majority of {@link Marshalled}s, which are only ever reached once.
     *
     * @return the {@link Optional} identity, otherwise {@link Optional#empty()}
     */
    default Optional<Integer> id() {
        return Optional.empty();
    }

    /**
     * If present, indicates {@code this} {@link Marshalled} is a back-reference to a previously-unmarshalled
     * {@link Object} (identified by the returned identity, as {@link #id()} would have reported it) rather
     * than a fresh description to construct from. {@link #schema()} and {@link #values()} are not supported
     * when this is present.
     *
     * @return the {@link Optional} identity being referenced, otherwise {@link Optional#empty()}
     */
    default Optional<Integer> reference() {
        return Optional.empty();
    }

    /**
     * When {@code this} {@link Marshalled} is a {@link #reference()}, provides the {@link Marshalled} it refers to
     * (the one whose {@link #id()} was reported), if known. This allows a reference to be resolved on demand, even
     * when the referenced {@link Object} has not yet been unmarshalled, for example because it is defined within a
     * deferred ({@link build.base.foundation.Lazy}) value that nothing has yet forced.
     *
     * @return the {@link Optional} referenced {@link Marshalled}, otherwise {@link Optional#empty()}
     */
    default Optional<Marshalled<T>> referent() {
        return Optional.empty();
    }
}
