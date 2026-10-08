package build.base.template.processor;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the closed directive set, the escapes and the configurable delimiters.
 *
 * @author reed.vonredwitz
 * @since Oct-2026
 */
class JtSyntaxTests {

    private static ParsedTemplate parseBody(final String body) {
        return JtParser.parse("out HtmlOut;\npackage p;\ntemplate T() {\n" + body + "@end\n", "t.jt");
    }

    // --- closed directive set ---

    @Test
    void annotationLinesAreText() {
        final var result = parseBody("  @Override\n@Test\n");

        assertThat(result.body()).containsExactly(
            new BodyNode.RawText("  @Override\n"),
            new BodyNode.RawText("@Test\n"));
    }

    @Test
    void cssAtRulesAndAlpineAttributesAreText() {
        final var result = parseBody("@media (min-width: 1px) {\n@click=\"go()\"\n@import url(x);\n");

        assertThat(result.body()).containsExactly(
            new BodyNode.RawText("@media (min-width: 1px) {\n"),
            new BodyNode.RawText("@click=\"go()\"\n"),
            new BodyNode.RawText("@import url(x);\n"));
    }

    @Test
    void interfaceIsText() {
        assertThat(parseBody("@interface Foo {\n").body())
            .containsExactly(new BodyNode.RawText("@interface Foo {\n"));
    }

    @Test
    void directiveNameMustBeWholeWord() {
        // "format" starts with "for" but is not the directive
        assertThat(parseBody("@format\n").body()).containsExactly(new BodyNode.RawText("@format\n"));
    }

    @Test
    void keywordDirectivesAreCode() {
        final var result = parseBody("""
            @if (a) {
            @} else {
            @var x = 1;
            @final int y = 2;
            @try {
            @} catch (Exception e) {
            @} finally {
            @}
            @}
            """);

        assertThat(result.body()).containsExactly(
            new BodyNode.CodeLine("if (a) {"),
            new BodyNode.CodeLine("} else {"),
            new BodyNode.CodeLine("var x = 1;"),
            new BodyNode.CodeLine("final int y = 2;"),
            new BodyNode.CodeLine("try {"),
            new BodyNode.CodeLine("} catch (Exception e) {"),
            new BodyNode.CodeLine("} finally {"),
            new BodyNode.CodeLine("}"),
            new BodyNode.CodeLine("}"));
    }

    @Test
    void javaDirectiveEmitsArbitraryStatement() {
        assertThat(parseBody("@java out.raw(x);\n").body()).containsExactly(new BodyNode.CodeLine("out.raw(x);"));
    }

    @Test
    void bareStatementIsNoLongerCode() {
        assertThat(parseBody("@out.raw(x);\n").body()).containsExactly(new BodyNode.RawText("@out.raw(x);\n"));
    }

    @Test
    void bareStatementWarns() {
        final var warnings = new ArrayList<String>();

        JtParser.parse("out HtmlOut;\npackage p;\ntemplate T() {\n@out.raw(x);\n@end\n", "t.jt",
                       outType -> Syntax.DEFAULT, warnings::add);

        assertThat(warnings).singleElement().satisfies(w -> assertThat(w)
            .contains("t.jt").contains("line 4").contains("@java ").contains("@@"));
    }

    @Test
    void annotationsAttributesAndAtRulesDoNotWarn() {
        final var warnings = new ArrayList<String>();

        JtParser.parse("""
                       out HtmlOut;
                       package p;
                       template T() {
                       @Override
                       @SuppressWarnings("unchecked")
                       @click.outside="close();"
                       @media (min-width: 1px) {
                       @import url(x);
                       @@out.raw(x);
                       @end
                       """, "t.jt", outType -> Syntax.DEFAULT, warnings::add);

        assertThat(warnings).isEmpty();
    }

    @Test
    void escapedPrefixLineReportsColumnsInSourceCoordinates() {
        // "  @@x #{oops": the unclosed expression starts at column 7 of the source line
        assertThatThrownBy(() -> parseBody("  @@x #{oops\n"))
            .isInstanceOf(JtParseException.class)
            .hasMessageContaining("line 4")
            .hasMessageContaining("column 7");
    }

    @Test
    void emptyJavaDirectiveIsAnError() {
        assertThatThrownBy(() -> parseBody("@java\n"))
            .isInstanceOf(JtParseException.class)
            .hasMessageContaining("@java requires a statement")
            .hasMessageContaining("line 4");
    }

    @Test
    void reservedDirectiveIsAnError() {
        assertThatThrownBy(() -> parseBody("@fragment row\n"))
            .isInstanceOf(JtParseException.class)
            .hasMessageContaining("@fragment is not supported yet");
    }

    @Test
    void contentAfterEndIsAnError() {
        assertThatThrownBy(() -> parseBody("@end now\n"))
            .isInstanceOf(JtParseException.class)
            .hasMessageContaining("unexpected content after @end");
    }

    // --- escapes ---

    @Test
    void doubledPrefixEmitsLiteralPrefixKeepingIndentation() {
        assertThat(parseBody("  @@for\n").body()).containsExactly(new BodyNode.RawText("  @for\n"));
    }

    @Test
    void doubledFirstCharacterEmitsLiteralInterpolationOpener() {
        final var body = new ArrayList<BodyNode>();
        JtParser.parseTextLine("a ##{b} #{c}\n", body);

        assertThat(body).containsExactly(
            new BodyNode.RawText("a #{b} "),
            new BodyNode.Expression("c"),
            new BodyNode.RawText("\n"));
    }

    // --- configurable delimiters ---

