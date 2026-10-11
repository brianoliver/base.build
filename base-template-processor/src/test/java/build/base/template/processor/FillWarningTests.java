package build.base.template.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import javax.tools.Diagnostic;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The fills of an include that the template it includes has no slot for, which render nothing, and the code generated
 * for fills, which is compiled here.
 *
 * @author reed.vonredwitz
 * @since Oct-2026
 */
class FillWarningTests {

    private static final String LAYOUT = """
        out HtmlOut;
        package com.acme;
        import build.base.template.Slots;
        
        template Layout(String title, Slots<HtmlOut> slots) {
        @slot head
        @slot
        @slot foot
        @end
        """;

    @Test
    void shouldWarnAboutAFillThatIsNoSlotOfTheLayout(@TempDir final Path dir) throws IOException {
        final var result = ProcessorHarness.run(dir,
            Map.of("com/acme/Layout.jt", LAYOUT,
                "com/acme/Page.jt", """
                    out HtmlOut;
                    package com.acme;
                    
                    template Page(String title) {
                    @include Layout(title) {
                    @fill head
                    h
                    @endfill
                    @fill haed
                    typo
                    @endfill
                    @}
                    @end
                    """),
            Map.of());

        assertThat(result.success()).as(result.allMessages()).isTrue();
        assertThat(result.messages(Diagnostic.Kind.WARNING)).singleElement().asString()
            .contains("Page.jt:9").contains("fill haed is not a slot of Layout (its slots are foot, head)");
    }

    @Test
    void shouldNotWarnAboutFillsThatAreSlotsOfTheLayout(@TempDir final Path dir) throws IOException {
        final var result = ProcessorHarness.run(dir,
            Map.of("com/acme/Layout.jt", LAYOUT,
                "com/acme/Page.jt", """
                    out HtmlOut;
                    package com.acme;
                    
                    template Page(String title) {
                    @include Layout(title) {
                    @fill head
                    h
                    @endfill
                    @fill foot
                    f
                    @endfill
                    main
                    @}
                    @end
                    """),
            Map.of());

        assertThat(result.success()).as(result.allMessages()).isTrue();
        assertThat(result.messages(Diagnostic.Kind.WARNING)).isEmpty();
    }

    @Test
    void shouldFindTheLayoutInAnotherPackageByItsImport(@TempDir final Path dir) throws IOException {
        final var result = ProcessorHarness.run(dir,
            Map.of("com/acme/Layout.jt", LAYOUT,
                "com/other/Page.jt", """
                    out HtmlOut;
                    package com.other;
                    import com.acme.Layout;
                    
                    template Page(String title) {
                    @include Layout(title) {
                    @fill nope
                    n
                    @endfill
                    @}
                    @end
                    """),
            Map.of());

        assertThat(result.success()).as(result.allMessages()).isTrue();
        assertThat(result.messages(Diagnostic.Kind.WARNING)).singleElement().asString()
            .contains("fill nope is not a slot of Layout");
    }

    @Test
    void shouldCheckTheFillsOfANestedIncludeAgainstTheNestedLayout(@TempDir final Path dir) throws IOException {
        final var result = ProcessorHarness.run(dir,
            Map.of("com/acme/Layout.jt", LAYOUT,
                "com/acme/Page.jt", """
                    out HtmlOut;
                    package com.acme;
                    
                    template Page(String title) {
                    @include Layout(title) {
                    @fill head
                    @include Layout(title) {
                    @fill foot
                    f
                    @endfill
                    @fill nope
                    n
                    @endfill
                    @}
                    @endfill
                    @}
                    @end
                    """),
            Map.of());

        assertThat(result.success()).as(result.allMessages()).isTrue();
        assertThat(result.messages(Diagnostic.Kind.WARNING)).singleElement().asString()
            .contains("Page.jt:11").contains("fill nope is not a slot of Layout");
    }

    @Test
    void shouldSuggestTheSlotAMisspeltFillWasMeantFor(@TempDir final Path dir) throws IOException {
        final var result = ProcessorHarness.run(dir,
            Map.of("com/acme/Layout.jt", LAYOUT,
                "com/acme/Page.jt", """
                    out HtmlOut;
                    package com.acme;
                    
                    template Page(String title) {
                    @include Layout(title) {
                    @fill haed
                    typo
                    @endfill
                    @fill unrelated
                    u
                    @endfill
                    @}
                    @end
                    """),
            Map.of());

        assertThat(result.messages(Diagnostic.Kind.WARNING)).hasSize(2);
        assertThat(result.messages(Diagnostic.Kind.WARNING).get(0)).contains("fill haed").contains("did you mean head?");
        assertThat(result.messages(Diagnostic.Kind.WARNING).get(1)).contains("fill unrelated")
            .doesNotContain("did you mean");
    }

