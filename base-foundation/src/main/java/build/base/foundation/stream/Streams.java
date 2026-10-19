package build.base.foundation.stream;

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

import build.base.foundation.Requires;
import build.base.foundation.tuple.Pair;
import build.base.foundation.tuple.Triple;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Helper methods for creating and manipulating {@link Stream}s.
 *
 * @author brian.oliver
 * @since Jun 2019
 */
public class Streams {

    /**
     * Private constructor to prevent instantiation.
     */
    private Streams() {
        // empty constructor
    }

    /**
     * Obtains a sequential {@link Stream} given an number of elements.
     *
     * @param <T>      the type of elements
     * @param elements the elements
     * @return a {@link Stream}
     */
    @SafeVarargs
    public static <T> Stream<T> of(final T... elements) {
        return elements == null ? Stream.empty() : Arrays.stream(elements);
    }

    /**
     * Obtains a sequential {@link Stream} given an {@link Iterable}.
     *
     * @param <T>      the type of elements
     * @param iterable the {@link Iterable}
     * @return a {@link Stream}
     */
    public static <T> Stream<T> stream(final Iterable<T> iterable) {
        return iterable == null ? Stream.empty() : StreamSupport.stream(iterable.spliterator(), false);
    }

    /**
     * Concatenates the specified {@link Stream}s.
     *
     * @param <T>     the type of elements
     * @param streams the {@link Stream}s
     * @return the concatenation of the {@link Stream}s
     */
    @SafeVarargs
    public static <T> Stream<T> concat(final Stream<? extends T>... streams) {
        if (streams == null) {
            return Stream.empty();
        }

        return Stream.of(streams)
            .filter(Objects::nonNull)
            .flatMap(Function.identity());
    }

    /**
     * Reverses a {@link Stream}.
     *
     * @param stream the {@link Stream}
     * @param <T>    the type of elements
     * @return a {@link Stream} containing the elements of the provided {@link Stream} in reverse order
     */
    public static <T> Stream<T> reverse(final Stream<T> stream) {
        final ArrayDeque<T> stack = new ArrayDeque<>();
        if (stream != null) {
            stream.forEach(stack::push);
        }
        return stack.stream();
    }