    @Test
    void optionsChangeThePrefix() {
        final var result = JtParser.parse("""
            out HtmlOut;
            option prefix = "%";
            package p;
            template T() {
            %for (var i : items) {
            @Override
            %}
            %end
            """, "t.jt");

        assertThat(result.body()).containsExactly(
            new BodyNode.CodeLine("for (var i : items) {"),
            new BodyNode.RawText("@Override\n"),
            new BodyNode.CodeLine("}"));
    }

    @Test
    void optionsChangeTheInterpolation() {
        final var result = JtParser.parse("""
            out HtmlOut;
            option interpolation = "${";
            package p;
            template T() {
            @Value("#{x}") ${name} $${lit}
            @end
            """, "t.jt");

        assertThat(result.body()).containsExactly(
            new BodyNode.RawText("@Value(\"#{x}\") "),
            new BodyNode.Expression("name"),
            new BodyNode.RawText(" ${lit}\n"));
    }

    @Test
    void outTypeSuppliesTheDefaultSyntaxAndOptionsOverrideIt() {
        final var lookedUp = new ArrayList<String>();
        final var result = JtParser.parse("""
            out com.acme.PctOut;
            option interpolation = "${";
            template T() {
            %for (var x : xs) {
            @Value #{y} ${y}
            %}
            %end
            """, "t.jt", outType -> {
            lookedUp.add(outType);
            return new Syntax("%", "#{");
        });

        assertThat(lookedUp).containsExactly("com.acme.PctOut");
        assertThat(result.body()).containsExactly(
            new BodyNode.CodeLine("for (var x : xs) {"),
            new BodyNode.RawText("@Value #{y} "),
            new BodyNode.Expression("y"),
            new BodyNode.RawText("\n"),
            new BodyNode.CodeLine("}"));
    }

    @Test
    void invalidSyntaxFromTheOutTypeIsAnError() {
        assertThatThrownBy(() -> JtParser.parse("""
            out com.acme.BadOut;
            template T() {
            @end
            """, "t.jt", outType -> new Syntax("x", "#{")))
            .isInstanceOf(JtParseException.class)
            .hasMessageContaining("invalid syntax declared by com.acme.BadOut")
            .hasMessageContaining("line 1");
    }

    @Test
    void prefixEscapeFollowsThePrefix() {
        final var result = JtParser.parse("""
            out HtmlOut;
            option prefix = "%";
            package p;
            template T() {
            %%for
            %end
            """, "t.jt");

        assertThat(result.body()).containsExactly(new BodyNode.RawText("%for\n"));
    }

    @Test
    void multiCharacterPrefixEscapeDoublesTheWholePrefix() {
        final var result = JtParser.parse("""
            out HtmlOut;
            option prefix = "::";
            package p;
            template T() {
            ::for (var x : xs) {
            ::::for
            ::}
            ::end
            """, "t.jt");

        assertThat(result.body()).containsExactly(
            new BodyNode.CodeLine("for (var x : xs) {"),
            new BodyNode.RawText("::for\n"),
            new BodyNode.CodeLine("}"));
    }

    @Test
    void optionsFollowTheOutDeclarationAndPrecedeThePackage() {
        final var result = JtParser.parse("""
            out HtmlOut;
            option prefix = "%";
            option interpolation = "${";
            package p;
            import java.util.List;
            template T() {
            %end
            """, "t.jt");

        assertThat(result.packageName()).isEqualTo("p");
        assertThat(result.imports()).containsExactly("import java.util.List");
    }

    @Test
    void unknownOptionIsAnError() {
        assertThatThrownBy(() -> JtParser.parse("""
            out HtmlOut;
            option colour = "red";
            template T() {
            @end
            """, "t.jt"))
            .isInstanceOf(JtParseException.class)
            .hasMessageContaining("unknown option 'colour'")
            .hasMessageContaining("line 2");
    }

    @Test
    void duplicateOptionIsAnError() {
        assertThatThrownBy(() -> JtParser.parse("""
            out HtmlOut;
            option prefix = "%";
            option prefix = "!";
            template T() {
            @end
            """, "t.jt"))
            .isInstanceOf(JtParseException.class)
            .hasMessageContaining("duplicate option 'prefix'")
            .hasMessageContaining("line 3");
    }

    @Test
    void invalidDelimitersAreErrors() {
        assertThatThrownBy(() -> JtParser.parse("""
            out HtmlOut;
            option prefix = "x";
            template T() {
            @end
            """, "t.jt"))
            .isInstanceOf(JtParseException.class)
            .hasMessageContaining("invalid option 'prefix'")
            .hasMessageContaining("prefix \"x\"")
            .hasMessageContaining("line 2");

        // The error points at the offending option, not at whatever follows the options
        assertThatThrownBy(() -> JtParser.parse("""
            out HtmlOut;
            option prefix = "%";
            option interpolation = "$";
            template T() {
            @end
            """, "t.jt"))
            .isInstanceOf(JtParseException.class)
            .hasMessageContaining("invalid option 'interpolation'")
            .hasMessageContaining("line 3");
    }

    @Test
    void unquotedOptionValueIsAnError() {
        assertThatThrownBy(() -> JtParser.parse("""
            out HtmlOut;
            option prefix = %;
            template T() {
            @end
            """, "t.jt"))
            .isInstanceOf(JtParseException.class)
            .satisfies(e -> assertThat(e.getMessage()).doesNotContain("null"));
    }

    @Test
    void unclosedExpressionNamesTheConfiguredOpener() {
        assertThatThrownBy(() -> JtParser.parseTextLine("a ${b\n", new ArrayList<>(), new Syntax("@", "${")))
            .isInstanceOf(JtParseException.class)
            .hasMessageContaining("unclosed '${'");
    }
}
