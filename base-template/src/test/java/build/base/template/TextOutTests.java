package build.base.template;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TextOut}.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
class TextOutTests {

    @Test
    void shouldEmitRawUnchanged() {
        final var out = new TextOut();
        out.raw("#!/bin/sh\n");
        assertThat(out.toString()).isEqualTo("#!/bin/sh\n");
    }

    @Test
    void shouldWriteWithoutEscaping() {
        final var out = new TextOut();
        out.write("<not escaped>");
        assertThat(out.toString()).isEqualTo("<not escaped>");
    }

    @Test
    void shouldHandleNullWrite() {
        final var out = new TextOut();
        out.write(null);
        assertThat(out.toString()).isEmpty();
    }
}
