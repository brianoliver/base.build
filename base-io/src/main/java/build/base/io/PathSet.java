package build.base.io;

/*-
 * #%L
 * base.build IO
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

import build.base.table.Table;
import build.base.table.Tabular;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * An immutable ordered set of {@link Path}s.
 *
 * @author brian.oliver
 * @since Jun-2020
 */
public interface PathSet
    extends Tabular, Iterable<Path> {

    /**
     * A {@link Collector} of {@link Path}s to create a {@link PathSet}.
     */
    Collector<Path, PathSetBuilder, PathSet> COLLECTOR = new Collector<>() {
        @Override
        public Supplier<PathSetBuilder> supplier() {
            return PathSetBuilder::create;
        }

        @Override
        public BiConsumer<PathSetBuilder, Path> accumulator() {
            return PathSetBuilder::add;
        }

        @Override
        public BinaryOperator<PathSetBuilder> combiner() {
            return PathSetBuilder::addAll;
        }

        @Override
        public Function<PathSetBuilder, PathSet> finisher() {
            return PathSetBuilder::build;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Collections.emptySet();
        }
    };

    /**
     * Determines if the {@link PathSet} is empty (contains no {@link Path}s).
     *
     * @return {@code true} when the {@link PathSet} is empty, {@code false} otherwise
     */
    boolean isEmpty();

    /**
     * Determines the number of {@link Path}s in the {@link PathSet}.
     *
     * @return the number of {@link Path}s
     */
    int size();

    /**
     * Obtain a {@link Stream} of the {@link Path}s defined by the {@link PathSet}.
     *
     * @return a {@link Stream} of {@link Path}s.
     */
    Stream<Path> stream();

    @Override
    default Iterator<Path> iterator() {
        return stream().iterator();
    }

    /**
     * Obtains an empty {@link PathSet}.
     *
     * @return an empty {@link PathSet}
     */
    static PathSet empty() {
        return EmptyPathSet.INSTANCE;
    }

    /**
     * Creates a {@link PathSet} given the specified {@link Path}s.
     *
     * @param paths the {@link Path}s
     * @return a {@link PathSet} containing the specified {@link Path}s
     */
    static PathSet of(final Path... paths) {
        return paths == null || paths.length == 0
            ? empty()
            : PathSetBuilder.create(paths)
                .build();
    }

    /**
     * Obtains a {@link Collector} of {@link Path}s to be place in a {@link PathSet}.
     *
     * @return a {@link Collector}
     */
    static Collector<Path, PathSetBuilder, PathSet> collector() {
        return COLLECTOR;
    }

    /**
     * A {@link PathSet} implementation that is always empty.
     */
    class EmptyPathSet
        implements PathSet {

        /**
         * An instance of the {@link EmptyPathSet}.
         */
        private static final PathSet INSTANCE = new EmptyPathSet();

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Stream<Path> stream() {
            return Stream.empty();
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(final Object object) {
            return object instanceof PathSet && ((PathSet) object).size() == 0;
        }

        @Override
        public void tabularize(final Table table) {
            table.addRow("(none)");
        }

        @Override
        public Iterator<Path> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public String toString() {
            return "[]";
        }
    }
}
