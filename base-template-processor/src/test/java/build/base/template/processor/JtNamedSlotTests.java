package build.base.template.processor;

import build.base.parsing.AbstractParser.Outcome;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Named slots: an include body can be given in named parts, each rendered by the layout at the slot of its name.
 *
 * @author reed.vonredwitz
 * @since Oct-2026
 */
class JtNamedSlotTests {

    private static final String HEADER = """
        out HtmlOut;
        package com.example;
        template Page(String title, List<String> items) {
        """;

    private static Outcome<ParsedTemplate> parseAll(final String body) {
        return JtParser.parseAll(HEADER + body + "@end\n", "Page.jt", outType -> Syntax.DEFAULT, warning -> {
        });
    }

    private static String generate(final String body) {
        final var outcome = parseAll(body);
        assertThat(outcome.errors()).isEmpty();
        return CodeGenerator.generate(outcome.value().orElseThrow());
    }

    private static List<String> messages(final Outcome<ParsedTemplate> outcome) {
        return outcome.errors().stream().map(Throwable::getMessage).toList();
    }

    @Test
    void shouldParseAFillIntoStartBodyAndEnd() {
        final var body = parseAll("""
            @include Layout(title) {
            @fill head
            <meta>
            @endfill
            @}
            """).value().orElseThrow().body();

        assertThat(body).containsExactly(
            new BodyNode.IncludeStart("Layout", "title"),
            new BodyNode.FillStart("head"),
            new BodyNode.RawText("<meta>\n"),
            new BodyNode.FillEnd(),
            new BodyNode.IncludeEnd());
    }

    @Test
    void shouldGiveTheFillsAndTheRestOfTheBodyByName() {
        final var source = generate("""
            @include Layout(title) {
            @fill head
            <meta>
            @endfill
            <main>hi</main>
            @fill foot
            <small>bye</small>
            @endfill
            @}
            """);

        assertThat(source).contains("new Layout(title, new build.base.template.Slots<HtmlOut>(java.util.Map."
                + "<String, Template<HtmlOut>>ofEntries(")
            .contains("java.util.Map.entry(\"head\", new Template<HtmlOut>() {")
            .contains("java.util.Map.entry(\"foot\", new Template<HtmlOut>() {")
            .contains("java.util.Map.entry(\"body\", new Template<HtmlOut>() {")
            .contains("})))).render(out);");
        // Each part holds only its own content
        assertThat(source.indexOf("<meta>")).isLessThan(source.indexOf("\"foot\""));
        assertThat(source.indexOf("<small>bye</small>")).isGreaterThan(source.indexOf("\"foot\""))
            .isLessThan(source.indexOf("\"body\""));
        assertThat(source.indexOf("<main>hi</main>")).isGreaterThan(source.indexOf("\"body\""));
    }

    @Test
    void shouldLeaveBlankTextBetweenFillsOutOfTheBody() {
        final var source = generate("""
            @include Layout(title) {
            @fill head
            h
            @endfill
            
            @fill foot
            f
            @endfill
            @}
            """);

        // The blank line between the fills is not written by the body
        assertThat(source.substring(source.indexOf("\"body\""))).doesNotContain("out.raw(");
    }

    @Test
    void shouldLeaveBlankLinesNextToTheFillsAndTheEndsOfTheBodyOutOfTheBody() {
        final var source = generate("""
            @include Layout(title) {
            
            <main>
            </main>
            
            @fill head
            h
            @endfill
            
            @}
            """);

        assertThat(source.substring(source.indexOf("\"body\""))).contains("out.raw(\"<main>\\n</main>\\n\");");
    }

    @Test
    void shouldKeepBlankLinesInTheMiddleOfTheBody() {
        final var source = generate("""
            @include Layout(title) {
            @fill head
            h
            @endfill
            <pre>
            a
            
            b
            </pre>
            @}
            """);

        assertThat(source.substring(source.indexOf("\"body\""))).contains("out.raw(\"<pre>\\na\\n\\nb\\n</pre>\\n\");");
    }

