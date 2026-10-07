package build.base.template.processor;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Error-path and edge-case tests for {@link JtParser}: messages must be informative (never {@code null}) and
 * malformed templates must be rejected rather than silently accepted.
 */
class JtParserErrorTests {

    private static final String HEADER = """
        out HtmlOut;
        package com.example;
        template T(String name) {
        """;

    // --- 1.1: messages and positions ---

    @Test
    void unclosedExpressionHasInformativeMessage() {
        assertThatThrownBy(() -> JtParser.parseTextLine("<p>#{name</p>\n", new ArrayList<>()))
            .isInstanceOf(JtParseException.class)
            .satisfies(e -> assertThat(e.getMessage()).doesNotContain("null"));
    }

    @Test
    void unclosedExpressionInTemplateNamesFileAndHasInformativeMessage() {
        assertThatThrownBy(() -> JtParser.parse(HEADER + "<p>#{name</p>\n@end\n", "hello.jt"))
            .isInstanceOf(JtParseException.class)
            .satisfies(e -> assertThat(e.getMessage()).doesNotContain("null"));
    }

    @Test
    void unclosedExpressionReportsColumnOfHash() {
        assertThatThrownBy(() -> JtParser.parseTextLine("<p>#{name</p>\n", new ArrayList<>()))
            .isInstanceOfSatisfying(JtParseException.class, e -> assertThat(e.getColumn()).isEqualTo(4));
    }

    @Test
    void missingSemicolonAfterPackageReportsPosition() {
        assertThatThrownBy(() -> JtParser.parse("""
            out HtmlOut;
            package com.example
            template T() {
            @end
            """, "hello.jt"))
            .isInstanceOf(JtParseException.class)
            .hasMessageContaining("hello.jt")
            .hasMessageContaining("line 3")
            .satisfies(e -> assertThat(e.getMessage()).doesNotContain("null"));
    }

    @Test
    void malformedHeaderReportsPosition() {
        assertThatThrownBy(() -> JtParser.parse("""
            out HtmlOut;
            package com.example;
            template (String name) {
            @end
            """, "hello.jt"))
            .isInstanceOf(JtParseException.class)
            .hasMessageContaining("line 3")
            .satisfies(e -> assertThat(e.getMessage()).doesNotContain("null"));
    }

    @Test
    void unbalancedParametersAreReported() {
        assertThatThrownBy(() -> JtParser.parse("""
            out HtmlOut;
            package com.example;
            template T(String name {
            <p>hi</p>
            @end
            """, "hello.jt"))
            .isInstanceOf(JtParseException.class)
            .satisfies(e -> assertThat(e.getMessage()).doesNotContain("null"));
    }

    // --- 1.2: malformed templates must be rejected ---

    @Test
    void missingEndIsAnError() {
        assertThatThrownBy(() -> JtParser.parse(HEADER + "<p>hi</p>\n", "t.jt"))
            .isInstanceOf(JtParseException.class)
            .hasMessageContaining("@end");
    }

    @Test
    void contentAfterEndIsAnError() {
        assertThatThrownBy(() -> JtParser.parse(HEADER + """
            <p>hi</p>
            @end
            template Second() {
            <p>second</p>
            @end
            """, "t.jt"))
            .isInstanceOf(JtParseException.class);
    }

    @Test
    void missingOpeningBraceIsAnError() {
        assertThatThrownBy(() -> JtParser.parse("""
            out HtmlOut;
            package com.example;
            template T()
            <p>hi</p>
            @end
            """, "t.jt"))
            .isInstanceOf(JtParseException.class)
            .hasMessageContaining("{");
    }

    @Test
    void garbageAfterParametersIsAnError() {
        assertThatThrownBy(() -> JtParser.parse("""
            out HtmlOut;
            package com.example;
            template T() garbage {
            <p>hi</p>
            @end
            """, "t.jt"))
            .isInstanceOf(JtParseException.class);
    }

    @Test
    void bareIncludeIsAnError() {
        assertThatThrownBy(() -> JtParser.parse(HEADER + "@include\n@end\n", "t.jt"))
            .isInstanceOf(JtParseException.class)
            .hasMessageContaining("@include");
    }

    @Test
    void includeWithOnlyWhitespaceIsAnError() {
        assertThatThrownBy(() -> JtParser.parse(HEADER + "@include   \n@end\n", "t.jt"))
            .isInstanceOf(JtParseException.class)
            .hasMessageContaining("@include");
    }

    // --- 1.9: edge cases in expressions and line endings ---

    @Test
    void expressionStringLiteralMayContainClosingBrace() {
        final var body = new ArrayList<BodyNode>();
        JtParser.parseTextLine("<p>#{ map.get(\"}\") }</p>\n", body);

        assertThat(body).containsExactly(
            new BodyNode.RawText("<p>"),
            new BodyNode.Expression(" map.get(\"}\") "),
            new BodyNode.RawText("</p>\n"));
    }

    @Test
    void expressionCommentMayContainClosingBrace() {
        final var body = new ArrayList<BodyNode>();
        JtParser.parseTextLine("<p>#{ name /* } */ }</p>\n", body);

        assertThat(body).containsExactly(
            new BodyNode.RawText("<p>"),
            new BodyNode.Expression(" name /* } */ "),
            new BodyNode.RawText("</p>\n"));
    }

    @Test
    void crlfTemplateParsesDirectivesAndKeepsTextLineEndings() {
        final var result = JtParser.parse(
            "out HtmlOut;\r\n"
            + "package com.example;\r\n"
            + "template T(java.util.List<String> xs) {\r\n"
            + "<p>hi</p>\r\n"
            + "@for (var x : xs) {\r\n"
            + "@}\r\n"
            + "@end\r\n", "t.jt");

        assertThat(result.params()).isEqualTo("java.util.List<String> xs");
        assertThat(result.body()).containsExactly(
            new BodyNode.RawText("<p>hi</p>\r\n"),
            new BodyNode.CodeLine("for (var x : xs) {"),
            new BodyNode.CodeLine("}"));
    }
}
