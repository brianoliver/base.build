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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An immutable directed or undirected graph.
 * <p>
 * Graphs are constructed via {@link Builder} obtained from {@link #directed()} or
 * {@link #undirected()}.  Once built, the graph is fully immutable — vertices and edges
 * cannot be added or removed.
 * <p>
 * The underlying collection types used for vertex sets, adjacency maps, and edge caches are
 * configurable via {@link GraphCollections}.  The default uses standard JDK collections;
 * high-performance alternatives (e.g. fastutil) may be substituted via
 * {@link Builder#withCollections(GraphCollections)}.
 * <p>
 * For algorithms operating on graphs, see {@link Graphs}.
 *
 * @param <V> the vertex type
 * @author reed.von.redwitz
 * @see Graphs
 * @see GraphCollections
 * @since Apr-2026
 */
public final class Graph<V> implements VertexGraph<V> {

    /**
     * Whether this graph treats edges as directed.
     */
    private final boolean directed;

    /**
     * The collection factories used for this graph's storage and algorithm scratch.
     */
    private final GraphCollections<V> collections;

    /**
     * All vertices in this graph, in insertion order.
     */
    private final Set<V> vertices;

    /**
     * Outgoing adjacency: vertex → set of successor vertices.
     */
    private final Map<V, Set<V>> successors;

    /**
     * Incoming adjacency: vertex → set of predecessor vertices.
     */
    private final Map<V, Set<V>> predecessors;

    /**
     * The complete set of edges derived from the adjacency maps.
     */
    private final Set<Edge<V>> edges;

    /**
     * Constructs a {@link Graph} from the given builder state using the provided collections.
     *
     * @param directed     whether the graph is directed
     * @param collections  the collection factories for storage and algorithm scratch
     * @param vertices     mutable insertion-ordered vertex set (will be deep-copied)
     * @param successors   mutable outgoing adjacency map (will be deep-copied)
     * @param predecessors mutable incoming adjacency map (will be deep-copied)
     */
    private Graph(final boolean directed,
                  final GraphCollections<V> collections,
                  final Set<V> vertices,
                  final Map<V, Set<V>> successors,
                  final Map<V, Set<V>> predecessors) {
        this.directed = directed;
        this.collections = collections;

        final Set<V> vSet = collections.newVertexSet();
        vSet.addAll(vertices);
        this.vertices = Collections.unmodifiableSet(vSet);

        this.successors = deepUnmodifiable(successors, collections);
        this.predecessors = deepUnmodifiable(predecessors, collections);

        final Set<Edge<V>> edgeSet = collections.newEdgeSet();
        for (final var entry : successors.entrySet()) {
            final V from = entry.getKey();
            for (final V to : entry.getValue()) {
                if (directed || !edgeSet.contains(new Edge<>(to, from))) {
                    edgeSet.add(new Edge<>(from, to));
                }
            }
        }
        this.edges = Collections.unmodifiableSet(edgeSet);
    }

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /**
     * Returns a {@link Builder} for constructing a directed {@link Graph}.
     *
     * @param <V> the vertex type
     * @return a new directed {@link Builder}
     */
    public static <V> Builder<V> directed() {
        return new Builder<>(true);
    }

    /**
     * Returns a {@link Builder} for constructing an undirected {@link Graph}.
     * <p>
     * When an edge {@code (u, v)} is added to an undirected graph, the reverse edge
     * {@code (v, u)} is automatically implied.
     *
     * @param <V> the vertex type
     * @return a new undirected {@link Builder}
     */
    public static <V> Builder<V> undirected() {
        return new Builder<>(false);
    }

    // -------------------------------------------------------------------------
    // Query methods
    // -------------------------------------------------------------------------

    /**
     * Returns all vertices in this graph.
     *
     * @return unmodifiable, insertion-ordered set of vertices
     */
    @Override
    public Set<V> vertices() {
        return vertices;
    }

    /**
     * Returns all edges in this graph.
     * <p>
     * For undirected graphs, each undirected edge is represented as a single {@link Edge}
     * with the {@code from} vertex being the one added first.
     *
     * @return unmodifiable set of edges
     */
    public Set<Edge<V>> edges() {
        return edges;
    }

    /**
     * Returns the set of vertices that {@code vertex} has outgoing edges to (its successors).
     * Returns an empty set if {@code vertex} is not in the graph.
     *
     * @param vertex the vertex to query
     * @return unmodifiable set of successor vertices
     */
    @Override
    public Set<V> successors(final V vertex) {
        return successors.getOrDefault(vertex, Set.of());
    }

    /**
     * Returns the set of vertices that have outgoing edges to {@code vertex} (its predecessors).
     * Returns an empty set if {@code vertex} is not in the graph.
     *
     * @param vertex the vertex to query
     * @return unmodifiable set of predecessor vertices
     */
    @Override
    public Set<V> predecessors(final V vertex) {
        return predecessors.getOrDefault(vertex, Set.of());
    }

    /**
     * Returns {@code true} if this graph contains the specified vertex.
     *
     * @param vertex the vertex to check
     * @return {@code true} if {@code vertex} is in this graph
     */
    @Override
    public boolean contains(final V vertex) {
        return vertices.contains(vertex);
    }

    /**
     * Returns {@code true} if this graph contains a directed edge from {@code from} to {@code to}.
     * <p>
     * For undirected graphs, also returns {@code true} if the reverse edge exists.
     *
     * @param from the source vertex
     * @param to   the target vertex
     * @return {@code true} if the edge exists
     */
    @Override
    public boolean hasEdge(final V from, final V to) {
        return successors.getOrDefault(from, Set.of()).contains(to);
    }

    /**
     * Returns {@code true} if this graph is directed.
     *
     * @return {@code true} for directed graphs, {@code false} for undirected
     */
    @Override
    public boolean isDirected() {
        return directed;
    }

    /**
     * Returns the number of vertices in this graph.
     *
     * @return the vertex count
     */
    @Override
    public int vertexCount() {
        return vertices.size();
    }

    /**
     * Returns the number of edges in this graph.
     *
     * @return the edge count
     */
    @Override
    public int edgeCount() {
        return edges.size();
    }

    /**
     * Returns {@code true} if this graph has no vertices.
     *
     * @return {@code true} if the graph is empty
     */
    @Override
    public boolean isEmpty() {
        return vertices.isEmpty();
    }

    /**
     * Returns the {@link GraphCollections} factory associated with this graph.
     * <p>
     * Graph algorithms in {@link Graphs} use this factory to allocate scratch collections
     * (visited sets, parent maps, etc.) that match the backend in use for this graph.
     *
     * @return the collections factory
     */
    @Override
    public GraphCollections<V> collections() {
        return collections;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns a deeply unmodifiable copy of the given adjacency map, using the supplied
     * collections factory to allocate the backing sets.
     *
     * @param map  the mutable adjacency map
     * @param col  the collections factory
     * @param <V>  the vertex type
     * @return unmodifiable adjacency map with unmodifiable neighbour sets
     */
    private static <V> Map<V, Set<V>> deepUnmodifiable(
            final Map<V, Set<V>> map,
            final GraphCollections<V> col) {
        final Map<V, Set<V>> copy = col.newAdjacencyMap();
        for (final var entry : map.entrySet()) {
            final Set<V> neighbors = col.newNeighborSet();
            neighbors.addAll(entry.getValue());
            copy.put(entry.getKey(), Collections.unmodifiableSet(neighbors));
        }
        return Collections.unmodifiableMap(copy);
    }

    @Override
    public String toString() {
        return "Graph{directed=" + directed
            + ", vertices=" + vertexCount()
            + ", edges=" + edgeCount() + '}';
    }

    // =========================================================================
    // Builder
    // =========================================================================

    /**
     * A fluent builder for constructing immutable {@link Graph} instances.
     * <p>
     * The collection backend used by the resulting {@link Graph} may be configured via
     * {@link #withCollections(GraphCollections)} before any vertices or edges are added.
     *
     * @param <V> the vertex type
     */
    public static final class Builder<V> {

        /**
         * Whether the graph being built is directed.
         */
        private final boolean directed;

        /**
         * The collections factory to use for the built graph.
         */
        private GraphCollections<V> collections = GraphCollections.standard();

        /**
         * Vertices in insertion order (builder scratch — always standard LinkedHashSet).
         */
        private final Set<V> vertices = new LinkedHashSet<>();

        /**
         * Mutable outgoing adjacency map (builder scratch — always standard LinkedHashSet).
         */
        private final java.util.HashMap<V, Set<V>> successors = new java.util.HashMap<>();

        /**
         * Mutable incoming adjacency map (builder scratch — always standard LinkedHashSet).
         */
        private final java.util.HashMap<V, Set<V>> predecessors = new java.util.HashMap<>();

        /**
         * Constructs a {@link Builder} for a directed or undirected graph.
         *
         * @param directed {@code true} for directed
         */
        private Builder(final boolean directed) {
            this.directed = directed;
        }

        /**
         * Sets the {@link GraphCollections} factory that the built {@link Graph} will use for
         * its internal storage and for algorithm scratch allocations.
         * <p>
         * This method may be called at any point before {@link #build()}.
         *
         * @param collections the collections factory (must not be {@code null})
         * @return this builder
         */
        public Builder<V> withCollections(final GraphCollections<V> collections) {
            this.collections = Objects.requireNonNull(collections, "The collections factory cannot be null");
            return this;
        }

        /**
         * Adds the specified vertex to the graph being built.  If the vertex is already
         * present, this is a no-op.
         *
         * @param vertex the vertex to add
         * @return this builder
         */
        public Builder<V> addVertex(final V vertex) {
            Objects.requireNonNull(vertex, "The vertex cannot be null");
            vertices.add(vertex);
            successors.computeIfAbsent(vertex, _ -> new LinkedHashSet<>());
            predecessors.computeIfAbsent(vertex, _ -> new LinkedHashSet<>());
            return this;
        }

        /**
         * Adds a directed edge from {@code from} to {@code to}, implicitly adding both
         * vertices if they are not already present.
         * <p>
         * For undirected graphs, the reverse edge is also recorded.
         *
         * @param from the source vertex
         * @param to   the target vertex
         * @return this builder
         */
        public Builder<V> addEdge(final V from, final V to) {
            Objects.requireNonNull(from, "The from vertex cannot be null");
            Objects.requireNonNull(to, "The to vertex cannot be null");

            addVertex(from);
            addVertex(to);

            successors.get(from).add(to);
            predecessors.get(to).add(from);

            if (!directed) {
                successors.get(to).add(from);
                predecessors.get(from).add(to);
            }

            return this;
        }

        /**
         * Adds the given {@link Edge} to the graph being built.
         *
         * @param edge the edge to add
         * @return this builder
         */
        public Builder<V> addEdge(final Edge<V> edge) {
            Objects.requireNonNull(edge, "The edge cannot be null");
            return addEdge(edge.from(), edge.to());
        }

        /**
         * Builds and returns an immutable {@link Graph} from the current builder state.
         * <p>
         * The graph's internal storage is allocated using the configured {@link GraphCollections}
         * factory.
         *
         * @return a new immutable {@link Graph}
         */
        public Graph<V> build() {
            return new Graph<>(directed, collections, vertices, successors, predecessors);
        }
    }
}
