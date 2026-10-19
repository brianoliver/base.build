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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;

/**
 * Convenience facade over all graph algorithm classes.
 * <p>
 * Delegates to the domain-specific classes:
 * <ul>
 *   <li>Traversal and reachability — {@link GraphTraversal}</li>
 *   <li>Topological ordering — {@link GraphOrdering}</li>
 *   <li>Cycle detection — {@link GraphCycles}</li>
 *   <li>Shortest paths — {@link GraphPaths}</li>
 *   <li>Strongly connected components — {@link GraphConnectivity}</li>
 *   <li>Transitive reduction — {@link GraphReduction}</li>
 * </ul>
 * All methods accept any {@link VertexGraph} implementation.  Prefer calling the domain
 * classes directly when only a single algorithm family is needed.
 *
 * @author reed.von.redwitz
 * @since Apr-2026
 */
public final class Graphs {

    private Graphs() {
        // utility class
    }

    // =========================================================================
    // Traversal — delegates to GraphTraversal
    // =========================================================================

    /**
     * @see GraphTraversal#breadthFirstSearch(VertexGraph, Object)
     */
    public static <V> List<V> breadthFirstSearch(final VertexGraph<V> graph, final V start) {
        return GraphTraversal.breadthFirstSearch(graph, start);
    }

    /**
     * @see GraphTraversal#depthFirstSearch(VertexGraph, Object)
     */
    public static <V> List<V> depthFirstSearch(final VertexGraph<V> graph, final V start) {
        return GraphTraversal.depthFirstSearch(graph, start);
    }

    /**
     * @see GraphTraversal#reachableFrom(VertexGraph, Object)
     */
    public static <V> Set<V> reachableFrom(final VertexGraph<V> graph, final V start) {
        return GraphTraversal.reachableFrom(graph, start);
    }

    /**
     * @see GraphTraversal#ancestors(VertexGraph, Object)
     */
    public static <V> Set<V> ancestors(final VertexGraph<V> graph, final V vertex) {
        return GraphTraversal.ancestors(graph, vertex);
    }

    /**
     * @see GraphTraversal#descendants(VertexGraph, Object)
     */
    public static <V> Set<V> descendants(final VertexGraph<V> graph, final V vertex) {
        return GraphTraversal.descendants(graph, vertex);
    }

    // =========================================================================
    // Ordering — delegates to GraphOrdering
    // =========================================================================

    /**
     * @see GraphOrdering#topologicalSort(VertexGraph)
     */
    public static <V> List<V> topologicalSort(final VertexGraph<V> graph) {
        return GraphOrdering.topologicalSort(graph);
    }

    /**
     * @see GraphOrdering#parallelizableGroups(VertexGraph)
     */
    public static <V> List<Set<V>> parallelizableGroups(final VertexGraph<V> graph) {
        return GraphOrdering.parallelizableGroups(graph);
    }

    // =========================================================================
    // Cycles — delegates to GraphCycles
    // =========================================================================

    /**
     * @see GraphCycles#hasCycle(VertexGraph)
     */
    public static <V> boolean hasCycle(final VertexGraph<V> graph) {
        return GraphCycles.hasCycle(graph);
    }

    /**
     * @see GraphCycles#findCycle(VertexGraph)
     */
    public static <V> Optional<List<V>> findCycle(final VertexGraph<V> graph) {
        return GraphCycles.findCycle(graph);
    }

    // =========================================================================
    // Shortest paths — delegates to GraphPaths
    // =========================================================================

    /**
     * @see GraphPaths#shortestPath(VertexGraph, Object, Object)
     */
    public static <V> Optional<Path<V>> shortestPath(final VertexGraph<V> graph,
                                                      final V start,
                                                      final V end) {
        return GraphPaths.shortestPath(graph, start, end);
    }

    /**
     * @see GraphPaths#shortestPath(WeightedGraph, Object, Object, Comparable, BinaryOperator)
     */
    public static <V, W extends Comparable<W>> Optional<WeightedPath<V, W>> shortestPath(final WeightedGraph<V, W> graph,
                                                                                          final V start,
                                                                                          final V end,
                                                                                          final W zero,
                                                                                          final BinaryOperator<W> add) {
        return GraphPaths.shortestPath(graph, start, end, zero, add);
    }

    // =========================================================================
    // Connectivity — delegates to GraphConnectivity
    // =========================================================================

    /**
     * @see GraphConnectivity#stronglyConnectedComponents(VertexGraph)
     */
    public static <V> List<Component<V>> stronglyConnectedComponents(final VertexGraph<V> graph) {
        return GraphConnectivity.stronglyConnectedComponents(graph);
    }

    // =========================================================================
    // Reduction — delegates to GraphReduction
    // =========================================================================

    /**
     * @see GraphReduction#transitiveReduction(VertexGraph)
     */
    public static <V> Graph<V> transitiveReduction(final VertexGraph<V> graph) {
        return GraphReduction.transitiveReduction(graph);
    }
}
