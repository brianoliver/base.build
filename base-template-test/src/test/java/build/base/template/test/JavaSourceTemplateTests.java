package build.base.template.test;

import build.base.template.JavaOut;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests rendering a template that generates Java source through {@link JavaOut}.
 *
 * @author reed.vonredwitz
 * @since Oct-2026
 */
class JavaSourceTemplateTests {

    @Test
    void shouldKeepAnnotationsAndHonourConfiguredDelimiters() {
        final var out = new JavaOut();
        new JavaSourceTemplate("Person", List.of("first", "last")).render(out);

        assertThat(out.content()).isEqualTo("""
            public class Person {
                @Deprecated
                @SuppressWarnings("#{unused}")
                private String first;
                @Deprecated
                @SuppressWarnings("#{unused}")
                private String last;
            }
            """);
    }
}