    @Test
    void shouldKeepTheNewlineOfALineWithAnExpressionBeforeAFill() {
        final var source = generate("""
            @include Layout(title) {
              #{title}
            @fill head
            h
            @endfill
            @}
            """);

        assertThat(source.substring(source.indexOf("\"body\""))).contains("out.write(title);")
            .contains("out.raw(\"\\n\");");
    }

    @Test
    void shouldGiveAnEmptyBodyWhenThereAreOnlyFills() {
        final var source = generate("@include Layout(title) {\n@fill head\nh\n@endfill\n@}\n");

        assertThat(source).contains("java.util.Map.entry(\"body\", new Template<HtmlOut>() {");
    }

    @Test
    void shouldNameTheContentOutsideTheFillsAsSlotsDoes() throws ReflectiveOperationException {
        // Slots is not visible to this module at compile time, as the processor does not depend on the library
        assertThat(CodeGenerator.BODY_SLOT)
            .isEqualTo(Class.forName("build.base.template.Slots").getField("BODY").get(null));
    }

    @Test
    void shouldKeepTheCodeAroundAFillInTheFill() {
        final var source = generate("""
            @include Layout(title) {
            @fill head
            @for (var item : items) {
            <meta content="#{item}">
            @}
            @endfill
            @}
            """);

        assertThat(source.indexOf("for (var item : items) {")).isGreaterThan(source.indexOf("\"head\""))
            .isLessThan(source.indexOf("\"body\""));
    }

    @Test
    void shouldTakeTheFillsOfANestedIncludeForTheNestedInclude() {
        final var source = generate("""
            @include Outer() {
            @fill a
            @include Inner() {
            @fill b
            x
            @endfill
            @}
            @endfill
            @}
            """);

        assertThat(source.indexOf("\"a\"")).isLessThan(source.indexOf("new Inner("));
        assertThat(source.indexOf("new Inner(")).isLessThan(source.indexOf("\"b\""));
        assertThat(source).contains("new Outer(new build.base.template.Slots<HtmlOut>(");
    }

    @Test
    void shouldAllowTheSameNameInDifferentIncludes() {
        assertThat(messages(parseAll("""
            @include A() {
            @fill head
            @endfill
            @}
            @include B() {
            @fill head
            @endfill
            @}
            """))).isEmpty();
    }

    @Test
    void shouldRenderAnyNameAtASlotOfTheSlotsParameter() {
        final var outcome = JtParser.parseAll("""
            out HtmlOut;
            template Layout(String title, build.base.template.Slots<HtmlOut> slots) {
            @slot head
            @slot
            @end
            """, "Layout.jt", outType -> Syntax.DEFAULT, warning -> {
        });

        assertThat(outcome.errors()).isEmpty();
        final String source = CodeGenerator.generate(outcome.value().orElseThrow());
        assertThat(source).contains("slots.render(\"head\", out);").contains("slots.render(\"body\", out);");
    }

    @Test
    void shouldRenderASlotFromTheSlotsParameterEvenWhenAnotherParameterHasItsName() {
        // A slot is always a part of the body of an include: there is no other kind of hole to be mistaken for
        final var outcome = JtParser.parseAll("""
            out HtmlOut;
            import build.base.template.Slots;
            template Layout(Template<HtmlOut> head, Slots<HtmlOut> slots) {
            @slot head
            @end
            """, "Layout.jt", outType -> Syntax.DEFAULT, warning -> {
        });

        assertThat(outcome.errors()).isEmpty();
        final String source = CodeGenerator.generate(outcome.value().orElseThrow());
        assertThat(source).contains("slots.render(\"head\", out);").doesNotContain("head.render(out);");
    }

