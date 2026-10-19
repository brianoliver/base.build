package build.base.query;

/*-
 * #%L
 * base.build Query
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
import build.base.foundation.memoizer.Memoizer;
import build.base.foundation.stream.Streamable;
import build.base.foundation.tuple.Pair;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * An {@code abstract} thread-safe heap-based {@link Index}.
 *
 * @author brian.oliver
 * @since Sep-2025
 */
public abstract class AbstractHeapBasedIndex implements Index {

    /**
     * A constant representing a {@code null} object, used to allow indexing of {@code null} values in
     * {@link Object} index.
     */
    private static final Object NULL_OBJECT = new Object();

    /**
     * Wraps the set of element-keys stored in the reverse index for a multi-valued {@link Indexable} function,
     * distinguishing it from a plain scalar value stored in the same map.
     */
    private record MultiValuedKeys(Set<Object> keys) {
    }

    /**
     * The {@link Object}s known to the index by {@link Class}.
     */
    private final ConcurrentHashMap<Class<?>, Set<Object>> objectByClass;

    /**
     * Reverse + forward maps for non-{@link Unique} {@link Indexable} functions, keyed by class then function.
     */
    private final ConcurrentHashMap<
        Class<?>,
        ConcurrentHashMap<
            Function<Object, Object>,
            Pair<ConcurrentHashMap<Object, Object>, ConcurrentHashMap<Object, Set<Object>>>>> objectsByClassIndexableFunctionAndValue;

    /**
     * Reverse + forward maps for {@link Unique} {@link Indexable} functions, keyed by class then function.
     */
    private final ConcurrentHashMap<
        Class<?>,
        ConcurrentHashMap<
            Function<Object, Object>,
            Pair<ConcurrentHashMap<Object, Object>, ConcurrentHashMap<Object, Object>>>> uniqueObjectsByClassFunctionAndKey;

    /**
     * The resolved {@link Indexable} (non-{@link Unique}) {@link Function}s per {@link Class}, memoized so that
     * reflection is performed at most once per class.
     */
    private final Memoizer<Class<?>, Streamable<Function<Object, Object>>> indexableFunctionsByClass;

    /**
     * The resolved {@link Indexable} {@link Unique} {@link Function}s per {@link Class}, memoized so that reflection
     * is performed at most once per class.
     */
    private final Memoizer<Class<?>, Streamable<Function<Object, Object>>> uniqueIndexableFunctionsByClass;

    /**
     * The resolved {@link Indexable} {@link Dynamic} (non-{@link Unique}) {@link Function}s per {@link Class},
     * memoized so that reflection is performed at most once per class.
     */
    private final Memoizer<Class<?>, Streamable<Function<Object, Object>>> dynamicNonUniqueFunctionsByClass;

    /**
     * The resolved {@link Indexable} {@link Dynamic} {@link Unique} {@link Function}s per {@link Class}, memoized so
     * that reflection is performed at most once per class.
     */
    private final Memoizer<Class<?>, Streamable<Function<Object, Object>>> dynamicUniqueFunctionsByClass;

    /**
     * The resolved {@link Indexable#each() each} (non-{@link Unique}) {@link Function}s per {@link Class}, memoized
     * so that reflection is performed at most once per class.
     */
    private final Memoizer<Class<?>, Streamable<Function<Object, Object>>> eachFunctionsByClass;

    /**
     * The resolved {@link Indexable#each() each} {@link Dynamic} (non-{@link Unique}) {@link Function}s per
     * {@link Class}, memoized so that reflection is performed at most once per class.
     */
    private final Memoizer<Class<?>, Streamable<Function<Object, Object>>> dynamicEachFunctionsByClass;

    /**
     * Constructs an empty {@link AbstractHeapBasedIndex}.
     */
    protected AbstractHeapBasedIndex() {
        this.objectByClass = new ConcurrentHashMap<>();
        this.objectsByClassIndexableFunctionAndValue = new ConcurrentHashMap<>();
        this.uniqueObjectsByClassFunctionAndKey = new ConcurrentHashMap<>();
        this.indexableFunctionsByClass = Memoizer.of(AbstractHeapBasedIndex::resolveIndexableFunctions).concurrent().build();
        this.uniqueIndexableFunctionsByClass = Memoizer.of(AbstractHeapBasedIndex::resolveUniqueIndexableFunctions).concurrent().build();
        this.dynamicNonUniqueFunctionsByClass = Memoizer.of(AbstractHeapBasedIndex::resolveDynamicNonUniqueFunctions).concurrent().build();
        this.dynamicUniqueFunctionsByClass = Memoizer.of(AbstractHeapBasedIndex::resolveDynamicUniqueFunctions).concurrent().build();
        this.eachFunctionsByClass = Memoizer.of(AbstractHeapBasedIndex::resolveEachFunctions).concurrent().build();
        this.dynamicEachFunctionsByClass = Memoizer.of(AbstractHeapBasedIndex::resolveDynamicEachFunctions).concurrent().build();
    }

