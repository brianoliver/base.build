package build.base.graph;

/*-
 * #%L
 * base.build Graph
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BinaryOperator;

/**
 * Shortest-path algorithms operating on {@link VertexGraph} and {@link WeightedGraph}.
 *
 * @author reed.von.redwitz
 * @since Apr-2026
 */
public final class GraphPaths {

    private GraphPaths() {
        // utility class
    }

    /**
     * Finds the shortest path (by number of hops) between {@code start} and {@code end}
     * in an unweighted graph using BFS.
     * <p>
     * Time complexity: O(V + E).
     *
     * @param <V>   the vertex type
     * @param graph the graph to search
     * @param start the starting vertex
     * @param end   the target vertex
     * @return an {@link Optional} containing the shortest {@link Path}, or empty if unreachable
     */
    public static <V> Optional<Path<V>> shortestPath(final VertexGraph<V> graph,
                                                      final V start,
                                                      final V end) {
        Objects.requireNonNull(graph, "The graph cannot be null");
        Objects.requireNonNull(start, "The start vertex cannot be null");
        Objects.requireNonNull(end, "The end vertex cannot be null");

        if (start.equals(end)) {
            return Optional.of(Path.of(start));
        }

        final GraphCollections<V> col = graph.collections();
        final Map<V, V> parent = col.newMap();
        final ArrayDeque<V> queue = new ArrayDeque<>();

        parent.put(start, null);
        queue.add(start);

        while (!queue.isEmpty()) {
            final V current = queue.poll();

            for (final V successor : graph.successors(current)) {
                if (!parent.containsKey(successor)) {
                    parent.put(successor, current);

                    if (successor.equals(end)) {
                        return Optional.of(buildPath(parent, start, end));
                    }

                    queue.add(successor);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Finds the shortest (minimum weight) path between {@code start} and {@code end} in a
     * weighted graph using Dijkstra's algorithm.
     * <p>
     * The weight type {@code W} must be {@link Comparable}.  A {@code zero} value (the additive
     * identity) and a {@code add} operator are required to accumulate path weights.
     * <p>
     * Time complexity: O(E log E) — uses lazy deletion to avoid O(V) priority-queue removes.
     * <p>
     * Example for integer weights:
     * <pre>{@code
     * GraphPaths.shortestPath(graph, "A", "D", 0, Integer::sum)
     * }</pre>
     *
     * @param <V>   the vertex type
     * @param <W>   the weight type (must be {@link Comparable})
     * @param graph the weighted graph to search
     * @param start the starting vertex
     * @param end   the target vertex
     * @param zero  the zero (additive identity) value for the weight type
     * @param add   the binary operator used to accumulate edge weights
     * @return an {@link Optional} containing the shortest {@link WeightedPath}, or empty if unreachable
     */
    public static <V, W extends Comparable<W>> Optional<WeightedPath<V, W>> shortestPath(final WeightedGraph<V, W> graph,
                                                                                          final V start,
                                                                                          final V end,
                                                                                          final W zero,
                                                                                          final BinaryOperator<W> add) {
        Objects.requireNonNull(graph, "The graph cannot be null");
        Objects.requireNonNull(start, "The start vertex cannot be null");
        Objects.requireNonNull(end, "The end vertex cannot be null");
        Objects.requireNonNull(zero, "The zero value cannot be null");
        Objects.requireNonNull(add, "The add operator cannot be null");

        if (start.equals(end)) {
            return Optional.of(WeightedPath.of(start, zero));
        }

        final GraphCollections<V> col = graph.collections();
        final Map<V, W> dist = col.newMap();
        final Map<V, WeightedPath<V, W>> paths = col.newMap();
        final Set<V> settled = col.newSet();
        final PriorityQueue<V> pq = new PriorityQueue<>((a, b) -> dist.get(a).compareTo(dist.get(b)));

        dist.put(start, zero);
        paths.put(start, WeightedPath.of(start, zero));
        pq.add(start);

        while (!pq.isEmpty()) {
            final V current = pq.poll();

            // Lazy deletion: skip stale entries (a shorter path was already settled)
            if (!settled.add(current)) {
                continue;
            }

            if (current.equals(end)) {
                return Optional.of(paths.get(current));
            }

            final W currentDist = dist.get(current);

            for (final V successor : graph.successors(current)) {
                if (settled.contains(successor)) {
                    continue;
                }
                final WeightedEdge<V, W> edge = graph.edge(current, successor).orElseThrow();
                final W newDist = add.apply(currentDist, edge.weight());
                final W existing = dist.get(successor);

                if (existing == null || newDist.compareTo(existing) < 0) {
                    dist.put(successor, newDist);
                    paths.put(successor, paths.get(current).extend(successor, edge, add));
                    pq.add(successor); // old entry remains; will be skipped when polled
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Reconstructs a {@link Path} from a BFS parent map.
     *
     * @param parent the parent map from BFS ({@code null} value for {@code start})
     * @param start  the start vertex
     * @param end    the end vertex
     * @param <V>    the vertex type
     * @return the reconstructed path
     */
    private static <V> Path<V> buildPath(final Map<V, V> parent, final V start, final V end) {
        final ArrayDeque<V> pathStack = new ArrayDeque<>();
        V current = end;

        while (current != null) {
            pathStack.addFirst(current);
            current = parent.get(current);
        }

        return Path.of(new ArrayList<>(pathStack));
    }
}