    @Test
    void shouldFindTheSlotsParameterOnlyWhenSlotsIsTheTypeOfTheLibrary() {
        // As the parser keeps them, without the semicolon
        final var imported = List.of("import build.base.template.Slots");
        final var onDemand = List.of("import build.base.template.*");
        final var elsewhere = List.of("import com.example.Slots");

        assertThat(CodeGenerator.slotsParameter("String t, Slots<HtmlOut> s", imported)).isEqualTo("s");
        assertThat(CodeGenerator.slotsParameter("String t, Slots<HtmlOut> s", onDemand)).isEqualTo("s");
        assertThat(CodeGenerator.slotsParameter("Slots s", imported)).isEqualTo("s");
        assertThat(CodeGenerator.slotsParameter("final Slots<Map<A, B>> s", imported)).isEqualTo("s");
        assertThat(CodeGenerator.slotsParameter("build.base.template.Slots<HtmlOut> s", List.of())).isEqualTo("s");
        assertThat(CodeGenerator.slotsParameter("Slots<HtmlOut> s", List.of())).isNull();
        assertThat(CodeGenerator.slotsParameter("Slots<HtmlOut> s", elsewhere)).isNull();
        assertThat(CodeGenerator.slotsParameter("com.example.Slots<HtmlOut> s", imported)).isNull();
        assertThat(CodeGenerator.slotsParameter("MySlots<HtmlOut> s", imported)).isNull();
    }

    @Test
    void shouldNotTakeATypeNamedSlotsFromElsewhereForTheSlotsParameter() {
        final var outcome = JtParser.parseAll("""
            out HtmlOut;
            import com.example.Slots;
            template Layout(Slots<HtmlOut> slots) {
            @slot head
            @end
            """, "Layout.jt", outType -> Syntax.DEFAULT, warning -> {
        });

        assertThat(messages(outcome)).singleElement().asString()
            .contains("@slot needs a parameter of the type build.base.template.Slots")
            .contains("template Layout has not");
    }

    @Test
    void shouldListTheSlotsATemplateCanBeFilledFor() {
        final var outcome = JtParser.parseAll("""
            out HtmlOut;
            import build.base.template.Slots;
            template Layout(Slots<HtmlOut> slots) {
            @slot nav
            @slot head
            @slot
            @slot head
            @fragment row(Slots<HtmlOut> inner)
            @slot elsewhere
            @endfragment
            @slot foot
            @end
            """, "Layout.jt", outType -> Syntax.DEFAULT, warning -> {
        });

        assertThat(CodeGenerator.fillableSlots(outcome.value().orElseThrow())).containsExactly("foot", "head", "nav");
    }

    @Test
    void shouldListTheSlotsOfALayoutInItsGeneratedClass() {
        final var outcome = JtParser.parseAll("""
            out HtmlOut;
            package com.example;
            import build.base.template.Slots;
            template Layout(Slots<HtmlOut> slots) {
            @slot nav
            @slot
            @slot head
            @end
            """, "Layout.jt", outType -> Syntax.DEFAULT, warning -> {
        });

        final String source = CodeGenerator.generate(outcome.value().orElseThrow());
        assertThat(source).contains("each of its named slots (head, nav)")
            .contains("public static final String SLOTS = \"head,nav\";");
        assertThat(source.indexOf("SLOTS")).isGreaterThan(source.indexOf("public record Layout"));
    }

    private static Outcome<ParsedTemplate> layout(final String header, final String body) {
        return JtParser.parseAll("out HtmlOut;\npackage com.example;\nimport build.base.template.Slots;\n" + header
                + " {\n" + body + "@end\n", "Layout.jt", outType -> Syntax.DEFAULT,
            warning -> {
            });
    }

    @Test
    void shouldDeclareTheSlotsOfATemplateInItsSignature() {
        final var outcome = layout("template Layout(String t, Slots<HtmlOut> slots) slots(head, foot)",
            "@slot head\n@slot\n@slot foot\n");

        assertThat(outcome.errors()).isEmpty();
        final var template = outcome.value().orElseThrow();
        assertThat(template.declaredSlots()).containsExactly("head", "foot");
        assertThat(template.params()).isEqualTo("String t, Slots<HtmlOut> slots");
        assertThat(CodeGenerator.fillableSlots(template)).containsExactly("foot", "head");
        assertThat(CodeGenerator.generate(template)).contains("SLOTS = \"foot,head\"");
    }

    @Test
    void shouldHaveNoDeclaredSlotsWithoutTheClause() {
        final var outcome = layout("template Layout(Slots<HtmlOut> slots)", "@slot head\n");

        assertThat(outcome.errors()).isEmpty();
        assertThat(outcome.value().orElseThrow().declaredSlots()).isNull();
    }

