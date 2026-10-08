package build.base.parsing;

import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractParserTests {

    private static final class DemoException extends RuntimeException {
        DemoException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Parses {@code <ident> = <ident>} and returns the pair.
     */
    private static final class AssignmentParser extends AbstractParser<String> {

        private static final Pattern IDENT = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

        AssignmentParser(final String input) {
            super(input);
        }

        AssignmentParser(final Reader input) {
            super(input);
        }

        @Override
        protected String parse() {
            final var lhs = expect(IDENT, "identifier");
            expect("=");
            final var rhs = expect(IDENT, "identifier");
            return lhs + "=" + rhs;
        }

        @Override
        protected RuntimeException translate(final ParseException cause) {
            return new DemoException("parse failed: " + cause.getMessage(), cause);
        }
    }

    /**
     * Exercises followsKeyword/consumeKeyword.
     */
    private static final class KeywordParser extends AbstractParser<String> {

        KeywordParser(final String input) {
            super(input);
        }

        @Override
        protected String parse() {
            if (followsKeyword("yes")) {
                consumeKeyword("yes");
                return "Y";
            }
            consumeKeyword("no");
            return "N";
        }

        @Override
        protected RuntimeException translate(final ParseException cause) {
            return new DemoException("kw fail", cause);
        }
    }

    @Test
    void parsesStringInput() {
        assertThat(new AssignmentParser("a=b").run()).isEqualTo("a=b");
    }

    @Test
    void parsesReaderInput() {
        assertThat(new AssignmentParser((Reader) new StringReader("foo = bar")).run()).isEqualTo("foo=bar");
    }

    @Test
    void translatesParseException() {
        assertThatThrownBy(() -> new AssignmentParser("a + b").run())
            .isInstanceOf(DemoException.class)
            .hasMessageContaining("parse failed")
            .hasCauseInstanceOf(ParseException.class);
    }

    @Test
    void requiresFullInputConsumption() {
        // The grammar parses "a=b" successfully but extra "; trailing" remains.
        assertThatThrownBy(() -> new AssignmentParser("a=b; trailing").run())
            .isInstanceOf(DemoException.class)
            .hasCauseInstanceOf(ParseException.class);
    }

    @Test
    void followsKeywordRespectsWordBoundary() {
        assertThat(new KeywordParser("yes").run()).isEqualTo("Y");
        assertThat(new KeywordParser("no").run()).isEqualTo("N");
        // "yesterday" starts with "yes" but is not the keyword
        assertThatThrownBy(() -> new KeywordParser("yesterday").run())
            .isInstanceOf(DemoException.class);
    }

    @Test
    void consumeKeywordFailsOnNonKeyword() {
        assertThatThrownBy(() -> new KeywordParser("yesno").run())
            .isInstanceOf(DemoException.class);
    }

    @Test
    void scannerFieldClearedAfterRun() throws Exception {
        final var p = new AssignmentParser("a=b");
        p.run();
        // package-private field; null after run() returns
        final var f = AbstractParser.class.getDeclaredField("scanner");
        f.setAccessible(true);
        assertThat(f.get(p)).isNull();
    }

    @Test
    void consumeBalancedReturnsBody() throws Exception {
        try (var s = new Scanner("{a + b}")) {
            assertThat(s.consumeBalanced('{', '}')).isEqualTo("a + b");
            assertThat(s.hasNext()).isFalse();
        }
    }

    @Test
    void consumeBalancedHandlesNesting() throws Exception {
        try (var s = new Scanner("{outer {inner} more}rest")) {
            assertThat(s.consumeBalanced('{', '}')).isEqualTo("outer {inner} more");
            assertThat(s.consumeChar()).isEqualTo('r');
        }
    }

    @Test
    void consumeBalancedSkipsQuotedDelimiters() throws Exception {
        try (var s = new Scanner("{\"}}}\" + 'x}'}")) {
            assertThat(s.consumeBalanced('{', '}')).isEqualTo("\"}}}\" + 'x}'");
        }
    }

    @Test
    void consumeBalancedHandlesEscapedQuotes() throws Exception {
        try (var s = new Scanner("{\"a\\\"b\"}")) {
            assertThat(s.consumeBalanced('{', '}')).isEqualTo("\"a\\\"b\"");
        }
    }

    @Test
    void consumeBalancedThrowsOnEofBeforeClose() throws Exception {
        try (var s = new Scanner("{unclosed")) {
            assertThatThrownBy(() -> s.consumeBalanced('{', '}'))
                .isInstanceOf(ParseException.class);
        }
    }

    @Test
    void consumeBalancedThrowsWhenNotPositionedOnOpen() throws Exception {
        try (var s = new Scanner("not-a-brace")) {
            assertThatThrownBy(() -> s.consumeBalanced('{', '}'))
                .isInstanceOf(ParseException.class);
        }
    }

    /**
     * Parses {@code <ident> = <ident> ;} statements, recovering from a bad statement by skipping past its semicolon.
     */
    private static final class RecoveringParser extends AbstractParser<List<String>> {

        private static final Pattern IDENT = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

        RecoveringParser(final String input) {
            super(input);
        }

        RecoveringParser(final Reader input) {
            super(input);
        }

        @Override
        protected List<String> parse() {
            final List<String> statements = new ArrayList<>();
            while (scanner.hasNext()) {
                try {
                    final var lhs = expect(IDENT, "identifier");
                    expect("=");
                    final var rhs = expect(IDENT, "identifier");
                    expect(";");
                    statements.add(lhs + "=" + rhs);
                } catch (final ParseException e) {
                    report(e);
                    skipPast(";");
                }
            }
            return statements;
        }

        void reportOutsideParse() {
            reportTranslated(new DemoException("late", null));
        }

        @Override
        protected RuntimeException translate(final ParseException cause) {
            return new DemoException("parse failed: " + cause.getMessage(), cause);
        }
    }

    @Test
    void runRecoveringReturnsEveryErrorAndThePartialValue() {
        final var outcome = new RecoveringParser("a=b; c+d; e=f; g=; h=i;").runRecovering();

        assertThat(outcome.succeeded()).isFalse();
        assertThat(outcome.value().orElseThrow()).containsExactly("a=b", "e=f", "h=i");
        assertThat(outcome.errors()).hasSize(2);
        assertThat(outcome.errors().get(0))
            .hasMessageContaining("Expected [=] but found [+d;");
        assertThat(outcome.errors().get(1))
            .hasMessageContaining("Expected [identifier] but found [; h=i;] at line 1, column 18");
    }

    @Test
    void runRecoveringSucceedsWithoutErrors() {
        final var outcome = new RecoveringParser("a=b; c=d;").runRecovering();

        assertThat(outcome.succeeded()).isTrue();
        assertThat(outcome.errors()).isEmpty();
        assertThat(outcome.value().orElseThrow()).containsExactly("a=b", "c=d");
    }

    @Test
    void runStillThrowsTheFirstReportedError() {
        assertThatThrownBy(() -> new RecoveringParser("a=b; c+d; e+f;").run())
            .isInstanceOf(DemoException.class)
            .hasMessageContaining("Expected [=] but found [+d; e+f;] at line 1, column 7");
    }

    @Test
    void skipPastStopsAtEndOfInputWhenThereIsNoDelimiter() {
        final var outcome = new RecoveringParser("a=b; c+d").runRecovering();

        assertThat(outcome.value().orElseThrow()).containsExactly("a=b");
        assertThat(outcome.errors()).hasSize(1);
    }

    @Test
    void runRecoveringRecordsAnEscapingParseExceptionAsTheLastError() {
        final var outcome = new AssignmentParser("a + b").runRecovering();

        assertThat(outcome.value()).isEmpty();
        assertThat(outcome.errors()).singleElement().isInstanceOf(DemoException.class);
    }

    @Test
    void runRecoveringReportsInputLeftUnconsumed() {
        final var outcome = new AssignmentParser("a=b; trailing").runRecovering();

        assertThat(outcome.value()).contains("a=b");
        assertThat(outcome.errors()).singleElement().isInstanceOf(DemoException.class);
    }

    @Test
    void reportOutsideAParseIsRejected() {
        assertThatThrownBy(() -> new RecoveringParser("").reportOutsideParse())
            .isInstanceOf(IllegalStateException.class);
    }

    /**
     * Parses one {@code <ident> = <ident>} statement per line with {@link Filter#WHITESPACE} registered, recovering
     * by skipping past the newline.
     */
    private static final class LineParser extends AbstractParser<List<String>> {

        private static final Pattern IDENT = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

        LineParser(final String input) {
            super(input);
        }

        @Override
        protected List<String> parse() {
            final List<String> statements = new ArrayList<>();
            while (scanner.hasNext()) {
                try {
                    final var lhs = expect(IDENT, "identifier");
                    expect("=");
                    statements.add(lhs + "=" + expect(IDENT, "identifier"));
                } catch (final ParseException e) {
                    report(e);
                    skipPast("\n");
                }
            }
            return statements;
        }

        @Override
        protected RuntimeException translate(final ParseException cause) {
            return new DemoException("parse failed: " + cause.getMessage(), cause);
        }
    }

    @Test
    void skipPastFindsANewlineEvenThoughWhitespaceIsFiltered() {
        final var outcome = new LineParser("a=b\nc+d\ne=f\n").runRecovering();

        assertThat(outcome.value().orElseThrow()).containsExactly("a=b", "e=f");
        assertThat(outcome.errors()).hasSize(1);
    }

    @Test
    void runRecoveringRecoversFromAReader() {
        final var outcome = new RecoveringParser(new StringReader("a=b; c+d; e=f;")).runRecovering();

        assertThat(outcome.value().orElseThrow()).containsExactly("a=b", "e=f");
        assertThat(outcome.errors()).hasSize(1);
    }

    @Test
    void runRecoveringRecoversAcrossReaderBufferBoundaries() {
        final var input = new StringBuilder();
        final var expected = new ArrayList<String>();
        for (int i = 0; i < 5000; i++) {
            if (i % 100 == 7) {
                input.append("a+b; ");
            } else {
                input.append("a=b; ");
                expected.add("a=b");
            }
        }

        final var outcome = new RecoveringParser(new StringReader(input.toString())).runRecovering();

        assertThat(outcome.value()).contains(expected);
        assertThat(outcome.errors()).hasSize(50);
    }

    @Test
    void outcomeCopiesItsErrors() {
        final var errors = new ArrayList<RuntimeException>();
        final var outcome = new AbstractParser.Outcome<>(Optional.of("v"), errors);

        errors.add(new RuntimeException("late"));

        assertThat(outcome.errors()).isEmpty();
        assertThat(outcome.succeeded()).isTrue();
    }

    @Test
    void runRecoveringPropagatesExceptionsOtherThanParseException() {
        final var parser = new AbstractParser<String>("x") {
            @Override
            protected String parse() {
                throw new IllegalStateException("boom");
            }

            @Override
            protected RuntimeException translate(final ParseException cause) {
                return new DemoException("unused", cause);
            }
        };

        assertThatThrownBy(parser::runRecovering)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("boom");
    }

    @Test
    void runRecoveringRecordsAParseExceptionFromRegisterFilters() {
        final var parser = new AbstractParser<String>("x") {
            @Override
            protected void registerFilters(final Scanner s) {
                throw new ParseException(s.getLocation(), "filters", "none");
            }

            @Override
            protected String parse() {
                return "unreachable";
            }

            @Override
            protected RuntimeException translate(final ParseException cause) {
                return new DemoException("filters failed", cause);
            }
        };

        final var outcome = parser.runRecovering();

        assertThat(outcome.value()).isEmpty();
        assertThat(outcome.errors()).singleElement().extracting(Throwable::getMessage).isEqualTo("filters failed");
    }

    @Test
    void translateRunsWhileTheScannerIsStillOpen() {
        final var parser = new AbstractParser<String>("a + b") {
            @Override
            protected String parse() {
                final var lhs = expect(Pattern.compile("[a-z]+"), "identifier");
                expect("=");
                return lhs;
            }

            @Override
            protected RuntimeException translate(final ParseException cause) {
                return new DemoException("remaining: " + scanner.peekChar(), cause);
            }
        };

        assertThat(parser.runRecovering().errors()).singleElement().extracting(Throwable::getMessage).asString()
            .startsWith("remaining: ");
    }

    @Test
    void errorsAreNotSharedBetweenRuns() {
        final var parser = new RecoveringParser("a+b;");

        assertThat(parser.runRecovering().errors()).hasSize(1);
        assertThat(parser.runRecovering().errors()).hasSize(1);
    }
}
