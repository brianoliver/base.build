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
import build.base.option.Attribute;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * An {@link ArchiveBuilder} for Java Archives (.jar files).
 *
 * @author brian.oliver
 * @see ArchiveBuilder
 * @since Jul-2021
 */
public class JarBuilder
    extends AbstractArchiveBuilder<JarBuilder> {

    /**
     * The {@link Manifest} for the Java Archive.
     */
    private final Manifest manifest;

    /**
     * Constructs an empty {@link JarBuilder}, using the specified {@link Manifest}.
     *
     * @param manifest the {@link Manifest}
     */
    public JarBuilder(final Manifest manifest) {
        this.manifest = manifest == null ? new Manifest() : manifest;
    }

    /**
     * Constructs an empty {@link JarBuilder} with an empty {@link Manifest}.
     */
    public JarBuilder() {
        this(new Manifest());
    }

    /**
     * Sets the specified main attribute value for the manifest produced for the Java Archive.
     *
     * @param name  the {@link Attributes.Name}
     * @param value the attribute value
     * @return the {@link JarBuilder} allowing fluent-style method invocation
     */
    public JarBuilder withMainAttribute(final Attributes.Name name,
                                        final Object value) {

        Objects.requireNonNull(name, "The Attribute.Name must not be null");
        Objects.requireNonNull(value, "The attribute value must not be null");

        this.manifest.getMainAttributes().put(name, value.toString());
        return this;
    }

    /**
     * Sets the specified main attribute value for the manifest produced for the Java Archive.
     *
     * @param name  the attribute name
     * @param value the attribute value
     * @return the {@link JarBuilder} allowing fluent-style method invocation
     */
    public JarBuilder withMainAttribute(final String name,
                                        final Object value) {
        return withMainAttribute(new Attributes.Name(name), value.toString());
    }

    /**
     * Sets the {@link Attributes.Name#MANIFEST_VERSION} for the manifest produced for the Java Archive.
     *
     * @param version the version
     * @return the {@link JarBuilder} allowing fluent-style method invocation
     */
    public JarBuilder withManifestVersion(final String version) {
        return withMainAttribute(Attributes.Name.MANIFEST_VERSION, version);
    }

    /**
     * Sets the {@link Attributes.Name#SIGNATURE_VERSION} for the manifest produced for the Java Archive.
     *
     * @param version the version
     * @return the {@link JarBuilder} allowing fluent-style method invocation
     */
    public JarBuilder withSignatureVersion(final String version) {
        return withMainAttribute(Attributes.Name.SIGNATURE_VERSION, version);
    }

    /**
     * Sets the {@link Attributes.Name#CLASS_PATH} for the manifest produced for the Java Archive.
     *
     * @param urls the {@link URL}s to be included in the class path
     * @return the {@link JarBuilder} allowing fluent-style method invocation
     */
    public JarBuilder withClassPath(final URL... urls) {
        Objects.requireNonNull(urls, "One or more URLs are required for the ClassPath");

        return withMainAttribute(
            Attributes.Name.CLASS_PATH,
            Arrays.stream(urls)
                .map(URL::toString)
                .collect(Collectors.joining(", ")));
    }

    /**
     * Sets the {@link Attributes.Name#MAIN_CLASS} for the manifest produced for the Java Archive.
     *
     * @param mainClass the main {@link Class}
     * @return the {@link JarBuilder} allowing fluent-style method invocation
     */
    public JarBuilder withMainClass(final Class<?> mainClass) {
        return withMainAttribute(Attributes.Name.MAIN_CLASS, mainClass.getName());
    }

    /**
     * Sets the {@link Attributes.Name#IMPLEMENTATION_TITLE} for the manifest produced for the Java Archive.
     *
     * @param title the title
     * @return the {@link JarBuilder} allowing fluent-style method invocation
     */
    public JarBuilder withImplementationTitle(final String title) {
        return withMainAttribute(Attributes.Name.IMPLEMENTATION_TITLE, title);
    }

    /**
     * Sets the {@link Attributes.Name#IMPLEMENTATION_VERSION} for the manifest produced for the Java Archive.
     *
     * @param version the version
     * @return the {@link JarBuilder} allowing fluent-style method invocation
     */
    public JarBuilder withImplementationVersion(final String version) {
        return withMainAttribute(Attributes.Name.IMPLEMENTATION_VERSION, version);
    }

    /**
     * Sets the {@link Attributes.Name#IMPLEMENTATION_VENDOR} for the manifest produced for the Java Archive.
     *
     * @param vendor the vendor
     * @return the {@link JarBuilder} allowing fluent-style method invocation
     */
    public JarBuilder withImplementationVendor(final String vendor) {
        return withMainAttribute(Attributes.Name.IMPLEMENTATION_VENDOR, vendor);
    }

    /**
     * Sets the {@link Attributes.Name#SPECIFICATION_TITLE} for the manifest produced for the Java Archive.
     *
     * @param title the title
     * @return the {@link JarBuilder} allowing fluent-style method invocation
     */
    public JarBuilder withSpecificationTitle(final String title) {
        return withMainAttribute(Attributes.Name.SPECIFICATION_TITLE, title);
    }

    /**
     * Sets the {@link Attributes.Name#SPECIFICATION_VERSION} for the manifest produced for the Java Archive.
     *
     * @param version the version
     * @return the {@link JarBuilder} allowing fluent-style method invocation
     */
    public JarBuilder withSpecificationVersion(final String version) {
        return withMainAttribute(Attributes.Name.SPECIFICATION_VERSION, version);
    }

    /**
     * Sets the {@link Attributes.Name#SPECIFICATION_VENDOR} for the manifest produced for the Java Archive.
     *
     * @param vendor the vendor
     * @return the {@link JarBuilder} allowing fluent-style method invocation
     */
    public JarBuilder withSpecificationVendor(final String vendor) {
        return withMainAttribute(Attributes.Name.SPECIFICATION_VENDOR, vendor);
    }

    /**
     * Sets the {@link Attributes.Name#SEALED} flag for the manifest produced for the Java Archive.
     *
     * @param isSealed the sealed value
     * @return the {@link JarBuilder} allowing fluent-style method invocation
     */
    public JarBuilder sealed(final boolean isSealed) {
        return withMainAttribute(Attributes.Name.SEALED, isSealed);
    }

    /**
     * Creates a manifest {@link Attributes} collection given zero or more {@link Attribute}s.
     *
     * @param attributes the optional {@link Attribute}s
     * @return {@code null} if no {@link Attribute}s were provided, otherwise a new {@link Attributes} collection
     * containing the specified {@link Attribute}s
     */
    private Attributes createAttributes(final Attribute... attributes) {
        if (attributes == null) {
            return null;
        }
        else {
            final Attributes mainfestAttributes = new Attributes();

            for (Attribute attribute : attributes) {
                mainfestAttributes.put(attribute.key(), attribute.value());
            }

            return mainfestAttributes;
        }
    }

    @Override
    public String getExtension() {
        return "jar";
    }

    @Override
    public Path build(final Path path)
        throws IOException {

        // TODO: ensure the Path is a File (with the correct extension)

        // include the Created-By attribute for the vendor and version of java
        final var version = System.getProperty("java.version");
        final var vendor = System.getProperty("java.vendor");

        if (version != null && vendor != null) {
            withMainAttribute("Created-By", version + " (" + vendor + ")");
        }

        // include the META-INF directory (so it can be created if necessary)
        in("META-INF");

        // establish the archive stream
        final JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(path), this.manifest);

        // create the folder content
        final var folders = new ArrayDeque<Folder>();
        folders.push(root());

        while (!folders.isEmpty()) {
            final var current = folders.pop();

            // include the immediate child folders to be created
            current.children().forEach(folders::push);

            // establish the JarEntry for the directory (ignoring the root directory)
            if (current.parent().isPresent()) {
                // ensure the path for the directory does not begin with the SEPARATOR
                // but ends with the SEPARATOR (otherwise the JarEntry may not be read)
                var directory = current.getPath();
                directory = directory.startsWith(SEPARATOR) ? directory.substring(1) : directory;
                directory = directory.endsWith(SEPARATOR) ? directory : directory + SEPARATOR;

                final var directoryEntry = new JarEntry(directory);
                jarOutputStream.putNextEntry(directoryEntry);
                jarOutputStream.closeEntry();
            }

            // archive the content of the current directory
            archive(current, jarOutputStream);
        }

        // close and create the archive
        jarOutputStream.close();

        return path;
    }

    /**
     * Archives the {@link Content} in the specified {@link Folder} into the provided {@link JarOutputStream}.
     *
     * @param folder          the {@link Folder}
     * @param jarOutputStream the {@link JarOutputStream}
     * @throws IOException should archiving fail
     */
    private void archive(final Folder folder, final JarOutputStream jarOutputStream)
        throws IOException {

        // obtain the path in which Folder Entries will be archived
        final var path = folder.getPath();

        // attempt to archive each of the Entries in the Folder
        try {
            folder.entries().forEach(entry -> {
                // drop the first path separator
                final var entryName = (path.startsWith(SEPARATOR) ? path.substring(1) : path) + entry.getName();

                try (InputStream inputStream = entry.getContent().getInputStream()) {

                    // establish the JarEntry for the Entry
                    final var jarEntry = new JarEntry(entryName);
                    jarOutputStream.putNextEntry(jarEntry);

                    // write the resource into jar
                    try (BufferedInputStream in = new BufferedInputStream(inputStream)) {

                        // establish a 4k buffer for reading and writing
                        final var buffer = new byte[4096];

                        int count;
                        while ((count = in.read(buffer)) != -1) {
                            jarOutputStream.write(buffer, 0, count);
                        }
                    }
                    finally {
                        jarOutputStream.closeEntry();
                    }

                }
                catch (final IOException e) {
                    Closeables.close(entry.getContent());

                    throw new RuntimeException("Failed to create JarEntry for " + entryName, e);
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
