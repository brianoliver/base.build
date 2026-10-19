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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Cycle detection algorithms operating on {@link VertexGraph}.
 *
 * @author reed.von.redwitz
 * @since Apr-2026
 */
public final class GraphCycles {

    private GraphCycles() {
        // utility class
    }

    /**
     * Returns {@code true} if the graph contains at least one directed cycle.
     * <p>
     * Time complexity: O(V + E).
     *
     * @param <V>   the vertex type
     * @param graph the graph to check
     * @return {@code true} if a cycle exists
     */
    public static <V> boolean hasCycle(final VertexGraph<V> graph) {
        Objects.requireNonNull(graph, "The graph cannot be null");
        return findCycle(graph).isPresent();
    }

    /**
     * Attempts to find a directed cycle in the graph and returns it as an ordered list
     * of vertices forming the cycle.
     * <p>
     * If a cycle exists, the returned list begins and ends with the same vertex
     * (e.g. {@code [A, B, C, A]}).  If no cycle exists, {@link Optional#empty()} is returned.
     * <p>
     * Uses iterative DFS with tri-colour marking (white/gray/black).
     * <p>
     * Time complexity: O(V + E).
     *
     * @param <V>   the vertex type
     * @param graph the graph to search
     * @return an {@link Optional} containing a cycle path, or empty if the graph is acyclic
     */
    public static <V> Optional<List<V>> findCycle(final VertexGraph<V> graph) {
        Objects.requireNonNull(graph, "The graph cannot be null");

        final GraphCollections<V> col = graph.collections();

        // 0 = unvisited, 1 = in-progress (gray), 2 = done (black)
        final Map<V, Integer> color = col.newIntMap();
        final Map<V, V> parent = col.newMap();

        for (final V vertex : graph.vertices()) {
            color.put(vertex, 0);
        }

        for (final V start : graph.vertices()) {
            if (color.get(start) != 0) {
                continue;
            }

            final ArrayDeque<V> dfsStack = new ArrayDeque<>();
            final Map<V, java.util.Iterator<V>> iteratorState = col.newMap();

            color.put(start, 1);
            dfsStack.push(start);
            iteratorState.put(start, graph.successors(start).iterator());

            while (!dfsStack.isEmpty()) {
                final V current = dfsStack.peek();
                final java.util.Iterator<V> iter = iteratorState.get(current);

                if (iter.hasNext()) {
                    final V next = iter.next();
                    final int nextColor = color.getOrDefault(next, 0);

                    if (nextColor == 1) {
                        // Back edge found — reconstruct cycle
                        return Optional.of(reconstructCycle(parent, next, current));
                    } else if (nextColor == 0) {
                        color.put(next, 1);
                        parent.put(next, current);
                        dfsStack.push(next);
                        iteratorState.put(next, graph.successors(next).iterator());
                    }
                    // nextColor == 2: already fully explored, skip
                } else {
                    color.put(current, 2);
                    dfsStack.pop();
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Reconstructs the cycle path from parent tracking.
     *
     * @param parent   the parent map from DFS
     * @param cycleEnd the vertex where the back edge lands (start of cycle)
     * @param current  the vertex with the back edge (end of cycle before wrap)
     * @param <V>      the vertex type
     * @return the cycle as a list from {@code cycleEnd} back to {@code cycleEnd}
     */
    private static <V> List<V> reconstructCycle(final Map<V, V> parent,
                                                 final V cycleEnd,
                                                 final V current) {
        final ArrayDeque<V> cycle = new ArrayDeque<>();
        cycle.addFirst(cycleEnd);

        V node = current;
        while (!node.equals(cycleEnd)) {
            cycle.addFirst(node);
            node = parent.get(node);
            if (node == null) {
                break;
            }
        }

        cycle.addFirst(cycleEnd);
        return List.copyOf(cycle);
    }
}
