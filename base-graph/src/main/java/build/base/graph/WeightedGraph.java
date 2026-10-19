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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * An immutable directed or undirected weighted graph.
 * <p>
 * Graphs are constructed via {@link Builder} obtained from {@link #directed()} or
 * {@link #undirected()}.  Once built, the graph is fully immutable.
 * <p>
 * If multiple edges are added between the same pair of vertices, the last weight wins.
 * <p>
 * The underlying collection types are configurable via {@link WeightedGraphCollections}.
 * The default uses standard JDK collections; high-performance alternatives may be substituted
 * via {@link Builder#withCollections(WeightedGraphCollections)}.
 * <p>
 * For algorithms operating on weighted graphs (e.g. Dijkstra's shortest path), see
 * {@link Graphs}.
 *
 * @param <V> the vertex type
 * @param <W> the weight type
 * @author reed.von.redwitz
 * @see Graphs
 * @see WeightedGraphCollections
 * @since Apr-2026
 */
public final class WeightedGraph<V, W> implements VertexGraph<V> {

    /**
     * Whether this graph treats edges as directed.
     */
    private final boolean directed;

    /**
     * The collection factories used for this graph's storage and algorithm scratch.
     */
    private final WeightedGraphCollections<V, W> collections;

    /**
     * All vertices in this graph, in insertion order.
     */
    private final Set<V> vertices;

    /**
     * Outgoing weighted adjacency: vertex → (successor → WeightedEdge).
     */
    private final Map<V, Map<V, WeightedEdge<V, W>>> outgoing;

    /**
     * All edges in this graph, in insertion order.
     */
    private final Set<WeightedEdge<V, W>> edges;

    /**
     * Incoming adjacency: vertex → set of predecessor vertices.
     */
    private final Map<V, Set<V>> incoming;

    /**
     * Constructs a {@link WeightedGraph} from builder state using the provided collections.
     *
     * @param directed    whether the graph is directed
     * @param collections the collection factories for storage and algorithm scratch
     * @param vertices    mutable insertion-ordered vertex set (will be deep-copied)
     * @param outgoing    mutable outgoing adjacency map (will be deep-copied)
     */
    private WeightedGraph(final boolean directed,
                          final WeightedGraphCollections<V, W> collections,
                          final Set<V> vertices,
                          final Map<V, Map<V, WeightedEdge<V, W>>> outgoing) {
        this.directed = directed;
        this.collections = collections;

        final Set<V> vSet = collections.newVertexSet();
        vSet.addAll(vertices);
        this.vertices = Collections.unmodifiableSet(vSet);

        final Map<V, Map<V, WeightedEdge<V, W>>> copy = collections.newOutgoingMap();
        for (final var entry : outgoing.entrySet()) {
            final Map<V, WeightedEdge<V, W>> edgeMap = collections.newEdgeMap();
            edgeMap.putAll(entry.getValue());
            copy.put(entry.getKey(), Collections.unmodifiableMap(edgeMap));
        }
        this.outgoing = Collections.unmodifiableMap(copy);

        final Set<WeightedEdge<V, W>> edgeSet = collections.newWeightedEdgeSet();
        for (final var adj : outgoing.values()) {
            edgeSet.addAll(adj.values());
        }
        this.edges = Collections.unmodifiableSet(edgeSet);

        // Build incoming (predecessors) map from outgoing
        final Map<V, Set<V>> incomingMut = collections.newAdjacencyMap();
        for (final V v : vertices) {
            incomingMut.put(v, collections.newNeighborSet());
        }
        for (final var entry : outgoing.entrySet()) {
            final V from = entry.getKey();
            for (final V to : entry.getValue().keySet()) {
                incomingMut.computeIfAbsent(to, _ -> collections.newNeighborSet()).add(from);
            }
        }
        final Map<V, Set<V>> incomingCopy = collections.newAdjacencyMap();
        for (final var e : incomingMut.entrySet()) {
            incomingCopy.put(e.getKey(), Collections.unmodifiableSet(e.getValue()));
        }
        this.incoming = Collections.unmodifiableMap(incomingCopy);
    }

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /**
     * Returns a {@link Builder} for constructing a directed {@link WeightedGraph}.
     *
     * @param <V> the vertex type
     * @param <W> the weight type
     * @return a new directed {@link Builder}
     */
    public static <V, W> Builder<V, W> directed() {
        return new Builder<>(true);
    }

    /**
     * Returns a {@link Builder} for constructing an undirected {@link WeightedGraph}.
     *
     * @param <V> the vertex type
     * @param <W> the weight type
     * @return a new undirected {@link Builder}
     */
    public static <V, W> Builder<V, W> undirected() {
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
     *
     * @return unmodifiable set of weighted edges
     */
    public Set<WeightedEdge<V, W>> edges() {
        return edges;
    }

    /**
     * Returns the set of successor vertices for the given vertex — vertices that {@code vertex}
     * has an outgoing edge to.  Returns an empty set if {@code vertex} is not in the graph.
     *
     * @param vertex the vertex to query
     * @return unmodifiable set of successor vertices
     */
    @Override
    public Set<V> successors(final V vertex) {
        final var adj = outgoing.get(vertex);
        return adj == null ? Set.of() : Collections.unmodifiableSet(adj.keySet());
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
        return incoming.getOrDefault(vertex, Set.of());
    }

    /**
     * Returns the {@link WeightedEdge} from {@code from} to {@code to}, if it exists.
     *
     * @param from the source vertex
     * @param to   the target vertex
     * @return an {@link Optional} containing the edge, or empty if no such edge exists
     */
    public Optional<WeightedEdge<V, W>> edge(final V from, final V to) {
        final var adj = outgoing.get(from);
        return adj == null ? Optional.empty() : Optional.ofNullable(adj.get(to));
    }

    /**
     * Returns the weight of the edge from {@code from} to {@code to}, if it exists.
     *
     * @param from the source vertex
     * @param to   the target vertex
     * @return an {@link Optional} containing the weight, or empty if no such edge exists
     */
    public Optional<W> weight(final V from, final V to) {
        return edge(from, to).map(WeightedEdge::weight);
    }

    /**
     * Returns {@code true} if this graph contains an edge from {@code from} to {@code to}.
     *
     * @param from the source vertex
     * @param to   the target vertex
     * @return {@code true} if the edge exists
     */
    @Override
    public boolean hasEdge(final V from, final V to) {
        final var adj = outgoing.get(from);
        return adj != null && adj.containsKey(to);
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
     * Returns {@code true} if this graph is directed.
     *
     * @return {@code true} for directed, {@code false} for undirected
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
     * Returns the {@link WeightedGraphCollections} factory associated with this graph.
     * <p>
     * Graph algorithms in {@link Graphs} use this factory to allocate scratch collections
     * that match the backend in use for this graph.
     *
     * @return the collections factory
     */
    @Override
    public WeightedGraphCollections<V, W> collections() {
        return collections;
    }

    /**
     * Returns a new unweighted {@link Graph} containing the same vertices and edge structure,
     * discarding all weights.
     *
     * @return the unweighted equivalent of this graph
     */
    public Graph<V> toUnweightedGraph() {
        final Graph.Builder<V> builder = directed ? Graph.directed() : Graph.undirected();
        vertices.forEach(builder::addVertex);
        edges.forEach(e -> builder.addEdge(e.from(), e.to()));
        return builder.build();
    }

    /**
     * Returns the internal outgoing adjacency map (package-private for use by {@link Graphs}).
     *
     * @return the outgoing adjacency map
     */
    Map<V, Map<V, WeightedEdge<V, W>>> outgoing() {
        return outgoing;
    }

    @Override
    public String toString() {
        return "WeightedGraph{directed=" + directed
            + ", vertices=" + vertexCount()
            + ", edges=" + edgeCount() + '}';
    }

    // =========================================================================
    // Builder
    // =========================================================================

    /**
     * A fluent builder for constructing immutable {@link WeightedGraph} instances.
     *
     * @param <V> the vertex type
     * @param <W> the weight type
     */
    public static final class Builder<V, W> {

        /**
         * Whether the graph being built is directed.
         */
        private final boolean directed;

        /**
         * The collections factory to use for the built graph.
         */
        private WeightedGraphCollections<V, W> collections = WeightedGraphCollections.standard();

        /**
         * Vertices in insertion order (builder scratch — always standard LinkedHashSet).
         */
        private final Set<V> vertices = new LinkedHashSet<>();

        /**
         * Mutable outgoing adjacency map (builder scratch — always standard LinkedHashMap).
         */
        private final Map<V, Map<V, WeightedEdge<V, W>>> outgoing = new LinkedHashMap<>();

        /**
         * Constructs a {@link Builder} for a directed or undirected weighted graph.
         *
         * @param directed {@code true} for directed
         */
        private Builder(final boolean directed) {
            this.directed = directed;
        }

        /**
         * Sets the {@link WeightedGraphCollections} factory that the built {@link WeightedGraph}
         * will use for its internal storage and for algorithm scratch allocations.
         *
         * @param collections the collections factory (must not be {@code null})
         * @return this builder
         */
        public Builder<V, W> withCollections(final WeightedGraphCollections<V, W> collections) {
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
        public Builder<V, W> addVertex(final V vertex) {
            Objects.requireNonNull(vertex, "The vertex cannot be null");
            vertices.add(vertex);
            outgoing.computeIfAbsent(vertex, _ -> new LinkedHashMap<>());
            return this;
        }

        /**
         * Adds a weighted edge from {@code from} to {@code to} with the specified weight,
         * implicitly adding both vertices if not already present.
         * <p>
         * If an edge between the same pair already exists, its weight is replaced.
         * For undirected graphs, the reverse edge is also recorded with the same weight.
         *
         * @param from   the source vertex
         * @param to     the target vertex
         * @param weight the edge weight
         * @return this builder
         */
        public Builder<V, W> addEdge(final V from, final V to, final W weight) {
            Objects.requireNonNull(from, "The from vertex cannot be null");
            Objects.requireNonNull(to, "The to vertex cannot be null");
            Objects.requireNonNull(weight, "The weight cannot be null");

            addVertex(from);
            addVertex(to);

            outgoing.get(from).put(to, new WeightedEdge<>(from, to, weight));

            if (!directed) {
                outgoing.get(to).put(from, new WeightedEdge<>(to, from, weight));
            }

            return this;
        }

        /**
         * Adds the given {@link WeightedEdge} to the graph being built.
         *
         * @param edge the weighted edge to add
         * @return this builder
         */
        public Builder<V, W> addEdge(final WeightedEdge<V, W> edge) {
            Objects.requireNonNull(edge, "The edge cannot be null");
            return addEdge(edge.from(), edge.to(), edge.weight());
        }

        /**
         * Builds and returns an immutable {@link WeightedGraph} from the current builder state.
         *
         * @return a new immutable {@link WeightedGraph}
         */
        public WeightedGraph<V, W> build() {
            return new WeightedGraph<>(directed, collections, vertices, outgoing);
        }
    }
}
