package build.base.template.processor;

import build.base.parsing.AbstractParser.Outcome;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Layouts and slots: an include with a body passes the body to the included template, which renders it with a slot.
 *
 * @author reed.vonredwitz
 * @since Oct-2026
 */
class JtLayoutTests {

    private static final String HEADER = """
        out HtmlOut;
        package com.example;
        template Page(String title, List<String> items) {
        """;

    private static Outcome<ParsedTemplate> parseAll(final String body) {
        return JtParser.parseAll(HEADER + body + "@end\n", "Page.jt", outType -> Syntax.DEFAULT, warning -> {
        });
    }

    private static String generate(final String body) {
        final var outcome = parseAll(body);
        assertThat(outcome.errors()).isEmpty();
        return CodeGenerator.generate(outcome.value().orElseThrow());
    }

    private static List<String> messages(final Outcome<ParsedTemplate> outcome) {
        return outcome.errors().stream().map(Throwable::getMessage).toList();
    }

    @Test
    void shouldParseAnIncludeWithABodyIntoStartBodyAndEnd() {
        final var body = parseAll("""
            @include Layout(title) {
            <p>#{title}</p>
            @}
            """).value().orElseThrow().body();

        assertThat(body).containsExactly(
            new BodyNode.IncludeStart("Layout", "title"),
            new BodyNode.RawText("<p>"),
            new BodyNode.Expression("title"),
            new BodyNode.RawText("</p>\n"),
            new BodyNode.IncludeEnd());
    }

    @Test
    void shouldPassTheBodyAsTheLastArgument() {
        final var source = generate("""
            @include Layout(title, "x") {
            <p>hi</p>
            @}
            """);

        assertThat(source).contains("new Layout(title, \"x\", new build.base.template.Slots<HtmlOut>(java.util.Map."
                                        + "<String, Template<HtmlOut>>ofEntries(")
            .contains("java.util.Map.entry(\"body\", new Template<HtmlOut>() {")
            .contains("public void render(final HtmlOut out) {")
            .contains("out.raw(\"<p>hi</p>\\n\");")
            .contains("})))).render(out);");
    }

    @Test
    void shouldLeaveBlankLinesAtTheEndsOfTheBodyOutWhetherOrNotThereAreFills() {
        final var source = generate("@include Layout(title) {\n\n<p>hi</p>\n\n@}\n");

        assertThat(source).contains("out.raw(\"<p>hi</p>\\n\");");
    }

    @Test
    void shouldPassOnlyTheBodyToALayoutWithoutArguments() {
        assertThat(generate("@include Layout() {\nhi\n@}\n")).contains("new Layout(new build.base.template.Slots<HtmlOut>(");
    }

    @Test
    void shouldAcceptAQualifiedLayout() {
        assertThat(generate("@include com.other.Layout(title) {\nhi\n@}\n"))
            .contains("new com.other.Layout(title, new build.base.template.Slots<HtmlOut>(");
    }

    @Test
    void shouldKeepTheBodyInsideTheCodeAroundIt() {
        final var source = generate("""
            @for (var item : items) {
            @include Card(item) {
            <b>#{item}</b>
            @}
            @}
            """);

        assertThat(source.indexOf("for (var item : items) {")).isLessThan(source.indexOf("new Card(item, "));
        assertThat(source.indexOf("out.write(item);")).isGreaterThan(source.indexOf("new Card(item, "));
    }

    @Test
    void shouldNestIncludesWithBodies() {
        final var source = generate("""
            @include Outer() {
            @include Inner() {
            deep
            @}
            @}
            """);

        assertThat(source.indexOf("new Outer(")).isLessThan(source.indexOf("new Inner("));
        assertThat(source.indexOf("out.raw(\"deep\\n\");")).isGreaterThan(source.indexOf("new Inner("));
        assertThat(source.split("\\}\\)\\)\\)\\)\\.render\\(out\\);", -1)).hasSize(3);
    }

    @Test
    void shouldStillIncludeAnExpression() {
        final var source = generate("@include new Item(title)\n");

        assertThat(source).contains("new Item(title).render(out);").doesNotContain("new Template");
    }

    @Test
    void shouldReportAMalformedIncludeWithABody() {
        assertThat(messages(parseAll("@include {\n@}\n"))).singleElement().asString()
            .contains("@include with a body requires a template and arguments").contains("line 4");
        assertThat(messages(parseAll("@include Layout {\n@}\n"))).singleElement().asString()
            .contains("@include with a body requires a template and arguments");
    }

    @Test
    void shouldReportAnIncludeBodyThatIsNeverClosedAtItsStart() {
        assertThat(messages(parseAll("<p>\n@include Layout(title) {\nhi\n"))).singleElement().asString()
            .contains("this block is never closed").contains("line 5");
    }

