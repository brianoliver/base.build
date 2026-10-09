package build.base.template.processor;

import build.base.parsing.AbstractParser.Outcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.tools.Diagnostic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A malformed template reports every error that can be found, not only the first.
 *
 * @author reed.vonredwitz
 * @since Oct-2026
 */
class JtMultipleErrorsTests {

    private static final String HEADER = """
        out HtmlOut;
        package com.example;
        template T(String name) {
        """;

    private static Outcome<ParsedTemplate> parseAll(final String content) {
        return JtParser.parseAll(content, "T.jt", outType -> Syntax.DEFAULT, warning -> { });
    }

    private static List<Integer> lines(final Outcome<ParsedTemplate> outcome) {
        return outcome.errors().stream().map(e -> ((JtParseException) e).getLine()).toList();
    }

    @Test
    void shouldReportEveryMalformedBodyLine() {
        final var result = parseAll(HEADER + """
            <p>#{name</p>
            fine
            @include
            fine
            @slot row()
            @end
            """);

        assertThat(result.succeeded()).isFalse();
        assertThat(lines(result)).containsExactly(4, 6, 8);
        assertThat(result.errors().get(1)).hasMessageContaining("include requires an expression");
        assertThat(result.errors().get(2)).hasMessageContaining("not supported yet");
    }

    @Test
    void shouldReportBodyErrorsBeforeAMissingEnd() {
        final var result = parseAll(HEADER + """
            @include
            <p>x</p>
            """);

        assertThat(result.errors()).hasSize(2);
        assertThat(result.errors().get(0)).hasMessageContaining("include requires an expression");
        assertThat(result.errors().get(1)).hasMessageContaining("missing @end");
    }

    @Test
    void shouldReportBodyErrorsBeforeContentAfterEnd() {
        final var result = parseAll(HEADER + """
            @include
            @end
            trailing
            """);

        assertThat(result.errors()).hasSize(2);
        assertThat(result.errors().get(1)).hasMessageContaining("unexpected content after @end");
    }

    @Test
    void shouldNotSucceedWhenOnlyBodyLinesAreMalformedAndTheTemplateEndsCleanly() {
        final var result = parseAll(HEADER + """
            @include
            @end
            """);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.errors()).hasSize(1);
        assertThat(lines(result)).containsExactly(4);
    }

    @Test
    void shouldStopAtABrokenHeaderWithoutCascading() {
        final var result = parseAll("""
            out HtmlOut;
            package com.example
            template T() {
            @include
            @end
            """);

        assertThat(result.errors()).hasSize(1);
        assertThat(lines(result)).containsExactly(3);
        assertThat(result.value()).isEmpty();
    }

    @Test
    void shouldSucceedForAWellFormedTemplate() {
        final var result = parseAll(HEADER + "<p>#{name}</p>\n@end\n");

        assertThat(result.succeeded()).isTrue();
        assertThat(result.value().orElseThrow().className()).isEqualTo("T");
    }

    @Test
    void shouldStillThrowOnlyTheFirstErrorFromParse() {
        assertThatThrownBy(() -> JtParser.parse(HEADER + "@include\n<p>#{name</p>\n@end\n", "T.jt"))
            .isInstanceOfSatisfying(JtParseException.class, e -> assertThat(e.getLine()).isEqualTo(4));
    }

    @Test
    void shouldReportEveryErrorOfATemplateAsACompilerDiagnostic(@TempDir final Path dir) throws IOException {
        final var result = ProcessorHarness.run(dir,
            Map.of("com/acme/Bad.jt", "out HtmlOut;\ntemplate Bad() {\n@include\nok\n<p>#{x</p>\n@end\n"),
            Map.of());

        assertThat(result.success()).isFalse();
        assertThat(result.messages(Diagnostic.Kind.ERROR))
            .hasSize(2)
            .satisfiesExactly(
                first -> assertThat(first).contains("Bad.jt:3:1: ").contains("include requires an expression"),
                second -> assertThat(second).contains("Bad.jt:5:4: ").contains("unclosed '#{'"));
    }
}
