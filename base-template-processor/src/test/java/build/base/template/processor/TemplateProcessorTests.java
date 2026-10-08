package build.base.template.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Drives the processor through {@code javac} to cover behaviour that cannot be tested from
 * {@code base-template-test} (a broken template would break that module's own build).
 *
 * @author reed.vonredwitz
 * @since Oct-2026
 */
class TemplateProcessorTests {

    private static final String MY_OUT = """
        package com.acme;
        
        public final class MyOut extends build.base.template.Out {
            @Override
            public void write(final Object value) {
                raw(String.valueOf(value));
            }
        }
        """;

    @Test
    void shouldCompileTemplateUsingOutFromAnotherPackage(@TempDir final Path dir) throws IOException {
        final var result = ProcessorHarness.run(dir,
            Map.of("com/acme/Greeting.jt", """
                out com.acme.MyOut;
                package com.acme;
                
                template Greeting(String name) {
                hello #{name}
                @end
                """),
            Map.of("com/acme/MyOut.java", MY_OUT));

        assertThat(result.success()).as(result.allMessages()).isTrue();
        assertThat(result.classes().resolve("com/acme/Greeting.class")).exists();
    }

    @Test
    void shouldInferPackageFromDirectoryWhenNoneDeclared(@TempDir final Path dir) throws IOException {
        final var result = ProcessorHarness.run(dir,
            Map.of("com/acme/Inferred.jt", "out HtmlOut;\ntemplate Inferred() {\n<p>x</p>\n@end\n"),
            Map.of());

        assertThat(result.success()).as(result.allMessages()).isTrue();
        assertThat(result.classes().resolve("com/acme/Inferred.class")).exists();
        assertThat(result.diagnostics()).noneSatisfy(d -> assertThat(d.getMessage(null)).contains("Inferred.jt"));
    }

    @Test
    void shouldReadDefaultSyntaxFromOutSyntaxAnnotationAndInheritIt(@TempDir final Path dir) throws IOException {
        final var result = ProcessorHarness.run(dir,
            Map.of("com/acme/Greeting.jt", """
                out com.acme.ChildOut;
                template Greeting(String name) {
                @Keep ${name}
                %java out.raw("!");
                %end
                """),
            Map.of("com/acme/PctOut.java", """
                    package com.acme;
                    
                    @build.base.template.OutSyntax(prefix = "%", interpolation = "${")
                    public class PctOut extends build.base.template.Out {
                        @Override
                        public void write(final Object value) {
                            raw(String.valueOf(value));
                        }
                    }
                    """,
                "com/acme/ChildOut.java", "package com.acme;\npublic final class ChildOut extends PctOut {}\n"));

        assertThat(result.success()).as(result.allMessages()).isTrue();
        assertThat(java.nio.file.Files.readString(result.generated().resolve("com/acme/Greeting.java")))
            .contains("out.write(name)")
            .contains("@Keep ");
    }

    @Test
    void shouldWarnWhenOutTypeIsNotFoundAndFallBackToDefaultSyntax(@TempDir final Path dir) throws IOException {
        final var result = ProcessorHarness.run(dir,
            Map.of("com/acme/Greeting.jt", """
                out com.acme.Missing;
                template Greeting() {
                <p>hi</p>
                @end
                """),
            Map.of());

        assertThat(result.messages(Diagnostic.Kind.WARNING))
            .anySatisfy(m -> assertThat(m).contains("com.acme.Missing").contains("default template syntax"));
        // The template is still generated with the default syntax; javac then rejects the unresolved import
        assertThat(result.generated().resolve("com/acme/Greeting.java")).exists();
        assertThat(result.success()).isFalse();
    }

    @Test
    void shouldImportQualifiedOutTypeAsWritten() {
        final var source = CodeGenerator.generate(JtParser.parse("""
            out com.acme.MyOut;
            package com.example;
            template T() {
            @end
            """, "t.jt"));

        assertThat(source).contains("import com.acme.MyOut;");
        assertThat(source).doesNotContain("build.base.template.com.acme");
    }

    @Test
    void shouldNotPinSupportedSourceVersion() {
        assertThat(TemplateProcessor.class.isAnnotationPresent(SupportedSourceVersion.class))
            .as("@SupportedSourceVersion pins the processor to one release")
            .isFalse();
        assertThat(new TemplateProcessor().getSupportedSourceVersion()).isEqualTo(SourceVersion.latestSupported());
    }

    @Test
    void shouldReportParseErrorWithRealMessage(@TempDir final Path dir) throws IOException {
        final var result = ProcessorHarness.run(dir,
            Map.of("com/acme/Broken.jt", """
                out HtmlOut;
                package com.acme;
                
                template Broken(String name) {
                <p>#{name</p>
                @end
                """),
            Map.of());

        assertThat(result.success()).isFalse();
        assertThat(result.messages(Diagnostic.Kind.ERROR))
            .anySatisfy(m -> assertThat(m).contains("Broken.jt").doesNotContain("null"));
    }

    @Test
    void shouldReportDuplicateClassNamesAsErrorsNamingBothFiles(@TempDir final Path dir) throws IOException {
        final var template = """
            out HtmlOut;
            package com.acme;
            
            template Dup() {
            <p>dup</p>
            @end
            """;

        final var result = assertThatCodeRuns(dir, Map.of(
            "one/Dup.jt", template,
            "two/Dup.jt", template));

        assertThat(result.success()).isFalse();
        final var errors = String.join("\n", result.messages(Diagnostic.Kind.ERROR));
        assertThat(errors).contains("com.acme.Dup").contains("one").contains("two");
    }

    @Test
    void shouldKeepProcessingOtherTemplatesAfterOneFails(@TempDir final Path dir) throws IOException {
        final var result = assertThatCodeRuns(dir, Map.of(
            "a/Bad.jt", "out HtmlOut;\npackage com.acme;\n\ntemplate Bad() {\n<p>#{oops</p>\n@end\n",
            "b/Good.jt", "out HtmlOut;\npackage com.acme;\n\ntemplate Good() {\n<p>ok</p>\n@end\n"));

        assertThat(result.messages(Diagnostic.Kind.ERROR)).anySatisfy(m -> assertThat(m).contains("Bad.jt"));
        assertThat(result.generated().resolve("com/acme/Good.java")).exists();
    }

    @Test
    void shouldFlagTemplateWhosePathDoesNotMatchItsPackage(@TempDir final Path dir) throws IOException {
        final var result = ProcessorHarness.run(dir,
            Map.of("wrong/place/Foo.jt", "out HtmlOut;\npackage com.acme;\n\ntemplate Foo() {\n<p>x</p>\n@end\n"),
            Map.of());

        assertThat(result.diagnostics())
            .anySatisfy(d -> assertThat(d.getMessage(null)).contains("Foo.jt").containsIgnoringCase("package"));
    }

    @Test
    void shouldNotFlagTemplateWhosePathMatchesItsPackage(@TempDir final Path dir) throws IOException {
        final var result = ProcessorHarness.run(dir,
            Map.of("com/acme/Foo.jt", "out HtmlOut;\npackage com.acme;\n\ntemplate Foo() {\n<p>x</p>\n@end\n"),
            Map.of());

        assertThat(result.success()).as(result.allMessages()).isTrue();
        assertThat(result.diagnostics()).noneSatisfy(d -> assertThat(d.getMessage(null)).contains("Foo.jt"));
    }

    private static ProcessorHarness.Result assertThatCodeRuns(final Path dir, final Map<String, String> templates) {
        final ProcessorHarness.Result[] holder = new ProcessorHarness.Result[1];
        assertThatCode(() -> holder[0] = ProcessorHarness.run(dir, templates, Map.of()))
            .as("the processor must report errors rather than crash the compiler")
            .doesNotThrowAnyException();
        return holder[0];
    }
}
