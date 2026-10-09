package build.base.template.test;

import build.base.template.HtmlOut;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A page renders its body inside a layout, where the layout includes it.
 *
 * @author reed.vonredwitz
 * @since Oct-2026
 */
class LayoutTemplateTests {

    @Test
    void shouldRenderThePageInsideTheLayout() {
        final var out = new HtmlOut();
        new PageTemplate("Tasks", List.of("a", "<b>")).render(out);

        assertThat(out.toString()).isEqualTo("""
            <html>
            <head><title>Tasks</title></head>
            <body>
            <ul>
              <li>a</li>
              <li>&lt;b&gt;</li>
            </ul>
            </body>
            </html>
            """);
    }

    @Test
    void shouldRenderAnyBodyInTheLayout() {
        final var out = new HtmlOut();
        new LayoutTemplate("T", o -> o.raw("<p>hi</p>\n")).render(out);

        assertThat(out.toString()).contains("<body>\n<p>hi</p>\n</body>");
    }
}
