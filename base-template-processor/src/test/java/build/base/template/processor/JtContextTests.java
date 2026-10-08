package build.base.template.processor;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for named output contexts (6.3).
 *
 * @author reed.vonredwitz
 * @since Oct-2026
 */
class JtContextTests {

    private static final Syntax HTML = new Syntax("@", "#{", Map.of("url", "writeUrl", "attr", "writeAttr"));

    private static java.util.List<BodyNode> parseLine(final String line, final Syntax syntax) {
        final var body = new ArrayList<BodyNode>();
        JtParser.parseTextLine(line, body, syntax);
        return body;
    }

    @Test
    void declaredContextCompilesToItsMethod() {
        assertThat(parseLine("<a href=\"#url{u}\" title=\"#attr{t}\">#{t}</a>\n", HTML)).containsExactly(
            new BodyNode.RawText("<a href=\""),
            new BodyNode.ContextExpression("writeUrl", "u"),
            new BodyNode.RawText("\" title=\""),
            new BodyNode.ContextExpression("writeAttr", "t"),
            new BodyNode.RawText("\">"),
            new BodyNode.Expression("t"),
            new BodyNode.RawText("</a>\n"));
    }

    @Test
    void rawIsAlwaysAContext() {
        assertThat(parseLine("#raw{x}\n", Syntax.DEFAULT)).containsExactly(
            new BodyNode.ContextExpression("raw", "x"),
            new BodyNode.RawText("\n"));
    }

    @Test
    void undeclaredNamesAreText() {
        // a CSS id selector must not be taken for a context
        assertThat(parseLine("#nav{ color: red }\n", HTML))
            .containsExactly(new BodyNode.RawText("#nav{ color: red }\n"));
    }

    @Test
    void escapeEmitsTheNamedOpenerAsText() {
        assertThat(parseLine("##url{x}\n", HTML))
            .containsExactly(new BodyNode.RawText("#url{x}\n"));
    }

    @Test
    void escapeEmitsTheRawOpenerAsText() {
        assertThat(parseLine("##raw{x}\n", Syntax.DEFAULT))
            .containsExactly(new BodyNode.RawText("#raw{x}\n"));
    }

    @Test
    void expressionHonoursNestedBracesAndLiterals() {
        assertThat(parseLine("#url{ m.get(\"}\") + f({1}) }\n", HTML)).containsExactly(
            new BodyNode.ContextExpression("writeUrl", " m.get(\"}\") + f({1}) "),
            new BodyNode.RawText("\n"));
    }

    @Test
    void namedOpenerFollowsTheConfiguredInterpolation() {
        final var syntax = new Syntax("@", "${", Map.of("url", "writeUrl"));

        assertThat(parseLine("$url{u} ${v} $${w} $$url{x}\n", syntax)).containsExactly(
            new BodyNode.ContextExpression("writeUrl", "u"),
            new BodyNode.RawText(" "),
            new BodyNode.Expression("v"),
            new BodyNode.RawText(" ${w} $url{x}\n"));
    }

    @Test
    void optionsKeepTheContextsOfTheOutType() {
        final var result = JtParser.parse("""
            out HtmlOut;
            option interpolation = "${";
            package p;
            template T() {
            $url{u}
            @end
            """, "t.jt", outType -> HTML);

        assertThat(result.body()).containsExactly(
            new BodyNode.ContextExpression("writeUrl", "u"),
            new BodyNode.RawText("\n"));
    }

    @Test
    void unclosedContextExpressionIsAParseError() {
        assertThatThrownBy(() -> parseLine("#url{oops\n", HTML))
            .isInstanceOf(JtParseException.class)
            .hasMessageContaining("unclosed")
            .hasMessageContaining("column 1");
    }

    @Test
    void generatorCallsTheMethodOnOut() {
        final var source = CodeGenerator.generate(JtParser.parse("""
            out HtmlOut;
            package p;
            template T(String u) {
            #url{u}
            @end
            """, "t.jt", outType -> HTML));

        assertThat(source).contains("out.writeUrl(u);");
    }

    @Test
    void generatorEmitsRawContextAsRaw() {
        final var source = CodeGenerator.generate(JtParser.parse("""
            out HtmlOut;
            package p;
            template T(String u) {
            #raw{u}
            @end
            """, "t.jt"));

        assertThat(source).contains("out.raw(u);");
    }
}
