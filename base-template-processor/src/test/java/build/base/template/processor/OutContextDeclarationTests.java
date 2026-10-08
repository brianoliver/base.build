package build.base.template.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import javax.tools.Diagnostic;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the processor through {@code javac} to cover how it reads {@code build.base.template.OutContext}
 * declarations from an out type, including the errors it reports for unusable ones.
 *
 * @author reed.vonredwitz
 * @since Oct-2026
 */
class OutContextDeclarationTests {

    private static final String TEMPLATE = """
        out com.acme.CtxOut;
        package com.acme;
        
        template T(String u) {
        #url{u}
        @end
        """;

    private static String out(final String members) {
        return """
            package com.acme;
            
            import build.base.template.OutContext;
            
            public final class CtxOut extends build.base.template.Out {
                @Override
                public void write(final Object value) {
                    raw(String.valueOf(value));
                }
            
            %s
            }
            """.formatted(members);
    }

    private static ProcessorHarness.Result run(final Path dir, final Map<String, String> sources) throws IOException {
        return ProcessorHarness.run(dir, Map.of("com/acme/T.jt", TEMPLATE), sources);
    }

    private static ProcessorHarness.Result runWithMembers(final Path dir, final String members) throws IOException {
        return run(dir, Map.of("com/acme/CtxOut.java", out(members)));
    }

    @Test
    void shouldCompileDeclaredContextToItsMethod(@TempDir final Path dir) throws IOException {
        final var result = runWithMembers(dir, """
            @OutContext("url")
            public void writeUrl(final Object value) {
                raw(String.valueOf(value));
            }
            """);

        assertThat(result.success()).as(result.allMessages()).isTrue();
        assertThat(Files.readString(result.generated().resolve("com/acme/T.java"))).contains("out.writeUrl(u);");
    }

    @Test
    void shouldDefaultContextNameToMethodName(@TempDir final Path dir) throws IOException {
        final var result = runWithMembers(dir, """
            @OutContext
            public void url(final Object value) {
                raw(String.valueOf(value));
            }
            """);

        assertThat(result.success()).as(result.allMessages()).isTrue();
        assertThat(Files.readString(result.generated().resolve("com/acme/T.java"))).contains("out.url(u);");
    }

    @Test
    void shouldInheritContextFromOverriddenMethod(@TempDir final Path dir) throws IOException {
        final var result = run(dir, Map.of(
            "com/acme/BaseOut.java", """
                package com.acme;
                
                public abstract class BaseOut extends build.base.template.Out {
                    @Override
                    public void write(final Object value) {
                        raw(String.valueOf(value));
                    }
                
                    @build.base.template.OutContext("url")
                    public void writeUrl(final Object value) {
                        raw(String.valueOf(value));
                    }
                }
                """,
            "com/acme/MidOut.java", """
                package com.acme;
                
                public abstract class MidOut extends BaseOut {
                }
                """,
            "com/acme/CtxOut.java", """
                package com.acme;
                
                public final class CtxOut extends MidOut {
                    @Override
                    public void writeUrl(final Object value) {
                        raw("override:" + value);
                    }
                }
                """));

        assertThat(result.success()).as(result.allMessages()).isTrue();
        assertThat(Files.readString(result.generated().resolve("com/acme/T.java"))).contains("out.writeUrl(u);");
    }

    @Test
    void shouldInheritContextFromOverriddenInterfaceMethod(@TempDir final Path dir) throws IOException {
        final var result = run(dir, Map.of(
            "com/acme/Urls.java", """
                package com.acme;
                
                public interface Urls {
                    @build.base.template.OutContext("url")
                    void writeUrl(Object value);
                }
                """,
            "com/acme/CtxOut.java", """
                package com.acme;
                
                public final class CtxOut extends build.base.template.Out implements Urls {
                    @Override
                    public void write(final Object value) {
                        raw(String.valueOf(value));
                    }
                
                    @Override
                    public void writeUrl(final Object value) {
                        raw(String.valueOf(value));
                    }
                }
                """));

        assertThat(result.success()).as(result.allMessages()).isTrue();
        assertThat(Files.readString(result.generated().resolve("com/acme/T.java"))).contains("out.writeUrl(u);");
    }