    @Test
    void shouldReportABodyClosedByAnythingButItsOwnBrace() {
        assertThat(messages(parseAll("@include Layout(title) {\nhi\n@} else {\n@}\n")))
            .anyMatch(message -> message.contains("must be closed by '@}' on a line of its own"));
    }

    @Test
    void shouldReportABraceWithNothingToClose() {
        assertThat(messages(parseAll("@}\n"))).singleElement().asString().contains("unmatched '}'");
    }

    @Test
    void shouldRenderTheContentOutsideAnyFillAtASlotWithoutAName() {
        final var outcome = JtParser.parseAll("""
            out HtmlOut;
            package com.example;
            import build.base.template.Slots;
            template Layout(String title, Slots<HtmlOut> slots) {
            <h1>#{title}</h1>
            @slot
            @end
            """, "Layout.jt", outType -> Syntax.DEFAULT, warning -> {
        });

        assertThat(outcome.errors()).isEmpty();
        assertThat(outcome.value().orElseThrow().body()).contains(new BodyNode.Slot(null));
        assertThat(CodeGenerator.generate(outcome.value().orElseThrow()))
            .contains("        slots.render(\"body\", out);");
    }

    @Test
    void shouldRenderAPartAtASlotOfItsName() {
        final var outcome = JtParser.parseAll("""
            out HtmlOut;
            template Layout(build.base.template.Slots<HtmlOut> parts) {
            @slot head
            @slot
            @end
            """, "Layout.jt", outType -> Syntax.DEFAULT, warning -> {
        });

        assertThat(outcome.errors()).isEmpty();
        final String source = CodeGenerator.generate(outcome.value().orElseThrow());
        assertThat(source.indexOf("parts.render(\"head\", out);")).isPositive()
            .isLessThan(source.indexOf("parts.render(\"body\", out);"));
    }

    @Test
    void shouldRenderASlotOfAFragmentFromTheParametersOfTheFragment() {
        final var outcome = JtParser.parseAll("""
            out HtmlOut;
            import build.base.template.Slots;
            template Layout(String title, Slots<HtmlOut> body) {
            @fragment card(String heading, Slots<HtmlOut> inner)
            <h2>#{heading}</h2>
            @slot
            @endfragment
            @end
            """, "Layout.jt", outType -> Syntax.DEFAULT, warning -> {
        });

        assertThat(outcome.errors()).isEmpty();
        final String source = CodeGenerator.generate(outcome.value().orElseThrow());
        assertThat(source).contains("inner.render(\"body\", out);").doesNotContain("body.render(");
    }

    @Test
    void shouldReportASlotInATemplateWithoutASlotsParameter() {
        assertThat(messages(parseAll("@slot content\n"))).singleElement().asString()
            .contains("@slot needs a parameter of the type build.base.template.Slots")
            .contains("template Page has not").contains("line 4");
    }

    @Test
    void shouldReportASlotInAFragmentAgainstTheParametersOfTheFragment() {
        // The template has a Slots parameter, but the fragment is a separate type that sees only its own
        final var outcome = JtParser.parseAll("""
            out HtmlOut;
            import build.base.template.Slots;
            template Page(Slots<HtmlOut> slots) {
            @fragment f()
            @slot title
            @endfragment
            @end
            """, "Page.jt", outType -> Syntax.DEFAULT, warning -> {
        });

        assertThat(messages(outcome)).singleElement().asString().contains("fragment f has not");
    }

    @Test
    void shouldReportASlotThatIsNotAName() {
        assertThat(messages(parseAll("@slot a.b\n"))).singleElement().asString()
            .contains("@slot takes the name of a part");
    }

    @Test
    void shouldAllowAFragmentInsideTheBodyOfAnInclude() {
        final var source = generate("""
            @include Layout(title) {
            @fragment row(String title)
            <li>#{title}</li>
            @endfragment
            @}
            """);

        assertThat(source).contains("new Row(title).render(out);")
            .contains("public record Row(String title) implements Template<HtmlOut> {");
    }

    @Test
    void shouldReportABodyThatStartsInsideAFragmentAndEndsOutsideIt() {
        assertThat(messages(parseAll("@fragment f()\n@include Layout() {\n@endfragment\n@}\n")))
            .anyMatch(message -> message.contains("must contain whole blocks"));
    }

    @Test
    void shouldNumberTheLinesOfABody() {
        final var source = generate("@include Layout(title) {\nhi\n@}\n");

        assertThat(source).contains("// Page.jt:4").contains("// Page.jt:5");
    }
}
