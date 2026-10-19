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
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.annotation.processing.Processor;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Fluent builder for running {@code javac} in-process against in-memory source files.
 *
 * <p>Instances are immutable; every {@code with*} method returns a new instance.</p>
 */
public final class Compiler {

    private final List<Processor> processors;
    private final List<String> options;
    private final List<Path> classpath;
    private final List<Path> modulePath;

    private Compiler(final List<Processor> processors,
                     final List<String> options,
                     final List<Path> classpath,
                     final List<Path> modulePath) {
        this.processors = processors;
        this.options = options;
        this.classpath = classpath;
        this.modulePath = modulePath;
    }

    /**
     * Returns a default compiler backed by the system {@code javac}.
     */
    public static Compiler javac() {
        return new Compiler(List.of(), List.of(), List.of(), List.of());
    }

    /**
     * Returns a new compiler with the given annotation processors appended.
     */
    public Compiler withProcessors(final Processor... processors) {
        final List<Processor> merged = new ArrayList<>(this.processors);
        merged.addAll(Arrays.asList(processors));
        return new Compiler(List.copyOf(merged), options, classpath, modulePath);
    }

    /**
     * Returns a new compiler with the given extra {@code javac} options appended.
     */
    public Compiler withOptions(final String... options) {
        final List<String> merged = new ArrayList<>(this.options);
        merged.addAll(Arrays.asList(options));
        return new Compiler(processors, List.copyOf(merged), classpath, modulePath);
    }

    /**
     * Returns a new compiler with the given paths added to the class path.
     */
    public Compiler withClasspath(final Path... paths) {
        final List<Path> merged = new ArrayList<>(this.classpath);
        merged.addAll(Arrays.asList(paths));
        return new Compiler(processors, options, List.copyOf(merged), modulePath);
    }

    /**
     * Returns a new compiler with the given paths added to the class path.
     */
    public Compiler withClasspath(final List<Path> paths) {
        final List<Path> merged = new ArrayList<>(this.classpath);
        merged.addAll(paths);
        return new Compiler(processors, options, List.copyOf(merged), modulePath);
    }

    /**
     * Returns a new compiler with the given paths added to the module path.
     */
    public Compiler withModulePath(final Path... paths) {
        final List<Path> merged = new ArrayList<>(this.modulePath);
        merged.addAll(Arrays.asList(paths));
        return new Compiler(processors, options, classpath, List.copyOf(merged));
    }

    /**
     * Returns a new compiler with the given paths added to the module path.
     */
    public Compiler withModulePath(final List<Path> paths) {
        final List<Path> merged = new ArrayList<>(this.modulePath);
        merged.addAll(paths);
        return new Compiler(processors, options, classpath, List.copyOf(merged));
    }

    /**
     * Compiles the given source files and returns the result.
     *
     * @param sources in-memory source files to compile; see {@link JavaFileObjects}
     * @throws IllegalStateException if no system Java compiler is available
     */
    public Compilation compile(final JavaFileObject... sources) {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException(
                "no system Java compiler available; ensure tools.jar / JDK (not JRE) is on the module path");
        }

        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        final StandardJavaFileManager standardFileManager =
            compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8);

        final List<String> allOptions = buildOptions();
        final List<JavaFileObject> sourceList = List.of(sources);

        try (InMemoryJavaFileManager fileManager = new InMemoryJavaFileManager(standardFileManager)) {
            final JavaCompiler.CompilationTask task = compiler.getTask(
                Writer.nullWriter(),
                fileManager,
                diagnostics,
                allOptions.isEmpty() ? null : allOptions,
                null,
                sourceList);

            if (!processors.isEmpty()) {
                task.setProcessors(processors);
            }

            final boolean success = task.call();

            return new Compilation(
                success ? Compilation.Status.SUCCESS : Compilation.Status.FAILURE,
                sourceList,
                List.copyOf(diagnostics.getDiagnostics()),
                fileManager.generatedFiles(),
                List.copyOf(processors),
                List.copyOf(allOptions));

        } catch (final IOException e) {
            throw new IllegalStateException("error closing file manager", e);
        }
    }

    private List<String> buildOptions() {
        final List<String> all = new ArrayList<>(options);

        if (!modulePath.isEmpty()) {
            all.add("--module-path");
            all.add(joinPaths(modulePath));
        }

        if (!classpath.isEmpty()) {
            all.add("--class-path");
            all.add(joinPaths(classpath));
        }

        return all;
    }

    private static String joinPaths(final List<Path> paths) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paths.size(); i++) {
            if (i > 0) {
                sb.append(File.pathSeparator);
            }
            sb.append(paths.get(i).toString());
        }
        return sb.toString();
    }
}
