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
import java.util.List;
import java.util.Objects;

/**
 * An immutable path through a {@link Graph}, represented as an ordered sequence of vertices
 * and the {@link Edge}s connecting them.
 * <p>
 * For a path of {@code n} vertices there are {@code n - 1} edges.  A path consisting of a
 * single vertex has no edges.
 *
 * @param <V>      the vertex type
 * @param vertices the ordered list of vertices along the path (at least one)
 * @param edges    the ordered list of edges connecting consecutive vertices
 * @author reed.von.redwitz
 * @since Apr-2026
 */
public record Path<V>(List<V> vertices, List<Edge<V>> edges) {

    /**
     * Constructs a {@link Path}, defensive-copying both lists and validating consistency.
     *
     * @param vertices the vertices along the path
     * @param edges    the edges connecting consecutive vertices
     */
    public Path {
        Objects.requireNonNull(vertices, "The vertices list cannot be null");
        Objects.requireNonNull(edges, "The edges list cannot be null");

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
     * Creates a {@link Path} from an ordered sequence of vertices, deriving edges automatically.
     *
     * @param <V>      the vertex type
     * @param vertices the vertices along the path (must contain at least one vertex)
     * @return a {@link Path} for the given vertices
     */
    public static <V> Path<V> of(final List<V> vertices) {
        Objects.requireNonNull(vertices, "The vertices list cannot be null");

        if (vertices.isEmpty()) {
            throw new IllegalArgumentException("A path must contain at least one vertex");
        }

        final List<Edge<V>> edges = new ArrayList<>(vertices.size() - 1);

        for (int i = 0; i < vertices.size() - 1; i++) {
            edges.add(new Edge<>(vertices.get(i), vertices.get(i + 1)));
        }

        return new Path<>(vertices, edges);
    }

    /**
     * Creates a single-vertex {@link Path} with no edges.
     *
     * @param <V>    the vertex type
     * @param vertex the single vertex
     * @return a {@link Path} containing only {@code vertex}
     */
    public static <V> Path<V> of(final V vertex) {
        Objects.requireNonNull(vertex, "The vertex cannot be null");
        return new Path<>(List.of(vertex), List.of());
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
}
