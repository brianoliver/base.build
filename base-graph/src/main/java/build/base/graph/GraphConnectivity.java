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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Connectivity algorithms operating on {@link VertexGraph}.
 *
 * @author reed.von.redwitz
 * @since Apr-2026
 */
public final class GraphConnectivity {

    private GraphConnectivity() {
        // utility class
    }

    /**
     * Returns all strongly connected components (SCCs) of the graph using Tarjan's algorithm.
     * <p>
     * The components are returned in reverse topological order of the condensation DAG.
     * A single-vertex component is a trivial SCC.
     * <p>
     * Time complexity: O(V + E).
     *
     * @param <V>   the vertex type
     * @param graph the directed graph to analyse
     * @return list of SCCs in reverse topological order
     * @throws IllegalArgumentException if the graph is undirected
     */
    public static <V> List<Component<V>> stronglyConnectedComponents(final VertexGraph<V> graph) {
        Objects.requireNonNull(graph, "The graph cannot be null");
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("Strongly connected components requires a directed graph");
        }

        final TarjanState<V> state = new TarjanState<>(graph.collections());

        for (final V vertex : graph.vertices()) {
            if (!state.index.containsKey(vertex)) {
                tarjanDfs(graph, vertex, state);
            }
        }

        return List.copyOf(state.components);
    }

    /**
     * Iterative Tarjan's SCC algorithm state.
     *
     * @param <V> the vertex type
     */
    private static final class TarjanState<V> {

        final GraphCollections<V> col;
        int counter = 0;
        final Map<V, Integer> index;
        final Map<V, Integer> lowlink;
        final Set<V> onStack;
        final ArrayDeque<V> stack = new ArrayDeque<>();
        final List<Component<V>> components = new ArrayList<>();

        TarjanState(final GraphCollections<V> col) {
            this.col = col;
            this.index = col.newIntMap();
            this.lowlink = col.newIntMap();
            this.onStack = col.newSet();
        }
    }

    /**
     * Iterative DFS for Tarjan's SCC.
     */
    private static <V> void tarjanDfs(final VertexGraph<V> graph,
                                       final V root,
                                       final TarjanState<V> state) {
        record Frame<V>(V vertex, java.util.Iterator<V> iterator) {
        }

        final ArrayDeque<Frame<V>> callStack = new ArrayDeque<>();

        state.index.put(root, state.counter);
        state.lowlink.put(root, state.counter);
        state.counter++;
        state.stack.push(root);
        state.onStack.add(root);
        callStack.push(new Frame<>(root, graph.successors(root).iterator()));

        while (!callStack.isEmpty()) {
            final Frame<V> frame = callStack.peek();
            final V v = frame.vertex();

            if (frame.iterator().hasNext()) {
                final V w = frame.iterator().next();

                if (!state.index.containsKey(w)) {
                    state.index.put(w, state.counter);
                    state.lowlink.put(w, state.counter);
                    state.counter++;
                    state.stack.push(w);
                    state.onStack.add(w);
                    callStack.push(new Frame<>(w, graph.successors(w).iterator()));
                } else if (state.onStack.contains(w)) {
                    state.lowlink.merge(v, state.index.get(w), Math::min);
                }
            } else {
                callStack.pop();

                if (!callStack.isEmpty()) {
                    final V parent = callStack.peek().vertex();
                    state.lowlink.merge(parent, state.lowlink.get(v), Math::min);
                }

                if (state.lowlink.get(v).equals(state.index.get(v))) {
                    final Set<V> component = state.col.newSet();
                    V w;
                    do {
                        w = state.stack.pop();
                        state.onStack.remove(w);
                        component.add(w);
                    }
                    while (!w.equals(v));

                    state.components.add(new Component<>(component));
                }
            }
        }
    }
}
