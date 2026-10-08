package build.base.template;

import build.base.json.Json;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the named output contexts of {@link HtmlOut}.
 *
 * @author reed.vonredwitz
 * @since Oct-2026
 */
class HtmlOutContextTests {

    private static String render(final Consumer<HtmlOut> action) {
        final var out = new HtmlOut();
        action.accept(out);
        return out.content();
    }

    private static void check(final BiConsumer<HtmlOut, String> method, final Map<String, String> table) {
        table.forEach((input, expected) ->
            assertThat(render(o -> method.accept(o, input))).as("input: %s", input).isEqualTo(expected));
    }

    /**
     * Strings that stress every context: markup, quotes, NUL, controls, line separators, BMP and surrogate pairs.
     */
    private static final List<String> HOSTILE = List.of(
        "",
        "plain",
        "<script>alert(1)</script>",
        "</script><!--",
        "\"'><img src=x onerror=alert(1)>",
        "a & b &amp; c",
        "\u0000nul\u0001\u001f",
        "line\u2028sep\u2029",
        "caf\u00e9 \u4e2d\u6587",
        "emoji \uD83D\uDE00 pair",
        "lone \uD83D high",
        "lone \uDE00 low",
        "javascript:alert(1)",
        "back\\slash",
        "}</style><script>");

    private static List<String> randomStrings() {
        final var random = new Random(42);
        final var alphabet = "<>\"'&\\/:;%#?= \u0000\u2028abcXYZ019\u00e9\uD83D\uDE00";
        final var strings = new ArrayList<>(HOSTILE);
        for (int i = 0; i < 500; i++) {
            final var sb = new StringBuilder();
            final int length = random.nextInt(24);
            for (int j = 0; j < length; j++) {
                sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            strings.add(sb.toString());
        }
        return strings;
    }

    // ---- writeAttr ----

    @Test
    void shouldEscapeAttr() {
        check(HtmlOut::writeAttr, Map.of(
            "a&b", "a&amp;b",
            "<b>", "&lt;b&gt;",
            "say \"hi\"", "say &quot;hi&quot;",
            "it's", "it&#39;s",
            "plain", "plain"));
    }

    @Test
    void shouldWriteNothingForNull() {
        assertThat(render(o -> o.writeAttr(null))).isEmpty();
        assertThat(render(o -> o.writeUrl(null))).isEmpty();
        assertThat(render(o -> o.writeCss(null))).isEmpty();
    }

    @Test
    void shouldEscapeSafeHtmlInAttr() {
        assertThat(render(o -> o.writeAttr(SafeHtml.trusted("<b>")))).isEqualTo("&lt;b&gt;");
    }

    // ---- writeUrl ----

    @Test
    void shouldAllowAndEncodeUrls() {
        check(HtmlOut::writeUrl, Map.ofEntries(
            Map.entry("/tasks/42/toggle", "/tasks/42/toggle"),
            Map.entry("relative/path?x=1&y=2", "relative/path?x=1&amp;y=2"),
            Map.entry("https://example.com/a#frag", "https://example.com/a#frag"),
            Map.entry("HTTP://EXAMPLE.COM", "HTTP://EXAMPLE.COM"),
            Map.entry("mailto:a@b.c", "mailto:a@b.c"),
            Map.entry("/a b", "/a%20b"),
            Map.entry("/a%20b", "/a%20b"),
            Map.entry("/caf\u00e9", "/caf%C3%A9"),
            Map.entry("/q?x=\">", "/q?x=%22%3E"),
            Map.entry("/it's", "/it%27s"),
            Map.entry("/back\\slash", "/back%5Cslash"),
            Map.entry("  /padded  ", "/padded")));
    }

    @Test
    void shouldRejectUnsafeSchemes() {
        for (final var url : List.of(
            "javascript:alert(1)",
            "JaVaScRiPt:alert(1)",
            "  javascript:alert(1)",
            "data:text/html;base64,AAAA",
            "vbscript:x",
            "file:///etc/passwd",
            "ftp://example.com")) {
            assertThat(render(o -> o.writeUrl(url))).as(url).isEqualTo(HtmlOut.UNSAFE_URL);
        }
    }

    @Test
    void shouldRejectProtocolRelativeUrls() {
        for (final var url : List.of("//evil.example.com/x", "  //evil.example.com", "///evil.example.com")) {
            assertThat(render(o -> o.writeUrl(url))).as(url).isEqualTo(HtmlOut.UNSAFE_URL);
        }
        // a single leading slash is a path on this host, and "//" later in a URL is just a path
        assertThat(render(o -> o.writeUrl("/a//b"))).isEqualTo("/a//b");
        assertThat(render(o -> o.writeUrl("https://example.com//x"))).isEqualTo("https://example.com//x");
    }

    @Test
    void shouldRejectUrlsWithControlCharacters() {
        // browsers ignore tabs and newlines inside a scheme
        assertThat(render(o -> o.writeUrl("java\tscript:alert(1)"))).isEqualTo(HtmlOut.UNSAFE_URL);
        assertThat(render(o -> o.writeUrl("java\nscript:alert(1)"))).isEqualTo(HtmlOut.UNSAFE_URL);
        assertThat(render(o -> o.writeUrl("/a\u0000b"))).isEqualTo(HtmlOut.UNSAFE_URL);
    }

    @Test
    void shouldTreatColonAfterPathAsRelative() {
        assertThat(render(o -> o.writeUrl("/a:b"))).isEqualTo("/a:b");
        assertThat(render(o -> o.writeUrl("?next=javascript:x"))).isEqualTo("?next=javascript:x");
    }

    // ---- writeJson ----

    @Test
    void shouldWriteJsonInAttribute() {
        final var map = new LinkedHashMap<String, Object>();
        map.put("id", 42);
        map.put("name", "a \"b\" <c>");
        map.put("tags", List.of("x", "y"));
        map.put("done", false);
        map.put("none", null);
        assertThat(render(o -> o.writeJson(map)))
            .isEqualTo("{&quot;id&quot;:42,&quot;name&quot;:&quot;a \\&quot;b\\&quot; &lt;c&gt;&quot;,"
                + "&quot;tags&quot;:[&quot;x&quot;,&quot;y&quot;],&quot;done&quot;:false,&quot;none&quot;:null}");
    }

    @Test
    void shouldWriteJsonScalarsArraysAndValues() {
        assertThat(render(o -> o.writeJson(null))).isEqualTo("null");
        assertThat(render(o -> o.writeJson(7))).isEqualTo("7");
        assertThat(render(o -> o.writeJson(true))).isEqualTo("true");
        assertThat(render(o -> o.writeJson(new Object[]{1, "a"}))).isEqualTo("[1,&quot;a&quot;]");
        assertThat(render(o -> o.writeJson(Json.of("v")))).isEqualTo("&quot;v&quot;");
        assertThat(render(o -> o.writeJson(Map.of()))).isEqualTo("{}");
    }

    @Test
    void shouldRejectUnsupportedJsonValues() {
        assertThatThrownBy(() -> render(o -> o.writeJson(new Object())))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> render(o -> o.writeJson(Map.of(1, "a"))))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> render(o -> o.writeJson(List.of(new Object()))))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- writeJs ----

