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

import java.util.Set;

/**
 * The common vertex-structure interface for all graph types in this library.
 * <p>
 * Exposes the vertex set, adjacency (successors and predecessors), and the
 * {@link GraphCollections} factory used for both graph storage and algorithm scratch
 * allocations.  All graph algorithms in the domain classes ({@link GraphTraversal},
 * {@link GraphOrdering}, {@link GraphCycles}, {@link GraphConnectivity},
 * {@link GraphReduction}) operate against this interface, making them applicable to
 * any conforming implementation.
 * <p>
 * Current implementations:
 * <ul>
 *   <li>{@link Graph} — simple directed or undirected graph</li>
 *   <li>{@link WeightedGraph} — directed or undirected graph with typed edge weights</li>
 * </ul>
 * Future implementations (e.g. multigraph) will also implement this interface, at which
 * point the existing algorithms will automatically apply to them without modification.
 * <p>
 * This interface does not expose edge objects directly, as the edge type varies across
 * implementations ({@link Edge}, {@link WeightedEdge}, and future multi-edge types).
 * Edge-type-specific operations are found on the concrete classes.
 *
 * @param <V> the vertex type
 * @author reed.von.redwitz
 * @see Graph
 * @see WeightedGraph
 * @since Apr-2026
 */
public interface VertexGraph<V> {

    /**
     * Returns all vertices in this graph.
     *
     * @return unmodifiable, insertion-ordered set of vertices
     */
    Set<V> vertices();

    /**
     * Returns the set of vertices that {@code vertex} has outgoing edges to (its successors).
     * Returns an empty set if {@code vertex} is not in the graph.
     *
     * @param vertex the vertex to query
     * @return unmodifiable set of successor vertices
     */
    Set<V> successors(V vertex);

    /**
     * Returns the set of vertices that have outgoing edges to {@code vertex} (its predecessors).
     * Returns an empty set if {@code vertex} is not in the graph.
     *
     * @param vertex the vertex to query
     * @return unmodifiable set of predecessor vertices
     */
    Set<V> predecessors(V vertex);

    /**
     * Returns {@code true} if this graph contains a directed edge from {@code from} to {@code to}.
     *
     * @param from the source vertex
     * @param to   the target vertex
     * @return {@code true} if the edge exists
     */
    boolean hasEdge(V from, V to);

    /**
     * Returns {@code true} if this graph contains the specified vertex.
     *
     * @param vertex the vertex to check
     * @return {@code true} if {@code vertex} is in this graph
     */
    boolean contains(V vertex);

    /**
     * Returns {@code true} if this graph is directed.
     *
     * @return {@code true} for directed graphs, {@code false} for undirected
     */
    boolean isDirected();

    /**
     * Returns the number of vertices in this graph.
     *
     * @return the vertex count
     */
    int vertexCount();

    /**
     * Returns the number of edges in this graph.
     *
     * @return the edge count
     */
    int edgeCount();

    /**
     * Returns {@code true} if this graph has no vertices.
     *
     * @return {@code true} if the graph is empty
     */
    boolean isEmpty();

    /**
     * Returns the {@link GraphCollections} factory associated with this graph.
     * <p>
     * Graph algorithms use this factory to allocate scratch collections (visited sets,
     * parent maps, etc.) that match the backend in use for this graph.
     *
     * @return the collections factory
     */
    GraphCollections<V> collections();
}
