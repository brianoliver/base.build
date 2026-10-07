package build.base.template;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
