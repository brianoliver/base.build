package build.base.parsing;

/*-
 * #%L
 * base.build Parsing
 * %%
 * Copyright (C) 2025 Workday Inc
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Base class for full-grammar parsers — Layer A of base-parsing's two parser styles.
 * <p>
 * A subclass implements {@link #parse()} (its imperative grammar, using the inherited {@link #scanner} field and
 * helper methods) and {@link #translate(ParseException)} (the mapping from {@link ParseException} to its
 * domain-specific exception type).  Public callers invoke {@link #run()}, which constructs the {@link Scanner},
 * delegates to {@link #parse()}, asserts that all input was consumed, and translates any {@link ParseException}.
 * <p>
 * For composable rule fragments, see {@link Rule}.  An {@link AbstractParser} subclass may use {@link Rule}-style
 * combinators inside its grammar methods by passing {@code this.scanner} to {@link Rule#tryParse(Scanner)}.
 *
 * @param <T> the type of value produced by the grammar
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public abstract class AbstractParser<T>
    implements Parser<T> {

    /**
     * Default identifier-continue character class used by {@link #followsKeyword(String)} to detect that a
     * keyword stands alone (i.e. is not the prefix of a longer identifier).  Matches the alphabet shared by EL,
     * the JDK module grammar, ABC, JSON identifiers, and Java in general.
     */
    private static final Pattern DEFAULT_IDENT_CONTINUE = Pattern.compile("[a-zA-Z0-9_$]");

    private final String stringInput;
    private final Reader readerInput;

    /**
     * The {@link Scanner} for the current parse.  Populated by {@link #run()} immediately before {@link #parse()}
     * is invoked, and cleared after.  Subclasses should only reference this field from inside grammar methods
     * called transitively by {@link #parse()}.
     */
    protected Scanner scanner;

    /**
     * The errors reported by the current parse; {@code null} when none is running.
     */
    private List<RuntimeException> errors;

    /**
     * Constructs an {@link AbstractParser} that will read from the given {@link String}.
     *
     * @param input the input string
     */
    protected AbstractParser(final String input) {
        this.stringInput = Objects.requireNonNull(input, "The input String must not be null");
        this.readerInput = null;
    }

    /**
     * Constructs an {@link AbstractParser} that will read from the given {@link Reader}.
     *
     * @param input the input reader
     */
    protected AbstractParser(final Reader input) {
        this.stringInput = null;
        this.readerInput = Objects.requireNonNull(input, "The input Reader must not be null");
    }

    /**
     * Subclass hook: implements the grammar.  Reads from {@link #scanner}, returns the parsed value.  Should not
     * catch {@link ParseException} — let it propagate; {@link #run()} will translate it via
     * {@link #translate(ParseException)}.  The one exception is a grammar that deliberately recovers, by catching at a
     * statement boundary and calling {@link #report(ParseException)}; {@link #run()} then still throws the first
     * error reported, and {@link #runRecovering()} returns them all.
     *
     * @return the parsed value
     */
    protected abstract T parse();

    /**
     * Subclass hook: translates a {@link ParseException} into the subclass's domain-specific exception type.
     * Has access to the original {@link ParseException} and, via {@code this}, any subclass state (input,
     * filename, etc.) needed to build a richer message.
     *
     * @param cause the {@link ParseException} from the grammar
     * @return the domain-specific exception to throw
     */
    protected abstract RuntimeException translate(ParseException cause);

    /**
     * Subclass hook: registers {@link Filter}s on the {@link Scanner} before parsing begins.  The default
     * registers {@link Filter#WHITESPACE}.  Override to register additional filters (e.g. comment filters), or
     * to register no filters if the grammar is whitespace-significant.
     *
     * @param s the {@link Scanner} to configure
     */
    protected void registerFilters(final Scanner s) {
        s.register(Filter.WHITESPACE);
    }

    /**
     * Runs the parse to completion.  Constructs a {@link Scanner} over the configured input, applies
     * {@link #registerFilters(Scanner)}, invokes {@link #parse()}, asserts the entire input was consumed, and
     * translates any {@link ParseException} via {@link #translate(ParseException)}.
     *
     * @return the parsed value
     * @throws RuntimeException whatever {@link #translate(ParseException)} returns, on parse failure
     */
    @Override
    public final T run() {
        final Outcome<T> outcome = runRecovering();
        if (!outcome.errors().isEmpty()) {
            throw outcome.errors().getFirst();
        }
        return outcome.value().orElse(null);
    }

    /**
     * Runs the parse like {@link #run()}, but returns every error instead of throwing the first.
     * <p>
     * A grammar opts in to recovery by catching a {@link ParseException} at a statement boundary, passing it to
     * {@link #report(ParseException)}, and moving the scanner past the damage (see {@link #skipPast(String)}) before
     * carrying on.  A {@link ParseException} that escapes {@link #parse()} is recorded as the last error, and the
     * value is then {@link Optional#empty()}.  Exceptions other than {@link ParseException} propagate as in {@link #run()}.
     *
     * @return the value (possibly partial when errors were reported) and the errors, translated by
     * {@link #translate(ParseException)}, in the order they were reported
     */
    public final Outcome<T> runRecovering() {
        try (var s = stringInput != null ? new Scanner(stringInput) : new Scanner(readerInput)) {
            this.scanner = s;
            this.errors = new ArrayList<>();
            final T result;
            try {
                registerFilters(s);
                result = parse();
            } catch (final ParseException e) {
                errors.add(translate(e));
                return new Outcome<>(Optional.empty(), errors);
            }
            if (s.hasNext()) {
                errors.add(translate(
                    new ParseException(s.getLocation(), "(end of input)", String.valueOf(s.peekChar()))));
            }
            return new Outcome<>(Optional.ofNullable(result), errors);
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        } finally {
            this.scanner = null;
            this.errors = null;
        }
    }

    /**
     * Records an error from a grammar that recovers from it.  The error is translated by
     * {@link #translate(ParseException)}; the caller must then advance the scanner, or the same input will be
     * parsed again.  Should {@link #translate(ParseException)} itself throw, that exception propagates to the caller.
     *
     * @param cause the error to record
     * @throws IllegalStateException if no parse is running
     */
    protected final void report(final ParseException cause) {
        reportTranslated(translate(cause));
    }

    /**
     * Records an already-translated error, for grammars that detect a problem themselves and throw their own
     * exception type.  See {@link #report(ParseException)}.
     *
     * @param error the error to record
     * @throws IllegalStateException if no parse is running
     */
    protected final void reportTranslated(final RuntimeException error) {
        if (errors == null) {
            throw new IllegalStateException("errors can only be reported while a parse is running");
        }
        errors.add(Objects.requireNonNull(error, "The error must not be null"));
    }

    /**
     * Helper for recovery: skips to the end of the next occurrence of {@code delimiter}, or to the end of the
     * input when there is none.  Always advances unless the input is already exhausted, so a recovery loop built
     * on it terminates.  Registered {@link Filter}s are not applied, so a newline delimiter is found even though
     * {@link Filter#WHITESPACE} is registered by default.
     *
     * @param delimiter the non-empty text to skip past, for example a newline or a semicolon
     * @throws IllegalArgumentException if the delimiter is {@code null} or empty
     */
    protected final void skipPast(final String delimiter) {
        scanner.skipPast(delimiter);
    }

    /**
     * The result of {@link #runRecovering()}.
     *
     * @param value  the parsed value, or {@link Optional#empty()} when an error ended the parse
     * @param errors the errors reported, in order; empty when the parse succeeded
     * @param <T>    the type of value produced by the grammar
     */
    public record Outcome<T>(Optional<T> value, List<RuntimeException> errors) {

        /**
         * Copies {@code errors} so the record is immutable however it is constructed.
         */
        public Outcome {
            Objects.requireNonNull(value, "The value must not be null");
            errors = List.copyOf(errors);
        }

        /**
         * @return {@code true} if no errors were reported
         */
        public boolean succeeded() {
            return errors.isEmpty();
        }
    }

    /**
     * Helper: consumes the literal {@code expected} or throws.  Equivalent to {@code scanner.consume(expected)}
     * but reads more directly at call sites.
     *
     * @param expected the literal to consume
     */
    protected final void expect(final String expected) {
        scanner.consume(expected);
    }

    /**
     * Helper: consumes a match of {@code pattern} and returns the matched text, or throws a {@link ParseException}
     * whose expected message is {@code description}.
     *
     * @param pattern     the pattern to match
     * @param description the description to use in the {@link ParseException} message
     * @return the matched text
     */
    protected final String expect(final Pattern pattern, final String description) {
        try {
            return scanner.consume(pattern);
        } catch (final ParseException e) {
            throw new ParseException(e.getLocation().orElse(null), description, e.getFound(), e);
        }
    }

    /**
     * Helper: returns {@code true} if {@code keyword} occurs at the current position and is not immediately
     * followed by an identifier-continue character ({@code [a-zA-Z0-9_$]}).  This distinguishes the keyword
     * {@code or} from the start of {@code orange}.
     *
     * @param keyword the keyword to test
     * @return {@code true} if the keyword stands alone at the current position
     */
    protected final boolean followsKeyword(final String keyword) {
        if (!scanner.follows(keyword)) {
            return false;
        }
        return !scanner.follows(Pattern.compile(Pattern.quote(keyword) + DEFAULT_IDENT_CONTINUE.pattern()));
    }

    /**
     * Helper: consumes {@code keyword} (with the same word-boundary check as {@link #followsKeyword(String)}),
     * or throws.
     *
     * @param keyword the keyword to consume
     */
    protected final void consumeKeyword(final String keyword) {
        if (!followsKeyword(keyword)) {
            throw new ParseException(scanner.getLocation(), keyword,
                scanner.hasNext() ? String.valueOf(scanner.peekChar()) : "(end of input)");
        }
        scanner.consume(keyword);
    }
}