    @Override
    public void index(final Object object) {
        final var objectClass = object.getClass();
        final var nonUniqueFunctions = this.indexableFunctionsByClass.compute(objectClass);
        final var eachFunctions = this.eachFunctionsByClass.compute(objectClass);
        final var uniqueFunctions = this.uniqueIndexableFunctionsByClass.compute(objectClass);

        nonUniqueFunctions.forEach(function -> indexNonUnique(objectClass, function, object));
        eachFunctions.forEach(function -> indexEach(objectClass, function, object));
        uniqueFunctions.forEach(function -> indexUnique(objectClass, function, object));

        if (isIndexParticipant(objectClass, nonUniqueFunctions, eachFunctions, uniqueFunctions)) {
            this.objectByClass.compute(objectClass, (_, existing) -> {
                final var objects = existing == null ? ConcurrentHashMap.newKeySet() : existing;
                objects.add(object);
                return objects;
            });
        }
    }

    @Override
    public void unindex(final Object object) {
        final var objectClass = object.getClass();
        final var nonUniqueFunctions = this.indexableFunctionsByClass.compute(objectClass);
        final var eachFunctions = this.eachFunctionsByClass.compute(objectClass);
        final var uniqueFunctions = this.uniqueIndexableFunctionsByClass.compute(objectClass);

        nonUniqueFunctions.forEach(function -> unindexNonUnique(objectClass, function, object));
        eachFunctions.forEach(function -> unindexNonUnique(objectClass, function, object));
        uniqueFunctions.forEach(function -> unindexUnique(objectClass, function, object));

        if (isIndexParticipant(objectClass, nonUniqueFunctions, eachFunctions, uniqueFunctions)) {
            this.objectByClass.compute(objectClass, (_, existing) -> {
                if (existing == null) {
                    return null;
                }
                existing.remove(object);
                return existing.isEmpty() ? null : existing;
            });
        }
    }

    @Override
    public void reindexDynamic(final Object object) {
        final var objectClass = object.getClass();
        this.dynamicNonUniqueFunctionsByClass.compute(objectClass).forEach(function -> reindexNonUnique(objectClass, function, object));
        this.dynamicEachFunctionsByClass.compute(objectClass).forEach(function -> reindexEach(objectClass, function, object));
        this.dynamicUniqueFunctionsByClass.compute(objectClass).forEach(function -> reindexUnique(objectClass, function, object));
    }

    @Override
    public <T> void add(final Class<T> valueClass, final T value) {
        Objects.requireNonNull(valueClass, "The value class must not be null");
        Objects.requireNonNull(value, "The value must not be null");

        this.objectByClass.compute(valueClass, (_, existing) -> {
            final var objects = existing == null ? ConcurrentHashMap.newKeySet() : existing;
            objects.add(value);
            return objects;
        });
    }

    @Override
    public <T> void remove(final Class<T> valueClass, final T value) {
        Objects.requireNonNull(valueClass, "The value class must not be null");
        Objects.requireNonNull(value, "The value must not be null");

        this.objectByClass.compute(valueClass, (_, existing) -> {
            if (existing == null) {
                return null;
            }
            existing.remove(value);
            return existing.isEmpty() ? null : existing;
        });
    }

    /**
     * Obtains an {@link Iterator} over the {@link Object}s of the specified {@link Class} that may be used to
     * match the query should the index not contain a match.
     *
     * @param <T>            the type of {@link Object}
     * @param matchableClass the {@link Class} of {@link Object}
     * @param scope          the {@link Scope} for obtaining {@link Object}s to query
     * @return an {@link Iterator} over the {@link Object}s of the specified {@link Class}
     */
    protected abstract <T> Iterator<T> traverse(Class<T> matchableClass,
                                                Scope scope);

    /**
     * Obtains an {@link Iterator} over {@link Object}s that may be used to match the query should the index not
     * contain a match.
     *
     * @param scope the {@link Scope} for obtaining {@link Object}s to query
     * @return an {@link Iterator} over the {@link Object}s
     */
    protected abstract Iterator<Object> traverse(Scope scope);

    @Override
    public <Q> Match<Q> match(final Class<Q> matchableClass) {
        return new Query<>(this, matchableClass);
    }

    // ---- index / unindex / reindex operations

    private void indexNonUnique(final Class<?> objectClass, final Function<Object, Object> function, final Object object) {
        onNonUniquePair(objectClass, function, pair -> {
            try {
                putNonUnique(pair, object, toIndexableValue(function.apply(object)));
            } catch (final Throwable e) {
                throw new UnsupportedOperationException("Failed to index [" + objectClass.getName() + "] as a function failed to extract a value from the object", e);
            }
        });
    }

    private void indexEach(final Class<?> objectClass, final Function<Object, Object> function, final Object object) {
        onNonUniquePair(objectClass, function, pair -> {
            try {
                indexMultiValued(object, toElementStream(function.apply(object), objectClass), pair);
            } catch (final UnsupportedOperationException e) {
                throw e;
            } catch (final Throwable e) {
                throw new UnsupportedOperationException("Failed to index [" + objectClass.getName() + "] as an each function failed to extract elements from the object", e);
            }
        });
    }

    private void indexUnique(final Class<?> objectClass, final Function<Object, Object> function, final Object object) {
        onUniquePair(objectClass, function, pair -> {
            try {
                final var value = function.apply(object);
                putUnique(pair, object, toIndexableValue(value), value, objectClass);
            } catch (final IllegalStateException e) {
                throw e;
            } catch (final Throwable e) {
                throw new UnsupportedOperationException("Failed to index [" + objectClass.getName() + "] as a unique function failed to extract a value from the object", e);
            }
        });
    }

