package build.base.template.test;

import build.base.template.JavaOut;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.tools.ToolProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the named contexts of {@link JavaOut} the way that matters: the generated source compiles, and the values
 * come back unchanged.
 *
 * @author reed.vonredwitz
 * @since Oct-2026
 */
class JavaContextTemplateTests {

    private static final Map<String, String> HOSTILE = new LinkedHashMap<>();

    static {
        HOSTILE.put("quotes", "say \"hi\" and 'bye'");
        HOSTILE.put("backslashes", "C:\\temp\\new");
        HOSTILE.put("lineBreaks", "a\nb\rc\r\nd");
        HOSTILE.put("unicodeEscape", "\\u000a // not a newline");
        HOSTILE.put("unicodeQuote", "\\u0022 + \"x");
        HOSTILE.put("controls", "\u0000\u0001\u001f\u007f");
        HOSTILE.put("separators", "\u2028\u2029");
        HOSTILE.put("text", "caf\u00e9 \u4e2d\u6587 \uD83D\uDE00");
        HOSTILE.put("lone", "\uD83D");
    }

    @Test
    void shouldGenerateSourceThatCompilesAndKeepsValues(@TempDir final Path dir) throws Exception {
        final var out = new JavaOut();
        new JavaContextTemplate("Hostile", "end */ \\u002a/ @author {@link X} <b>", HOSTILE).render(out);
        final var source = out.content();

        final var file = dir.resolve("Hostile.java");
        Files.writeString(file, source);
        final var compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler).as("a JDK is required").isNotNull();
        final var diagnostics = new java.io.ByteArrayOutputStream();
        final int result = compiler.run(null, diagnostics, diagnostics, "-d", dir.toString(), file.toString());
        assertThat(result).as(() -> diagnostics + "\n" + source).isZero();

        try (var loader = new URLClassLoader(new java.net.URL[]{dir.toUri().toURL()})) {
            final var type = loader.loadClass("Hostile");
            for (final var entry : HOSTILE.entrySet()) {
                assertThat(type.getField(entry.getKey()).get(null)).as(entry.getKey()).isEqualTo(entry.getValue());
            }
        }
    }

    @Test
    void shouldRejectAnInvalidIdentifier() {
        assertThatThrownBy(() -> new JavaContextTemplate("Ok", "doc", Map.of("not valid", "x")).render(new JavaOut()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JavaContextTemplate("class", "doc", Map.of()).render(new JavaOut()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRenderReadableSource() {
        final var out = new JavaOut();
        new JavaContextTemplate("Greeting", "Says <hello>.", Map.of("HELLO", "hi \"you\"")).render(out);

        assertThat(out.content()).isEqualTo("""
            /** Says &lt;hello&gt;. */
            public class Greeting {
                public static final String HELLO = "hi \\"you\\"";
            }
            """);
    }
}
