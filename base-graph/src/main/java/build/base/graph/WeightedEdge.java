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

/**
 * A directed, weighted edge in a {@link WeightedGraph}, connecting a source vertex
 * {@code from} to a target vertex {@code to} with an associated {@code weight}.
 *
 * @param <V> the vertex type
 * @param <W> the weight type
 * @param from   the source vertex
 * @param to     the target vertex
 * @param weight the edge weight
 * @author reed.von.redwitz
 * @since Apr-2026
 */
public record WeightedEdge<V, W>(V from, V to, W weight) {

    /**
     * Constructs a {@link WeightedEdge}, validating that neither endpoint nor weight is {@code null}.
     *
     * @param from   the source vertex
     * @param to     the target vertex
     * @param weight the edge weight
     */
    public WeightedEdge {
        Objects.requireNonNull(from, "The from vertex cannot be null");
        Objects.requireNonNull(to, "The to vertex cannot be null");
        Objects.requireNonNull(weight, "The weight cannot be null");
    }

    /**
     * Returns a new {@link WeightedEdge} with the direction reversed, preserving the weight.
     *
     * @return a reversed {@link WeightedEdge} from {@code to} to {@code from}
     */
    public WeightedEdge<V, W> reversed() {
        return new WeightedEdge<>(to, from, weight);
    }

    /**
     * Returns the unweighted {@link Edge} equivalent of this edge.
     *
     * @return an {@link Edge} with the same {@code from} and {@code to} vertices
     */
    public Edge<V> toEdge() {
        return new Edge<>(from, to);
    }

    /**
     * Returns {@code true} if this edge is a self-loop (i.e. {@code from} equals {@code to}).
     *
     * @return {@code true} if this is a self-loop
     */
    public boolean isSelfLoop() {
        return from.equals(to);
    }
}
