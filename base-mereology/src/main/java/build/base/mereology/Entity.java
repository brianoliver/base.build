package build.base.mereology;

/*-
 * #%L
 * base.build Mereology
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

import build.base.foundation.stream.Streamable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Describes the <a href="https://en.wikipedia.org/wiki/Mereology">Mereological</a> <i>Entity</i>, possibly being a
 * <i>direct</i> <i>part</i> of an {@link Optional}ly available {@link Composite}.
 *
 * @param <T> the type of the {@link Object} <i>Entity</i>
 * @author brian.oliver
 * @see Composite
 * @see Hierarchical
 * @since Sep-2025
 */
public interface Entity<T> {

    /**
     * Obtains the {@link Object} represented by the {@link Entity}, that of which may be a <i>direct</i> <i>part</i>
     * of the {@link Optional}ly defined {@link Composite}.
     *
     * @return the {@link Entity} {@link Object}
     */
    T object();

    /**
     * Obtains the {@link Optional} {@link Composite} in which the {@link Entity} {@link #object()} is a <i>direct</i>
     * <i>part</i>.
     * <p>
     * Should the {@link #object()} not be a <i>direct</i> <i>part</i> of a {@link Composite},
     * an {@link Optional#empty()}} will be returned.
     *
     * @return the {@link Optional} {@link Composite}
     */
    Optional<Composite> composite();

    /**
     * Obtains the {@link Optional} {@link Entity} {@link #object()} when it is a <i>direct</i> <i>part</i> of the
     * {@link Composite}.
     * <p>
     * Should the {@link #object()} not be a <i>direct</i> <i>part</i> of a {@link Composite},
     * an {@link Optional#empty()}} will be returned.
     *
     * @return the {@link Optional} <i>direct</i> <i>part</i>
     */
    Optional<T> part();

    /**
     * Obtains the distance of the {@link Entity} {@link Object} within the {@link #hierarchy()} from the {@link Entity}
     * that is the <i>bounds</i>.
     *
     * @return the depth
     */
    default int distance() {
        return Math.toIntExact(hierarchy()
            .count());
    }

    /**
     * Determines if the {@link Entity} {@link Object} is at the <i>boundary</i> of the {@link #hierarchy()}, meaning
     * the {@link Object} is it not contained within an accessible {@link Composite} of a traversal.
     *
     * @return {@code true} if the {@link Object} at the <i>boundary</i>, {@code false} otherwise
     */
    default boolean isBoundary() {
        return composite()
            .isEmpty();
    }

    /**
     * Determines if the {@link Entity} {@link Object} is an <i>atom</i>, meaning it has no proper parts.
     *
     * @return {@code true} if the {@link Object} has no proper parts, {@code false} otherwise
     */
    default boolean isAtom() {
        return !(object() instanceof Composite c) || !c.iterator(Object.class).hasNext();
    }

    /**
     * Obtains the {@link Composite}s in which the {@link Entity} {@link Object} is enclosed, ordered from the
     * <i>boundary</i> of the hierarchy to the {@link Composite} in which the {@link Object} may be <i>directly</i>
     * enclosed.
     *
     * @return the {@link Stream} of enclosing {@link Composite}s, top-most first.
     */
    Streamable<Composite> hierarchy();

    /**
     * Creates a <i>boundary</i> {@link Object}.
     *
     * @param <T>    the type of {@link Object}
     * @param object the {@link Object}
     * @return a {@link Entity} representing the specified <i>top</i> {@link Object}
     */
    static <T> Entity<T> boundary(final T object) {
        Objects.requireNonNull(object, "The object must not be null");

        return new Entity<>() {
            @Override
            public T object() {
                return object;
            }

            @Override
            public Optional<Composite> composite() {
                return Optional.empty();
            }

            @Override
            public Optional<T> part() {
                return Optional.empty();
            }

            @Override
            public Streamable<Composite> hierarchy() {
                return Streamable.empty();
            }
        };
    }

    /**
     * Creates a {@link Entity} for an {@link Object} that is a <i>direct</i> <i>part</i> of a {@link Composite}.
     *
     * @param object    the {@link Object}
     * @param composite the {@link Composite}
     * @param <T>       the type of {@link Object}
     * @return a {@link Entity} representing the specified {@link Object} within the specified {@link Composite}
     */
    static <T> Entity<T> of(final T object, final Composite composite) {
        Objects.requireNonNull(object, "The object must not be null");

        final var optionalComposite = Optional.ofNullable(composite);

        if (composite == null) {
            return boundary(object);
        }

        final Streamable<Composite> hierarchyStreamable = Streamable.of(composite);

        return new Entity<>() {
            @Override
            public T object() {
                return object;
            }

            @Override
            public Optional<Composite> composite() {
                return optionalComposite;
            }

            @Override
            public Optional<T> part() {
                return Optional.of(object);
            }

            @Override
            public int distance() {
                return 1;
            }

            @Override
            public Streamable<Composite> hierarchy() {
                return hierarchyStreamable;
            }
        };
    }

    /**
     * Creates a {@link Entity} for an {@link Object} with the specified hierarchy of {@link Composite}s from the
     * <i>top</i> to the {@link Object}.
     *
     * @param object    the {@link Object}
     * @param hierarchy the hierarchy of {@link Composite}s
     * @param <T>       the type of {@link Object}
     * @return a {@link Entity} representing the specified {@link Object} within the specified hierarchy of
     * {@link Composite}s
     */
    static <T> Entity<T> of(final T object, final Stream<Composite> hierarchy) {
        Objects.requireNonNull(object, "The object must not be null");

        // parents: innermost (direct) composite first, boundary last
        final List<Composite> parents = hierarchy == null ? List.of() : hierarchy.toList();

        if (parents.isEmpty()) {
            return boundary(object);
        }

        final int depth = parents.size();
        final Streamable<Composite> hierarchyStreamable = Streamable.reversed(parents.stream());

        return new Entity<>() {

            @Override
            public T object() {
                return object;
            }

            @Override
            public Optional<Composite> composite() {
                return Optional.of(parents.get(0));
            }

            @Override
            public Optional<T> part() {
                return Optional.of(object);
            }

            @Override
            public int distance() {
                return depth;
            }

            @Override
            public Streamable<Composite> hierarchy() {
                return hierarchyStreamable;
            }
        };
    }
}
