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

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

/**
 * The result of a {@link Compiler#compile} invocation.
 */
public record Compilation(Status status,
                          List<JavaFileObject> sources,
                          List<Diagnostic<? extends JavaFileObject>> diagnostics,
                          List<JavaFileObject> generatedFiles,
                          List<Processor> processors,
                          List<String> options
) {

    /**
     * Whether the compilation succeeded or failed.
     */
    public enum Status {
        SUCCESS,
        FAILURE
    }

    /**
     * Returns {@code true} if the compilation succeeded.
     */
    public boolean succeeded() {
        return status == Status.SUCCESS;
    }

    /**
     * Returns a stream of all error diagnostics.
     */
    public Stream<Diagnostic<? extends JavaFileObject>> errors() {
        return diagnostics.stream().filter(d -> d.getKind() == Diagnostic.Kind.ERROR);
    }

    /**
     * Returns a stream of all warning diagnostics.
     */
    public Stream<Diagnostic<? extends JavaFileObject>> warnings() {
        return diagnostics.stream().filter(d -> d.getKind() == Diagnostic.Kind.WARNING
            || d.getKind() == Diagnostic.Kind.MANDATORY_WARNING);
    }

    /**
     * Returns a stream of all note diagnostics.
     */
    public Stream<Diagnostic<? extends JavaFileObject>> notes() {
        return diagnostics.stream().filter(d -> d.getKind() == Diagnostic.Kind.NOTE);
    }

    /**
     * Returns the generated source file for the given qualified class name, if any.
     *
     * @param qualifiedName fully-qualified class name, e.g. {@code com.example.Foo}
     */
    public Optional<JavaFileObject> generatedSourceFile(final String qualifiedName) {
        final String expectedPath = qualifiedName.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension;
        return generatedFiles.stream()
            .filter(f -> {
                final String path = f.toUri().getPath();
                return path != null && path.endsWith(expectedPath);
            })
            .findFirst();
    }

    /**
     * Returns the generated file at the given location, package, and relative name, if any.
     */
    public Optional<JavaFileObject> generatedFile(final JavaFileManager.Location location,
                                                  final String packageName,
                                                  final String relativeName) {
        final String packagePath = packageName.isEmpty() ? "" : packageName.replace('.', '/') + "/";
        final String expectedSuffix = "/" + location.getName() + "/" + packagePath + relativeName;
        return generatedFiles.stream()
            .filter(f -> {
                final String path = f.toUri().getPath();
                return path != null && path.endsWith(expectedSuffix);
            })
            .findFirst();
    }
}
