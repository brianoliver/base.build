package build.base.template.test;

import build.base.template.HtmlOut;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A fragment renders in place within its template and, as a template of its own, on its own.
 *
 * @author reed.vonredwitz
 * @since Oct-2026
 */
class FragmentTemplateTests {

    @Test
    void shouldRenderTheFragmentInPlace() {
        final var out = new HtmlOut();
        new FragmentTemplate("Tasks", List.of("a", "<b>")).render(out);

        assertThat(out.toString()).isEqualTo("""
            <h1>Tasks</h1>
            <ul>
              <li id="item-0">a</li>
              <li id="item-1">&lt;b&gt;</li>
            </ul>
            """);
    }

    @Test
    void shouldRenderTheFragmentOnItsOwn() {
        final var out = new HtmlOut();
        new FragmentTemplate.Row("a", 0).render(out);

        assertThat(out.toString()).isEqualTo("  <li id=\"item-0\">a</li>\n");
    }
}
