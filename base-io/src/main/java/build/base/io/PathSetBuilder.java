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

import build.base.foundation.Strings;
import build.base.foundation.stream.Streams;
import build.base.table.Table;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A non-thread-safe mutable builder of {@link PathSet}s.
 *
 * @author brian.oliver
 * @since Jun-2020
 */
public class PathSetBuilder {

    /**
     * The {@link Path}s making up the {@link PathSet}.
     */
    private final LinkedHashSet<Path> paths;

    /**
     * Constructs a {@link PathSet} {@link PathSetBuilder}.
     */
    private PathSetBuilder() {
        this.paths = new LinkedHashSet<>();
    }

    /**
     * Adds the {@link Path}(s) define in the specified {@link String}, using the {@link File#pathSeparator}
     * to separate {@link Path}s.
     *
     * @param string the {@link String} representation of the {@link Path}(s)
     * @return this {@link PathSetBuilder} to permit fluent-style method calls
     */
    public PathSetBuilder add(final String string) {
        if (!Strings.isEmpty(string)) {
            Arrays.stream(string.split(File.pathSeparator))
                .map(Paths::get)
                .forEach(this.paths::add);
        }
        return this;
    }

    /**
     * Adds the specified {@link Path} to the {@link PathSetBuilder}.
     *
     * @param path the {@link Path}
     * @return this {@link PathSetBuilder} to permit fluent-style method calls
     */
    public PathSetBuilder add(final Path path) {
        if (path != null) {
            this.paths.add(path);
        }
        return this;
    }

    /**
     * Adds the specified sequence of {@link Path}s to the {@link PathSetBuilder}.
     *
     * @param paths the {@link Path}s
     * @return this {@link PathSetBuilder} to permit fluent-style method calls
     */
    public PathSetBuilder addAll(final Path... paths) {
        if (paths != null) {
            for (final Path path : paths) {
                if (path != null) {
                    this.paths.add(path);
                }
            }
        }

        return this;
    }

    /**
     * Adds the {@link Path}s in the specified {@link Stream} to the {@link PathSetBuilder}.
     *
     * @param stream the {@link Stream} of {@link Path}s
     * @return this {@link PathSetBuilder} to permit fluent-style method calls
     */
    public PathSetBuilder addAll(final Stream<Path> stream) {
        if (stream != null) {
            stream.forEach(this.paths::add);
        }
        return this;
    }

    /**
     * Adds the {@link Path}s in the specified {@link PathSetBuilder} to the {@link PathSetBuilder}.
     *
     * @param builder the {@link PathSetBuilder}
     * @return this {@link PathSetBuilder} to permit fluent-style method calls
     */
    public PathSetBuilder addAll(final PathSetBuilder builder) {
        if (builder != null) {
            this.paths.addAll(builder.paths);
        }
        return this;
    }

    /**
     * Removes the {@link Path}s from the {@link PathSet} that satisfied the specified {@link Predicate}.
     *
     * @param predicate the {@link Predicate}
     * @return this {@link PathSetBuilder} to permit fluent-style method calls
     */
    public PathSetBuilder removeIf(final Predicate<? super Path> predicate) {
        if (predicate != null) {
            this.paths.removeIf(predicate);
        }
        return this;
    }

    /**
     * Constructs a {@link PathSet} given the current {@link Path}s added to the {@link PathSetBuilder}.
     *
     * @return a new {@link PathSet}
     */
    public PathSet build() {
        return new InternalPathSet(this.paths);
    }

    /**
     * Creates a {@link PathSetBuilder} given an initial sequence of {@link Path}s.
     *
     * @param paths the {@link Path}s
     * @return a new {@link PathSetBuilder}
     */
    public static PathSetBuilder create(final Path... paths) {
        return new PathSetBuilder().addAll(paths);
    }

    /**
     * An internal {@link PathSet}.
     */
    private static class InternalPathSet
        implements PathSet {

        /**
         * The {@link Path}s making up the {@link PathSet}.
         */
        private final LinkedHashSet<Path> paths;

        /**
         * Constructs an {@link InternalPathSet}.
         *
         * @param paths the {@link Path}
         */
        private InternalPathSet(final Collection<Path> paths) {
            this.paths = new LinkedHashSet<>(paths);
        }

        @Override
        public boolean isEmpty() {
            return this.paths.isEmpty();
        }

        @Override
        public int size() {
            return this.paths.size();
        }

        @Override
        public Stream<Path> stream() {
            return this.paths.stream();
        }

        @Override
        public void tabularize(final Table table) {
            final var fileTable = Table.create();
            stream().forEach(file -> fileTable.addRow(file.toString()));
            table.addRow(this.paths.isEmpty() ? "(none)" : fileTable.toString());
        }

        @Override
        public boolean equals(final Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof final PathSet other)) {
                return false;
            }
            return Streams.equals(stream(), other.stream());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.paths);
        }

        @Override
        public String toString() {
            return this.paths.toString();
        }
    }
}
