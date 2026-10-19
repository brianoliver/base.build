package build.base.archiving;

/*-
 * #%L
 * base.build Archiving
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

import build.base.configuration.Option;
import build.base.foundation.Closeables;
import build.base.foundation.Strings;
import build.base.foundation.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * An abstract {@link ArchiveBuilder}.
 *
 * @author brian.oliver
 * @since Jul-2021
 */
public abstract class AbstractArchiveBuilder<B extends AbstractArchiveBuilder<B>>
    implements ArchiveBuilder<B> {

    /**
     * The root {@link Folder} of the archive to build.
     */
    private final Folder root;

    /**
     * Constructs an empty {@link AbstractArchiveBuilder}.
     */
    public AbstractArchiveBuilder() {
        this.root = new Folder();
    }

    @Override
    public Directory content() {
        return this.root;
    }

    @Override
    public Directory in(final String path) {
        return content().in(path);
    }

    @Override
    public Path build()
        throws IOException {

        // determine the file name extension for the archive
        final var extension = Strings.trim(getExtension());
        final var dotExtension = Strings.isEmpty(extension)
            ? ".unknown"
            : (extension.startsWith(".") ? extension : "." + extension);

        // create a temporary file
        final var archivePath = Files.createTempFile("archive-", dotExtension);

        return build(archivePath);
    }

    /**
     * Obtains the root {@link Folder} for the archive.
     *
     * @return the root {@link Folder}
     */
    protected Folder root() {
        return this.root;
    }

    /**
     * A {@link Directory} of {@link Content} and other {@link Directory}s to create in an archive.
     */
    public class Folder
        implements Directory {

        /**
         * The parent {@link Folder}, {@code null} when the root.
         */
        private final Folder parent;

        /**
         * The name of the {@link Folder} (case-sensitive).
         */
        private final String name;

        /**
         * The immediate child {@link Folder}s, keyed by name (case-sensitive).
         */
        private final LinkedHashMap<String, Folder> children;

        /**
         * The {@link Content} in the {@link Directory} to be included in the archive, keyed by name (case-sensitive).
         */
        private final LinkedHashMap<String, Content> content;

        /**
         * Constructs an empty root {@link Folder}.
         */
        Folder() {
            this(null, ROOT_DIRECTORY);
        }

        /**
         * Constructs an empty {@link Folder} with the specified parent {@link Folder} and name.
         */
        Folder(final Folder parent, final String name) {

            this.parent = parent;
            this.name = name.trim();
            this.children = new LinkedHashMap<>();
            this.content = new LinkedHashMap<>();
        }

        /**
         * Obtains the {@link Optional} parent {@link Folder}.
         *
         * @return the {@link Optional} parent {@link Folder}
         */
        public Optional<Folder> parent() {
            return Optional.ofNullable(this.parent);
        }

        /**
         * Obtains the root {@link Folder}.
         *
         * @return the root {@link Folder}
         */
        public Folder root() {
            return AbstractArchiveBuilder.this.root;
        }

        /**
         * Obtains a {@link Stream} of the child {@link Folder}s of this {@link Folder}.
         *
         * @return a {@link Stream} of {@link Folder}s
         */
        public Stream<Folder> children() {
            return this.children.values().stream();
        }

        /**
         * Obtains a {@link Stream} of {@link Entry}s contained in this {@link Folder}.
         *
         * @return a {@link Stream} of {@link Entry}s
         */
        public Stream<Entry> entries() {
            return this.content.entrySet().stream()
                .map(entry -> new Entry() {
                    @Override
                    public String getName() {
                        return entry.getKey();
                    }

                    @Override
                    public Directory getDirectory() {
                        return Folder.this;
                    }

                    @Override
                    public Content getContent() {
                        return entry.getValue();
                    }
                });
        }

        /**
         * Obtains a {@link String} representation of the {@link Folder} in the archive, from the root {@link Folder},
         * including all parent {@link Folder}s, each separated by a {@link #SEPARATOR} and terminated
         * with a {@link #SEPARATOR}.
         *
         * @return the path
         */
        public String getPath() {
            return this.parent == null
                ? ROOT_DIRECTORY
                : this.parent.getPath() + this.name + SEPARATOR;
        }

        @Override
        public Directory in(final String path) {

            Objects.requireNonNull(path, "The path must not be null");

            // we commence traversing from this directory
            var current = this;

            final var sanitizedPath = path.trim();

            if (sanitizedPath.equals(ROOT_DIRECTORY)) {
                return AbstractArchiveBuilder.this.root;
            } else {
                final var element = sanitizedPath.split(SEPARATOR);

                for (final String s : element) {
                    final var parent = current;
                    final var sanitizedName = s.trim();

                    if (sanitizedName.equals(PARENT_DIRECTORY)) {
                        current = current.parent;

                        if (current == null) {
                            throw new IllegalArgumentException(
                                "The path [" + sanitizedPath
                                    + "] attempts to traverse beyond the root directory");
                        }
                    } else if (!sanitizedName.isEmpty() && !sanitizedName.equals(CURRENT_DIRECTORY)) {
                        current = current.children
                            .computeIfAbsent(sanitizedName, __ -> new Folder(parent, sanitizedName));
                    }
                }

                return current;
            }
        }

        @Override
        public Directory add(final String name,
                             final Path path,
                             final Option... options)
            throws IOException {

            Objects.requireNonNull(name, "The name must not be null");
            Objects.requireNonNull(path, "The path must not be null");

            final var sanitizedName = name.trim();

            if (!Files.exists(path)) {
                return this;
            }
            if (Files.isDirectory(path)) {

                // obtain the target Directory in which to copy the Files from the specified Directory
                final var target = in(sanitizedName);

                final var directories = new ArrayDeque<Pair<Directory, Path>>();
                directories.push(Pair.of(target, path));

                while (!directories.isEmpty()) {
                    final var pair = directories.pop();
                    final var currentDirectory = pair.first();
                    final var currentPath = pair.second();

                    for (Path p : Files.list(currentPath).toArray(Path[]::new)) {
                        if (Files.isDirectory(p)) {
                            directories.push(Pair.of(currentDirectory.in(p.getFileName().toString()), p));
                        } else {
                            currentDirectory.add(p);
                        }
                    }
                }

                return this;
            } else {
                if (sanitizedName.endsWith(SEPARATOR)) {
                    // as the name is a directory, add the file to that directory
                    return in(sanitizedName).add(path, options);
                } else {
                    // add the file itself
                    return add(sanitizedName, new Content() {
                        @Override
                        public InputStream getInputStream()
                            throws IOException {

                            return Files.newInputStream(path);
                        }

                        @Override
                        public long size()
                            throws IOException {

                            return Files.size(path);
                        }

                        @Override
                        public void close() {
                            // the FileInputStream will be closed automatically once the file is read
                        }
                    }, options);
                }
            }
        }

        @Override
        public Directory add(final String name,
                             final Content content,
                             final Option... options) {

            Objects.requireNonNull(name, "The name must not be null");
            Objects.requireNonNull(content, "The content must not be null");

            final var sanitizedName = name.trim();

            if (sanitizedName.endsWith(SEPARATOR)) {
                throw new IllegalArgumentException(
                    "The specified name [" + sanitizedName + "] is a folder.  It must be the name of a file.");
            }

            final var lastIndexOf = sanitizedName.lastIndexOf(SEPARATOR);
            if (lastIndexOf == -1) {
                this.content.put(sanitizedName, content);
            } else {
                // when the name includes a Directory, we add the Content to that Directory, not this one
                final var path = sanitizedName.substring(0, lastIndexOf);
                in(path).add(sanitizedName.substring(lastIndexOf + 1), content, options);
            }

            return this;
        }

        @Override
        public Directory add(final Class<?> classToInclude,
                             final Option... options) {

            final var resourceName = classToInclude.getName().replace('.', '/') + ".class";

            add(resourceName, new Content() {
                /**
                 * The {@link URLClassLoader} to use for loading the specified {@link Class}.
                 */
                private URLClassLoader urlClassLoader;

                @Override
                public InputStream getInputStream() {

                    // determine the URL of the resource
                    final var url = classToInclude.getProtectionDomain().getCodeSource().getLocation();

                    // establish a URLClassLoader to access the resource as a stream
                    this.urlClassLoader = new URLClassLoader(new URL[]{url});

                    return this.urlClassLoader.getResourceAsStream(resourceName);
                }

                @Override
                public void close() {
                    Closeables.close(this.urlClassLoader);
                }
            }, options);

            return this;
        }

        @Override
        public Path build(final Path path)
            throws IOException {

            return AbstractArchiveBuilder.this.build(path);
        }

        @Override
        public Path build()
            throws IOException {

            return AbstractArchiveBuilder.this.build();
        }
    }
}