    private void unindexNonUnique(final Class<?> objectClass, final Function<Object, Object> function, final Object object) {
        onExistingNonUniquePair(objectClass, function, pair -> {
            if (pair == null) {
                return null;
            }
            removeNonUnique(pair, object);
            return pair.second().isEmpty() ? null : pair;
        });
    }

    private void unindexUnique(final Class<?> objectClass, final Function<Object, Object> function, final Object object) {
        onExistingUniquePair(objectClass, function, pair -> {
            if (pair == null) {
                return null;
            }
            removeUnique(pair, object);
            return pair.second().isEmpty() ? null : pair;
        });
    }

    private void reindexNonUnique(final Class<?> objectClass, final Function<Object, Object> function, final Object object) {
        onNonUniquePair(objectClass, function, pair -> {
            removeNonUnique(pair, object);
            try {
                putNonUnique(pair, object, toIndexableValue(function.apply(object)));
            } catch (final Throwable e) {
                throw new UnsupportedOperationException("Failed to reindex [" + objectClass.getName() + "] as a dynamic function failed to extract a value from the object", e);
            }
        });
    }

    private void reindexEach(final Class<?> objectClass, final Function<Object, Object> function, final Object object) {
        onNonUniquePair(objectClass, function, pair -> {
            removeNonUnique(pair, object);
            try {
                indexMultiValued(object, toElementStream(function.apply(object), objectClass), pair);
            } catch (final UnsupportedOperationException e) {
                throw e;
            } catch (final Throwable e) {
                throw new UnsupportedOperationException("Failed to reindex [" + objectClass.getName() + "] as a dynamic each function failed to extract elements from the object", e);
            }
        });
    }

    private void reindexUnique(final Class<?> objectClass, final Function<Object, Object> function, final Object object) {
        onUniquePair(objectClass, function, pair -> {
            removeUnique(pair, object);
            try {
                final var value = function.apply(object);
                putUnique(pair, object, toIndexableValue(value), value, objectClass);
            } catch (final IllegalStateException e) {
                throw e;
            } catch (final Throwable e) {
                throw new UnsupportedOperationException("Failed to reindex [" + objectClass.getName() + "] as a dynamic unique function failed to extract a value from the object", e);
            }
        });
    }

    // ---- structural helpers: outer-compute + inner-compute skeleton

    /**
     * Runs {@code action} on the pair for {@code function} within {@code objectClass}, creating maps if absent.
     */
    private void onNonUniquePair(final Class<?> objectClass,
                                 final Function<Object, Object> function,
                                 final Consumer<Pair<ConcurrentHashMap<Object, Object>, ConcurrentHashMap<Object, Set<Object>>>> action) {
        this.objectsByClassIndexableFunctionAndValue.compute(objectClass, (_, existingFunctions) -> {
            final var functions = existingFunctions == null
                ? new ConcurrentHashMap<Function<Object, Object>, Pair<ConcurrentHashMap<Object, Object>, ConcurrentHashMap<Object, Set<Object>>>>()
                : existingFunctions;

            functions.compute(function, (_, existingPair) -> {
                final var pair = existingPair == null
                    ? Pair.of(new ConcurrentHashMap<>(), new ConcurrentHashMap<Object, Set<Object>>())
                    : existingPair;
                action.accept(pair);
                return pair;
            });

            return functions;
        });
    }

    /**
     * Runs {@code action} on the pair for {@code function} within {@code objectClass}, creating maps if absent.
     */
    private void onUniquePair(final Class<?> objectClass,
                              final Function<Object, Object> function,
                              final Consumer<Pair<ConcurrentHashMap<Object, Object>, ConcurrentHashMap<Object, Object>>> action) {
        this.uniqueObjectsByClassFunctionAndKey.compute(objectClass, (_, existingFunctions) -> {
            final var functions = existingFunctions == null
                ? new ConcurrentHashMap<Function<Object, Object>, Pair<ConcurrentHashMap<Object, Object>, ConcurrentHashMap<Object, Object>>>()
                : existingFunctions;

            functions.compute(function, (_, existingPair) -> {
                final var pair = existingPair == null
                    ? Pair.of(new ConcurrentHashMap<>(), new ConcurrentHashMap<Object, Object>())
                    : existingPair;
                action.accept(pair);
                return pair;
            });

            return functions;
        });
    }

    /**
     * Runs {@code action} on the existing pair (or {@code null}) for {@code function}, cleaning up empty maps.
     */
    private void onExistingNonUniquePair(final Class<?> objectClass,
                                         final Function<Object, Object> function,
                                         final UnaryOperator<Pair<ConcurrentHashMap<Object, Object>, ConcurrentHashMap<Object, Set<Object>>>> action) {
        this.objectsByClassIndexableFunctionAndValue.compute(objectClass, (_, existingFunctions) -> {
            if (existingFunctions == null) {
                return null;
            }
            existingFunctions.compute(function, (_, existingPair) -> action.apply(existingPair));
            return existingFunctions.isEmpty() ? null : existingFunctions;
        });
    }

