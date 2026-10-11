package build.base.template;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Slots}.
 *
 * @author reed.vonredwitz
 * @since Oct-2026
 */
class SlotsTests {

    @Test
    void shouldRenderThePartOfAName() {
        final var slots = new Slots<>(Map.<String, Template<HtmlOut>>of("head", o -> o.raw("<meta>")));
        final var out = new HtmlOut();
        slots.render("head", out);

        assertThat(out.toString()).isEqualTo("<meta>");
        assertThat(slots.has("head")).isTrue();
    }

    @Test
    void shouldRenderNothingForAPartThatWasNotGiven() {
        final var slots = new Slots<>(Map.<String, Template<HtmlOut>>of());
        final var out = new HtmlOut();
        slots.render("foot", out);
        slots.render(out);

        assertThat(out.toString()).isEmpty();
        assertThat(slots.has("foot")).isFalse();
    }

    @Test
    void shouldBuildSlotsFromJava() {
        final Template<HtmlOut> slots = Slots.<HtmlOut>of(o -> o.raw("body"))
            .fill("head", o -> o.raw("head"))
            .fill("head", o -> o.raw("head2"));
        final var out = new HtmlOut();
        slots.render(out);
        ((Slots<HtmlOut>) slots).render("head", out);

        assertThat(out.toString()).isEqualTo("bodyhead2");
    }

    @Test
    void shouldRenderTheBodyAsATemplate() {
        final Template<HtmlOut> slots = new Slots<>(Map.<String, Template<HtmlOut>>of(Slots.BODY, o -> o.raw("hi")));
        final var out = new HtmlOut();
        slots.render(out);

        assertThat(out.toString()).isEqualTo("hi");
    }
}
