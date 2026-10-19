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

import java.util.Objects;
import java.util.Set;

/**
 * A strongly connected component (SCC) in a {@link Graph} — a maximal set of vertices
 * such that every vertex is reachable from every other vertex within the component.
 * <p>
 * A component containing a single vertex is a trivial SCC.
 *
 * @param <V>      the vertex type
 * @param vertices the set of vertices in this component (non-empty)
 * @author reed.von.redwitz
 * @since Apr-2026
 */
public record Component<V>(Set<V> vertices) {

    /**
     * Constructs a {@link Component}, defensive-copying the vertex set and ensuring it is non-empty.
     *
     * @param vertices the vertices in this component
     */
    public Component {
        Objects.requireNonNull(vertices, "The vertices set cannot be null");

        if (vertices.isEmpty()) {
            throw new IllegalArgumentException("A component must contain at least one vertex");
        }

        vertices = Set.copyOf(vertices);
    }

    /**
     * Returns the number of vertices in this component.
     *
     * @return the size of this component
     */
    public int size() {
        return vertices.size();
    }

    /**
     * Returns {@code true} if this component contains more than one vertex.
     *
     * @return {@code true} if this component is non-trivial
     */
    public boolean isNonTrivial() {
        return vertices.size() > 1;
    }

    /**
     * Returns {@code true} if this component contains exactly one vertex.
     *
     * @return {@code true} if this component is trivial
     */
    public boolean isTrivial() {
        return vertices.size() == 1;
    }

    /**
     * Returns {@code true} if this component contains the given vertex.
     *
     * @param vertex the vertex to check
     * @return {@code true} if {@code vertex} is in this component
     */
    public boolean contains(final V vertex) {
        return vertices.contains(vertex);
    }
}
