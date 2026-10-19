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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Graph traversal and reachability algorithms operating on {@link VertexGraph}.
 * <p>
 * All methods are stateless and accept any {@link VertexGraph} implementation —
 * {@link Graph}, {@link WeightedGraph}, or future types such as multigraphs.
 *
 * @author reed.von.redwitz
 * @since Apr-2026
 */
public final class GraphTraversal {

    private GraphTraversal() {
        // utility class
    }

    // =========================================================================
    // Traversal
    // =========================================================================

    /**
     * Returns a breadth-first traversal of the graph starting from {@code start}.
     * <p>
     * Only vertices reachable from {@code start} are included.  If {@code start} is not in
     * the graph, a list containing only {@code start} is returned.
     * <p>
     * Time complexity: O(V + E).
     *
     * @param <V>   the vertex type
     * @param graph the graph to traverse
     * @param start the starting vertex
     * @return vertices in BFS order, beginning with {@code start}
     */
    public static <V> List<V> breadthFirstSearch(final VertexGraph<V> graph, final V start) {
        Objects.requireNonNull(graph, "The graph cannot be null");
        Objects.requireNonNull(start, "The start vertex cannot be null");

        final GraphCollections<V> col = graph.collections();
        final List<V> result = new ArrayList<>();
        final Set<V> visited = col.newSet();
        final ArrayDeque<V> queue = new ArrayDeque<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            final V current = queue.poll();
            result.add(current);

            for (final V successor : graph.successors(current)) {
                if (visited.add(successor)) {
                    queue.add(successor);
                }
            }
        }

        return List.copyOf(result);
    }

    /**
     * Returns a depth-first traversal of the graph starting from {@code start}.
     * <p>
     * Only vertices reachable from {@code start} are included.  The traversal uses an
     * iterative approach to avoid stack overflow on deep graphs.
     * <p>
     * Time complexity: O(V + E).
     *
     * @param <V>   the vertex type
     * @param graph the graph to traverse
     * @param start the starting vertex
     * @return vertices in DFS order (pre-order), beginning with {@code start}
     */
    public static <V> List<V> depthFirstSearch(final VertexGraph<V> graph, final V start) {
        Objects.requireNonNull(graph, "The graph cannot be null");
        Objects.requireNonNull(start, "The start vertex cannot be null");

        final GraphCollections<V> col = graph.collections();
        final List<V> result = new ArrayList<>();
        final Set<V> visited = col.newSet();
        final ArrayDeque<V> stack = new ArrayDeque<>();

        stack.push(start);

        while (!stack.isEmpty()) {
            final V current = stack.pop();

            if (visited.add(current)) {
                result.add(current);

                // Push in reverse to preserve natural iteration order
                final List<V> successors = new ArrayList<>(graph.successors(current));
                Collections.reverse(successors);
                for (final V successor : successors) {
                    if (!visited.contains(successor)) {
                        stack.push(successor);
                    }
                }
            }
        }

        return List.copyOf(result);
    }

    // =========================================================================
    // Reachability
    // =========================================================================

    /**
     * Returns all vertices reachable from {@code start} via outgoing edges, excluding
     * {@code start} itself (unless there is a cycle back to it).
     * <p>
     * Time complexity: O(V + E).
     *
     * @param <V>   the vertex type
     * @param graph the graph to query
     * @param start the starting vertex
     * @return unmodifiable set of reachable vertices (not including {@code start} unless cyclic)
     */
    public static <V> Set<V> reachableFrom(final VertexGraph<V> graph, final V start) {
        Objects.requireNonNull(graph, "The graph cannot be null");
        Objects.requireNonNull(start, "The start vertex cannot be null");

        final GraphCollections<V> col = graph.collections();
        final Set<V> reachable = col.newSet();
        final ArrayDeque<V> queue = new ArrayDeque<>();

        for (final V successor : graph.successors(start)) {
            if (reachable.add(successor)) {
                queue.add(successor);
            }
        }

        while (!queue.isEmpty()) {
            final V current = queue.poll();
            for (final V successor : graph.successors(current)) {
                if (reachable.add(successor)) {
                    queue.add(successor);
                }
            }
        }

        return Collections.unmodifiableSet(reachable);
    }

    /**
     * Returns all ancestors of {@code vertex} — vertices from which {@code vertex} is reachable
     * by following outgoing edges.  The vertex itself is not included unless there is a cycle.
     * <p>
     * Time complexity: O(V + E).
     *
     * @param <V>    the vertex type
     * @param graph  the graph to query
     * @param vertex the vertex whose ancestors to find
     * @return unmodifiable set of ancestor vertices
     */
    public static <V> Set<V> ancestors(final VertexGraph<V> graph, final V vertex) {
        Objects.requireNonNull(graph, "The graph cannot be null");
        Objects.requireNonNull(vertex, "The vertex cannot be null");

        final GraphCollections<V> col = graph.collections();
        final Set<V> result = col.newSet();
        final ArrayDeque<V> queue = new ArrayDeque<>();

        for (final V pred : graph.predecessors(vertex)) {
            if (result.add(pred)) {
                queue.add(pred);
            }
        }

        while (!queue.isEmpty()) {
            final V current = queue.poll();
            for (final V pred : graph.predecessors(current)) {
                if (result.add(pred)) {
                    queue.add(pred);
                }
            }
        }

        return Collections.unmodifiableSet(result);
    }

    /**
     * Returns all descendants of {@code vertex} — vertices reachable from {@code vertex}
     * by following outgoing edges.  The vertex itself is not included unless there is a cycle.
     * <p>
     * Time complexity: O(V + E).
     *
     * @param <V>    the vertex type
     * @param graph  the graph to query
     * @param vertex the vertex whose descendants to find
     * @return unmodifiable set of descendant vertices
     */
    public static <V> Set<V> descendants(final VertexGraph<V> graph, final V vertex) {
        Objects.requireNonNull(graph, "The graph cannot be null");
        Objects.requireNonNull(vertex, "The vertex cannot be null");
        return reachableFrom(graph, vertex);
    }
}
