package build.base.template.processor;

/*-
 * #%L
 * base.build Template Processor
 * %%
 * Copyright (C) 2026 Workday, Inc.
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

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Drives {@link TemplateProcessor} through a real {@code javac} invocation over a throw-away module, so that error
 * paths (which would break the build of {@code base-template-test}) can be asserted on.
 */
final class ProcessorHarness {

    record Result(boolean success,
                  List<Diagnostic<? extends JavaFileObject>> diagnostics,
                  Path classes,
                  Path generated) {

        List<String> messages(final Diagnostic.Kind kind) {
            return diagnostics.stream()
                .filter(d -> d.getKind() == kind)
                .map(d -> d.getMessage(null))
                .toList();
        }

        String allMessages() {
            return String.join("\n", diagnostics.stream().map(d -> d.getKind() + ": " + d.getMessage(null)).toList());
        }
    }

    private ProcessorHarness() {
    }

    /**
     * @param templates {@code .jt} files, keyed by path relative to {@code src/main/jt}
     * @param sources   {@code .java} files, keyed by path relative to {@code src/main/java}
     */
    static Result run(final Path workDir,
                      final Map<String, String> templates,
                      final Map<String, String> sources) throws IOException {

        final Path jtDir = workDir.resolve("src/main/jt");
        final Path javaDir = workDir.resolve("src/main/java");
        final Path classes = workDir.resolve("classes");
        final Path generated = workDir.resolve("generated");
        Files.createDirectories(jtDir);
        Files.createDirectories(classes);
        Files.createDirectories(generated);

        for (final var template : templates.entrySet()) {
            write(jtDir.resolve(template.getKey()), template.getValue());
        }

        final List<Path> toCompile = new ArrayList<>();
        final Path moduleInfo = javaDir.resolve("module-info.java");
        write(moduleInfo, """
            @build.base.template.ProcessTemplates
            module test.templates {
                requires build.base.template;
            }
            """);
        toCompile.add(moduleInfo);
        for (final var source : sources.entrySet()) {
            final Path path = javaDir.resolve(source.getKey());
            write(path, source.getValue());
            toCompile.add(path);
        }

        final var compiler = ToolProvider.getSystemJavaCompiler();
        final var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, null)) {
            final String modulePath = String.join(java.io.File.pathSeparator,
                System.getProperty("jdk.module.path", ""),
                System.getProperty("java.class.path", ""));

            final var task = compiler.getTask(new StringWriter(), files, diagnostics,
                List.of("--module-path", modulePath,
                    "-d", classes.toString(),
                    "-s", generated.toString(),
                    "-Ajt.sourceDir=" + jtDir),
                null,
                files.getJavaFileObjectsFromPaths(toCompile));
            task.setProcessors(List.of(new TemplateProcessor()));
            final boolean success = task.call();
            return new Result(success, diagnostics.getDiagnostics(), classes, generated);
        }
    }

    private static void write(final Path path, final String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }
}
