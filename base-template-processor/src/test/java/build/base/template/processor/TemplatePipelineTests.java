package build.base.template.processor;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author reed.vonredwitz
 * @since Apr-2026
 */
class TemplatePipelineTests {

    private static String load(final String name) throws IOException {
        final var resource = TemplatePipelineTests.class.getClassLoader()
            .getResourceAsStream("templates/" + name);
        assertThat(resource).as("test resource: " + name).isNotNull();
        return new String(resource.readAllBytes());
    }

    @Test
    void shouldProcessHelloTemplate() throws IOException {
        final var parsed = JtParser.parse(load("hello.jt"), "hello.jt");

        assertThat(parsed.className()).isEqualTo("HelloTemplate");
        assertThat(parsed.outType()).isEqualTo("HtmlOut");
        assertThat(parsed.params()).isEqualTo("String name");

        final var source = CodeGenerator.generate(parsed);

        assertThat(source).contains("public record HelloTemplate(String name) implements Template<HtmlOut>");
        assertThat(source).contains("out.write(name)");
        assertThat(source).contains("<h1>Hello, ");
        assertThat(source).contains("!</h1>\\n");
    }

    @Test
    void shouldProcessTasksTemplate() throws IOException {
        final var parsed = JtParser.parse(load("tasks.jt"), "tasks.jt");

        assertThat(parsed.className()).isEqualTo("TasksTemplate");
        assertThat(parsed.imports()).contains("import java.util.List");

        final var source = CodeGenerator.generate(parsed);

        assertThat(source).contains("import java.util.List;");
        assertThat(source).contains("for (var item : items) {");
        assertThat(source).contains("out.write(item)");
    }

    @Test
    void shouldProcessItemTemplateWithConditionalExpression() throws IOException {
        final var parsed = JtParser.parse(load("item.jt"), "item.jt");

        assertThat(parsed.className()).isEqualTo("ItemTemplate");

        final var source = CodeGenerator.generate(parsed);

        assertThat(source).contains("out.write(active ? \" active\" : \"\")");
        assertThat(source).contains("out.write(label)");
    }
}
