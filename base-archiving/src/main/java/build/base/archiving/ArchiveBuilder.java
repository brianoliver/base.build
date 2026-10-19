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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A fluent <i>non-thread safe</i> builder for dynamically creating archives.
 * <p>
 * {@link ArchiveBuilder}s provide a file-system-like approach for defining the contents of archives, including
 * creating and navigating between directories with file-system separators like: {@code /}, {@code .} and {@code ..}.
 * <p>
 * Attempting to use the same instance of an {@link ArchiveBuilder} across multiple-threads or for building multiple
 * archives is unsupported and the behavior is undefined.  Instead a new {@link ArchiveBuilder}s should be created
 * for each thread and/or archive to be built.
 *
 * @author brian.oliver
 * @since Jul-2021
 */
public interface ArchiveBuilder<B extends ArchiveBuilder<B>> {

    /**
     * The separator of path elements in a path with in an archive.
     */
    String SEPARATOR = "/";

    /**
     * The path of the root {@link Directory}.
     */
    String ROOT_DIRECTORY = "/";

    /**
     * The path of the parent {@link Directory}.
     */
    String PARENT_DIRECTORY = "..";

    /**
     * The current {@link Directory}.
     */
    String CURRENT_DIRECTORY = ".";

    /**
     * Creates a {@link Directory} with the specified path, possibly relative to the <i>root</i> {@link Directory}.
     * <p>
     * Should the {@link Directory} with the specified path already exist, it will be returned.  Otherwise it will be
     * created, including all sub-paths that may be specified.
     * <p>
     * Paths may be specified as absolute or relative to the current {@link Directory}.
     * <p>
     * Paths may include the following:
     * <ol>
     *     <li>Zero or more path element (sub-{@code Directory}) separators, using a forward slash {@code /}</li>
     *     <li>Zero or more references to the current {@code Directory}, using a period ({@code .})</li>
     *     <li>Zero or more references to the parent {@code Directory}, using double periods ({@code ..})</li>
     *     <li>A single reference to the root {@code Directory}, using a forward slash ({@code /}),
     *     as the first character.</li>
     * </ol>
     *
     * @param path the path to the desired {@link Directory}
     * @return the created {@link Directory}
     */
    default Directory in(final String path) {
        return content().in(path);
    }

    /**
     * Obtains the <i>root</i> {@link Directory} for the contents of the archive.
     *
     * @return the {@link Directory}
     */
    Directory content();

    /**
     * Obtains the default file name extension used for the archives built using the {@link ArchiveBuilder}.
     *
     * @return the file name extension;
     */
    String getExtension();

    /**
     * Builds an archive using the content defined by the {@link ArchiveBuilder}, returning the {@link Path} to the
     * archive that was built.
     *
     * @param path the {@link Path} in which to create the archive
     * @return the {@link Path} to the archive that was built
     * @throws IOException should it not be possible to create the archive
     */
    Path build(Path path)
        throws IOException;

    /**
     * Builds an archive using the content defined by the {@link ArchiveBuilder}, returning the {@link Path} to the
     * archive that was built.
     *
     * @return the {@link Path} of the newly built archive
     * @throws IOException should it not be possible to create the archive
     */
    Path build()
        throws IOException;

    /**
     * Provides the ability to specify the contents of a directory to be created in an archive.
     */
    interface Directory {

        /**
         * Creates a {@link Directory} with the specified path, possibly relative to the current {@link Directory}.
         * <p>
         * Should the {@link Directory} with the specified path already exist, it will be returned.  Otherwise it will
         * be created, including all sub-paths that may be specified.
         * <p>
         * Paths may be specified as absolute or relative to the current {@link Directory}.
         * <p>
         * Paths may include the following:
         * <ol>
         *     <li>Zero or more path element (sub-{@code Directory}) separators, using a forward slash {@code /}</li>
         *     <li>Zero or more references to the current {@code Directory}, using a period ({@code .})</li>
         *     <li>Zero or more references to the parent {@code Directory}, using double periods ({@code ..})</li>
         *     <li>A single reference to the root {@code Directory}, using a forward slash ({@code /}),
         *     as the first character.</li>
         * </ol>
         *
         * @param path the path to the desired {@link Directory}
         * @return the created {@link Directory}
         */
        Directory in(String path);

        /**
         * Specifies a {@link Path} to be added to the {@link Directory} of the archive, using the specified name.
         * <p>
         * If the {@link Path} is a directory, the contents will be copied into a {@link Directory}
         * using the specified name.
         *
         * @param name    the name for the {@link Path} in the {@link Directory}
         * @param path    the {@link Path}
         * @param options the {@link Option}s for entry in the produced archive
         * @return the {@link Directory} to allow fluent-style method invocations on the {@link Directory}
         * @throws IOException should the specified {@link Path} not be accessible
         */
        Directory add(String name, Path path, Option... options)
            throws IOException;