    @Test
    void shouldAllowAnEmptyDeclaration() {
        final var outcome = layout("template Layout(Slots<HtmlOut> slots) slots()", "@slot\n");

        assertThat(outcome.errors()).isEmpty();
        assertThat(outcome.value().orElseThrow().declaredSlots()).isEmpty();
        assertThat(CodeGenerator.generate(outcome.value().orElseThrow())).contains("SLOTS = \"\"")
            .contains("no named slot");
    }

    @Test
    void shouldReportASlotThatIsNotDeclared() {
        assertThat(messages(layout("template Layout(Slots<HtmlOut> slots) slots(head, foot)",
            "@slot head\n@slot fot\n"))).singleElement().asString()
            .contains("slot fot is not declared by slots(...) of template Layout (its slots are head, foot)")
            .contains("line 6");
    }

    @Test
    void shouldReportASlotThatIsNotDeclaredWhenNothingIs() {
        assertThat(messages(layout("template Layout(Slots<HtmlOut> slots) slots()", "@slot head\n")))
            .singleElement().asString().contains("(its slots are none)");
    }

    @Test
    void shouldNotCheckASlotOfAFragmentAgainstTheSlotsOfTheTemplate() {
        final var outcome = layout("template Layout(Slots<HtmlOut> slots) slots(head)",
            "@slot head\n@fragment card(Slots<HtmlOut> inner)\n@slot other\n@endfragment\n");

        assertThat(outcome.errors()).isEmpty();
    }

    @Test
    void shouldAllowTheUnnamedSlotAndBodyWhateverIsDeclared() {
        assertThat(layout("template Layout(Slots<HtmlOut> slots) slots(head)", "@slot head\n@slot\n@slot body\n")
            .errors()).isEmpty();
    }

    @Test
    void shouldReportAMalformedDeclaration() {
        assertThat(messages(layout("template Layout(Slots<HtmlOut> slots) slots(a.b)", "")))
            .singleElement().asString().contains("slots(...) takes the names of the slots");
        assertThat(messages(layout("template Layout(Slots<HtmlOut> slots) slots(a,)", "")))
            .singleElement().asString().contains("slots(...) takes the names of the slots");
        assertThat(messages(layout("template Layout(Slots<HtmlOut> slots) slots(body)", "")))
            .singleElement().asString().contains("'body' is the name of the content that is in no fill");
        assertThat(messages(layout("template Layout(Slots<HtmlOut> slots) slots(a, a)", "")))
            .singleElement().asString().contains("slot a is declared twice");
    }

    @Test
    void shouldRequireASlotsParameterForADeclaration() {
        assertThat(messages(layout("template Layout(String title) slots(head)", "")))
            .singleElement().asString().contains("slots(...) needs a parameter of the type build.base.template.Slots");
    }

    @Test
    void shouldWarnAboutADeclaredSlotThatIsNeverRendered() {
        final var warnings = new java.util.ArrayList<String>();
        final var outcome = JtParser.parseAll("""
            out HtmlOut;
            import build.base.template.Slots;
            template Layout(Slots<HtmlOut> slots) slots(head, foot) {
            @slot head
            @end
            """, "Layout.jt", outType -> Syntax.DEFAULT, warnings::add);

        assertThat(outcome.errors()).isEmpty();
        assertThat(warnings).singleElement().asString().contains("Layout.jt: line 3")
            .contains("slot foot is declared by slots(...) but the template never renders it");
    }

    @Test
    void shouldListNoSlotsConstantForATemplateWithoutASlotsParameter() {
        final var outcome = JtParser.parseAll("""
            out HtmlOut;
            template Plain(String title) {
            hi
            @end
            """, "Plain.jt", outType -> Syntax.DEFAULT, warning -> {
        });

        assertThat(CodeGenerator.generate(outcome.value().orElseThrow())).doesNotContain("SLOTS");
    }

    @Test
    void shouldListNoSlotsForATemplateWithoutASlotsParameter() {
        final var outcome = JtParser.parseAll("""
            out HtmlOut;
            template Layout(Template<HtmlOut> nav) {
            @include nav
            @end
            """, "Layout.jt", outType -> Syntax.DEFAULT, warning -> {
        });

        assertThat(CodeGenerator.fillableSlots(outcome.value().orElseThrow())).isNull();
    }

