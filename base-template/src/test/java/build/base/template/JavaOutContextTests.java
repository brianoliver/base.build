package build.base.template;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the named output contexts of {@link JavaOut}.
 *
 * @author reed.vonredwitz
 * @since Oct-2026
 */
class JavaOutContextTests {

    private static String render(final Consumer<JavaOut> action) {
        final var out = new JavaOut();
        action.accept(out);
        return out.content();
    }

    private static void check(final BiConsumer<JavaOut, String> method, final Map<String, String> table) {
        table.forEach((input, expected) ->
            assertThat(render(o -> method.accept(o, input))).as("input: %s", input).isEqualTo(expected));
    }

    private static List<String> randomStrings() {
        final var random = new Random(42);
        final var alphabet = "\\\"'*/@<>&{}\n\r\t\u0000\u007f\u2028\u2029u0123abc\u00e9\uD83D\uDE00\uD83D\uDE00";
        final var strings = new java.util.ArrayList<String>();
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

    // ---- write ----

    @Test
    void shouldWriteVerbatim() {
        assertThat(render(o -> o.write("a \"quoted\" \\ <b>"))).isEqualTo("a \"quoted\" \\ <b>");
        assertThat(render(o -> o.write(null))).isEmpty();
    }

    // ---- writeStr ----

    @Test
    void shouldEscapeStr() {
        check(JavaOut::writeStr, Map.ofEntries(
            Map.entry("plain", "plain"),
            Map.entry("say \"hi\"", "say \\\"hi\\\""),
            Map.entry("it's", "it\\'s"),
            Map.entry("back\\slash", "back\\\\slash"),
            Map.entry("a\nb\rc\td", "a\\nb\\rc\\td"),
            Map.entry("\b\f", "\\b\\f"),
            Map.entry("\u0000\u0001\u001f\u007f", "\\000\\001\\037\\177"),
            Map.entry("\u2028\u2029", "\\u2028\\u2029"),
            Map.entry("caf\u00e9 \u4e2d\u6587", "caf\u00e9 \u4e2d\u6587"),
            Map.entry("emoji \uD83D\uDE00", "emoji \uD83D\uDE00"),
            Map.entry("lone \uD83D high", "lone \\ud83d high"),
            Map.entry("lone \uDE00 low", "lone \\ude00 low"),
            Map.entry("\\u000a", "\\\\u000a")));
    }

    @Test
    void shouldWriteNothingForNull() {
        assertThat(render(o -> o.writeStr(null))).isEmpty();
        assertThat(render(o -> o.writeJavadoc(null))).isEmpty();
    }

    @Test
    void shouldEscapeStrSoItCannotEndTheLiteralOrTheLine() {
        for (final var s : randomStrings()) {
            final var escaped = render(o -> o.writeStr(s));
            assertThat(escaped).as("input: %s", s)
                .doesNotContain("\n", "\r", "\u2028", "\u2029", "\u0000")
                .doesNotContainPattern("(?<!\\\\)(\\\\\\\\)*\"")
                .doesNotContainPattern("(?<!\\\\)(\\\\\\\\)*'");
        }
    }

    // ---- writeId ----

    @Test
    void shouldWriteValidIdentifiers() {
        for (final var id : List.of("name", "_x", "$y", "Caf\u00e9", "a1", "\u4e2d\u6587", "record", "var", "String")) {
            assertThat(render(o -> o.writeId(id))).isEqualTo(id);
        }
    }

    @Test
    void shouldRejectInvalidIdentifiers() {
        for (final var id : List.of("", " ", "a b", "a-b", "1a", "a.b", "class", "_", "true", "null", "int",
            "a\u200bb", "\u200ba", "x;y", "a\nb")) {
            assertThatThrownBy(() -> render(o -> o.writeId(id))).as("input: %s", id)
                .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> render(o -> o.writeId(null))).isInstanceOf(IllegalArgumentException.class);
    }

    // ---- writeJavadoc ----

    @Test
    void shouldEscapeJavadoc() {
        check(JavaOut::writeJavadoc, Map.ofEntries(
            Map.entry("plain text", "plain text"),
            Map.entry("a < b && c > d", "a &lt; b &amp;&amp; c &gt; d"),
            Map.entry("*/", "*&#47;"),
            Map.entry("a/b", "a/b"),
            Map.entry("/* nested */", "/* nested *&#47;"),
            Map.entry("{@link X} @author me", "{&#64;link X} &#64;author me"),
            Map.entry("\\u002a/", "&#92;u002a/"),
            Map.entry("caf\u00e9", "caf\u00e9")));
    }

    @Test
    void shouldEscapeJavadocSoItCannotCloseTheComment() {
        for (final var s : randomStrings()) {
            final var escaped = render(o -> o.writeJavadoc(s));
            assertThat(escaped).as("input: %s", s)
                .doesNotContain("*/", "\\", "@", "<", ">")
                .doesNotContainPattern("&(?!(amp|lt|gt|#64|#92|#47);)");
        }
    }
}
