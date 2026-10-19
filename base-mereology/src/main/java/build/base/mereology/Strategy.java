package build.base.mereology;

/*-
 * #%L
 * base.build Mereology
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

/**
 * The strategy for <a href="https://en.wikipedia.org/wiki/Tree_traversal">traversing</a> <i>parts</i> of a
 * {@link Composite}.
 *
 * @author brian.oliver
 * @since Sep-2025
 */
public enum Strategy {

    /**
     * Only traverse <i>direct</i> <i>parts</i> of a {@link Composite} and not transitive <i>indirect</i> <i>parts</i>.
     */
    Direct,

    /**
     * Transitively traverse <i>parts</i> of a {@link Composite} using a <i>depth-first</i> strategy.
     * <p>
     * Visited {@link Composite}s are tracked by identity ({@code ==}) so that cycles in the graph are broken
     * without requiring {@link Object#equals(Object)} to be meaningful on {@link Composite} implementations.
     */
    DepthFirst,

    /**
     * Transitively traverse <i>parts</i> of a {@link Composite} using a <i>breadth-first</i> strategy.
     * <p>
     * Visited {@link Composite}s are tracked by identity ({@code ==}) so that cycles in the graph are broken
     * without requiring {@link Object#equals(Object)} to be meaningful on {@link Composite} implementations.
     */
    BreadthFirst;
}
