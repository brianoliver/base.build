package build.base.template.processor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author reed.vonredwitz
 * @since Oct-2026
 */
class CodeGeneratorTests {

    @Test
    void shouldGenerateRecord() {
        final var template = new ParsedTemplate(
            "com.example",
            List.of("import java.util.List"),
            "HtmlOut",
            "HelloTemplate",
            "String name",
            List.of(
                new BodyNode.RawText("<h1>Hello, "),
                new BodyNode.Expression("name"),
                new BodyNode.RawText("!</h1>\n")
            )
        );

        final var source = CodeGenerator.generate(template);

        assertThat(source).contains("package com.example;");
        assertThat(source).contains("import java.util.List;");
        assertThat(source).contains("import build.base.template.HtmlOut;");
        assertThat(source).contains("import build.base.template.Template;");
        assertThat(source).contains("public record HelloTemplate(String name) implements Template<HtmlOut>");
        assertThat(source).contains("out.raw(\"<h1>Hello, \")");
        assertThat(source).contains("out.write(name)");
        assertThat(source).contains("out.raw(\"!</h1>\\n\")");
    }

    @Test
    void shouldGenerateCodeLine() {
        final var template = new ParsedTemplate(
            "com.example", List.of(), "HtmlOut", "T", "java.util.List<String> items",
            List.of(
                new BodyNode.CodeLine("for (var item : items) {"),
                new BodyNode.RawText("<li>"),
                new BodyNode.Expression("item"),
                new BodyNode.RawText("</li>\n"),
                new BodyNode.CodeLine("}")
            )
        );

        final var source = CodeGenerator.generate(template);

        assertThat(source).contains("for (var item : items) {");
        assertThat(source).contains("out.raw(\"<li>\")");
        assertThat(source).contains("out.write(item)");
        assertThat(source).contains("out.raw(\"</li>\\n\")");
    }

    @Test
    void shouldGenerateInclude() {
        final var template = new ParsedTemplate(
            "com.example", List.of(), "HtmlOut", "T", "Object item",
            List.of(new BodyNode.Include("new ItemTemplate(item)"))
        );

        final var source = CodeGenerator.generate(template);

        assertThat(source).contains("new ItemTemplate(item).render(out);");
    }

    @Test
    void shouldMergeAdjacentRawText() {
        final var template = new ParsedTemplate(
            "com.example", List.of(), "HtmlOut", "T", "",
            List.of(
                new BodyNode.RawText("<div>"),
                new BodyNode.RawText("<p>hello</p>"),
                new BodyNode.RawText("</div>\n")
            )
        );

        final var source = CodeGenerator.generate(template);

        assertThat(source).contains("out.raw(\"<div><p>hello</p></div>\\n\")");
    }

    @Test
    void shouldEscapeSpecialCharsInRawText() {
        final var template = new ParsedTemplate(
            "com.example", List.of(), "TextOut", "T", "",
            List.of(new BodyNode.RawText("say \"hello\"\n"))
        );

        final var source = CodeGenerator.generate(template);

        assertThat(source).contains("out.raw(\"say \\\"hello\\\"\\n\")");
    }
}
