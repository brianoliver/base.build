package build.base.template.processor;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JtParserTests {

    @Test
    void shouldParseMinimalTemplate() {
        final var result = JtParser.parse("""
            out HtmlOut;
            package com.example;

            template HelloTemplate(String name) {
            <h1>Hello</h1>
            @end
            """, "hello.jt");

        assertThat(result.packageName()).isEqualTo("com.example");
        assertThat(result.outType()).isEqualTo("HtmlOut");
        assertThat(result.className()).isEqualTo("HelloTemplate");
        assertThat(result.params()).isEqualTo("String name");
    }

    @Test
    void shouldParseTemplateWithoutPackage() {
        final var result = JtParser.parse("""
            out TextOut;
            template T() {
            @end
            """, "t.jt");

        assertThat(result.packageName()).isEmpty();
        assertThat(result.outType()).isEqualTo("TextOut");
    }

    @Test
    void shouldParseQualifiedOutType() {
        final var result = JtParser.parse("""
            out com.acme.MyOut;
            template T() {
            @end
            """, "t.jt");

        assertThat(result.outType()).isEqualTo("com.acme.MyOut");
    }

    @Test
    void shouldParseImports() {
        final var result = JtParser.parse("""
            out HtmlOut;
            package com.example;

            import java.util.List;
            import com.example.Task;

            template TasksTemplate(List<Task> tasks) {
            @end
            """, "tasks.jt");

        assertThat(result.imports()).containsExactly("import java.util.List", "import com.example.Task");
    }

    @Test
    void shouldParseTextLine() {
        final var body = new ArrayList<BodyNode>();
        JtParser.parseTextLine("<h1>Hello</h1>\n", body);

        assertThat(body).containsExactly(new BodyNode.RawText("<h1>Hello</h1>\n"));
    }

    @Test
    void shouldParseExpressionInTextLine() {
        final var body = new ArrayList<BodyNode>();
        JtParser.parseTextLine("<h1>#{name}</h1>\n", body);

        assertThat(body).containsExactly(
            new BodyNode.RawText("<h1>"),
            new BodyNode.Expression("name"),
            new BodyNode.RawText("</h1>\n")
        );
    }

    @Test
    void shouldParseMultipleExpressionsOnOneLine() {
        final var body = new ArrayList<BodyNode>();
        JtParser.parseTextLine("<li id=\"#{task.id()}\">#{task.title()}</li>\n", body);

        assertThat(body).containsExactly(
            new BodyNode.RawText("<li id=\""),
            new BodyNode.Expression("task.id()"),
            new BodyNode.RawText("\">"),
            new BodyNode.Expression("task.title()"),
            new BodyNode.RawText("</li>\n")
        );
    }

    @Test
    void shouldParseExpressionWithStringLiteralContainingBrace() {
        final var body = new ArrayList<BodyNode>();
        JtParser.parseTextLine("#{task.done() ? \" done\" : \"\"}\n", body);

        assertThat(body).containsExactly(
            new BodyNode.Expression("task.done() ? \" done\" : \"\""),
            new BodyNode.RawText("\n")
        );
    }

    @Test
    void shouldParseCodeLine() {
        final var result = JtParser.parse("""
            out HtmlOut;
            package com.example;
            template T(java.util.List<String> items) {
            @for (var item : items) {
            <li>#{item}</li>
            @}
            @end
            """, "t.jt");
        assertThat(result.body()).contains(new BodyNode.CodeLine("for (var item : items) {"));
        assertThat(result.body()).contains(new BodyNode.CodeLine("}"));
    }

    @Test
    void shouldParseInclude() {
        final var result = JtParser.parse("""
            out HtmlOut;
            package com.example;
            template T(Object item) {
            @include new ItemTemplate(item)
            @end
            """, "t.jt");
        assertThat(result.body()).contains(new BodyNode.Include("new ItemTemplate(item)"));
    }

    @Test
    void shouldParseWildcardImport() {
        final var result = JtParser.parse("""
            out HtmlOut;
            package com.example;
            import java.util.*;
            template T() {
            @end
            """, "t.jt");
        assertThat(result.imports()).containsExactly("import java.util.*");
    }

    @Test
    void shouldParseStaticImport() {
        final var result = JtParser.parse("""
            out HtmlOut;
            package com.example;
            import static java.util.List.of;
            template T() {
            @end
            """, "t.jt");
        assertThat(result.imports()).containsExactly("import static java.util.List.of");
    }

    @Test
    void shouldParseStaticWildcardImport() {
        final var result = JtParser.parse("""
            out HtmlOut;
            package com.example;
            import static java.util.Collections.*;
            template T() {
            @end
            """, "t.jt");
        assertThat(result.imports()).containsExactly("import static java.util.Collections.*");
    }

    @Test
    void shouldPreserveBareClosingBraceInBody() {
        final var result = JtParser.parse("""
            out HtmlOut;
            package com.example;
            template T() {
            <style>
            body {
                color: red;
            }
            </style>
            <script>
            function x() {
                return 1;
            }
            </script>
            @end
            """, "t.jt");

        assertThat(result.body()).contains(
            new BodyNode.RawText("body {\n"),
            new BodyNode.RawText("}\n"),
            new BodyNode.RawText("function x() {\n"),
            new BodyNode.RawText("</script>\n"));
    }

    @Test
    void shouldThrowOnMissingDeclaration() {
        assertThatThrownBy(() -> JtParser.parse("out HtmlOut;\npackage com.example;", "bad.jt"))
            .isInstanceOf(JtParseException.class)
            .hasMessageContaining("missing template declaration");
    }

    @Test
    void shouldThrowOnMissingOutDeclaration() {
        assertThatThrownBy(() -> JtParser.parse("""
            package com.example;
            template T() {
            @end
            """, "bad.jt"))
            .isInstanceOf(JtParseException.class)
            .hasMessageContaining("missing 'out' declaration")
            .hasMessageContaining("line 1");
    }

    @Test
    void shouldRejectOutDeclarationAfterPackage() {
        assertThatThrownBy(() -> JtParser.parse("""
            package com.example;
            out HtmlOut;
            template T() {
            @end
            """, "bad.jt"))
            .isInstanceOf(JtParseException.class)
            .hasMessageContaining("missing 'out' declaration");
    }

    @Test
    void shouldRejectOptionBeforeOutDeclaration() {
        assertThatThrownBy(() -> JtParser.parse("""
            option prefix = "%";
            out HtmlOut;
            template T() {
            %end
            """, "bad.jt"))
            .isInstanceOf(JtParseException.class)
            .hasMessageContaining("missing 'out' declaration");
    }

    @Test
    void shouldRejectOptionAfterPackage() {
        assertThatThrownBy(() -> JtParser.parse("""
            out HtmlOut;
            package com.example;
            option prefix = "%";
            template T() {
            @end
            """, "bad.jt"))
            .isInstanceOf(JtParseException.class)
            .hasMessageContaining("'option' must come directly after the 'out' declaration")
            .hasMessageContaining("line 3");
    }
}
