package build.base.template;

import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutTests {

    @Test
    void contentReturnsBufferedOutput() {
        final var out = new TextOut();
        out.raw("hello");

        assertThat(out.content()).isEqualTo("hello");
    }

    @Test
    void contentFailsLoudlyWhenWriterBacked() {
        final var writer = new StringWriter();
        final var out = new TextOut(writer);
        out.raw("hello");

        assertThatThrownBy(out::content)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Writer");
        assertThat(writer).hasToString("hello");
    }

    @Test
    void toStringMatchesContentWhenBuffered() {
        final var out = new TextOut();
        out.raw("hello");

        assertThat(out).hasToString(out.content());
    }

    @Test
    void toStringDoesNotThrowWhenWriterBacked() {
        final var out = new TextOut(new StringWriter());
        out.raw("hello");

        assertThat(out.toString()).isEmpty();
    }
}