    /**
     * Runs {@code action} on the existing pair (or {@code null}) for {@code function}, cleaning up empty maps.
     */
    private void onExistingUniquePair(final Class<?> objectClass,
                                      final Function<Object, Object> function,
                                      final UnaryOperator<Pair<ConcurrentHashMap<Object, Object>, ConcurrentHashMap<Object, Object>>> action) {
        this.uniqueObjectsByClassFunctionAndKey.compute(objectClass, (_, existingFunctions) -> {
            if (existingFunctions == null) {
                return null;
            }
            existingFunctions.compute(function, (_, existingPair) -> action.apply(existingPair));
            return existingFunctions.isEmpty() ? null : existingFunctions;
        });
    }

    // ---- pair-level helpers

    private static Stream<Object> toElementStream(final Object value, final Class<?> objectClass) {
        if (value instanceof Stream<?> s) {
            return s.map(e -> e == null ? NULL_OBJECT : e);
        } else if (value instanceof Collection<?> c) {
            return c.stream().map(e -> e == null ? NULL_OBJECT : e);
        } else if (value instanceof Iterable<?> it) {
            return StreamSupport.stream(it.spliterator(), false).map(e -> e == null ? NULL_OBJECT : e);
        } else {
            throw new UnsupportedOperationException(
                "@Indexable(each = true) function on [" + objectClass.getName()
                    + "] must return a Stream, Collection, or Iterable, but returned ["
                    + (value == null ? "null" : value.getClass().getName()) + "]");
        }
    }

    private static void putNonUnique(final Pair<ConcurrentHashMap<Object, Object>, ConcurrentHashMap<Object, Set<Object>>> pair,
                                     final Object object,
                                     final Object indexableValue) {
        pair.first().put(object, indexableValue);
        pair.second().compute(indexableValue, (_, existing) -> {
            final var objects = existing == null ? ConcurrentHashMap.newKeySet() : existing;
            objects.add(object);
            return objects;
        });
    }

    private static void removeNonUnique(final Pair<ConcurrentHashMap<Object, Object>, ConcurrentHashMap<Object, Set<Object>>> pair,
                                        final Object object) {
        final var old = pair.first().remove(object);
        if (old instanceof MultiValuedKeys(Set<Object> keys)) {
            for (final var key : keys) {
                pair.second().compute(key, (_, existing) -> {
                    if (existing == null) {
                        return null;
                    }
                    existing.remove(object);
                    return existing.isEmpty() ? null : existing;
                });
            }
        } else if (old != null) {
            pair.second().compute(old, (_, existing) -> {
                if (existing == null) {
                    return null;
                }
                existing.remove(object);
                return existing.isEmpty() ? null : existing;
            });
        }
    }

    private static void putUnique(final Pair<ConcurrentHashMap<Object, Object>, ConcurrentHashMap<Object, Object>> pair,
                                  final Object object,
                                  final Object indexableValue,
                                  final Object rawValue,
                                  final Class<?> objectClass) {
        final var displaced = pair.second().putIfAbsent(indexableValue, object);
        if (displaced != null && displaced != object) {
            throw new IllegalStateException(
                "Unique key violation: key [" + rawValue + "] on [" + objectClass.getName() + "] is already held by [" + displaced + "]");
        }
        pair.first().put(object, indexableValue);
    }

    private static void removeUnique(final Pair<ConcurrentHashMap<Object, Object>, ConcurrentHashMap<Object, Object>> pair,
                                     final Object object) {
        final var key = pair.first().remove(object);
        if (key != null) {
            pair.second().remove(key);
        }
    }

    // ---- shared utilities

    /**
     * Returns {@code true} if {@code objectClass} participates in class-membership: either it carries
     * {@link Indexable} directly, or it declares at least one {@link Indexable} function field.
     */
    private boolean isIndexParticipant(final Class<?> objectClass,
                                       final Streamable<Function<Object, Object>> nonUnique,
                                       final Streamable<Function<Object, Object>> each,
                                       final Streamable<Function<Object, Object>> unique) {
        return Introspection.hasDeclaredAnnotation(objectClass, Indexable.class)
            || !nonUnique.isEmpty()
            || !each.isEmpty()
            || !unique.isEmpty();
    }

    /**
     * Replaces {@code null} with {@link #NULL_OBJECT} so that {@code null} values can be stored in
     * {@link ConcurrentHashMap}s that do not permit {@code null} keys.
     */
    private static Object toIndexableValue(final Object value) {
        return value == null ? NULL_OBJECT : value;
    }

    /**
     * Resolves the value of a {@code public static final} {@link Function} {@link Field}, making it accessible
     * first. Called at memoization time — once per field per class.
     */
    @SuppressWarnings("unchecked")
    private static Function<Object, Object> resolveFunction(final Field field) {
        try {
            field.setAccessible(true);
            return (Function<Object, Object>) field.get(null);
        } catch (final IllegalAccessException e) {
            throw new RuntimeException("Cannot access @Indexable field [" + field.getName() + "] on ["
                + field.getDeclaringClass().getName() + "]", e);
        }
    }

    private static boolean isIndexableFunctionField(final Field field) {
        return field.getAnnotation(Indexable.class) != null
            && Modifier.isPublic(field.getModifiers())
            && Modifier.isStatic(field.getModifiers())
            && Modifier.isFinal(field.getModifiers())
            && Function.class.isAssignableFrom(field.getType());
    }