    @Test
    void shouldCheckTheFillsOfALayoutThatIsCompiledAlreadyByTheSlotsItLists(@TempDir final Path dir)
        throws IOException {
        // Stands in for a layout from another module, which the processor can only see as a class
        final var result = ProcessorHarness.run(dir,
            Map.of("com/acme/Page.jt", """
                out HtmlOut;
                package com.acme;
                
                template Page(String title) {
                @include com.other.External(title) {
                @fill head
                h
                @endfill
                @fill foott
                f
                @endfill
                @}
                @end
                """),
            Map.of("com/other/External.java", """
                package com.other;
                
                import build.base.template.HtmlOut;
                import build.base.template.Slots;
                import build.base.template.Template;
                
                public record External(String title, Slots<HtmlOut> slots) implements Template<HtmlOut> {
                    public static final String SLOTS = "foot,head";
                
                    @Override
                    public void render(final HtmlOut out) {
                        slots.render(out);
                    }
                }
                """));

        assertThat(result.success()).as(result.allMessages()).isTrue();
        assertThat(result.messages(Diagnostic.Kind.WARNING)).singleElement().asString()
            .contains("fill foott is not a slot of com.other.External (its slots are foot, head)")
            .contains("did you mean foot?");
    }

    @Test
    void shouldNotCheckAnIncludeOfAClassThatListsNoSlots(@TempDir final Path dir) throws IOException {
        final var result = ProcessorHarness.run(dir,
            Map.of("com/acme/Page.jt", """
                out HtmlOut;
                package com.acme;
                
                template Page(String title) {
                @include com.other.Plain(title) {
                @fill head
                h
                @endfill
                @}
                @end
                """),
            Map.of("com/other/Plain.java", """
                package com.other;
                
                import build.base.template.HtmlOut;
                import build.base.template.Slots;
                import build.base.template.Template;
                
                public record Plain(String title, Slots<HtmlOut> slots) implements Template<HtmlOut> {
                    @Override
                    public void render(final HtmlOut out) {
                        slots.render("head", out);
                    }
                }
                """));

        assertThat(result.success()).as(result.allMessages()).isTrue();
        assertThat(result.messages(Diagnostic.Kind.WARNING)).isEmpty();
    }

    @Test
    void shouldNotCheckAnIncludeOfALayoutWithoutASlotsParameter(@TempDir final Path dir) throws IOException {
        final var result = ProcessorHarness.run(dir,
            Map.of("com/acme/Plain.jt", """
                    out HtmlOut;
                    package com.acme;
                    
                    template Plain(String title, Template<HtmlOut> content) {
                    @include content
                    @end
                    """,
                "com/acme/Page.jt", """
                    out HtmlOut;
                    package com.acme;
                    
                    template Page(String title) {
                    @include Plain(title) {
                    main
                    @}
                    @end
                    """),
            Map.of());

        assertThat(result.success()).as(result.allMessages()).isTrue();
        assertThat(result.messages(Diagnostic.Kind.WARNING)).isEmpty();
    }

    @Test
    void shouldCompileAnIncludeWithManyFills(@TempDir final Path dir) throws IOException {
        final var layout = new StringBuilder("""
            out HtmlOut;
            package com.acme;
            import build.base.template.Slots;
            
            template Wide(Slots<HtmlOut> slots) {
            """);
        final var page = new StringBuilder("""
            out HtmlOut;
            package com.acme;
            
            template WidePage() {
            @include Wide() {
            """);
        for (int i = 0; i < 15; i++) {
            layout.append("@slot f").append(i).append("\n");
            page.append("@fill f").append(i).append("\nx").append(i).append("\n@endfill\n");
        }
        layout.append("@slot\n@end\n");
        page.append("rest\n@}\n@end\n");

        final var result = ProcessorHarness.run(dir,
            Map.of("com/acme/Wide.jt", layout.toString(), "com/acme/WidePage.jt", page.toString()),
            Map.of());

        assertThat(result.success()).as(result.allMessages()).isTrue();
        assertThat(result.messages(Diagnostic.Kind.WARNING)).isEmpty();
    }
}
