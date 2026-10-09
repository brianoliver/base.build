package build.base.template.processor;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A plain interpolation only HTML-escapes, so it is warned about where that is not enough.
 *
 * @author reed.vonredwitz
 * @since Oct-2026
 */
class JtUnsafeContextTests {

    private static final Syntax HTML = new Syntax("@", "#{", Map.of(
        "attr", "writeAttr", "url", "writeUrl", "json", "writeJson", "js", "writeJs", "css", "writeCss"));

    private final List<String> warnings = new ArrayList<>();

    private void parse(final Syntax syntax, final String body) {
        JtParser.parseAll("out HtmlOut;\npackage p;\ntemplate T(String x) {\n" + body + "@end\n", "t.jt",
            outType -> syntax, warnings::add);
    }

    private void parse(final String body) {
        parse(HTML, body);
    }

    @Test
    void shouldWarnWhenAnInterpolationStartsAUrl() {
        for (final String line : List.of(
            "<a href=\"#{x}\">",
            "<a href=#{x}>",
            "<img src='#{x}'>",
            "<form action = \"#{x}\">",
            "<button hx-get=\"#{x}\">",
            "<a class=\"c\" HREF=\"#{x}\">",
            "<script src=\"#{x}\"></script>")) {
            warnings.clear();
            parse(line + "\n");

            assertThat(warnings).as(line).singleElement().asString()
                .contains("at the start of the ")
                .contains("'javascript:' URL")
                .contains("use '#url{'");
        }
    }

    @Test
    void shouldWarnInsideAttributesThatHoldJavaScript() {
        for (final String line : List.of(
            "<button onclick=\"go(#{x})\">",
            "<button onClick='go(\"#{x}\")'>",
            "<div x-data=\"{ n: #{x} }\">",
            "<div x-init=\"load('#{x}')\">",
            "<b @click=\"f(#{x})\">",
            "<b x-on:click.prevent=\"f(#{x})\">",
            "<b :class=\"#{x}\">",
            "<b hx-on:click=\"f(#{x})\">",
            "<b hx-vals='{\"a\": \"#{x}\"}'>",
            "<b hx-headers='{\"a\": \"#{x}\"}'>")) {
            warnings.clear();
            parse(line + "\n");

            assertThat(warnings).as(line).singleElement().asString()
                .contains("holds JavaScript or JSON")
                .contains("use '#json{'");
        }
    }

    @Test
    void shouldWarnInsideAnAttributeValueThatContinuesFromAPreviousLine() {
        parse("""
            <button onclick="go(1);
              go(#{x})">
            """);

        assertThat(warnings).singleElement().asString()
            .contains("line 5")
            .contains("holds JavaScript or JSON")
            .contains("use '#json{'");
    }

    @Test
    void shouldWarnForAUrlAttributeWhoseValueStartsOnTheNextLine() {
        parse("""
            <a href="
              #{x}">
            """);

        assertThat(warnings).singleElement().asString().contains("at the start of the href attribute");
    }

    @Test
    void shouldNotLetQuotesInsideAnInterpolationDecideWhereATagEnds() {
        parse("""
            <b title="#{x.charAt(0) == '"' ? "a" : "b"}"
              data-n="1">
            <i>#{x}</i>
            """);

        assertThat(warnings).isEmpty();
    }

    @Test
    void shouldNotCarryATagOverOnceItHasEnded() {
        parse("""
            <b onclick="go(1)"
              title="#{x}">
            <i>#{x}</i>
            """);

        assertThat(warnings).isEmpty();
    }

    @Test
    void shouldNotTreatATagNamedInAScriptStringAsTheEndOfTheScript() {
        parse("""
            <script>
            var s = "<style>";
            var n = #{x};
            </script>
            """);

        assertThat(warnings).singleElement().asString()
            .contains("line 6")
            .contains("inside a <script> element");
    }

    @Test
    void shouldNotTakeATagInAJavaStringOnADirectiveLineForMarkup() {
        parse("""
            @java var s = "<script>";
            <p>#{x}</p>
            """);

        assertThat(warnings).isEmpty();
    }

    @Test
    void shouldWarnInsideAScriptElement() {
        parse("""
            <script>
            var n = #{x};
            </script>
            """);

        assertThat(warnings).singleElement().asString()
            .contains("line 5")
            .contains("inside a <script> element")
            .contains("use '#js{'");
    }

