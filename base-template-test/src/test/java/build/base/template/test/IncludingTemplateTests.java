package build.base.template.test;

import build.base.template.HtmlOut;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IncludingTemplateTests {

    @Test
    void shouldRenderIncludedTemplatesInPlace() {
        final var out = new HtmlOut();
        new IncludingTemplate("First").render(out);

        assertThat(out.toString()).isEqualTo("""
            <ul>
            <li class="item active">First</li>
            <li class="item">&lt;b&gt;other&lt;/b&gt;</li>
            </ul>
            """);
    }
}
