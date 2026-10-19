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
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Topological ordering algorithms operating on {@link VertexGraph}.
 * <p>
 * All methods require the input graph to be a DAG (directed acyclic graph).
 *
 * @author reed.von.redwitz
 * @since Apr-2026
 */
public final class GraphOrdering {

    private GraphOrdering() {
        // utility class
    }

    /**
     * Returns a topological ordering of all vertices in the graph using Kahn's algorithm.
     * <p>
     * In a topological order, for every directed edge {@code (u, v)}, vertex {@code u}
     * appears before vertex {@code v}.  This is the natural build order for a dependency graph.
     * <p>
     * Time complexity: O(V + E).
     *
     * @param <V>   the vertex type
     * @param graph the directed graph to sort (must be a DAG)
     * @return vertices in topological order
     * @throws IllegalArgumentException if the graph is undirected, or contains a cycle
     */
    public static <V> List<V> topologicalSort(final VertexGraph<V> graph) {
        Objects.requireNonNull(graph, "The graph cannot be null");
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("Topological sort requires a directed graph");
        }

        // Edge convention: A → B means "A depends on B". Nodes with no outgoing edges
        // (no dependencies) are processed first — this is the natural build order.
        final GraphCollections<V> col = graph.collections();
        final Map<V, Integer> outDegree = col.newIntMap();

        for (final V vertex : graph.vertices()) {
            outDegree.put(vertex, graph.successors(vertex).size());
        }

        final ArrayDeque<V> queue = new ArrayDeque<>();
        for (final V vertex : graph.vertices()) {
            if (outDegree.get(vertex) == 0) {
                queue.add(vertex);
            }
        }

        final List<V> result = new ArrayList<>(graph.vertexCount());

        while (!queue.isEmpty()) {
            final V current = queue.poll();
            result.add(current);

            for (final V predecessor : graph.predecessors(current)) {
                final int remaining = outDegree.merge(predecessor, -1, Integer::sum);
                if (remaining == 0) {
                    queue.add(predecessor);
                }
            }
        }

        if (result.size() != graph.vertexCount()) {
            throw new IllegalArgumentException(
                "The graph contains a cycle and cannot be topologically sorted");
        }

        return List.copyOf(result);
    }

    /**
     * Returns the vertices of a DAG grouped into parallelizable layers.
     * <p>
     * Layer 0 contains vertices with no predecessors.  Layer {@code k} contains vertices
     * whose predecessors all appear in earlier layers.  All vertices within the same layer
     * are independent of each other and may be processed concurrently.
     * <p>
     * Time complexity: O(V + E).
     *
     * @param <V>   the vertex type
     * @param graph the directed graph to partition (must be a DAG)
     * @return ordered list of layers; each layer is an unmodifiable set of vertices
     * @throws IllegalArgumentException if the graph is undirected, or contains a cycle
     */
    public static <V> List<Set<V>> parallelizableGroups(final VertexGraph<V> graph) {
        Objects.requireNonNull(graph, "The graph cannot be null");
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("Parallelizable groups requires a directed graph");
        }

        // Edge convention: A → B means "A depends on B". Layer 0 = nodes with no outgoing
        // edges (no dependencies). Layer k = nodes all of whose dependencies are in layers 0..k-1.
        final GraphCollections<V> col = graph.collections();
        final Map<V, Integer> layer = col.newIntMap();
        final Map<V, Integer> remaining = col.newIntMap();  // unprocessed successor count

        for (final V vertex : graph.vertices()) {
            remaining.put(vertex, graph.successors(vertex).size());
        }

        final ArrayDeque<V> queue = new ArrayDeque<>();
        for (final V vertex : graph.vertices()) {
            if (remaining.get(vertex) == 0) {
                queue.add(vertex);
                layer.put(vertex, 0);
            }
        }

        int processedCount = 0;

        while (!queue.isEmpty()) {
            final V current = queue.poll();
            processedCount++;
            final int currentLayer = layer.get(current);

            for (final V predecessor : graph.predecessors(current)) {
                // predecessor can be placed in at least currentLayer + 1
                layer.merge(predecessor, currentLayer + 1, Math::max);

                final int rem = remaining.merge(predecessor, -1, Integer::sum);
                if (rem == 0) {
                    queue.add(predecessor);
                }
            }
        }

        if (processedCount != graph.vertexCount()) {
            throw new IllegalArgumentException(
                "The graph contains a cycle and cannot be partitioned into parallelizable groups");
        }

        if (layer.isEmpty()) {
            return List.of();
        }

        final int maxLayer = layer.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        final List<Set<V>> groups = new ArrayList<>(maxLayer + 1);

        for (int i = 0; i <= maxLayer; i++) {
            groups.add(col.newSet());
        }

        for (final Map.Entry<V, Integer> entry : layer.entrySet()) {
            groups.get(entry.getValue()).add(entry.getKey());
        }

        return groups.stream()
            .map(Collections::unmodifiableSet)
            .toList();
    }
}