    @Test
    void shouldRequireASlotsParameterForASlot() {
        assertThat(messages(parseAll("@slot head\n"))).singleElement().asString()
            .contains("@slot needs a parameter of the type build.base.template.Slots")
            .contains("template Page has not");
    }

    @Test
    void shouldReportAFillOutsideAnIncludeBody() {
        assertThat(messages(parseAll("@fill head\nh\n@endfill\n"))).singleElement().asString()
            .contains("fill head must be directly inside the body of an include").contains("line 4");
        assertThat(messages(parseAll("@include L() {\n@for (var i : items) {\n@fill head\n@endfill\n@}\n@}\n")))
            .singleElement().asString().contains("fill head must be directly inside the body of an include");
    }

    @Test
    void shouldReportAMalformedFill() {
        assertThat(messages(parseAll("@include L() {\n@fill\n@endfill\n@}\n"))).anyMatch(m ->
            m.contains("@fill requires a name"));
        assertThat(messages(parseAll("@include L() {\n@fill a b\n@endfill\n@}\n"))).anyMatch(m ->
            m.contains("@fill requires a name"));
        assertThat(messages(parseAll("@include L() {\n@fill body\n@endfill\n@}\n"))).anyMatch(m ->
            m.contains("'body' is the name of the content that is in no @fill"));
    }

    @Test
    void shouldReportContentAfterEndfill() {
        assertThat(messages(parseAll("@include L() {\n@fill a\n@endfill now\n@}\n"))).anyMatch(m ->
            m.contains("unexpected content after @endfill"));
    }

    @Test
    void shouldReportANestedFill() {
        assertThat(messages(parseAll("@include L() {\n@fill a\n@fill b\n@endfill\n@endfill\n@}\n")))
            .singleElement().asString().contains("fills cannot be nested (fill a is still open)");
    }

    @Test
    void shouldReportAFillDeclaredTwice() {
        assertThat(messages(parseAll("@include L() {\n@fill a\n@endfill\n@fill a\n@endfill\n@}\n")))
            .singleElement().asString().contains("fill a is already declared in this include").contains("line 7");
    }

    @Test
    void shouldAllowAnyNumberOfFills() {
        final var fills = new StringBuilder("@include L() {\n");
        for (int i = 0; i < 20; i++) {
            fills.append("@fill f").append(i).append("\nx\n@endfill\n");
        }

        final var source = generate(fills + "@}\n");

        assertThat(source).contains("java.util.Map.entry(\"f0\", ").contains("java.util.Map.entry(\"f19\", ");
    }

    @Test
    void shouldReportAFillThatIsNeverClosed() {
        assertThat(messages(parseAll("@include L() {\n@fill a\nx\n@}\n"))).singleElement().asString()
            .contains("fill a is never closed").contains("line 5");
    }

    @Test
    void shouldReportAnEndfillWithNothingToClose() {
        assertThat(messages(parseAll("@endfill\n"))).singleElement().asString()
            .contains("@endfill has no fill to close");
    }

    @Test
    void shouldReportAFillThatOpensABlockItDoesNotClose() {
        assertThat(messages(parseAll("@include L() {\n@fill a\n@for (var i : items) {\n@endfill\n@}\n@}\n")))
            .anyMatch(m -> m.contains("fill a must contain whole blocks"));
    }

    @Test
    void shouldReportAFillThatStartsInAFragmentAndEndsOutside() {
        assertThat(messages(parseAll("@include L() {\n@fragment f()\n@fill a\n@endfragment\n@endfill\n@}\n")))
            .anyMatch(m -> m.contains("fill a must contain whole blocks and fragments"));
    }

    @Test
    void shouldAllowAFragmentInsideAFill() {
        final var source = generate("""
            @include L() {
            @fill a
            @fragment row(String t)
            <li>#{t}</li>
            @endfragment
            @endfill
            @}
            """);

        assertThat(source).contains("new Row(t).render(out);").contains("public record Row(String t)");
    }
}
