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
 * A factory for the collection types used by {@link WeightedGraph} and the weighted graph
 * algorithms in {@link Graphs} (e.g. Dijkstra's shortest path).
 * <p>
 * Extends {@link GraphCollections} so that a single implementation covers both the weighted
 * graph's storage needs and the unweighted scratch collections used by algorithms that operate
 * on the underlying vertex-keyed maps.
 * <p>
 * The default implementation, obtained via {@link #standard()}, uses standard JDK collections.
 * Alternative implementations may substitute higher-performance backends such as fastutil.
 * <p>
 * A custom implementation is supplied to a {@link WeightedGraph.Builder} via
 * {@link WeightedGraph.Builder#withCollections(WeightedGraphCollections)}:
 * <pre>{@code
 * WeightedGraph.<Integer, Double>directed()
 *     .withCollections(new MyFastutilWeightedGraphCollections())
 *     .addEdge(1, 2, 1.5)
 *     .build();
 * }</pre>
 *
 * @param <V> the vertex type
 * @param <W> the weight type
 * @author reed.von.redwitz
 * @see GraphCollections
 * @since Apr-2026
 */
public interface WeightedGraphCollections<V, W> extends GraphCollections<V> {

    /**
     * Returns a new empty map for storing outgoing edges per vertex:
     * {@code vertex → (successor → WeightedEdge)}.
     *
     * @return a new empty outgoing adjacency map
     */
    Map<V, Map<V, WeightedEdge<V, W>>> newOutgoingMap();

    /**
     * Returns a new empty map for storing the neighbours and their edges for a single vertex:
     * {@code successor → WeightedEdge}.
     *
     * @return a new empty edge map
     */
    Map<V, WeightedEdge<V, W>> newEdgeMap();

    /**
     * Returns a new empty set for storing {@link WeightedEdge} objects (the edge cache).
     *
     * @return a new empty weighted edge set
     */
    Set<WeightedEdge<V, W>> newWeightedEdgeSet();

    /**
     * Returns the standard JDK-backed implementation of {@link WeightedGraphCollections}.
     *
     * @param <V> the vertex type
     * @param <W> the weight type
     * @return the standard {@link WeightedGraphCollections} implementation
     */
    static <V, W> WeightedGraphCollections<V, W> standard() {
        return new StandardWeightedGraphCollections<>();
    }
}
