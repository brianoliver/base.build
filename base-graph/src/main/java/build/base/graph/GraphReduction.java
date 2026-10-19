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

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Graph reduction algorithms operating on {@link VertexGraph}.
 *
 * @author reed.von.redwitz
 * @since Apr-2026
 */
public final class GraphReduction {

    private GraphReduction() {
        // utility class
    }

    /**
     * Returns a transitively reduced version of the given directed graph — the graph with
     * the fewest edges that preserves all reachability relationships.
     * <p>
     * An edge {@code (u, v)} is removed if {@code v} is reachable from {@code u} via a path
     * of length ≥ 2 (i.e. the direct edge is redundant).
     * <p>
     * Time complexity: O(V · (V + E)) — reachability is precomputed once per vertex via BFS,
     * then each edge check is O(1).
     *
     * @param <V>   the vertex type
     * @param graph the directed graph to reduce
     * @return a new {@link Graph} with redundant edges removed
     * @throws IllegalArgumentException if the graph is undirected
     */
    public static <V> Graph<V> transitiveReduction(final VertexGraph<V> graph) {
        Objects.requireNonNull(graph, "The graph cannot be null");
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("Transitive reduction requires a directed graph");
        }

        final GraphCollections<V> col = graph.collections();

        // Precompute reachability from every vertex once: O(V · (V+E))
        final Map<V, Set<V>> reachable = col.newMap();
        for (final V v : graph.vertices()) {
            reachable.put(v, GraphTraversal.reachableFrom(graph, v));
        }

        final Graph.Builder<V> builder = Graph.<V>directed().withCollections(col);
        graph.vertices().forEach(builder::addVertex);

        for (final V u : graph.vertices()) {
            for (final V v : graph.successors(u)) {
                // Edge (u,v) is redundant if v is reachable from u via another successor
                boolean redundant = false;
                for (final V w : graph.successors(u)) {
                    if (!w.equals(v) && reachable.get(w).contains(v)) {
                        redundant = true;
                        break;
                    }
                }
                if (!redundant) {
                    builder.addEdge(u, v);
                }
            }
        }

        return builder.build();
    }
}
