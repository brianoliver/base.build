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
 * A directed edge in a {@link Multigraph} with reference-based identity.
 * <p>
 * Unlike {@link Edge}, which is a record whose identity is defined by its {@code (from, to)}
 * vertex pair, {@code MultiEdge} uses default {@link Object} identity — two {@code MultiEdge}
 * instances are equal only if they are the same object.  This allows multiple parallel edges
 * between the same pair of vertices to coexist and remain independently referenceable.
 * <p>
 * Instances are created exclusively by {@link Multigraph.Builder#addEdge(Object, Object)},
 * which returns the created edge so callers can retain a reference to each specific edge.
 *
 * @param <V> the vertex type
 * @author reed.von.redwitz
 * @see Edge
 * @see Multigraph
 * @since Apr-2026
 */
public final class MultiEdge<V> {

    private final V from;
    private final V to;

    /**
     * Constructs a {@link MultiEdge}.  Package-private — created only by
     * {@link Multigraph.Builder}.
     *
     * @param from the source vertex
     * @param to   the target vertex
     */
    MultiEdge(final V from, final V to) {
        this.from = Objects.requireNonNull(from, "The from vertex cannot be null");
        this.to = Objects.requireNonNull(to, "The to vertex cannot be null");
    }

    /**
     * Returns the source vertex of this edge.
     *
     * @return the source vertex
     */
    public V from() {
        return from;
    }

    /**
     * Returns the target vertex of this edge.
     *
     * @return the target vertex
     */
    public V to() {
        return to;
    }

    /**
     * Returns {@code true} if this edge is a self-loop (i.e. {@code from} equals {@code to}).
     *
     * @return {@code true} if this is a self-loop
     */
    public boolean isSelfLoop() {
        return from.equals(to);
    }

    /**
     * Returns the unweighted {@link Edge} representation of this edge, discarding identity.
     * <p>
     * Note: the returned {@link Edge} is a value type — any other edge with the same
     * {@code (from, to)} pair will be considered equal to it.
     *
     * @return an {@link Edge} with the same endpoints
     */
    public Edge<V> toEdge() {
        return new Edge<>(from, to);
    }

    // No equals/hashCode override — identity is reference identity (Object default).
    // This is intentional: two MultiEdge instances are the same edge only if they are
    // the same object, allowing parallel edges between the same vertex pair.

    @Override
    public String toString() {
        return "MultiEdge{" + from + " -> " + to + "}@" + Integer.toHexString(System.identityHashCode(this));
    }
}