    /**
     * Indexes each element produced by a multi-valued {@link Indexable} function into the forward and reverse maps.
     *
     * @param object   the object being indexed
     * @param elements the stream of non-{@code null} element keys (nulls already replaced with {@link #NULL_OBJECT})
     * @param pair     the pair of reverse and forward maps for the function
     */
    private static void indexMultiValued(final Object object,
                                         final Stream<Object> elements,
                                         final Pair<ConcurrentHashMap<Object, Object>, ConcurrentHashMap<Object, Set<Object>>> pair) {

        final var keys = ConcurrentHashMap.newKeySet();

        elements.forEach(key -> {
            keys.add(key);
            pair.second().compute(key, (_, existing) -> {
                final var set = existing == null ? ConcurrentHashMap.newKeySet() : existing;
                set.add(object);
                return set;
            });
        });

        pair.first().put(object, new MultiValuedKeys(keys));
    }

    /**
     * A {@link Terminal} implementation for checking membership in a multi-valued extracted value.
     * <p>
     * When the underlying {@link Indexable} function has been indexed, performs an O(1) reverse-map lookup because
     * each element was stored as an individual key during {@link #index(Object)}. Falls back to a linear scan that
     * re-invokes the function and tests containment for unindexed objects.
     *
     * @param <Q> the type of {@link Object} being queried
     * @param <V> the type of value extracted by the {@link Where} clause
     */
    private class Contains<Q, V>
        extends AbstractTerminal<Q, V, Contains<Q, V>> {

        private final Object element;

        Contains(final Where<Q, V> where,
                 final Object element) {

            super(where);
            this.element = element;
        }

        @Override
        public Stream<Q> findAll() {
            final var key = this.element == null ? NULL_OBJECT : this.element;

            final var indexPairs = this.where.matchingIndexPairs();
            if (!indexPairs.isEmpty()) {
                return indexPairs.stream()
                    .map(pair -> pair.second().get(key))
                    .filter(objects -> objects != null && !objects.isEmpty())
                    .flatMap(Set::stream)
                    .map(this.where.select.objectClass::cast);
            }

            // fallback: linear scan with containment check
            return this.where.select.stream(this.scope)
                .filter(q -> containsElement(this.where.function.apply(q), this.element));
        }

        static boolean containsElement(final Object container, final Object element) {
            if (container instanceof Collection<?> c) {
                return c.contains(element);
            }
            if (container instanceof Stream<?> s) {
                return s.anyMatch(e -> Objects.equals(e, element));
            }
            if (container instanceof Iterable<?> it) {
                for (final var e : it) {
                    if (Objects.equals(e, element)) {
                        return true;
                    }
                }
                return false;
            }
            return Objects.equals(container, element);
        }
    }

    /**
     * A {@link Terminal} implementation for checking that an element is absent from a multi-valued extracted value.
     * <p>
     * Always performs a linear scan. The reverse index only supports positive membership lookups; negation would
     * require enumerating all objects and subtracting those in the membership set, which is no better than scanning.
     *
     * @param <Q> the type of {@link Object} being queried
     * @param <V> the type of value extracted by the {@link Where} clause
     */
    private class DoesNotContain<Q, V>
        extends AbstractTerminal<Q, V, DoesNotContain<Q, V>> {

        private final Object element;

        DoesNotContain(final Where<Q, V> where,
                       final Object element) {

            super(where);
            this.element = element;
        }

        @Override
        public Stream<Q> findAll() {
            return this.where.select.stream(this.scope)
                .filter(q -> !Contains.containsElement(this.where.function.apply(q), this.element));
        }
    }

    /**
     * Obtains the resolved {@link Indexable} (non-{@link Unique}) {@link Function}s for the specified {@link Class}.
     *
     * @param indexableClass the {@link Class} of queryable
     * @return the {@link Streamable} of resolved {@link Function}s
     */
    protected static Streamable<Function<Object, Object>> resolveIndexableFunctions(final Class<?> indexableClass) {
        return Streamable.of(Introspection.getAllDeclaredFields(indexableClass)
            .filter(field -> isIndexableFunctionField(field)
                && !field.getAnnotation(Indexable.class).each()
                && field.getAnnotation(Unique.class) == null)
            .map(AbstractHeapBasedIndex::resolveFunction));
    }

    /**
     * Obtains the resolved {@link Indexable} {@link Unique} {@link Function}s for the specified {@link Class}.
     *
     * @param indexableClass the {@link Class} of queryable
     * @return the {@link Streamable} of resolved {@link Function}s
     */
    protected static Streamable<Function<Object, Object>> resolveUniqueIndexableFunctions(final Class<?> indexableClass) {
        return Streamable.of(Introspection.getAllDeclaredFields(indexableClass)
            .filter(field -> isIndexableFunctionField(field) && field.getAnnotation(Unique.class) != null)
            .map(AbstractHeapBasedIndex::resolveFunction));
    }