        /**
         * Specifies a {@link Path} to be added to the {@link Directory} of the archive,
         * using the {@link Path#getFileName()} as the name.
         * <p>
         * If the {@link Path} is a directory, the contents will be copied into a {@link Directory} named
         * using {@link Path#getFileName()}.
         *
         * @param path    the {@link Path}
         * @param options the {@link Option}s for entry in the produced archive
         * @return the {@link Directory} to allow fluent-style method invocations on the {@link Directory}
         * @throws IOException should the specified {@link Path} not be accessible
         */
        default Directory add(final Path path, final Option... options)
            throws IOException {

            return add(Files.isDirectory(path) ? "." : path.getFileName().toFile().getName(), path, options);
        }

        /**
         * Specifies a {@link File} to be added to the {@link Directory} of the archive,
         * using the {@link File#getName()} as the name.
         * <p>
         * If the {@link File} is a directory, the contents will be copied into a {@link Directory} named
         * using {@link File#getName()}.
         *
         * @param file    the {@link File}
         * @param options the {@link Option}s for entry in the produced archive
         * @return the {@link Directory} to allow fluent-style method invocations on the {@link Directory}
         * @throws IOException should the specified {@link Path} not be accessible
         */
        default Directory add(final File file, final Option... options)
            throws IOException {

            return add(file.toPath(), options);
        }

        /**
         * Specifies the {@link Content} to be added in the {@link Directory} as a file using the provided name.
         *
         * @param name    the name for the {@link File} in the {@link Directory}
         * @param content the {@link Content}
         * @param options the {@link Option}s for entry in the produced archive
         * @return the {@link Directory} to allow fluent-style method invocations on the {@link Directory}
         */
        Directory add(String name, Content content, Option... options);

        /**
         * Specifies a {@link Class} to be added to the produced archive.
         * <p>
         * The byte-code for specified {@link Class} will be copied into produce archive, using the {@link Class}
         * name as the archive entry name.
         *
         * @param classToInclude the {@link Class}
         * @param options        the {@link Option}s for entry in the produced archive
         * @return the {@link Directory} allowing fluent-style method invocation
         */
        Directory add(Class<?> classToInclude, Option... options);

        /**
         * Builds an archive using the content defined by the {@link ArchiveBuilder}, returning the {@link Path} to the
         * archive that was built.
         *
         * @param path the {@link Path} in which to create the archive
         * @return the {@link Path} to the archive that was built
         * @throws IOException should it not be possible to create the archive
         */
        Path build(Path path)
            throws IOException;

        /**
         * Builds an archive using the content defined by the {@link ArchiveBuilder}, returning the {@link Path} to the
         * archive that was built.
         *
         * @return the {@link Path} of the newly built archive
         * @throws IOException should it not be possible to create the archive
         */
        Path build()
            throws IOException;
    }

    /**
     * Represents an entry in a {@link Directory} for an archive.
     */
    interface Entry {

        /**
         * Obtains the name of the {@link Entry} in the archive, typically the filename.
         *
         * @return the name of the {@link Entry}
         */
        String getName();

        /**
         * Obtains the {@link Directory} in which the {@link Entry} is located.
         *
         * @return the {@link Directory}
         */
        Directory getDirectory();

        /**
         * Obtains the {@link Content} supplier for the {@link Entry}.
         *
         * @return the {@link Content}
         */
        Content getContent();
    }

    /**
     * Provides a mechanism to obtain content for an {@link Entry} to be included in an archive.
     */
    interface Content
        extends AutoCloseable {

        /**
         * Obtains an {@link InputStream} from which the {@link Entry} {@link Content} can be read.
         *
         * @return an {@link InputStream}
         * @throws IOException should establishing the {@link InputStream} fail
         */
        InputStream getInputStream()
            throws IOException;

        /**
         * Obtains the size of the {@link Content} in bytes.
         *
         * @return the size of the {@link Content} in bytes
         * @throws IOException should reading the size of the {@link Content} {@link InputStream} fail
         */
        default long size()
            throws IOException {

            try (InputStream stream = new BufferedInputStream(getInputStream())) {
                final var buffer = new byte[4096];
                int read = 0;
                int length = 0;
                while ((read = stream.read(buffer)) != -1) {
                    length += read;
                }
                return length;
            }
        }
    }
}
