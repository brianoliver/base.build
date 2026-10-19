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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;

/**
 * A {@link StandardJavaFileManager} that captures all compiler output in memory.
 */
final class InMemoryJavaFileManager extends ForwardingStandardJavaFileManager {

    private final Map<URI, JavaFileObject> inMemoryOutputs = new LinkedHashMap<>();

    InMemoryJavaFileManager(final StandardJavaFileManager delegate) {
        super(delegate);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(final Location location,
                                               final String className,
                                               final JavaFileObject.Kind kind,
                                               final FileObject sibling) {
        final URI uri = uriForJavaFile(location, className, kind);
        final JavaFileObject file = new InMemoryJavaFileObject(uri, kind);
        inMemoryOutputs.put(uri, file);
        return file;
    }

    @Override
    public FileObject getFileForOutput(final Location location,
                                       final String packageName,
                                       final String relativeName,
                                       final FileObject sibling) {
        final URI uri = uriForFile(location, packageName, relativeName);
        final JavaFileObject file = new InMemoryJavaFileObject(uri, JavaFileObject.Kind.OTHER);
        inMemoryOutputs.put(uri, file);
        return file;
    }

    List<JavaFileObject> generatedFiles() {
        return List.copyOf(inMemoryOutputs.values());
    }

    private static URI uriForJavaFile(final Location location,
                                      final String className,
                                      final JavaFileObject.Kind kind) {
        return URI.create("mem:///" + location.getName() + "/" + className.replace('.', '/') + kind.extension);
    }

    private static URI uriForFile(final Location location,
                                  final String packageName,
                                  final String relativeName) {
        final String packagePath = packageName.isEmpty() ? "" : packageName.replace('.', '/') + "/";
        return URI.create("mem:///" + location.getName() + "/" + packagePath + relativeName);
    }

    private static final class InMemoryJavaFileObject extends SimpleJavaFileObject {

        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        InMemoryJavaFileObject(final URI uri,
                               final Kind kind) {
            super(uri, kind);
        }

        @Override
        public OutputStream openOutputStream() {
            buffer.reset();
            return buffer;
        }

        @Override
        public InputStream openInputStream() {
            return new ByteArrayInputStream(buffer.toByteArray());
        }

        @Override
        public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
            return buffer.toString(StandardCharsets.UTF_8);
        }
    }
}
