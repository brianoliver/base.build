package build.base.template.test;

import build.base.template.HtmlOut;
import build.base.template.Slots;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A page gives its layout named parts, and the layout renders each at its slot.
 *
 * @author reed.vonredwitz
 * @since Oct-2026
 */
class NamedSlotTemplateTests {

    @Test
    void shouldRenderEachPartAtItsSlot() {
        final var out = new HtmlOut();
        new SlotsPageTemplate("Tasks", List.of("a", "<b>")).render(out);

        assertThat(out.toString()).isEqualTo("""
            <html>
            <head>
            <title>Tasks</title>
            <meta name="count" content="2">
            </head>
            <body>
            <ul>
              <li>a</li>
              <li>&lt;b&gt;</li>
            </ul>
            <footer>
            <small>Tasks</small>
            </footer>
            </body>
            </html>
            """);
    }

    @Test
    void shouldFillALayoutFromJava() {
        final var out = new HtmlOut();
        new SlotsLayoutTemplate("T", Slots.<HtmlOut>of(o -> o.raw("<p>hi</p>\n"))
            .fill("footer", o -> o.raw("<small>f</small>\n"))).render(out);

        assertThat(out.toString()).contains("<footer>\n<small>f</small>\n</footer>");
    }

    @Test
    void shouldRenderNothingAtASlotThatWasNotFilled() {
        final var out = new HtmlOut();
        new SlotsLayoutTemplate("T", Slots.<HtmlOut>of(o -> o.raw("<p>hi</p>\n"))).render(out);

        assertThat(out.toString()).isEqualTo("""
            <html>
            <head>
            <title>T</title>
            </head>
            <body>
            <p>hi</p>
            <footer>
            </footer>
            </body>
            </html>
            """);
    }
}