    /**
     * Obtains the resolved {@link Indexable} {@link Dynamic} (non-{@link Unique}) {@link Function}s for the
     * specified {@link Class}.
     *
     * @param indexableClass the {@link Class} of queryable
     * @return the {@link Streamable} of resolved {@link Function}s
     */
    protected static Streamable<Function<Object, Object>> resolveDynamicNonUniqueFunctions(final Class<?> indexableClass) {
        return Streamable.of(Introspection.getAllDeclaredFields(indexableClass)
            .filter(field -> isIndexableFunctionField(field)
                && !field.getAnnotation(Indexable.class).each()
                && field.getAnnotation(Dynamic.class) != null
                && field.getAnnotation(Unique.class) == null)
            .map(AbstractHeapBasedIndex::resolveFunction));
    }

    protected static Streamable<Function<Object, Object>> resolveEachFunctions(final Class<?> indexableClass) {
        return Streamable.of(Introspection.getAllDeclaredFields(indexableClass)
            .filter(field -> isIndexableFunctionField(field)
                && field.getAnnotation(Indexable.class).each()
                && field.getAnnotation(Unique.class) == null)
            .map(AbstractHeapBasedIndex::resolveFunction));
    }

    protected static Streamable<Function<Object, Object>> resolveDynamicEachFunctions(final Class<?> indexableClass) {
        return Streamable.of(Introspection.getAllDeclaredFields(indexableClass)
            .filter(field -> isIndexableFunctionField(field)
                && field.getAnnotation(Indexable.class).each()
                && field.getAnnotation(Dynamic.class) != null
                && field.getAnnotation(Unique.class) == null)
            .map(AbstractHeapBasedIndex::resolveFunction));
    }

    /**
     * Obtains the resolved {@link Indexable} {@link Dynamic} {@link Unique} {@link Function}s for the specified
     * {@link Class}.
     *
     * @param indexableClass the {@link Class} of queryable
     * @return the {@link Streamable} of resolved {@link Function}s
     */
    protected static Streamable<Function<Object, Object>> resolveDynamicUniqueFunctions(final Class<?> indexableClass) {
        return Streamable.of(Introspection.getAllDeclaredFields(indexableClass)
            .filter(field -> isIndexableFunctionField(field)
                && field.getAnnotation(Dynamic.class) != null
                && field.getAnnotation(Unique.class) != null)
            .map(AbstractHeapBasedIndex::resolveFunction));
    }

    /**
     * An internal {@link Match} implementation.
     *
     * @param <Q> the type of {@link Object} being queried
     */
    private class Query<Q>
        implements Match<Q> {

        /**
         * The {@link AbstractHeapBasedIndex} from which this {@link Query} was created.
         */
        private final AbstractHeapBasedIndex index;

        /**
         * The {@link Class} of the {@link Object} being selected.
         */
        private final Class<Q> objectClass;

        /**
         * The {@link Scope} for querying.
         */
        private Scope scope;

        /**
         * Constructs an {@link Query} for the specified {@link Class} of {@link Object}.
         *
         * @param index       the {@link AbstractHeapBasedIndex} from which this {@link Query} was created
         * @param objectClass the {@link Class} of {@link Object} to select
         */
        Query(final AbstractHeapBasedIndex index,
              final Class<Q> objectClass) {

            this.index = Objects.requireNonNull(index, "The Index must not be null");
            this.objectClass = Objects.requireNonNull(objectClass, "The Object class must not be null");
            this.scope = Scope.Direct;
        }

        @Override
        public Query<Q> scope(final Scope scope) {
            this.scope = scope == null ? Scope.Direct : scope;
            return this;
        }

        /**
         * Returns a {@link Stream} of all objects in the index whose concrete class is assignable to
         * {@link #objectClass}, drawn from {@code objectByClass} across all matching keys.
         */
        private Stream<Q> indexedStream() {
            return AbstractHeapBasedIndex.this.objectByClass.entrySet().stream()
                .filter(e -> this.objectClass.isAssignableFrom(e.getKey()))
                .flatMap(e -> e.getValue().stream())
                .map(this.objectClass::cast);
        }

        /**
         * Obtain the {@link Stream} of {@link Object}s of the specified {@link Class}.
         *
         * @param scope the {@link Scope} for obtaining {@link Object}s to query
         * @return the {@link Stream} of {@link Object}s
         */
        Stream<Q> stream(final Scope scope) {
            if (scope == Scope.Indexed) {
                return indexedStream();
            }

            // For Direct/BreadthFirst/DepthFirst: traversal is additive, not a fallback
            final var traversedByClass = this.index.traverse(this.objectClass, scope);
            final Stream<Q> traversalStream;

            if (traversedByClass != null && traversedByClass.hasNext()) {
                traversalStream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(traversedByClass, 0), false);
            } else {
                final var traversedAll = this.index.traverse(scope);
                traversalStream = traversedAll == null || !traversedAll.hasNext()
                    ? Stream.empty()
                    : StreamSupport.stream(Spliterators.spliteratorUnknownSize(traversedAll, 0), false)
                      .filter(this.objectClass::isInstance)
                      .map(this.objectClass::cast);
            }

            // Concatenate indexed (all assignable subclasses) + traversal, deduplicated by identity
            final Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            return Stream.concat(
                indexedStream().filter(seen::add),
                traversalStream.filter(seen::add));
        }

