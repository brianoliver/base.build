package build.base.template.processor;

/*-
 * #%L
 * base.build Template Processor
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;

@SupportedAnnotationTypes("build.base.template.ProcessTemplates")
@SupportedOptions("jt.sourceDir")
public final class TemplateProcessor extends AbstractProcessor {

    private boolean processed = false;

    /** The {@code .jt} file that produced each generated class, to detect two templates generating the same one. */
    private final Map<String, Path> generated = new HashMap<>();

    /** The templates that were generated, by the name of the class, to check includes against the templates they name. */
    private final Map<String, ParsedTemplate> templates = new TreeMap<>();

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations,
                           final RoundEnvironment roundEnv) {
        if (processed || roundEnv.processingOver()) {
            return false;
        }
        processed = true;

        final Path jtSourceDir = resolveJtSourceDir();
        if (jtSourceDir == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                "base-template-processor: could not determine jt source directory; "
                + "set -Ajt.sourceDir=<path> if your layout is non-standard");
            return false;
        }
        if (!Files.isDirectory(jtSourceDir)) {
            return false;
        }

        try (Stream<Path> files = Files.walk(jtSourceDir)) {
            files.filter(p -> p.toString().endsWith(".jt"))
                .sorted()
                .forEach(jtFile -> processTemplate(jtSourceDir, jtFile));
        } catch (final IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                "base-template-processor: failed to walk " + jtSourceDir + ": " + e.getMessage());
        }

        warnAboutFillsThatFillNothing();

        return false;
    }

    /**
     * Warns about each fill of an include of a template with a {@code Slots} parameter that is not a slot of that
     * template, which renders nothing. This is a warning, as the template may render the part some other way (as in
     * {@code @java slots.render(name, out);}), and an include of a type that is no template of this build is not checked.
     */
    private void warnAboutFillsThatFillNothing() {
        for (final ParsedTemplate page : templates.values()) {
            final List<BodyNode> body = page.body();
            for (int i = 0; i < body.size(); i++) {
                if (!(body.get(i) instanceof BodyNode.IncludeStart(final String type, final String args))) {
                    continue;
                }
                final ParsedTemplate layout = templateNamed(page, type);
                final Set<String> slots = layout != null ? CodeGenerator.fillableSlots(layout)
                    : compiledSlots(page, type);
                if (slots == null) {
                    continue;
                }
                final String layoutName = layout != null ? layout.className() : type;
                int nested = 0;
                for (int j = i + 1; j < body.size() && nested >= 0; j++) {
                    final BodyNode node = body.get(j);
                    if (node instanceof BodyNode.IncludeStart) {
                        nested++;
                    } else if (node instanceof BodyNode.IncludeEnd) {
                        nested--;
                    } else if (nested == 0 && node instanceof BodyNode.FillStart(final String name)
                               && !slots.contains(name)) {
                        final int line = j < page.lines().size() ? page.lines().get(j) : 0;
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                            "base-template-processor: " + page.sourceFile() + (line > 0 ? ":" + line : "")
                            + ": fill " + name + " is not a slot of " + layoutName + " (its slots are "
                            + (slots.isEmpty() ? "none" : String.join(", ", slots)) + ")"
                            + didYouMean(name, slots) + ", so it renders nothing");
                    }
                }
            }
        }
    }

    /**
     * The named slots of a layout that is compiled already, such as one from another module, which its generated class
     * lists in a constant, or {@code null} if the type is not found or is not such a layout.
     */
    private Set<String> compiledSlots(final ParsedTemplate from, final String type) {
        for (final String candidate : candidateNames(from, type)) {
            final TypeElement element = processingEnv.getElementUtils().getTypeElement(candidate);
            if (element == null) {
                continue;
            }
            for (final Element member : element.getEnclosedElements()) {
                if (member instanceof VariableElement field
                    && field.getSimpleName().contentEquals(CodeGenerator.SLOTS_CONSTANT)
                    && field.getModifiers().contains(Modifier.STATIC)
                    && field.getConstantValue() instanceof String names) {
                    final Set<String> slots = new TreeSet<>();
                    for (final String name : names.split(",")) {
                        if (!name.isEmpty()) {
                            slots.add(name);
                        }
                    }
                    return slots;
                }
            }
        }
        return null;
    }

    /** A hint at the slot a misspelt fill was likely meant for: the slot one or two edits away, or nothing. */
    private static String didYouMean(final String name, final Set<String> slots) {
        String best = null;
        int bestDistance = 3;
        for (final String slot : slots) {
            final int distance = editDistance(name, slot);
            if (distance < bestDistance) {
                best = slot;
                bestDistance = distance;
            }
        }
        return best == null ? "" : "; did you mean " + best + "?";
    }

    private static int editDistance(final String a, final String b) {
        int[] previous = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            final int[] current = new int[b.length() + 1];
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                final int substitution = previous[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                current[j] = Math.min(substitution, Math.min(previous[j], current[j - 1]) + 1);
            }
            previous = current;
        }
        return previous[b.length()];
    }

    /**
     * The template that a type named in an include of {@code from} is, or {@code null} if it is none of those that were
     * generated: the type as written, or else in the package of {@code from} or one of its imports.
     */
    private ParsedTemplate templateNamed(final ParsedTemplate from, final String type) {
        for (final String candidate : candidateNames(from, type)) {
            final ParsedTemplate template = templates.get(candidate);
            if (template != null) {
                return template;
            }
        }
        return null;
    }

    /** The qualified names a type named in an include of {@code from} can be: as written, or else in its package or imports. */
    private static List<String> candidateNames(final ParsedTemplate from, final String type) {
        final List<String> candidates = new ArrayList<>();
        if (type.contains(".")) {
            candidates.add(type);
        } else {
            candidates.add(from.packageName().isEmpty() ? type : from.packageName() + "." + type);
            for (final String declaration : from.imports()) {
                final String imported = declaration.substring("import ".length()).trim();
                if (imported.endsWith("." + type)) {
                    candidates.add(imported);
                } else if (imported.endsWith(".*")) {
                    candidates.add(imported.substring(0, imported.length() - 1) + type);
                }
            }
        }
        return candidates;
    }

    /**
     * Generates the class for one template. Failures are reported as compiler diagnostics naming the {@code .jt}
     * file, so that the remaining templates are still processed.
     */
    private void processTemplate(final Path jtSourceDir, final Path jtFile) {
        try {
            final var outcome = JtParser.parseAll(Files.readString(jtFile), jtFile.toString(),
                                                  this::outSyntax,
                                                  message -> processingEnv.getMessager()
                                                      .printMessage(Diagnostic.Kind.WARNING,
                                                                    "base-template-processor: " + message));
            if (!outcome.succeeded()) {
                // Every error from JtParser.parseAll is a JtParseException
                outcome.errors().forEach(e -> error(((JtParseException) e).diagnostic()));
                return;
            }
            final ParsedTemplate declared = outcome.value().orElseThrow();
            final ParsedTemplate parsed = declared.packageName().isEmpty()
                ? declared.withPackageName(packageOf(jtSourceDir, jtFile))
                : declared;
            final String className = parsed.qualifiedClassName();

            final Path existing = generated.get(className);
            if (existing != null) {
                error("duplicate template class " + className + ": generated by both " + existing
                    + " and " + jtFile);
                return;
            }

            templates.put(className, parsed);

            warnIfPathDoesNotMatchPackage(jtSourceDir, jtFile, parsed);

            final String source = CodeGenerator.generate(parsed);
            final javax.tools.JavaFileObject output = processingEnv.getFiler().createSourceFile(className);
            generated.put(className, jtFile);

            try (var writer = output.openWriter()) {
                writer.write(source);
            }
        } catch (final IOException | UncheckedIOException e) {
            error(jtFile + ": failed to generate class: " + e.getMessage());
        } catch (final JtParseException e) {
            error(e.diagnostic());
        }
    }

    /** The package implied by a template's directory under the source root, used when it declares none. */
    private static String packageOf(final Path jtSourceDir, final Path jtFile) {
        final Path directory = jtSourceDir.relativize(jtFile).getParent();
        if (directory == null) {
            return "";
        }
        final StringJoiner name = new StringJoiner(".");
        directory.forEach(part -> name.add(part.toString()));
        return name.toString();
    }

    /**
     * The default syntax declared by an out type through {@code build.base.template.OutSyntax}, or
     * {@link Syntax#DEFAULT} when the type is not found or declares none. A simple name resolves against
     * {@code build.base.template}.
     */
    private Syntax outSyntax(final String outType) {
        final var elements = processingEnv.getElementUtils();
        final TypeElement type = elements.getTypeElement(
            outType.contains(".") ? outType : "build.base.template." + outType);
        if (type == null) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.WARNING,
                "out type " + outType + " not found; using the default template syntax");
            return Syntax.DEFAULT;
        }
        String prefix = Syntax.DEFAULT.prefix();
        String interpolation = Syntax.DEFAULT.interpolation();
        // getAllAnnotationMirrors includes annotations inherited from superclasses
        for (final AnnotationMirror mirror : elements.getAllAnnotationMirrors(type)) {
            final TypeElement annotation = (TypeElement) mirror.getAnnotationType().asElement();
            if (annotation.getQualifiedName().contentEquals("build.base.template.OutSyntax")) {
                for (final var entry : elements.getElementValuesWithDefaults(mirror).entrySet()) {
                    final String value = (String) entry.getValue().getValue();
                    switch (entry.getKey().getSimpleName().toString()) {
                        case "prefix" -> prefix = value;
                        case "interpolation" -> interpolation = value;
                        default -> {
                        }
                    }
                }
                break;
            }
        }
        return new Syntax(prefix, interpolation, contextsOf(type));
    }

    /**
     * The output contexts declared by the methods of an out type (including inherited ones) through
     * {@code build.base.template.OutContext}, as context name to method name.
     */
    private Map<String, String> contextsOf(final TypeElement type) {
        final var elements = processingEnv.getElementUtils();
        final Map<String, String> contexts = new TreeMap<>();
        for (final Element member : elements.getAllMembers(type)) {
            if (member.getKind() != ElementKind.METHOD) {
                continue;
            }
            final AnnotationMirror mirror = outContextOf((ExecutableElement) member);
            if (mirror != null) {
                final String method = member.getSimpleName().toString();
                String name = method;
                for (final var entry : elements.getElementValuesWithDefaults(mirror).entrySet()) {
                    final String value = (String) entry.getValue().getValue();
                    if (!value.isEmpty()) {
                        name = value;
                    }
                }
                final var executable = (ExecutableElement) member;
                if (!SourceVersion.isIdentifier(name) || SourceVersion.isKeyword(name)
                    || Syntax.RESERVED_CONTEXTS.contains(name)) {
                    // an error, not a warning: an ignored context would leave its opener in the output as text
                    processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "base-template-processor: " + type.getQualifiedName() + "#" + method
                        + " declares output context '" + name + "', which is not a usable name "
                        + "(it must be a Java identifier other than raw, write or include)");
                } else if (!member.getModifiers().contains(Modifier.PUBLIC)
                           || member.getModifiers().contains(Modifier.STATIC)
                           || executable.getParameters().size() != 1) {
                    processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "base-template-processor: " + type.getQualifiedName() + "#" + method
                        + " declares output context '" + name
                        + "' but is not a public instance method with one parameter");
                } else if (contexts.containsKey(name) && !contexts.get(name).equals(method)) {
                    processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "base-template-processor: output context '" + name + "' of " + type.getQualifiedName()
                        + " is declared by both " + contexts.get(name) + " and " + method);
                } else {
                    contexts.put(name, method);
                }
            }
        }
        return contexts;
    }

    /**
     * The {@code build.base.template.OutContext} annotation of a method, or {@code null}. An override that is not
     * itself annotated inherits the annotation of the method it overrides, as {@code getAllMembers} only returns the
     * overriding method.
     */
    private AnnotationMirror outContextOf(final ExecutableElement method) {
        for (final AnnotationMirror mirror : method.getAnnotationMirrors()) {
            final TypeElement annotation = (TypeElement) mirror.getAnnotationType().asElement();
            if (annotation.getQualifiedName().contentEquals("build.base.template.OutContext")) {
                return mirror;
            }
        }
        return inheritedOutContext(method, (TypeElement) method.getEnclosingElement());
    }

    /**
     * Searches the supertypes of {@code type} (transitively) for an annotated method that {@code method} overrides.
     */
    private AnnotationMirror inheritedOutContext(final ExecutableElement method, final TypeElement type) {
        final var elements = processingEnv.getElementUtils();
        final var types = processingEnv.getTypeUtils();
        final var owner = (TypeElement) method.getEnclosingElement();
        for (final var supertype : types.directSupertypes(type.asType())) {
            final var supertypeElement = (TypeElement) types.asElement(supertype);
            for (final Element candidate : supertypeElement.getEnclosedElements()) {
                if (candidate.getKind() == ElementKind.METHOD
                    && elements.overrides(method, (ExecutableElement) candidate, owner)) {
                    final AnnotationMirror inherited = outContextOf((ExecutableElement) candidate);
                    if (inherited != null) {
                        return inherited;
                    }
                }
            }
            final AnnotationMirror further = inheritedOutContext(method, supertypeElement);
            if (further != null) {
                return further;
            }
        }
        return null;
    }

    private void warnIfPathDoesNotMatchPackage(final Path jtSourceDir,
                                               final Path jtFile,
                                               final ParsedTemplate parsed) {
        final Path directory = jtSourceDir.relativize(jtFile).getParent();
        final Path expected = Path.of(parsed.packageName().replace('.', '/'));
        if (!expected.equals(directory == null ? Path.of("") : directory)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                "base-template-processor: " + jtFile + ": package '" + parsed.packageName()
                + "' does not match the file's directory under " + jtSourceDir);
        }
    }

    private void error(final String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "base-template-processor: " + message);
    }

    private Path resolveJtSourceDir() {
        final String explicit = processingEnv.getOptions().get("jt.sourceDir");
        if (explicit != null) {
            return Path.of(explicit);
        }
        try {
            final javax.tools.FileObject probe = processingEnv.getFiler()
                .createResource(StandardLocation.CLASS_OUTPUT, "", ".base-template-probe");
            final URI uri = probe.toUri();
            probe.delete();

            // Walk up from the class output directory until we find a parent that contains src/main/jt.
            // This handles both Maven (target/classes) and spin (.build/main/target) layouts.
            Path dir = Path.of(uri).getParent();
            while (dir != null) {
                final Path candidate = dir.resolve("src/main/jt");
                if (Files.isDirectory(candidate)) {
                    return candidate;
                }
                dir = dir.getParent();
            }
            return null;
        } catch (final IOException e) {
            return null;
        }
    }
}
