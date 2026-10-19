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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Standard JDK-backed implementation of {@link GraphCollections}.
 * <p>
 * Uses {@link LinkedHashSet} for ordered sets (vertices, neighbours, edges) and
 * {@link HashMap} for maps, matching the existing behaviour of the graph library prior
 * to the introduction of pluggable collection backends.
 *
 * @param <V> the vertex type
 * @author reed.von.redwitz
 * @since Apr-2026
 */
final class StandardGraphCollections<V> implements GraphCollections<V> {

    @Override
    public Set<V> newVertexSet() {
        return new LinkedHashSet<>();
    }

    @Override
    public Map<V, Set<V>> newAdjacencyMap() {
        return new LinkedHashMap<>();
    }

    @Override
    public Set<V> newNeighborSet() {
        return new LinkedHashSet<>();
    }

    @Override
    public Set<Edge<V>> newEdgeSet() {
        return new LinkedHashSet<>();
    }

    @Override
    public Set<V> newSet() {
        return new HashSet<>();
    }

    @Override
    public <T> Map<V, T> newMap() {
        return new HashMap<>();
    }

    @Override
    public Map<V, Integer> newIntMap() {
        return new HashMap<>();
    }
}
