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

import build.base.foundation.Closeables;
import build.base.tar.TarEntry;
import build.base.tar.TarHeader;
import build.base.tar.TarOutputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;

/**
 * An abstract {@link ArchiveBuilder} of Tape Archives (.tar files).
 *
 * @author brian.oliver
 * @see ArchiveBuilder
 * @since Jul-2021
 */
public abstract class AbstractTarBuilder<B extends AbstractTarBuilder<B>>
    extends AbstractArchiveBuilder<B> {

    /**
     * Constructs an empty {@link AbstractTarBuilder}.
     */
    public AbstractTarBuilder() {
        super();
    }

    @Override
    public String getExtension() {
        return "tar";
    }

    @Override
    public Path build(final Path path)
        throws IOException {

        // TODO: ensure the Path is a File (with the correct extension)

        // establish the creation time (for the tar format)
        final var creationInstant = System.currentTimeMillis() / 1000;

        // determine the permissions for entries (based on those for the archive itself)
        final var permissions = 0755;

        // establish the archive stream
        final var tarOutputStream = new TarOutputStream(Files.newOutputStream(path));

        // create the folder content
        final var folders = new ArrayDeque<Folder>();
        folders.push(root());

        while (!folders.isEmpty()) {
            final var current = folders.pop();

            // include all of the immediate child folders to be created
            current.children().forEach(folders::push);

            // establish the TarEntry for the directory (ignoring the root directory)
            if (current.parent().isPresent()) {
                // ensure the path for the directory does not begin with the SEPARATOR
                // but ends with the SEPARATOR (otherwise the JarEntry may not be read)
                var directory = current.getPath();
                directory = directory.startsWith(SEPARATOR) ? directory.substring(1) : directory;
                directory = directory.endsWith(SEPARATOR) ? directory : directory + SEPARATOR;

                // establish a TarHeader and TarEntry for the Directory
                final var tarHeader = TarHeader.createHeader(directory, 0, creationInstant, true, permissions);
                final var directoryEntry = new TarEntry(tarHeader);
                tarOutputStream.putNextEntry(directoryEntry);
            }

            // archive the content of the current directory
            archive(current, tarOutputStream, creationInstant, permissions);
        }

        // close and create the archive
        tarOutputStream.close();

        return path;
    }

    /**
     * Archives the {@link Content} in the specified {@link Folder} into the provided {@link TarOutputStream}.
     *
     * @param folder          the {@link Folder}
     * @param tarOutputStream the {@link TarOutputStream}
     * @param creationTime    the time (for the archive) to record file creation
     * @param permissions     the permissions for the files in the archive
     * @throws IOException should archiving fail
     */
    private void archive(final Folder folder,
                         final TarOutputStream tarOutputStream,
                         final long creationTime,
                         final int permissions)
        throws IOException {

        // obtain the path in which Folder Entries will be archived
        final var path = folder.getPath();

        // attempt to archive each of the Entries in the Folder
        try {
            folder.entries().forEach(entry -> {
                // drop the first path separator
                final var entryName = (path.startsWith(SEPARATOR) ? path.substring(1) : path) + entry.getName();

                try (InputStream inputStream = entry.getContent().getInputStream()) {
                    // determine the size of the Entry (for the TarHeader)
                    final var size = entry.getContent().size();

                    // establish the TarHeader and TarEntry for the Entry
                    final var tarHeader = TarHeader.createHeader(entryName, size, creationTime, false, permissions);

                    final var tarEntry = new TarEntry(tarHeader);
                    tarOutputStream.putNextEntry(tarEntry);

                    // write the resource into jar
                    try (BufferedInputStream in = new BufferedInputStream(inputStream)) {

                        // establish a 4k buffer for reading and writing
                        final var buffer = new byte[4096];

                        int count;
                        while ((count = in.read(buffer)) != -1) {
                            tarOutputStream.write(buffer, 0, count);
                        }
                    }

                }
                catch (final IOException e) {
                    Closeables.close(entry.getContent());

                    throw new RuntimeException("Failed to create TarEntry for " + entryName, e);
                }
            });
        }
        catch (final RuntimeException e) {
            // unwrap the RuntimeException that was caused by an IOException
            if (e.getCause() instanceof IOException) {
                throw new IOException(e.getMessage(), e.getCause());
            }
            else {
                throw e;
            }
        }
    }
}