    @Test
    void shouldReportNameThatIsNotAnIdentifier(@TempDir final Path dir) throws IOException {
        final var result = runWithMembers(dir, """
            @OutContext("not-an-id")
            public void writeUrl(final Object value) {
            }
            """);

        assertThat(result.success()).isFalse();
        assertThat(result.messages(Diagnostic.Kind.ERROR))
            .anySatisfy(m -> assertThat(m).contains("CtxOut#writeUrl").contains("'not-an-id'").contains("usable name"));
    }

    @Test
    void shouldReportJavaKeywordAsName(@TempDir final Path dir) throws IOException {
        final var result = runWithMembers(dir, """
            @OutContext("class")
            public void writeUrl(final Object value) {
            }
            """);

        assertThat(result.success()).isFalse();
        assertThat(result.messages(Diagnostic.Kind.ERROR))
            .anySatisfy(m -> assertThat(m).contains("'class'").contains("usable name"));
    }

    @Test
    void shouldReportReservedNames(@TempDir final Path dir) throws IOException {
        for (final var reserved : new String[]{"raw", "write", "include"}) {
            final var result = runWithMembers(dir.resolve(reserved), """
                @OutContext("%s")
                public void writeUrl(final Object value) {
                }
                """.formatted(reserved));

            assertThat(result.success()).as(reserved).isFalse();
            assertThat(result.messages(Diagnostic.Kind.ERROR))
                .as(reserved)
                .anySatisfy(m -> assertThat(m).contains("'" + reserved + "'").contains("usable name"));
        }
    }

    @Test
    void shouldReportMethodThatIsNotPublic(@TempDir final Path dir) throws IOException {
        final var result = runWithMembers(dir, """
            @OutContext("url")
            void writeUrl(final Object value) {
            }
            """);

        assertThat(result.success()).isFalse();
        assertThat(result.messages(Diagnostic.Kind.ERROR))
            .anySatisfy(m -> assertThat(m).contains("CtxOut#writeUrl").contains("public instance method"));
    }

    @Test
    void shouldReportStaticMethod(@TempDir final Path dir) throws IOException {
        final var result = runWithMembers(dir, """
            @OutContext("url")
            public static void writeUrl(final Object value) {
            }
            """);

        assertThat(result.success()).isFalse();
        assertThat(result.messages(Diagnostic.Kind.ERROR))
            .anySatisfy(m -> assertThat(m).contains("CtxOut#writeUrl").contains("public instance method"));
    }

    @Test
    void shouldReportMethodWithoutExactlyOneParameter(@TempDir final Path dir) throws IOException {
        for (final var parameters : new String[]{"", "final Object a, final Object b"}) {
            final var result = runWithMembers(dir.resolve("p" + parameters.length()), """
                @OutContext("url")
                public void writeUrl(%s) {
                }
                """.formatted(parameters));

            assertThat(result.success()).as("(" + parameters + ")").isFalse();
            assertThat(result.messages(Diagnostic.Kind.ERROR))
                .as("(" + parameters + ")")
                .anySatisfy(m -> assertThat(m).contains("CtxOut#writeUrl").contains("one parameter"));
        }
    }

    @Test
    void shouldReportTwoMethodsDeclaringTheSameContext(@TempDir final Path dir) throws IOException {
        final var result = runWithMembers(dir, """
            @OutContext("url")
            public void writeUrl(final Object value) {
            }
            
            @OutContext("url")
            public void writeLink(final Object value) {
            }
            """);

        assertThat(result.success()).isFalse();
        assertThat(result.messages(Diagnostic.Kind.ERROR))
            .anySatisfy(m -> assertThat(m)
                .contains("output context 'url'")
                .contains("declared by both")
                .contains("writeUrl")
                .contains("writeLink"));
    }

    @Test
    void shouldAcceptOverloadsOfTheSameMethodForOneContext(@TempDir final Path dir) throws IOException {
        final var result = runWithMembers(dir, """
            @OutContext("url")
            public void writeUrl(final Object value) {
                raw(String.valueOf(value));
            }
            
            @OutContext("url")
            public void writeUrl(final String value) {
                raw(value);
            }
            """);

        assertThat(result.success()).as(result.allMessages()).isTrue();
    }

    @Test
    void shouldLeaveContextOpenerAsTextWhenNothingIsDeclared(@TempDir final Path dir) throws IOException {
        final var result = runWithMembers(dir, "");

        assertThat(result.success()).as(result.allMessages()).isTrue();
        assertThat(Files.readString(result.generated().resolve("com/acme/T.java")))
            .doesNotContain("out.writeUrl")
            .contains("#url{u}");
    }
}