        @Override
        public <V> Condition<Q, V> where(final Function<? super Q, V> function) {
            return new Where<>(this, function);
        }

        @Override
        public Stream<Q> findAll() {
            return stream(this.scope);
        }
    }


    /**
     * An {@link Condition} implementation to extract values from an {@link Object}.
     *
     * @param <Q> the type of {@link Object}
     * @param <V> the type of value
     */
    private class Where<Q, V>
        implements Condition<Q, V> {

        /**
         * The {@link Query} that created this {@link Where}.
         */
        private final Query<Q> select;

        /**
         * The {@link Function} to extract a value from a {@link Object}.
         */
        private final Function<? super Q, V> function;

        /**
         * Constructs an {@link Where} with the specified function to extract values from the {@link Object}.
         *
         * @param function the extractor {@link Function}
         */
        Where(final Query<Q> select,
              final Function<? super Q, V> function) {

            this.select = Objects.requireNonNull(select, "The Select must not be null");
            this.function = Objects.requireNonNull(function, "The value extractor function must not be null");
        }

        /**
         * Ensures that the value is not {@code null} by replacing it with a constant {@link #NULL_OBJECT} if it is.
         *
         * @param value the value
         * @return the non-{@code null} value, or {@link #NULL_OBJECT} if the value is {@code null}
         */
        @SuppressWarnings("unchecked")
        private V nonNull(final V value) {
            return value == null ? (V) NULL_OBJECT : value;
        }

        /**
         * Returns all unique-index pairs for this function across assignable classes.
         */
        private List<Pair<ConcurrentHashMap<Object, Object>, ConcurrentHashMap<Object, Object>>> matchingUniquePairs() {
            return AbstractHeapBasedIndex.this.uniqueObjectsByClassFunctionAndKey.entrySet().stream()
                .filter(e -> this.select.objectClass.isAssignableFrom(e.getKey()))
                .map(e -> e.getValue().get(this.function))
                .filter(Objects::nonNull)
                .toList();
        }

        /**
         * Returns all non-unique index pairs for this function across assignable classes.
         */
        private List<Pair<ConcurrentHashMap<Object, Object>, ConcurrentHashMap<Object, Set<Object>>>> matchingIndexPairs() {
            return AbstractHeapBasedIndex.this.objectsByClassIndexableFunctionAndValue.entrySet().stream()
                .filter(e -> this.select.objectClass.isAssignableFrom(e.getKey()))
                .map(e -> e.getValue().get(this.function))
                .filter(Objects::nonNull)
                .toList();
        }

        @Override
        public IsEqualTo<Q, V> isEqualTo(final V value) {
            return new IsEqualTo<>(this, nonNull(value));
        }

        @Override
        public IsNotEqualTo<Q, V> isNotEqualTo(final V value) {
            return new IsNotEqualTo<>(this, nonNull(value));
        }

        @Override
        public Terminal<Q, Matches<Q, V>> matches(final Predicate<? super V> predicate) {
            return new Matches<>(this, predicate);
        }

        @Override
        public Terminal<Q, ?> contains(final Object element) {
            return new Contains<Q, V>(this, element);
        }

        @Override
        public Terminal<Q, ?> doesNotContain(final Object element) {
            return new DoesNotContain<Q, V>(this, element);
        }

        @Override
        public IsIn<Q, V> isIn(final Collection<? extends V> values) {
            return new IsIn<>(this, values);
        }
    }


    /**
     * Shared base for the three terminal condition classes, holding the {@link Where} clause and mutable
     * {@link Scope}.
     *
     * @param <Q>    the type of {@link Object} being queried
     * @param <V>    the type of value extracted by the {@link Where} clause
     * @param <Self> the concrete terminal type (for the fluent {@link #scope} override)
     */
    private abstract class AbstractTerminal<Q, V, Self extends AbstractTerminal<Q, V, Self>>
        implements Terminal<Q, Self> {

        final Where<Q, V> where;
        Scope scope;

        AbstractTerminal(final Where<Q, V> where) {
            this.where = Objects.requireNonNull(where, "The Where must not be null");
            this.scope = where.select.scope;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Self scope(final Scope scope) {
            this.scope = scope == null ? Scope.Direct : scope;
            return (Self) this;
        }
    }


    /**
     * A {@link Terminal} implementation for checking if a value is equal to a specified value.
     *
     * @param <Q> the type of {@link Object}
     * @param <V> the type of value
     */
    private class IsEqualTo<Q, V>
        extends AbstractTerminal<Q, V, IsEqualTo<Q, V>> {

        /**
         * The non-{@code null} value to compare against.
         */
        private final V value;

        IsEqualTo(final Where<Q, V> where, final V value) {
            super(where);
            this.value = Objects.requireNonNull(value, "The Value must not be null");
        }

        @Override
        public Stream<Q> findAll() {
            final var uniquePairs = this.where.matchingUniquePairs();
            if (!uniquePairs.isEmpty()) {
                return uniquePairs.stream()
                    .map(pair -> pair.second().get(this.value))
                    .filter(Objects::nonNull)
                    .map(this.where.select.objectClass::cast);
            }

            final var indexPairs = this.where.matchingIndexPairs();
            if (!indexPairs.isEmpty()) {
                return indexPairs.stream()
                    .map(pair -> pair.second().get(this.value))
                    .filter(objects -> objects != null && !objects.isEmpty())
                    .flatMap(Set::stream)
                    .map(this.where.select.objectClass::cast);
            }

            return this.where.select.stream(this.scope)
                .filter(queryable -> Objects.equals(this.where.nonNull(this.where.function.apply(queryable)), this.value));
        }
    }


    /**
     * A {@link Terminal} implementation for checking if a value is not equal to a specified value.
     *
     * @param <Q> the type of {@link Object}
     * @param <V> the type of value
     */
    private class IsNotEqualTo<Q, V>
        extends AbstractTerminal<Q, V, IsNotEqualTo<Q, V>> {

        /**
         * The non-{@code null} value to compare against.
         */
        private final V value;

        IsNotEqualTo(final Where<Q, V> where, final V value) {
            super(where);
            this.value = Objects.requireNonNull(value, "The Value must not be null");
        }

        @Override
        public Stream<Q> findAll() {
            final var uniquePairs = this.where.matchingUniquePairs();
            if (!uniquePairs.isEmpty()) {
                return uniquePairs.stream()
                    .flatMap(pair -> pair.second().entrySet().stream())
                    .filter(entry -> !Objects.equals(entry.getKey(), this.value))
                    .map(entry -> this.where.select.objectClass.cast(entry.getValue()));
            }

            final var indexPairs = this.where.matchingIndexPairs();
            if (!indexPairs.isEmpty()) {
                return indexPairs.stream()
                    .flatMap(pair -> pair.second().entrySet().stream())
                    .filter(entry -> !Objects.equals(entry.getKey(), this.value))
                    .flatMap(entry -> entry.getValue().stream())
                    .map(this.where.select.objectClass::cast);
            }

            return this.where.select.stream(this.scope)
                .filter(queryable -> !Objects.equals(this.where.nonNull(this.where.function.apply(queryable)), this.value));
        }
    }


    /**
     * A {@link Terminal} implementation for checking if a value is one of a specified set of values.
     * <p>
     * When the underlying {@link Indexable} function has been indexed, performs one reverse-map lookup per value and
     * unions the results. Falls back to a linear scan testing membership for unindexed objects.
     *
     * @param <Q> the type of {@link Object}
     * @param <V> the type of value
     */
    private class IsIn<Q, V>
        extends AbstractTerminal<Q, V, IsIn<Q, V>> {

        /**
         * The non-{@code null} values to compare against.
         */
        private final Set<Object> values;

        IsIn(final Where<Q, V> where, final Collection<? extends V> values) {
            super(where);
            this.values = values.stream()
                .map(where::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        @Override
        public Stream<Q> findAll() {
            final var uniquePairs = this.where.matchingUniquePairs();
            if (!uniquePairs.isEmpty()) {
                return uniquePairs.stream()
                    .flatMap(pair -> this.values.stream().map(pair.second()::get))
                    .filter(Objects::nonNull)
                    .map(this.where.select.objectClass::cast);
            }

            final var indexPairs = this.where.matchingIndexPairs();
            if (!indexPairs.isEmpty()) {
                return indexPairs.stream()
                    .flatMap(pair -> this.values.stream().map(pair.second()::get))
                    .filter(objects -> objects != null && !objects.isEmpty())
                    .flatMap(Set::stream)
                    .map(this.where.select.objectClass::cast);
            }

            return this.where.select.stream(this.scope)
                .filter(queryable -> this.values.contains(this.where.nonNull(this.where.function.apply(queryable))));
        }
    }


    /**
     * A {@link Terminal} implementation for checking if an extracted value matches the specified {@link Predicate}.
     *
     * @param <Q> the type of {@link Object}
     * @param <V> the type of {@link Predicate} value
     */
    private class Matches<Q, V>
        extends AbstractTerminal<Q, V, Matches<Q, V>> {

        /**
         * The {@link Predicate} to compare match.
         */
        private final Predicate<? super V> predicate;

        Matches(final Where<Q, V> where, final Predicate<? super V> predicate) {
            super(where);
            this.predicate = Objects.requireNonNull(predicate, "The Predicate must not be null");
        }

        @Override
        @SuppressWarnings("unchecked")
        public Stream<Q> findAll() {
            final var uniquePairs = this.where.matchingUniquePairs();
            if (!uniquePairs.isEmpty()) {
                return uniquePairs.stream()
                    .flatMap(pair -> pair.second().entrySet().stream())
                    .filter(entry -> this.predicate.test((V) (entry.getKey() == NULL_OBJECT ? null : entry.getKey())))
                    .map(entry -> this.where.select.objectClass.cast(entry.getValue()));
            }

            final var indexPairs = this.where.matchingIndexPairs();
            if (!indexPairs.isEmpty()) {
                return indexPairs.stream()
                    .flatMap(pair -> pair.second().entrySet().stream())
                    .filter(entry -> this.predicate.test((V) (entry.getKey() == NULL_OBJECT ? null : entry.getKey())))
                    .flatMap(entry -> entry.getValue().stream())
                    .map(this.where.select.objectClass::cast);
            }

            return this.where.select.stream(this.scope)
                .filter(queryable -> this.predicate.test(this.where.function.apply(queryable)));
        }
    }
}
