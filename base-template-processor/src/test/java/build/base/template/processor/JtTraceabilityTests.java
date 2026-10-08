package build.base.template.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the link from generated code and compiler diagnostics back to the {@code .jt} source.
 *
 * @author reed.vonredwitz
 * @since Oct-2026
 */
class JtTraceabilityTests {

    private static final String TEMPLATE = """
        out HtmlOut;
        template T(String name) {
        <h1>
          Hello #{name}
        </h1>
        @if (name.isEmpty()) {
        <p>empty</p>
        @}
        @end
        """;

    @Test
    void shouldCommentGeneratedStatementsWithTheirTemplateLine() {
        final String source = CodeGenerator.generate(JtParser.parse(TEMPLATE, "/work/src/main/jt/T.jt"));

        assertThat(source).contains("""
                    // T.jt:3
                    out.raw("<h1>\\n  Hello ");
                    // T.jt:4
                    out.write(name);
                    out.raw("\\n</h1>\\n");
                    // T.jt:6
                    if (name.isEmpty()) {
                    // T.jt:7
                    out.raw("<p>empty</p>\\n");
                    // T.jt:8
                    }
            """.replaceAll("(?m)^ {12}", "        "));
    }

    @Test
    void shouldOmitCommentsWhenLinesAreUnknown() {
        final var template = new ParsedTemplate("p", java.util.List.of(), "HtmlOut", "T", "",
            java.util.List.of(new BodyNode.RawText("x\n")));

        assertThat(CodeGenerator.generate(template)).doesNotContain("//");
    }

    @Test
    void shouldReportParseErrorsAsFileLineColumn(@TempDir final Path dir) throws IOException {
        final var result = ProcessorHarness.run(dir,
            Map.of("com/acme/Bad.jt", "out HtmlOut;\ntemplate Bad() {\nhello #{name\n@end\n"),
            Map.of());

        assertThat(result.success()).isFalse();
        assertThat(result.messages(javax.tools.Diagnostic.Kind.ERROR))
            .singleElement()
            .asString()
            .contains("Bad.jt:3:7: unclosed '#{' expression")
            .doesNotContain("at line");
    }
}
