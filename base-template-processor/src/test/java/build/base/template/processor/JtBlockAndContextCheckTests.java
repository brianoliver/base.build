package build.base.template.processor;

import build.base.parsing.AbstractParser.Outcome;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mistakes that would otherwise surface as confusing {@code javac} errors in generated code, or not at all, are
 * reported against the template.
 *
 * @author reed.vonredwitz
 * @since Oct-2026
 */
class JtBlockAndContextCheckTests {

    private static final Syntax HTML = new Syntax("@", "#{", Map.of("url", "writeUrl", "json", "writeJson"));

    private final List<String> warnings = new ArrayList<>();

    private Outcome<ParsedTemplate> parse(final String body) {
        return JtParser.parseAll("out HtmlOut;\npackage p;\ntemplate T(String x) {\n" + body + "@end\n", "t.jt",
            outType -> HTML, warnings::add);
    }

    private static List<String> messages(final Outcome<ParsedTemplate> outcome) {
        return outcome.errors().stream().map(Throwable::getMessage).toList();
    }

    // --- blocks ---

    @Test
    void shouldAcceptBalancedBlocks() {
        final var outcome = parse("""
            @if (x.isEmpty()) {
            @for (var c : x.toCharArray()) {
            @}
            @} else {
            @}
            """);

        assertThat(outcome.succeeded()).isTrue();
    }

    @Test
    void shouldReportABlockThatIsNeverClosedAtTheLineThatOpenedIt() {
        final var outcome = parse("""
            <p>
            @if (x.isEmpty()) {
            <b>none</b>
            """);

        assertThat(messages(outcome)).singleElement().asString()
            .contains("t.jt: this block is never closed (missing '@}') at line 5, column 1");
    }

    @Test
    void shouldReportEveryUnclosedBlockOutermostFirst() {
        final var outcome = parse("""
            @if (x.isEmpty()) {
            @for (var c : x.toCharArray()) {
            """);

        assertThat(messages(outcome)).hasSize(2);
        assertThat(messages(outcome).get(0)).contains("line 4");
        assertThat(messages(outcome).get(1)).contains("line 5");
    }

    @Test
    void shouldReportAClosingBraceWithNothingToClose() {
        final var outcome = parse("""
            <p>
            @}
            """);

        assertThat(messages(outcome)).singleElement().asString()
            .contains("unmatched '}'")
            .contains("at line 5, column 1");
    }

    @Test
    void shouldReportBlockErrorsAlongsideOtherLineErrors() {
        final var outcome = parse("""
            @include
            @}
            """);

        assertThat(outcome.errors()).hasSize(2);
        assertThat(messages(outcome).get(0)).contains("include requires an expression");
        assertThat(messages(outcome).get(1)).contains("unmatched '}'");
    }

    @Test
    void shouldNotReportBlockMismatchesAfterABlockDirectiveFailedToParse() {
        final var outcome = parse("""
            @fragment row() {
            <td>#{x}</td>
            @}
            """);

        assertThat(messages(outcome)).singleElement().asString().contains("not supported yet");
    }

    @Test
    void shouldNotReportAnUnclosedBlockAfterABlockDirectiveFailedToParse() {
        final var outcome = parse("""
            @if (x.isEmpty()) {
            @fragment row() {
            """);

        assertThat(messages(outcome)).singleElement().asString().contains("not supported yet");
    }

    @Test
    void shouldIgnoreBracesInLiteralsCommentsAndText() {
        final var outcome = parse("""
            @java var s = "}" + '{' + "\\"{";
            @java var t = s; // {
            @java /* { */ var u = t;
            @java var v = new int[] { 1 };
            a { color: red }
            #{x}
            """);

        assertThat(outcome.succeeded()).isTrue();
    }

    @Test
    void shouldCountBracesOpenedByAnyDirectiveLine() {
        final var outcome = parse("""
            @java x.chars().forEach(c -> {
            @});
            """);

        assertThat(outcome.succeeded()).isTrue();
    }

    @Test
    void shouldUseTheConfiguredPrefixInTheMessage() {
        final var outcome = JtParser.parseAll("out HtmlOut;\noption prefix = \"%\";\npackage p;\ntemplate T() {\n%if (x) {\n%end\n",
            "t.jt", outType -> HTML, warnings::add);

        assertThat(messages(outcome)).singleElement().asString().contains("(missing '%}')");
    }
}
