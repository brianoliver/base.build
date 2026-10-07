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
                package com.acme;

                template com.acme.MyOut Greeting(String name) {
                hello #{name}
                @end
                """),
            Map.of("com/acme/MyOut.java", MY_OUT));

        assertThat(result.success()).as(result.allMessages()).isTrue();
        assertThat(result.classes().resolve("com/acme/Greeting.class")).exists();
    }

    @Test
    void shouldImportQualifiedOutTypeAsWritten() {
        final var source = CodeGenerator.generate(JtParser.parse("""
            package com.example;
            template com.acme.MyOut T() {
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
                package com.acme;

                template HtmlOut Broken(String name) {
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
            package com.acme;

            template HtmlOut Dup() {
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
            "a/Bad.jt", "package com.acme;\n\ntemplate HtmlOut Bad() {\n<p>#{oops</p>\n@end\n",
            "b/Good.jt", "package com.acme;\n\ntemplate HtmlOut Good() {\n<p>ok</p>\n@end\n"));

        assertThat(result.messages(Diagnostic.Kind.ERROR)).anySatisfy(m -> assertThat(m).contains("Bad.jt"));
        assertThat(result.generated().resolve("com/acme/Good.java")).exists();
    }

    @Test
    void shouldFlagTemplateWhosePathDoesNotMatchItsPackage(@TempDir final Path dir) throws IOException {
        final var result = ProcessorHarness.run(dir,
            Map.of("wrong/place/Foo.jt", "package com.acme;\n\ntemplate HtmlOut Foo() {\n<p>x</p>\n@end\n"),
            Map.of());

        assertThat(result.diagnostics())
            .anySatisfy(d -> assertThat(d.getMessage(null)).contains("Foo.jt").containsIgnoringCase("package"));
    }

    @Test
    void shouldNotFlagTemplateWhosePathMatchesItsPackage(@TempDir final Path dir) throws IOException {
        final var result = ProcessorHarness.run(dir,
            Map.of("com/acme/Foo.jt", "package com.acme;\n\ntemplate HtmlOut Foo() {\n<p>x</p>\n@end\n"),
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
