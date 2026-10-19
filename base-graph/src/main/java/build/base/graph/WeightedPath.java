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
import java.util.Objects;
import java.util.function.BinaryOperator;

/**
 * An immutable weighted path through a {@link WeightedGraph}, represented as an ordered
 * sequence of vertices, the {@link WeightedEdge}s connecting them, and the total accumulated
 * {@code weight} of the path.
 *
 * @param <V>         the vertex type
 * @param <W>         the weight type
 * @param vertices    the ordered list of vertices along the path (at least one)
 * @param edges       the ordered list of weighted edges connecting consecutive vertices
 * @param totalWeight the total accumulated weight of all edges in the path
 * @author reed.von.redwitz
 * @since Apr-2026
 */
public record WeightedPath<V, W>(List<V> vertices, List<WeightedEdge<V, W>> edges, W totalWeight) {

    /**
     * Constructs a {@link WeightedPath}, defensive-copying both lists and validating consistency.
     *
     * @param vertices    the vertices along the path
     * @param edges       the weighted edges connecting consecutive vertices
     * @param totalWeight the total accumulated weight
     */
    public WeightedPath {
        Objects.requireNonNull(vertices, "The vertices list cannot be null");
        Objects.requireNonNull(edges, "The edges list cannot be null");
        Objects.requireNonNull(totalWeight, "The totalWeight cannot be null");

        if (vertices.isEmpty()) {
            throw new IllegalArgumentException("A path must contain at least one vertex");
        }

        if (edges.size() != vertices.size() - 1) {
            throw new IllegalArgumentException(
                "A path with %d vertices must have %d edge(s), but got %d"
                    .formatted(vertices.size(), vertices.size() - 1, edges.size()));
        }

        vertices = List.copyOf(vertices);
        edges = List.copyOf(edges);
    }

    /**
     * Creates a single-vertex {@link WeightedPath} with no edges and the supplied zero weight.
     *
     * @param <V>         the vertex type
     * @param <W>         the weight type
     * @param vertex      the single vertex
     * @param zeroWeight  the zero-value weight (e.g. {@code 0} for integers)
     * @return a {@link WeightedPath} containing only {@code vertex}
     */
    public static <V, W> WeightedPath<V, W> of(final V vertex, final W zeroWeight) {
        Objects.requireNonNull(vertex, "The vertex cannot be null");
        Objects.requireNonNull(zeroWeight, "The zeroWeight cannot be null");
        return new WeightedPath<>(List.of(vertex), List.of(), zeroWeight);
    }

    /**
     * Returns a new {@link WeightedPath} extended by one edge and vertex, accumulating weight
     * using the supplied {@code add} operator.
     *
     * @param next the next vertex to append
     * @param edge the weighted edge connecting the current end vertex to {@code next}
     * @param add  the binary operator used to accumulate weight
     * @return the extended path
     */
    public WeightedPath<V, W> extend(final V next, final WeightedEdge<V, W> edge, final BinaryOperator<W> add) {
        Objects.requireNonNull(next, "The next vertex cannot be null");
        Objects.requireNonNull(edge, "The edge cannot be null");
        Objects.requireNonNull(add, "The add operator cannot be null");

        final var newVertices = new java.util.ArrayList<>(vertices);
        newVertices.add(next);

        final var newEdges = new java.util.ArrayList<>(edges);
        newEdges.add(edge);

        return new WeightedPath<>(newVertices, newEdges, add.apply(totalWeight, edge.weight()));
    }

    /**
     * Returns the first vertex of this path.
     *
     * @return the start vertex
     */
    public V start() {
        return vertices.getFirst();
    }

    /**
     * Returns the last vertex of this path.
     *
     * @return the end vertex
     */
    public V end() {
        return vertices.getLast();
    }

    /**
     * Returns the number of edges in this path.  A single-vertex path has length {@code 0}.
     *
     * @return the path length (number of edges)
     */
    public int length() {
        return edges.size();
    }

    /**
     * Returns this path as an unweighted {@link Path}, discarding edge weights.
     *
     * @return the equivalent unweighted {@link Path}
     */
    public Path<V> toUnweightedPath() {
        final List<Edge<V>> unweightedEdges = edges.stream()
            .map(WeightedEdge::toEdge)
            .toList();
        return new Path<>(vertices, unweightedEdges);
    }
}
