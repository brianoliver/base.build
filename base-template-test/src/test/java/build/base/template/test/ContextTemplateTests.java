package build.base.template.test;

import build.base.template.HtmlOut;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests rendering a template that writes to each named output context of {@link HtmlOut}.
 *
 * @author reed.vonredwitz
 * @since Oct-2026
 */
class ContextTemplateTests {

    private static String render(final ContextTemplate template) {
        final var out = new HtmlOut();
        template.render(out);
        return out.content();
    }

    @Test
    void shouldWriteEachContext() {
        final var html = render(new ContextTemplate("/a b?x=1&y=2", "t\"<", Map.of("id", 1), "a;b"));

        assertThat(html)
            .contains("href=\"/a%20b?x=1&amp;y=2\"")
            .contains("title=\"t&quot;&lt;\"")
            .contains("hx-vals=\"{&quot;id&quot;:1}\"")
            .contains(">t&quot;&lt;</a>")
            .contains("var t = \"t\\\"\\u003c\";")
            .contains("content: \"a\\3b b\";")
            .contains("<b>raw</b>");
    }

    @Test
    void shouldRejectUnsafeUrl() {
        assertThat(render(new ContextTemplate("javascript:alert(1)", "t", Map.of(), "")))
            .contains("href=\"#unsafe-url\"");
    }

    @Test
    void shouldLeaveUndeclaredNamesAndEscapesAsText() {
        final var html = render(new ContextTemplate("/", "t", Map.of(), ""));

        assertThat(html)
            .contains("#nav{ color: red }")
            .contains("#url{literal}");
    }
}