    /**
     * Zips two {@link Stream}s of equal length together.
     *
     * @param stream1 the first {@link Stream}
     * @param stream2 the second {@link Stream}
     * @param <X>     the type of first {@link Stream} element
     * @param <Y>     the type of second {@link Stream} element
     * @return a {@link Stream} of {@link Pair}s of elements from the {@link Stream}s
     */
    public static <X, Y> Stream<Pair<X, Y>> zip(final Stream<X> stream1, final Stream<Y> stream2) {
        final var iterator1 = Objects.requireNonNull(stream1, "The first Stream must not be null").iterator();
        final var iterator2 = Objects.requireNonNull(stream2, "The second Stream must not be null").iterator();

        final var zippedIterator = new Iterator<Pair<X, Y>>() {
            @Override
            public boolean hasNext() {
                return iterator1.hasNext() && iterator2.hasNext();
            }

            @Override
            public Pair<X, Y> next() {
                return Pair.of(iterator1.next(), iterator2.next());
            }
        };

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(zippedIterator, 0), false);
    }

    /**
     * Zips three {@link Stream}s of equal length together.
     *
     * @param stream1 the first {@link Stream}
     * @param stream2 the second {@link Stream}
     * @param <X>     the type of first {@link Stream} element
     * @param <Y>     the type of second {@link Stream} element
     * @param <Z>     the type of third {@link Stream} element
     * @return a {@link Stream} of {@link Triple}s of elements from the {@link Stream}s
     */
    public static <X, Y, Z> Stream<Triple<X, Y, Z>> zip(final Stream<X> stream1,
                                                        final Stream<Y> stream2,
                                                        final Stream<Z> stream3) {

        final var iterator1 = Objects.requireNonNull(stream1, "The first Stream must not be null").iterator();
        final var iterator2 = Objects.requireNonNull(stream2, "The second Stream must not be null").iterator();
        final var iterator3 = Objects.requireNonNull(stream3, "The third Stream must not be null").iterator();

        final var zippedIterator = new Iterator<Triple<X, Y, Z>>() {
            @Override
            public boolean hasNext() {
                return iterator1.hasNext() && iterator2.hasNext();
            }

            @Override
            public Triple<X, Y, Z> next() {
                return Triple.of(iterator1.next(), iterator2.next(), iterator3.next());
            }
        };

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(zippedIterator, 0), false);
    }

    /**
     * Compares the items in the provided {@link Stream}s are equal using {@link Objects#equals(Object, Object)}.
     *
     * @param stream1 the first {@link Stream}
     * @param stream2 the second {@link Stream}
     * @return {@code true} the elements are equal, {@code false} otherwise
     */
    public static boolean equals(final Stream<?> stream1, final Stream<?> stream2) {
        final Iterator<?> iterator1 = stream1.iterator();
        final Iterator<?> iterator2 = stream2.iterator();

        while (iterator1.hasNext() && iterator2.hasNext()) {
            if (!Objects.equals(iterator1.next(), iterator2.next())) {
                return false;
            }
        }
        return !iterator1.hasNext() && !iterator2.hasNext();
    }

    /**
     * Computes a hashcode of the elements in the specified {@link Stream}.
     *
     * @param stream the {@link Stream}
     * @return a hashcode
     */
    public static int hashCode(final Stream<?> stream) {
        if (stream == null) {
            return 0;
        }

        var hashCode = 1;

        final var iterator = stream.iterator();
        while (iterator.hasNext()) {
            final var element = iterator.next();
            hashCode = 31 * hashCode + (element == null ? 0 : element.hashCode());
        }

        return hashCode;
    }

    /**
     * Execute a Topological Sort on a {@link Stream} of nodes which constitutes a Directed Acyclic Graph.
     * <p>
     * Implements both dependencies of the node (faster) and dependents (which requires a further iteration through the
     * dependents tree).  The stream can be reversed for usage which adds O(n) which will be minimal for a complicated
     * graph.
     * <p>
     * The algorithm is from Cormen, Leiserson, and Rivest, and Stein,
     * <i>Introduction to Algorithms</i> (CLRS), where it is called "DFS-Visit" and "Topological-Sort".
     *
     * @param nodes                the {@link Stream} of graph nodes
     * @param successorExtractor   a {@link Function} for extracting the successors, which can be {@code null}
     * @param predecessorExtractor a {@link Function} for extracting the predecessors, which can be {@code null}
     * @param independentNodeOrder the {@link SortOrder} for independent nodes
     * @param <T>                  the type of the graph node
     * @return a sorted {@link Stream} of graph nodes
     * @throws IllegalArgumentException if a cycle is detected
     */
    public static <T> Stream<T> topologicalSort(final Stream<T> nodes,
                                                final Function<T, Stream<T>> successorExtractor,
                                                final Function<T, Stream<T>> predecessorExtractor,
                                                final SortOrder independentNodeOrder) {
        final LinkedHashSet<T> result;
        final Function<T, Stream<T>> extractor;
        final Stream<T> theStream;

        if (predecessorExtractor != null) {
            final List<T> collect = nodes.toList();
            result = new LinkedHashSet<>(collect.size());
            final Map<T, List<T>> reverseMap = new HashMap<>(collect.size());
            collect.forEach(n -> {
                final Stream<T> tStream = predecessorExtractor.apply(n);
                tStream.forEach(m -> reverseMap.compute(m, (k, v) -> {
                    final List<T> myResult = v == null ? new ArrayList<>() : v;
                    myResult.add(n);
                    return myResult;
                }));
            });
            extractor = a -> {
                final List<T> list = reverseMap.get(a);
                final Stream<T> second = list == null ? Stream.empty() : list.stream();
                return successorExtractor == null ? second : Stream.concat(successorExtractor.apply(a), second);
            };
            theStream = collect.stream();
        }
        else {
            // We don't know the exact size
            result = new LinkedHashSet<>();
            extractor = successorExtractor;
            theStream = nodes;
        }

        // A stack of a nodes to do the depth first search
        final ArrayDeque<Pair<T, Iterator<T>>> stack = new ArrayDeque<>();

        // the ordered path we have travelled
        final Set<T> path = new LinkedHashSet<>();

        // nodes we have already visited
        final Set<T> visited = new HashSet<>();

        // nodes which are connected
        final Set<T> connected = new HashSet<>();

        theStream.forEach(current -> {
            stack.push(Pair.of(current, extractor.apply(current).iterator()));
            visited.add(current);
            path.add(current);

            do {
                final Pair<T, Iterator<T>> peek = stack.peek();
                final T candidate = peek.first();
                final Iterator<T> iterator = peek.second();
                if (iterator.hasNext()) {
                    final T edge = iterator.next();
                    if (path.contains(edge)) {
                        throw new IllegalArgumentException("Cycle detected : " + edge + " : " + path);
                    }

                    connected.add(candidate);
                    connected.add(edge);
                    if (visited.add(edge)) {
                        path.add(edge);
                        stack.push(Pair.of(edge, extractor.apply(edge).iterator()));
                    }
                }
                else {
                    stack.pop();
                    result.add(candidate);
                    path.remove(candidate);
                }
            }
            while (!stack.isEmpty());
        });

        // Topological sort reverses the stream and we don't want that
        final List<T> list = Streams.reverse(result.stream()).toList();
        result.clear();
        result.addAll(list);

        if (!SortOrder.UNCHANGED.equals(independentNodeOrder)) {
            final int size = result.size();
            final int capacity = size - connected.size();
            if (capacity != 0) {
                if (independentNodeOrder.equals(SortOrder.LAST)) {
                    final ArrayList<T> independents = new ArrayList<>(capacity);
                    result.removeIf(n -> {
                        if (!connected.contains(n)) {
                            independents.add(n);
                            return true;
                        }
                        return false;
                    });
                    // make sure they are in the correct order
                    java.util.Collections.reverse(independents);
                    result.addAll(independents);
                }
                else {
                    // SortOrder.FIRST, since we can't add to the beginning of the list, we rewrite
                    @SuppressWarnings("unchecked") final T[] independentResult = (T[]) new Object[size];
                    int dependentOffset = size - connected.size();
                    int independentOffset = dependentOffset;
                    for (T node : result) {
                        final int offset = connected.contains(node) ? dependentOffset++ : --independentOffset;
                        independentResult[offset] = node;
                    }
                    return Arrays.stream(independentResult);
                }
            }
        }

        return result.stream();
    }

    /**
     * The order for independent nodes, they can either go first in the list, last or just where they land up.
     */
    public enum SortOrder {
        /**
         * First in the list.
         */
        FIRST,

        /**
         * Last in the list.
         */
        LAST,

        /**
         * Unchanged.
         */
        UNCHANGED
    }

    /**
     * Sorts the elements in the provided {@link Stream} according to their {@link Class} annotated {@link Requires}
     * dependencies.
     *
     * @param stream                  the {@link Stream} of elements
     * @param independentElementOrder the ordering of independent elements
     * @param <T>                     the type of elements
     * @return the sorted {@link Stream}
     */
    public static <T> Stream<T> sortByRequires(final Stream<T> stream,
                                               final SortOrder independentElementOrder) {

        final var elements = stream.toList();

        if (elements.isEmpty()) {
            return Stream.empty();
        }

        final Function<T, Stream<T>> successor = element -> {
            final var requires = element.getClass().getAnnotation(Requires.class);

            return requires == null
                ? Stream.empty()
                : elements.stream().filter(
                    e -> Arrays.stream(requires.value()).anyMatch(requiredClass -> requiredClass.isInstance(e)));
        };

        return topologicalSort(elements.stream(), null, successor, independentElementOrder);
    }
}