    @Test
    void shouldWarnInsideAScriptElementOnTheLineThatOpensIt() {
        parse("<script type=\"module\">var n = #{x};</script>\n");

        assertThat(warnings).singleElement().asString().contains("use '#js{'");
    }

    @Test
    void shouldTellTheAuthorToDropQuotesAroundAJsonOrJsValue() {
        parse("<button onclick=\"go('#{x}')\">\n");
        parse("<script>var s = '#{x}';</script>\n");
        parse("<a href=\"#{x}\">\n");

        assertThat(warnings).hasSize(3);
        assertThat(warnings.get(0)).contains("use '#json{', and drop any quotes around it");
        assertThat(warnings.get(1)).contains("use '#js{', and drop any quotes around it");
        assertThat(warnings.get(2)).doesNotContain("quotes");
    }

    @Test
    void shouldNotSuggestATypoFixForASelectorInAStyleElement() {
        parse("""
            <style>
            #row{color:red}
            </style>
            <p>#row{x}</p>
            """);

        assertThat(warnings).singleElement().asString().contains("line 7").contains("did you mean '#raw{'?");
    }

    @Test
    void shouldWarnInsideAStyleElement() {
        parse("""
            <style>
            a { color: #{x}; }
            </style>
            """);

        assertThat(warnings).singleElement().asString()
            .contains("inside a <style> element")
            .contains("use '#css{'");
    }

    @Test
    void shouldStopWarningOnceTheElementIsClosed() {
        parse("""
            <script>var a = 1;</script>
            <p>#{x}</p>
            <style>a {}</style>
            <p>#{x}</p>
            """);

        assertThat(warnings).isEmpty();
    }

    @Test
    void shouldNotTakeAttributeLookingTextOutsideATagForAnAttribute() {
        parse("<p>set onclick=\"go(#{x}\n");
        parse("<p>set href=\"#{x}\n");

        assertThat(warnings).isEmpty();
    }

    @Test
    void shouldReportTheColumnOfTheInterpolation() {
        parse("  <a href=\"#{x}\">\n");

        assertThat(warnings).singleElement().asString().startsWith("t.jt: line 4, column 12: ");
    }

    @Test
    void shouldWarnOnceForEachInterpolationThatIsAtFault() {
        parse("<a href=\"#{x}#{x}\" onclick=\"#{x}\">\n");

        assertThat(warnings).hasSize(2);
    }

    @Test
    void shouldNotWarnWhereEscapingIsEnoughOrAContextIsNamed() {
        for (final String line : List.of(
            "<a href=\"/tasks/#{x}/toggle\">",
            "<a href=\"#url{x}\">",
            "<a href=\"#attr{x}\">",
            "<button onclick=\"go(#json{x})\">",
            "<p>#{x}</p>",
            "<a title=\"#{x}\" class=\"#{x}\" id='#{x}'>",
            "<input value=\"#{x}\" data-id=\"#{x}\" name=\"#{x}\">",
            "<div style=\"color: #{x}\">",
            "<p>use href=\"x\" in an anchor, then #{x}</p>",
            "<b onclick=\"go()\" title=\"#{x}\">")) {
            parse(line + "\n");

            assertThat(warnings).as(line).isEmpty();
        }
    }

    @Test
    void shouldNotWarnForAnOutTypeThatDeclaresNoSuchContext() {
        parse(new Syntax("%", "${"), "<a href=\"${x}\">\n<script>\n${x}\n</script>\n");

        assertThat(warnings).isEmpty();
    }

    @Test
    void shouldOnlyAdviseAContextTheOutTypeDeclares() {
        parse(new Syntax("@", "#{", Map.of("attr", "writeAttr")), "<a href=\"#{x}\">\n");

        assertThat(warnings).isEmpty();
    }

    @Test
    void shouldStillCompileTheInterpolationAsAPlainWrite() {
        final var outcome = JtParser.parseAll("out HtmlOut;\npackage p;\ntemplate T(String x) {\n<a href=\"#{x}\">\n@end\n",
            "t.jt", outType -> HTML, warnings::add);

        assertThat(outcome.value().orElseThrow().body()).containsExactly(
            new BodyNode.RawText("<a href=\""),
            new BodyNode.Expression("x"),
            new BodyNode.RawText("\">\n"));
    }
}
