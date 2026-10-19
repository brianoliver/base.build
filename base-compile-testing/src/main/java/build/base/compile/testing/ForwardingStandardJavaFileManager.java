package build.base.compile.testing;

/*-
 * #%L
 * base.build Compile Testing
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

/**
 * A {@link StandardJavaFileManager} that forwards all calls to a delegate.
 */
abstract class ForwardingStandardJavaFileManager
    extends ForwardingJavaFileManager<StandardJavaFileManager>
    implements StandardJavaFileManager {

    protected ForwardingStandardJavaFileManager(final StandardJavaFileManager delegate) {
        super(delegate);
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(
        final Iterable<? extends File> files) {
        return fileManager.getJavaFileObjectsFromFiles(files);
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromPaths(
        final Collection<? extends Path> paths) {
        return fileManager.getJavaFileObjectsFromPaths(paths);
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjects(final File... files) {
        return fileManager.getJavaFileObjects(files);
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjects(final Path... paths) {
        return fileManager.getJavaFileObjects(paths);
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjects(final String... names) {
        return fileManager.getJavaFileObjects(names);
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(final Iterable<String> names) {
        return fileManager.getJavaFileObjectsFromStrings(names);
    }

    @Override
    public void setLocation(final Location location,
                            final Iterable<? extends File> path)
        throws IOException {
        fileManager.setLocation(location, path);
    }

    @Override
    public void setLocationFromPaths(final Location location,
                                     final Collection<? extends Path> paths)
        throws IOException {
        fileManager.setLocationFromPaths(location, paths);
    }

    @Override
    public void setLocationForModule(final Location location,
                                     final String moduleName,
                                     final Collection<? extends Path> paths) throws IOException {
        fileManager.setLocationForModule(location, moduleName, paths);
    }

    @Override
    public Iterable<? extends File> getLocation(final Location location) {
        return fileManager.getLocation(location);
    }

    @Override
    public Iterable<? extends Path> getLocationAsPaths(final Location location) {
        return fileManager.getLocationAsPaths(location);
    }

    @Override
    public String inferModuleName(final Location location) throws IOException {
        return fileManager.inferModuleName(location);
    }

    @Override
    public Iterable<Set<Location>> listLocationsForModules(final Location location)
        throws IOException {
        return fileManager.listLocationsForModules(location);
    }

    @Override
    public boolean contains(final Location location,
                            final FileObject fo) throws IOException {
        return fileManager.contains(location, fo);
    }
}
