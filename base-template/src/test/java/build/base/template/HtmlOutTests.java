package build.base.template;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HtmlOut}.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
class HtmlOutTests {

    @Test
    void shouldEmitRawWithoutEscaping() {
        final var out = new HtmlOut();
        out.raw("<h1>Hello</h1>");
        assertThat(out.toString()).isEqualTo("<h1>Hello</h1>");
    }

    @Test
    void shouldEscapeHtmlEntities() {
        final var out = new HtmlOut();
        out.write("<script>alert('xss')</script>");
        assertThat(out.toString()).isEqualTo("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;");
    }

    @Test
    void shouldEscapeAmpersand() {
        final var out = new HtmlOut();
        out.write("cats & dogs");
        assertThat(out.toString()).isEqualTo("cats &amp; dogs");
    }

    @Test
    void shouldEscapeDoubleQuote() {
        final var out = new HtmlOut();
        out.write("say \"hello\"");
        assertThat(out.toString()).isEqualTo("say &quot;hello&quot;");
    }

    @Test
    void shouldNotEscapeSafeStrings() {
        final var out = new HtmlOut();
        out.write("Hello World 123");
        assertThat(out.toString()).isEqualTo("Hello World 123");
    }

    @Test
    void shouldHandleNullWrite() {
        final var out = new HtmlOut();
        out.write(null);
        assertThat(out.toString()).isEmpty();
    }

    @Test
    void shouldInterleaveRawAndWrite() {
        final var out = new HtmlOut();
        out.raw("<li>");
        out.write("<b>bold</b>");
        out.raw("</li>");
        assertThat(out.toString()).isEqualTo("<li>&lt;b&gt;bold&lt;/b&gt;</li>");
    }

    @Test
    void shouldEmitSafeHtmlWithoutEscaping() {
        final var out = new HtmlOut();
        out.write(SafeHtml.trusted("<b>bold</b>"));
        assertThat(out.toString()).isEqualTo("<b>bold</b>");
    }

    @Test
    void shouldRenderTemplateParameterWithoutDoubleEscaping() {
        final Template<HtmlOut> inner = o -> {
            o.raw("<span>");
            o.write("a < b");
            o.raw("</span>");
        };
        final var out = new HtmlOut();
        out.raw("<div>");
        out.write(inner);
        out.raw("</div>");
        assertThat(out.toString()).isEqualTo("<div><span>a &lt; b</span></div>");
    }

    @Test
    void shouldRenderTemplateAcceptingSuperTypeOfHtmlOut() {
        final Template<Out> inner = o -> o.write("x & y");
        final var out = new HtmlOut();
        out.write(inner);
        assertThat(out.toString()).isEqualTo("x &amp; y");
    }

    @Test
    void shouldStillEscapeStringsAlongsideSafeHtmlAndTemplates() {
        final Template<HtmlOut> inner = o -> o.raw("<i>t</i>");
        final var out = new HtmlOut();
        out.write("<p>");
        out.write(SafeHtml.trusted("<b>"));
        out.write(inner);
        assertThat(out.toString()).isEqualTo("&lt;p&gt;<b><i>t</i>");
    }

    @Test
    void shouldComposeTemplates() {
        final Template<HtmlOut> inner = out -> {
            out.raw("<span>");
            out.write("hello");
            out.raw("</span>");
        };
        final var out = new HtmlOut();
        out.raw("<div>");
        inner.render(out);
        out.raw("</div>");
        assertThat(out.toString()).isEqualTo("<div><span>hello</span></div>");
    }
}
