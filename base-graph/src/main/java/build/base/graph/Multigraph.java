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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An immutable directed or undirected multigraph — a graph that permits multiple parallel
 * edges between the same pair of vertices.
 * <p>
 * Graphs are constructed via {@link Builder} obtained from {@link #directed()} or
 * {@link #undirected()}.  Once built, the graph is fully immutable.
 * <p>
 * Unlike {@link Graph}, which uses the {@code (from, to)} vertex pair as edge identity,
 * {@link Multigraph} uses {@link MultiEdge} instances with reference identity.  Each call
 * to {@link Builder#addEdge(Object, Object)} creates a new, distinct edge even if an edge
 * between the same vertices already exists.  The builder returns the created {@link MultiEdge}
 * so callers can retain references to specific edges.
 * <p>
 * Because {@link Multigraph} implements {@link VertexGraph}, all graph algorithms in
 * {@link GraphTraversal}, {@link GraphOrdering}, {@link GraphCycles}, {@link GraphConnectivity},
 * and {@link GraphReduction} apply directly without conversion.
 *
 * @param <V> the vertex type
 * @author reed.von.redwitz
 * @see Graph
 * @see MultiEdge
 * @see VertexGraph
 * @since Apr-2026
 */
public final class Multigraph<V> implements VertexGraph<V> {

    private final boolean directed;
    private final GraphCollections<V> collections;
    private final Set<V> vertices;

    /**
     * Outgoing adjacency: vertex → list of outgoing edges (in insertion order).
     * Lists may contain multiple edges to the same target vertex.
     */
    private final Map<V, List<MultiEdge<V>>> outgoing;

    /**
     * Incoming adjacency: vertex → list of incoming edges (in insertion order).
     */
    private final Map<V, List<MultiEdge<V>>> incoming;

    /**
     * All edges in insertion order.
     */
    private final List<MultiEdge<V>> edges;

    private Multigraph(final boolean directed,
                       final GraphCollections<V> collections,
                       final Set<V> vertices,
                       final Map<V, List<MultiEdge<V>>> outgoing,
                       final Map<V, List<MultiEdge<V>>> incoming,
                       final List<MultiEdge<V>> edges) {
        this.directed = directed;
        this.collections = collections;

        final Set<V> vSet = collections.newVertexSet();
        vSet.addAll(vertices);
        this.vertices = Collections.unmodifiableSet(vSet);

        final Map<V, List<MultiEdge<V>>> outCopy = new LinkedHashMap<>();
        for (final var entry : outgoing.entrySet()) {
            outCopy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        this.outgoing = Collections.unmodifiableMap(outCopy);

        final Map<V, List<MultiEdge<V>>> inCopy = new LinkedHashMap<>();
        for (final var entry : incoming.entrySet()) {
            inCopy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        this.incoming = Collections.unmodifiableMap(inCopy);

        this.edges = List.copyOf(edges);
    }

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /**
     * Returns a {@link Builder} for constructing a directed {@link Multigraph}.
     *
     * @param <V> the vertex type
     * @return a new directed {@link Builder}
     */
    public static <V> Builder<V> directed() {
        return new Builder<>(true);
    }

    /**
     * Returns a {@link Builder} for constructing an undirected {@link Multigraph}.
     *
     * @param <V> the vertex type
     * @return a new undirected {@link Builder}
     */
    public static <V> Builder<V> undirected() {
        return new Builder<>(false);
    }

    // -------------------------------------------------------------------------
    // VertexGraph implementation
    // -------------------------------------------------------------------------

    @Override
    public Set<V> vertices() {
        return vertices;
    }

    @Override
    public Set<V> successors(final V vertex) {
        final List<MultiEdge<V>> adj = outgoing.get(vertex);
        if (adj == null || adj.isEmpty()) {
            return Set.of();
        }
        final Set<V> result = new LinkedHashSet<>();
        for (final MultiEdge<V> edge : adj) {
            result.add(edge.to());
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public Set<V> predecessors(final V vertex) {
        final List<MultiEdge<V>> adj = incoming.get(vertex);
        if (adj == null || adj.isEmpty()) {
            return Set.of();
        }
        final Set<V> result = new LinkedHashSet<>();
        for (final MultiEdge<V> edge : adj) {
            result.add(edge.from());
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public boolean hasEdge(final V from, final V to) {
        final List<MultiEdge<V>> adj = outgoing.get(from);
        if (adj == null) {
            return false;
        }
        for (final MultiEdge<V> edge : adj) {
            if (edge.to().equals(to)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(final V vertex) {
        return vertices.contains(vertex);
    }

    @Override
    public boolean isDirected() {
        return directed;
    }

    @Override
    public int vertexCount() {
        return vertices.size();
    }

    @Override
    public int edgeCount() {
        return edges.size();
    }

    @Override
    public boolean isEmpty() {
        return vertices.isEmpty();
    }

    @Override
    public GraphCollections<V> collections() {
        return collections;
    }

    // -------------------------------------------------------------------------
    // Multigraph-specific query methods
    // -------------------------------------------------------------------------

    /**
     * Returns all edges in this graph, in insertion order.
     * <p>
     * For undirected graphs, each logical undirected edge appears once.
     *
     * @return unmodifiable list of all edges
     */
    public List<MultiEdge<V>> edges() {
        return edges;
    }

    /**
     * Returns all edges from {@code from} to {@code to}, in insertion order.
     * Returns an empty list if no such edges exist.
     *
     * @param from the source vertex
     * @param to   the target vertex
     * @return unmodifiable list of parallel edges from {@code from} to {@code to}
     */
    public List<MultiEdge<V>> edgesConnecting(final V from, final V to) {
        final List<MultiEdge<V>> adj = outgoing.get(from);
        if (adj == null) {
            return List.of();
        }
        final List<MultiEdge<V>> result = new ArrayList<>();
        for (final MultiEdge<V> edge : adj) {
            if (edge.to().equals(to)) {
                result.add(edge);
            }
        }
        return List.copyOf(result);
    }

    /**
     * Returns the number of edges from {@code from} to {@code to} (the multiplicity).
     *
     * @param from the source vertex
     * @param to   the target vertex
     * @return the number of parallel edges between the two vertices
     */
    public int multiplicity(final V from, final V to) {
        return edgesConnecting(from, to).size();
    }

    /**
     * Returns a simple {@link Graph} derived from this multigraph by collapsing all
     * parallel edges between each vertex pair into a single edge.
     *
     * @return the simple graph equivalent of this multigraph
     */
    public Graph<V> toSimpleGraph() {
        final Graph.Builder<V> builder = directed ? Graph.directed() : Graph.undirected();
        builder.withCollections(collections);
        vertices.forEach(builder::addVertex);
        for (final MultiEdge<V> edge : edges) {
            builder.addEdge(edge.from(), edge.to());
        }
        return builder.build();
    }

    @Override
    public String toString() {
        return "Multigraph{directed=" + directed
            + ", vertices=" + vertexCount()
            + ", edges=" + edgeCount() + '}';
    }

    // =========================================================================
    // Builder
    // =========================================================================

    /**
     * A fluent builder for constructing immutable {@link Multigraph} instances.
     * <p>
     * Unlike {@link Graph.Builder}, {@link #addEdge(Object, Object)} returns the created
     * {@link MultiEdge} rather than the builder, since callers typically need to retain
     * references to specific parallel edges.  Use {@link #addVertex(Object)} for fluent
     * vertex-only chains.
     *
     * @param <V> the vertex type
     */
    public static final class Builder<V> {

        private final boolean directed;
        private GraphCollections<V> collections = GraphCollections.standard();
        private final Set<V> vertices = new LinkedHashSet<>();
        private final Map<V, List<MultiEdge<V>>> outgoing = new LinkedHashMap<>();
        private final Map<V, List<MultiEdge<V>>> incoming = new LinkedHashMap<>();
        private final List<MultiEdge<V>> edges = new ArrayList<>();

        private Builder(final boolean directed) {
            this.directed = directed;
        }

        /**
         * Sets the {@link GraphCollections} factory for the built graph.
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
            outgoing.computeIfAbsent(vertex, _ -> new ArrayList<>());
            incoming.computeIfAbsent(vertex, _ -> new ArrayList<>());
            return this;
        }

        /**
         * Adds a new directed edge from {@code from} to {@code to}, implicitly adding
         * both vertices if they are not already present.
         * <p>
         * Each call creates a distinct {@link MultiEdge} even if an edge between the same
         * pair already exists.  The created edge is returned so callers can retain a
         * reference to it.
         *
         * @param from the source vertex
         * @param to   the target vertex
         * @return the newly created {@link MultiEdge}
         */
        public MultiEdge<V> addEdge(final V from, final V to) {
            Objects.requireNonNull(from, "The from vertex cannot be null");
            Objects.requireNonNull(to, "The to vertex cannot be null");

            addVertex(from);
            addVertex(to);

            final MultiEdge<V> edge = new MultiEdge<>(from, to);
            outgoing.get(from).add(edge);
            incoming.get(to).add(edge);
            edges.add(edge);

            if (!directed) {
                final MultiEdge<V> reverse = new MultiEdge<>(to, from);
                outgoing.get(to).add(reverse);
                incoming.get(from).add(reverse);
                // reverse not added to edges list — logically the same undirected edge
            }

            return edge;
        }

        /**
         * Builds and returns an immutable {@link Multigraph} from the current builder state.
         *
         * @return a new immutable {@link Multigraph}
         */
        public Multigraph<V> build() {
            return new Multigraph<>(directed, collections, vertices, outgoing, incoming, edges);
        }
    }
}
