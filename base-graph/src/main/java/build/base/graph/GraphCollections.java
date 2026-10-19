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
import java.util.Set;

/**
 * A factory for the collection types used by {@link Graph} and the graph algorithms in
 * {@link Graphs}.
 * <p>
 * The default implementation, obtained via {@link #standard()}, uses standard JDK collections
 * ({@code LinkedHashSet}, {@code HashMap}, etc.).  Alternative implementations may substitute
 * higher-performance backends such as
 * <a href="https://fastutil.di.unimi.it/">fastutil</a> open-addressing maps and sets.
 * <p>
 * A custom implementation is supplied to a {@link Graph.Builder} via
 * {@link Graph.Builder#withCollections(GraphCollections)}:
 * <pre>{@code
 * Graph.<Integer>directed()
 *     .withCollections(new MyFastutilGraphCollections())
 *     .addEdge(1, 2)
 *     .build();
 * }</pre>
 * <p>
 * The interface covers both <em>graph storage</em> (the vertex set, adjacency maps, and edge
 * cache held inside a {@link Graph}) and <em>algorithm scratch allocations</em> (the visited
 * sets, parent maps, distance maps, and index maps used internally by {@link Graphs}).
 *
 * @param <V> the vertex type
 * @author reed.von.redwitz
 * @see WeightedGraphCollections
 * @since Apr-2026
 */
public interface GraphCollections<V> {

    // -------------------------------------------------------------------------
    // Graph storage factories
    // -------------------------------------------------------------------------

    /**
     * Returns a new empty set for storing graph vertices, preserving insertion order.
     *
     * @return a new empty vertex set
     */
    Set<V> newVertexSet();

    /**
     * Returns a new empty map for storing adjacency information (vertex → neighbour set).
     * <p>
     * Used for both the successor and predecessor maps inside {@link Graph}.
     *
     * @return a new empty adjacency map
     */
    Map<V, Set<V>> newAdjacencyMap();

    /**
     * Returns a new empty set for storing the neighbours of a single vertex.
     *
     * @return a new empty neighbour set
     */
    Set<V> newNeighborSet();

    /**
     * Returns a new empty set for storing {@link Edge} objects (the edge cache).
     *
     * @return a new empty edge set
     */
    Set<Edge<V>> newEdgeSet();

    // -------------------------------------------------------------------------
    // Algorithm scratch factories
    // -------------------------------------------------------------------------

    /**
     * Returns a new empty set for algorithm scratch use (e.g. visited sets,
     * on-stack markers, SCC membership).
     * <p>
     * Ordering within this set is not guaranteed.
     *
     * @return a new empty scratch set
     */
    Set<V> newSet();

    /**
     * Returns a new empty map from vertices to an arbitrary value type {@code T}, for
     * algorithm scratch use (e.g. parent maps, distance maps, colour maps).
     *
     * @param <T> the value type
     * @return a new empty scratch map
     */
    <T> Map<V, T> newMap();

    /**
     * Returns a new empty map from vertices to {@link Integer} values, for algorithm
     * scratch use (e.g. in-degree counts, DFS index and lowlink values, layer numbers).
     * <p>
     * A dedicated method is provided so that implementations may substitute a
     * primitive-value specialisation (e.g. {@code fastutil}'s {@code Object2IntOpenHashMap}
     * or {@code IntIntOpenHashMap}) to avoid boxing overhead.
     *
     * @return a new empty integer-valued scratch map
     */
    Map<V, Integer> newIntMap();

    // -------------------------------------------------------------------------
    // Default implementation
    // -------------------------------------------------------------------------

    /**
     * Returns the standard JDK-backed implementation of {@link GraphCollections}.
     * <p>
     * Uses {@code LinkedHashSet} for ordered sets and {@code HashMap} for maps.
     *
     * @param <V> the vertex type
     * @return the standard {@link GraphCollections} implementation
     */
    static <V> GraphCollections<V> standard() {
        return new StandardGraphCollections<>();
    }
}
