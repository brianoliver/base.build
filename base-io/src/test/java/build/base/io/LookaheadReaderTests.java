package build.base.io;

import build.base.foundation.Strings;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link LookaheadReader}s.
 *
 * @author brian.oliver
 * @since Jan-2024
 */
public class LookaheadReaderTests {

    /**
     * Ensure a {@link LookaheadReader} isn't created for a {@code null} {@link java.io.Reader}.
     */
    @Test
    void shouldNotCreateLookaheadReader() {
        assertThrows(NullPointerException.class, () -> new LookaheadReader(null));
    }

    /**
     * Ensure growing the buffer part way through keeps the unconsumed content.
     */
    @Test
    void shouldKeepUnconsumedContentWhenBufferGrows() {
        final var reader = new LookaheadReader(new StringReader("abcdefgh"), 4);

        assertThat(reader.consume(2)).isEqualTo("ab");
        assertThat(reader.peek(6).toString()).isEqualTo("cdefgh");
    }

    /**
     * Ensure a {@link LookaheadReader} can be created for an empty {@link java.io.Reader}.
     */
    @Test
    void shouldCreateLookaheadReaderForAnEmptyString()
        throws IOException {

        final var reader = new LookaheadReader(new StringReader(""));

        assertThat(reader.markSupported())
            .isFalse();

        assertThatExceptionOfType(IOException.class)
            .isThrownBy(() -> reader.mark(1));

        assertThatExceptionOfType(IOException.class)
            .isThrownBy(reader::reset);

        assertThat(reader.ready())
            .isTrue();

        assertThat(reader.available())
            .isFalse();

        assertThat(reader.getLocation())
            .isEqualTo(LookaheadReader.Location.START);

        assertThat(reader.read())
            .isEqualTo(-1);

        assertThat(reader.consume())
            .isEqualTo(-1);

        assertThat(reader.peek())
            .isEqualTo(-1);

        assertThat(reader.follows("Ozzie"))
            .isFalse();

        assertThat(reader.follows(__ -> true))
            .isFalse();

        assertThat(reader.consume(42))
            .isEqualTo("");
    }

    /**
     * Ensure a {@link LookaheadReader} can be created for a single line {@link java.io.Reader}.
     */
    @Test
    void shouldCreateLookaheadReaderForASingleLineOfContent()
        throws IOException {

        final var reader = new LookaheadReader(new StringReader("**Hello World**"));

        assertThat(reader.markSupported())
            .isFalse();

        assertThatExceptionOfType(IOException.class)
            .isThrownBy(() -> reader.mark(1));

        assertThatExceptionOfType(IOException.class)
            .isThrownBy(reader::reset);

        assertThat(reader.ready())
            .isTrue();

        assertThat(reader.available())
            .isTrue();

        assertThat(reader.getLocation())
            .isEqualTo(LookaheadReader.Location.START);

        assertThat(reader.peek())
            .isEqualTo('*');

        assertThat(reader.peek(2))
            .isEqualTo("**");

        final var expectedHelloWorld = "**Hello World**";
        assertThat(reader.peek(expectedHelloWorld.length()))
            .isEqualTo(expectedHelloWorld);

        assertThat(reader.read())
            .isEqualTo('*');

        assertThat(reader.consume())
            .isEqualTo('*');

        assertThat(reader.follows("Hello"))
            .isTrue();

        assertThat(reader.consume(5))
            .isEqualTo("Hello");

        assertThat(reader.follows(x -> x == (int) ' '))
            .isTrue();

        assertThat(reader.skip(1))
            .isEqualTo(1L);

        final var expectedWorld = "World**";
        assertThat(reader.peek(expectedWorld.length()))
            .isEqualTo(expectedWorld);

        assertThat(reader.peek(5))
            .isEqualTo("World");

        final char[] chars = new char[5];
        assertThat(reader.read(chars, 0, 5))
            .isEqualTo(5);

        assertThat(new String(chars))
            .isEqualTo("World");

        assertThat(reader.consume(2))
            .isEqualTo("**");

        assertThat(reader.available())
            .isFalse();

        assertThat(reader.getLocation())
            .isEqualTo(LookaheadReader.Location.of(1, 16));

        assertThat(reader.peek())
            .isEqualTo(-1);
    }

