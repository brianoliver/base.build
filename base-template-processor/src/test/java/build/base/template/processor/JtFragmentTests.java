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

/**
 * Fragments: parts of a template that render in place and are also templates of their own.
 *
 * @author reed.vonredwitz
 * @since Oct-2026
 */
class JtFragmentTests {

    private static final String HEADER = """
        out HtmlOut;
        package com.example;
        template TasksPage(List<String> tasks) {
        """;

    private static Outcome<ParsedTemplate> parseAll(final String body) {
        return JtParser.parseAll(HEADER + body + "@end\n", "TasksPage.jt", outType -> Syntax.DEFAULT, warning -> {
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
    void shouldParseAFragmentIntoStartBodyAndEnd() {
        final var body = parseAll("""
            @fragment row(String t)
            <li>#{t}</li>
            @endfragment
            """).value().orElseThrow().body();

        assertThat(body).containsExactly(
            new BodyNode.FragmentStart("row", "String t"),
            new BodyNode.RawText("<li>"),
            new BodyNode.Expression("t"),
            new BodyNode.RawText("</li>\n"),
            new BodyNode.FragmentEnd());
    }

    @Test
    void shouldRenderAFragmentInPlaceAndNestItsTypeInTheTemplate() {
        final var source = generate("""
            <ul>
            @for (var t : tasks) {
            @fragment row(String t)
              <li>#{t}</li>
            @endfragment
            @}
            </ul>
            """);

        assertThat(source).contains("for (var t : tasks) {")
            .contains("new Row(t).render(out);")
            .contains("public record Row(String t) implements Template<HtmlOut> {");
        // The body of the fragment is in its own type and not repeated in the template
        assertThat(source.indexOf("out.write(t);")).isEqualTo(source.lastIndexOf("out.write(t);"))
            .isGreaterThan(source.indexOf("public record Row"));
    }

    @Test
    void shouldPassTheNamesOfAllParametersWhateverTheirTypes() {
        assertThat(CodeGenerator.parameterNames("Map<String, List<Integer>> m, final int[] a, String... rest"))
            .containsExactly("m", "a", "rest");
        assertThat(CodeGenerator.parameterNames("")).isEmpty();
    }

    @Test
    void shouldGenerateAFragmentWithoutParameters() {
        final var source = generate("""
            @fragment footer()
            <hr>
            @endfragment
            """);

        assertThat(source).contains("new Footer().render(out);").contains("public record Footer()");
    }

    @Test
    void shouldReportAMalformedDeclaration() {
        assertThat(messages(parseAll("@fragment\n@endfragment\n"))).hasSize(1).first().asString()
            .contains("@fragment requires a name and parameters").contains("line 4");
        assertThat(messages(parseAll("@fragment row\n@endfragment\n"))).hasSize(1).first().asString()
            .contains("@fragment requires a name and parameters");
    }

    @Test
    void shouldReportContentAfterEndfragment() {
        assertThat(messages(parseAll("@fragment f()\n@endfragment now\n"))).hasSize(1).first().asString()
            .contains("unexpected content after @endfragment");
    }

    @Test
    void shouldReportAFragmentThatIsNeverClosedAtItsStart() {
        assertThat(messages(parseAll("<p>\n@fragment f()\n<b>x</b>\n"))).singleElement().asString()
            .contains("fragment f is never closed").contains("line 5");
    }

    @Test
    void shouldReportAnEndWithNothingToClose() {
        assertThat(messages(parseAll("@endfragment\n"))).singleElement().asString()
            .contains("endfragment has no fragment to close").contains("line 4");
    }

    @Test
    void shouldReportNestedFragments() {
        // Only the inner fragment is wrong: the ends are not reported as well
        assertThat(messages(parseAll("@fragment a()\n@fragment b()\n@endfragment\n@endfragment\n")))
            .singleElement().asString().contains("fragments cannot be nested").contains("line 5");
    }

    @Test
    void shouldReportAFragmentThatSplitsABlockAndOpensAnother() {
        // As many blocks are open at the end as at the start, but not the same ones
        assertThat(messages(parseAll("@if (tasks.isEmpty()) {\n@fragment f()\n@} else {\n@endfragment\n@}\n")))
            .singleElement().asString().contains("fragment f must contain whole blocks");
    }

    @Test
    void shouldAcceptAFragmentThatContainsWholeBlocks() {
        assertThat(messages(parseAll("@fragment f(List<String> l)\n@for (var t : l) {\n@} \n@endfragment\n")))
            .isEmpty();
    }

    @Test
    void shouldReportAFragmentThatWouldHideAnImportedType() {
        final var outcome = JtParser.parseAll("""
            out HtmlOut;
            package com.example;
            import java.util.List;
            template TasksPage(List<String> tasks) {
            @fragment list(String t)
            @endfragment
            @end
            """, "TasksPage.jt", outType -> Syntax.DEFAULT, warning -> {
        });

        assertThat(messages(outcome)).singleElement().asString()
            .contains("fragment list would generate the type List").contains("line 5");
    }

    @Test
    void shouldReportAFragmentThatWouldHideATypeOfJavaLang() {
        assertThat(messages(parseAll("@fragment string()\n@endfragment\n"))).singleElement().asString()
            .contains("would generate the type String");
    }

    @Test
    void shouldReportAFragmentThatWouldHideATypeNamedInAParameterList() {
        // TasksPage(List<String> tasks) uses List, and the fragment's own parameter uses Task
        assertThat(messages(parseAll("@fragment list()\n@endfragment\n"))).singleElement().asString()
            .contains("fragment list would generate the type List");
        assertThat(messages(parseAll("@fragment task(Task task)\n@endfragment\n"))).singleElement().asString()
            .contains("fragment task would generate the type Task");
    }

    @Test
    void shouldReportAFragmentThatWouldHideATypeImportedOnDemand() {
        final var outcome = JtParser.parseAll("""
            out HtmlOut;
            package com.example;
            import java.util.*;
            template TasksPage(int count) {
            @fragment deque()
            @endfragment
            @end
            """, "TasksPage.jt", outType -> Syntax.DEFAULT, warning -> {
        });

        assertThat(messages(outcome)).singleElement().asString().contains("would generate the type Deque");
    }

    @Test
    void shouldGiveAFragmentOnlyItsParameters() {
        final var source = generate("""
            @for (var t : tasks) {
            @fragment row(String t)
            <li>#{t}</li>
            @endfragment
            @}
            """);

        assertThat(source).contains("public record Row(String t) implements")
            .doesNotContain("Row(List<String> tasks");
    }

    @Test
    void shouldNotLetAFragmentUseTheVariablesOfItsTemplate(@TempDir final Path dir) throws IOException {
        final var result = ProcessorHarness.run(dir,
            Map.of("com/acme/Page.jt", """
                out HtmlOut;
                import java.util.List;
                template Page(List<String> tasks) {
                @fragment count()
                <p>#{tasks.size()}</p>
                @endfragment
                @end
                """),
            Map.of());

        assertThat(result.success()).isFalse();
        assertThat(result.messages(Diagnostic.Kind.ERROR)).anySatisfy(message -> assertThat(message)
            .contains("tasks").contains("cannot be referenced from a static context"));
    }

    @Test
    void shouldCompileAFragmentThatUsesOnlyItsParameters(@TempDir final Path dir) throws IOException {
        final var result = ProcessorHarness.run(dir,
            Map.of("com/acme/Page.jt", """
                out HtmlOut;
                import java.util.List;
                template Page(List<String> tasks) {
                @java var label = "tasks: " + tasks.size();
                @fragment count(String label)
                <p>#{label}</p>
                @endfragment
                @end
                """),
            Map.of());

        assertThat(result.messages(Diagnostic.Kind.ERROR)).isEmpty();
        assertThat(result.success()).isTrue();
    }

    @Test
    void shouldReportADuplicateFragmentIncludingOneThatDiffersOnlyInTheCaseOfItsFirstLetter() {
        assertThat(messages(parseAll("@fragment row()\n@endfragment\n@fragment Row()\n@endfragment\n")))
            .singleElement().asString().contains("fragment Row is already declared").contains("line 6");
    }

    @Test
    void shouldReportAFragmentNamedLikeTheTemplate() {
        assertThat(messages(parseAll("@fragment tasksPage()\n@endfragment\n")))
            .singleElement().asString().contains("name of the template");
    }

    @Test
    void shouldReportAFragmentThatSplitsABlock() {
        assertThat(messages(parseAll("@for (var t : tasks) {\n@fragment f()\n@}\n@endfragment\n")))
            .singleElement().asString().contains("fragment f must contain whole blocks");
    }

    @Test
    void shouldKeepTrackingMarkupAcrossFragmentBoundaries() {
        // The fragment starts inside a script element, so an unsafe interpolation is still warned about in it
        final var warnings = new java.util.ArrayList<String>();
        JtParser.parseAll(HEADER + "<script>\n@fragment f(String t)\nvar x = #{t};\n@endfragment\n</script>\n@end\n",
            "TasksPage.jt", outType -> new Syntax("@", "#{", java.util.Map.of("js", "writeJs")), warnings::add);

        assertThat(warnings).singleElement().asString().contains("<script>").contains("line 6");
    }
}