    @Test
    void shouldWriteJsLiteral() {
        assertThat(render(o -> o.writeJs("hi"))).isEqualTo("\"hi\"");
        assertThat(render(o -> o.writeJs(List.of(1, 2)))).isEqualTo("[1,2]");
    }

    @Test
    void shouldNotAllowScriptBreakout() {
        final var js = render(o -> o.writeJs("</script><!-- & \u2028\u2029"));
        assertThat(js)
            .isEqualTo("\"\\u003c/script\\u003e\\u003c!-- \\u0026 \\u2028\\u2029\"");
        assertThat(js).doesNotContain("<", ">", "&", "\u2028", "\u2029");
    }

    // ---- writeCss ----

    @Test
    void shouldEscapeCss() {
        check(HtmlOut::writeCss, Map.of(
            "red", "red",
            "a-b_c 1", "a-b_c 1",
            "a;b", "a\\3b b",
            "\\", "\\5c ",
            "\"", "\\22 ",
            "</style>", "\\3c \\2f style\\3e "));
    }

    @Test
    void shouldEscapeCssSupplementaryCodePointsAsOne() {
        assertThat(render(o -> o.writeCss("\uD83D\uDE00"))).isEqualTo("\\1f600 ");
    }

    // ---- properties ----

    private static void forEachContext(final BiConsumer<String, BiConsumer<HtmlOut, String>> body) {
        body.accept("write", HtmlOut::write);
        body.accept("writeAttr", HtmlOut::writeAttr);
        body.accept("writeUrl", HtmlOut::writeUrl);
        body.accept("writeJson", HtmlOut::writeJson);
        body.accept("writeCss", HtmlOut::writeCss);
    }

    @Test
    void shouldNeverEmitMarkupOrQuoteCharacters() {
        for (final var input : randomStrings()) {
            forEachContext((name, method) -> {
                final var out = new HtmlOut();
                method.accept(out, input);
                assertThat(out.content())
                    .as("%s(%s)", name, input)
                    .doesNotContain("<", ">", "\"", "'");
            });
        }
    }

    @Test
    void shouldNeverEmitMarkupFromJs() {
        for (final var input : randomStrings()) {
            final var js = render(o -> o.writeJs(input));
            assertThat(js).as("writeJs(%s)", input).doesNotContain("<", ">", "&", "\u2028", "\u2029");
        }
    }

    @Test
    void shouldLeaveNoBareAmpersandInAttributeContexts() {
        // every '&' that is emitted must start an entity we produced
        for (final var input : randomStrings()) {
            forEachContext((name, method) -> {
                final var out = new HtmlOut();
                method.accept(out, input);
                assertThat(out.content().replaceAll("&(amp|lt|gt|quot|#39);", ""))
                    .as("%s(%s)", name, input)
                    .doesNotContain("&");
            });
        }
    }

    @Test
    void shouldRoundTripJsonThroughAttributeEscaping() {
        for (final var input : randomStrings()) {
            final var encoded = render(o -> o.writeJson(input));
            final var unescaped = encoded
                .replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
            assertThat(Json.parse(unescaped).asString().value()).as("input: %s", input).isEqualTo(input);
        }
    }

    @Test
    void shouldAlwaysYieldAllowedUrlOrPlaceholder() {
        for (final var input : randomStrings()) {
            final var url = render(o -> o.writeUrl(input)).replace("&amp;", "&");
            assertThat(url).as("writeUrl(%s)", input)
                .satisfiesAnyOf(
                    u -> assertThat(u).isEqualTo(HtmlOut.UNSAFE_URL),
                    u -> assertThat(u).doesNotContainPattern("^(?i)(?!https?:|mailto:)[a-z][a-z0-9+.-]*:"));
        }
    }
}