    /**
     * Ensure a {@link LookaheadReader} can be created for {@link java.io.Reader} with multi-line content.
     */
    @Test
    void shouldCreateLookaheadReaderForMultiLineContent()
        throws IOException {

        final var reader = new LookaheadReader(new StringReader("h\ne\nl\nl\no\n"));

        assertThat(reader.markSupported())
            .isFalse();

        assertThatExceptionOfType(IOException.class)
            .isThrownBy(() -> reader.mark(1));

        assertThatExceptionOfType(IOException.class)
            .isThrownBy(reader::reset);

        assertThat(reader.ready())
            .isTrue();

        assertThat(reader.available())
            .isTrue();

        assertThat(reader.getLocation())
            .isEqualTo(LookaheadReader.Location.START);

        assertThat(reader.read())
            .isEqualTo('h');

        assertThat(reader.peek())
            .isEqualTo('\n');

        assertThat(reader.consume())
            .isEqualTo('\n');

        assertThat(reader.getLocation())
            .isEqualTo(LookaheadReader.Location.of(2, 1));

        assertThat(reader.skip(2))
            .isEqualTo(2L);

        assertThat(reader.consume())
            .isEqualTo('l');

        final var expected = "\nl\no\n";
        assertThat(reader.peek(expected.length()))
            .isEqualTo(expected);

        reader.skipWhile(__ -> true);

        assertThat(reader.available())
            .isFalse();

        assertThat(reader.getLocation())
            .isEqualTo(LookaheadReader.Location.of(6, 1));

        assertThat(reader.peek())
            .isEqualTo(-1);
    }

    /**
     * Ensure a {@link LookaheadReader#peek(int)} adjusts the buffer size attempting to peek beyond the buffer size.
     */
    @Test
    void shouldLookaheadPeekNSelfAdjustsBufferSize() {
        final var string = Strings.repeat("a", 256);
        final var initialBufferSize = 5;

        final var reader = new LookaheadReader(new StringReader(string), initialBufferSize);

        assertThat(reader.peek(1))
            .isEqualTo("a");

        assertThat(reader.peek(initialBufferSize))
            .isEqualTo(Strings.repeat("a", initialBufferSize));

        assertThat(reader.peek(initialBufferSize * 2))
            .isEqualTo(Strings.repeat("a", initialBufferSize * 2));
    }

    /**
     * Ensure a {@link LookaheadReader#consume(int)} adjusts the buffer size attempting to consume beyond the buffer
     * size.
     */
    @Test
    void shouldLookaheadConsumeNSelfAdjustsBufferSize() {
        final var string = Strings.repeat("a", 256);
        final var initialBufferSize = 5;

        final var reader = new LookaheadReader(new StringReader(string), initialBufferSize);

        assertThat(reader.consume(1))
            .isEqualTo("a");

        assertThat(reader.consume(initialBufferSize))
            .isEqualTo(Strings.repeat("a", initialBufferSize));

        assertThat(reader.consume(initialBufferSize * 2))
            .isEqualTo(Strings.repeat("a", initialBufferSize * 2));
    }

    // prepare() previously passed DEFAULT_LOOKAHEAD as the desired size even for single-character
    // operations, so "remaining < desired" became true again after consuming just one character
    // from a full buffer, forcing a fresh underlying read() on every single consume(). Asserting
    // on the raw read() call count (rather than just consumed output) is what catches that.
    @Test
    void shouldNotRereadUnderlyingStreamForEverySingleCharacterConsumed() {
        final var content = Strings.repeat("a", 10_000);
        final var readCalls = new AtomicInteger();

        final var reader = new LookaheadReader(new StringReader(content) {
            @Override
            public int read(char [] cbuf, int off, int len) throws IOException {
                readCalls.incrementAndGet();
                return super.read(cbuf, off, len);
            }
        });

        int consumed = 0;
        while (reader.available()) {
            reader.consume();
            consumed++;
        }

        assertThat(consumed)
            .isEqualTo(content.length());

        assertThat(readCalls.get())
            .isLessThan(content.length() / 10);
    }
}
