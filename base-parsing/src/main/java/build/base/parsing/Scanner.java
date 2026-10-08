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

import build.base.foundation.stream.Streamable;
import build.base.io.LookaheadReader;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A text scanner that can filter, match and break input into {@link String}s and specific types of values.
 * <p>
 * {@link Scanner}s may be configured with zero or more {@link Filter}s, that will be used to automatically filter out
 * and ignore text during the scanning process.
 * <p>
 * {@link Scanner}s may also be used and configured with {@link Evaluator}s, allowing matching and evaluation to return
 * specific types of values.
 * <p>
 * As {@link Scanner}s ultimately read input to scan from {@link Reader}s, scanning operations may block waiting for
 * input.  Should the underlying {@link Reader} throw an {@link java.io.IOException}, the {@link Scanner} will assume
 * the end of input has been reached.
 * <p>
 * When a {@link Scanner} throws a {@link ParseException} while matching, the {@link Scanner} will not consume the
 * failed match from the underlying {@link Reader}, thus allowing further attempts to either skip or consume from
 * the {@link Scanner} with some other pattern.
 * <p>
 * When a {@link Scanner} is closed, the underlying {@link Reader} will also be closed.
 *
 * @author brian.oliver
 * @see Filter#WHITESPACE
 * @see Filter#JAVA_SINGLE_LINE_COMMENT
 * @see Filter#JAVA_MULTILINE_COMMENT
 * @see Evaluator
 * @since Aug-2019
 */
public class Scanner
    implements AutoCloseable {

    /**
     * The {@link ScannerInput} from which content will be read for parsing.
     */
    private final ScannerInput input;

    /**
     * The {@link Filter}s for skipping {@link ScannerInput} content to ignore and not parse.
     */
    private final ArrayList<Filter> filters;

    /**
     * The {@link Evaluator}s for evaluating and converting content into specific {@link Class}es.
     */
    private final HashMap<Class<?>, Evaluator<?>> evaluators;

    /**
     * Constructs a {@link Scanner} using the specified {@link Reader}.
     *
     * @param reader a {@link Reader}
     */
    public Scanner(final Reader reader) {
        Objects.requireNonNull(reader, "The Reader must not be null");

        this.input = new LookaheadReaderInput(reader);
        this.filters = new ArrayList<>();
        this.evaluators = new HashMap<>();
    }

    /**
     * Constructs a {@link Scanner} for the specified {@link String}.
     *
     * @param string the {@link String}
     */
    public Scanner(final String string) {
        Objects.requireNonNull(string, "The String must not be null");

        this.input = new StringInput(string);
        this.filters = new ArrayList<>();
        this.evaluators = new HashMap<>();
    }

    @Override
    public void close()
        throws Exception {
        this.input.close();
    }

    /**
     * Registers the specified {@link Filter} to be used for filtering content while being scanned.
     *
     * @param filter the {@link Filter}
     * @return the {@link Scanner} to permit fluent-style method calls
     */
    public Scanner register(final Filter filter) {
        if (filter != null) {
            this.filters.add(filter);
        }
        return this;
    }

    /**
     * Registers an {@link Evaluator} for a specific {@link Class} of value.
     *
     * @param <T>        the type of value
     * @param valueClass the {@link Class} of value
     * @param evaluator  the {@link Evaluator}
     * @return the {@link Scanner} to permit fluent-style method calls
     */
    public <T> Scanner register(final Class<T> valueClass, final Evaluator<T> evaluator) {
        if (valueClass != null && evaluator != null) {
            this.evaluators.put(valueClass, evaluator);
        }
        return this;
    }

    /**
     * Obtains the current {@link LookaheadReader.Location} for the {@link Scanner}.
     *
     * @return the {@link LookaheadReader.Location}
     */
    public LookaheadReader.Location getLocation() {
        return this.input.getLocation();
    }

    /**
     * Returns {@code true} if this {@link Scanner} supports backtracking via {@link #save()} and
     * {@link #restore(int)}.
     *
     * @return {@code true} if backtracking is supported
     */
    public boolean supportsBacktracking() {
        return this.input.supportsBacktracking();
    }

    /**
     * Saves the current scanner position and returns a checkpoint.
     *
     * @return an opaque checkpoint value
     * @throws UnsupportedOperationException if backtracking is not supported
     */
    public int save() {
        return this.input.save();
    }

    /**
     * Restores the scanner to the position recorded by {@link #save()}.
     *
     * @param checkpoint a value previously returned by {@link #save()}
     * @throws UnsupportedOperationException if backtracking is not supported
     */
    public void restore(final int checkpoint) {
        this.input.restore(checkpoint);
    }

    /**
     * Normalizes a {@link Pattern} for matching at the current position in the input.
     * <p>
     * If the provided {@link Pattern} does not start with {@code ^}, one is prepended so that matching is
     * anchored to the current scanner position.
     * <p>
     * A leading {@code ^} in the caller's pattern is treated as a line-start assertion: the pattern will only
     * match when the scanner is at column 1 (the start of a line). To match from the current position regardless
     * of column, omit the {@code ^} — the scanner adds it automatically.
     * <p>
     * Returns {@link Optional#empty()} when {@code pattern} is {@code null} or when a line-anchored pattern
     * is used outside column 1.
     *
     * @param pattern the {@link Pattern}
     * @return the {@link Optional}ly normalized {@link Pattern}
     */
    private Optional<Pattern> normalize(final Pattern pattern) {
        if (pattern == null) {
            return Optional.empty();
        }

        final String regex = pattern.pattern();

        if (regex.startsWith("^") && this.input.getLocation().getColumn() != 1) {
            return Optional.empty();
        }

        final Pattern normalized = regex.startsWith("^")
            ? pattern
            : Pattern.compile("^" + regex, pattern.flags());

        return Optional.of(normalized);
    }

    /**
     * Attempts to match the {@link Pattern} with the current content in the {@link String} to parse.
     *
     * @param pattern the {@link Pattern}
     * @return the {@link Optional}ly matched {@link String} or {@link Optional#empty()} if the match was unsuccessful
     */
    private Optional<String> match(final Pattern pattern) {
        if (pattern != null && this.input.available()) {
            final Optional<Pattern> normalized = normalize(pattern);

            if (normalized.isPresent()) {
                final Matcher matcher = normalized.get().matcher(this.input.peekMaximum());

                if (matcher.find()) {
                    // obtain the match
                    final String match = matcher.group();

                    // skip the matched length
                    this.input.consume(match.length());

                    return Optional.of(match);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Attempts to match the specified {@link String} with the current content in the {@link String} to parse.
     * <p>
     * Matching is performed using {@link String#equals(Object)}, which means matching is case-sensitive.
     *
     * @param string the {@link String} to match
     * @return the {@link Optional}ly matched {@link String} or {@link Optional#empty()} if the match was unsuccessful
     */
    private Optional<String> match(final String string) {
        if (string != null && !string.isEmpty() && this.input.available()) {
            if (this.input.peek(string.length()).equals(string)) {

                // move the column past the match
                this.input.consume(string.length());

                return Optional.of(string);
            }
        }
        return Optional.empty();
    }

    /**
     * Attempts to ensure content is available for parsing, including filtering any content according to the
     * defined {@link Filter}s.
     *
     * @return {@code true} if there is content available for parsing,
     * {@code false} when there is no more content available
     */
    private boolean available() {
        if (!this.filters.isEmpty()) {
            int index = 0;

            while (this.input.available() && index < this.filters.size()) {

                final Filter filter = this.filters.get(index);
                final LookaheadReader.Location location = this.input.getLocation();

                filter.accept(this.input);

                // did the filter consume any content?
                if (location.equals(this.input.getLocation())) {
                    // the location wasn't changed, so we can proceed to the next filter
                    index++;
                } else {
                    // the location did change, so we have to restart filtering
                    index = 0;
                }
            }
        }

        return this.input.available();
    }

    /**
     * Skips the specified regular expression {@link Pattern}.  Should the {@link Pattern} not match,
     * nothing happens.
     *
     * @param pattern the regular expression {@link Pattern}
     */
    public void skip(final Pattern pattern) {
        optionallyConsume(pattern);
    }

    /**
     * Skips the specified {@link String}.  Should the {@link String} not match, nothing happens.
     * <p>
     * Matching is performed using {@link String#equals(Object)}, which means matching is case-sensitive.
     *
     * @param string the {@link String} to match
     */
    public void skip(final String string) {
        optionallyConsume(string);
    }

    /**
     * Skips the value matched by the specified {@link Evaluator}.  Should the {@link Evaluator} not match,
     * nothing happens.
     *
     * @param token the {@link Evaluator}
     */
    public void skip(final Evaluator<?> token) {
        optionallyConsume(token);
    }

    /**
     * Skip the specified {@link Class} of value using a known (registered) {@link Evaluator}.  Should the
     * {@link Class} be unknown or the {@link Evaluator} not match, nothing happens.
     *
     * @param valueClass the {@link Class} of value
     * @see #register(Class, Evaluator)
     */
    public void skip(final Class<?> valueClass) {
        final Evaluator<?> evaluator = this.evaluators.get(valueClass);

        if (evaluator != null) {
            optionallyConsume(evaluator);
        }
    }

    /**
     * Consumes the specified number of characters, returned that which was consumed
     *
     * @param count the number of characters to skip
     * @return the consumed characters
     * @throws ParseException when the specified number of characters could not be consumed
     */
    public String consume(final int count)
        throws ParseException {
        final String consumed = this.input.consume(count);

        if (consumed.length() != count) {
            throw new ParseException(getLocation(),
                "Failed to consume " + count + " character(s)",
                consumed);
        }

        return consumed;
    }

    /**
     * Consumes the specified regular expression {@link Pattern}, returning that which was consumed.
     *
     * @param pattern the regular expression {@link Pattern}
     * @return the matched {@link String}
     * @throws ParseException when the {@link Pattern} does not match, or the {@link Pattern} was {@code null}
     */
    public String consume(final Pattern pattern)
        throws ParseException {

        if (available()) {
            // attempt to match the provided pattern
            final Optional<String> match = match(pattern);

            return match.orElseThrow(
                () -> new ParseException(this.input.getLocation(), pattern.toString(), this.input.peekMaximum()));
        } else {
            throw new ParseException(this.input.getLocation(), pattern.toString(), "(end of input)");
        }
    }

    /**
     * Consumes the specified {@link String}, returning that which was consumed.
     *
     * @param string the {@link String} to match
     * @return the matched {@link String}
     * @throws ParseException when the {@link String} does not match, or the {@link String} was {@code null}
     */
    public String consume(final String string)
        throws ParseException {

        if (available()) {
            // attempt to match the provided pattern
            final Optional<String> match = match(string);

            return match.orElseThrow(
                () -> new ParseException(this.input.getLocation(), string, this.input.peekMaximum()));
        } else {
            throw new ParseException(this.input.getLocation(), string, "(end of input)");
        }
    }

    /**
     * Consumes a specific type of value defined by an {@link Evaluator}.
     *
     * @param <T>       the type of value
     * @param evaluator the {@link Evaluator}
     * @return the value
     * @throws ParseException when the {@link Evaluator} can not produce a value, or the {@link Evaluator} was
     *                        {@code null}
     */
    public <T> T consume(final Evaluator<T> evaluator)
        throws ParseException {

        if (evaluator == null) {
            throw new ParseException(getLocation(), "The specified Evaluator was null", "A non-null Evaluator");
        }

        if (available()) {
            // attempt to match the provided pattern
            if (evaluator.test(this)) {
                try {
                    return evaluator.apply(this);
                } catch (final ParseException e) {
                    throw new ParseException(getLocation(), evaluator.getDescription(), this.input.peekMaximum());
                } catch (final Exception e) {
                    throw new ParseException(
                        getLocation(),
                        "Failed to evaluate and convert the character sequence into the required type of value",
                        evaluator.getDescription(),
                        e);
                }
            } else {
                throw new ParseException(
                    this.input.getLocation(),
                    evaluator.getDescription(),
                    this.input.peekMaximum());
            }
        } else {
            throw new ParseException(this.input.getLocation(), evaluator.getDescription(), "(end of input)");
        }
    }

    /**
     * Consumes a specific type of value known (registered) with the {@link Scanner}.
     *
     * @param <T>        the type of value
     * @param valueClass the {@link Class} of value
     * @return the value
     * @throws ParseException when the {@link Class} of value can not be matched, or the {@link Class} was
     *                        {@code null}
     * @see #register(Class, Evaluator)
     */
    @SuppressWarnings("unchecked")
    public <T> T consume(final Class<T> valueClass) {
        final Evaluator<T> evaluator = (Evaluator<T>) this.evaluators.get(valueClass);

        if (evaluator != null) {
            return consume(evaluator);
        }

        throw new ParseException(
            this.input.getLocation(),
            "Unable to determine Evaluator for " + valueClass,
            this.input.peekMaximum());
    }

    /**
     * Determines if the specified regular expression {@link Pattern} immediately follows (occurs next).
     *
     * @param pattern the regular expression {@link Pattern}
     * @return {@code true} if the {@link Pattern} matches, {@code false} otherwise
     */
    public boolean follows(final Pattern pattern) {
        final Optional<Pattern> normalized = normalize(pattern);

        return normalized.isPresent() && available() && normalized.get().matcher(this.input.peekMaximum()).find();
    }

    /**
     * Determines if the specified {@link String} immediately follows (occurs next).
     * <p>
     * Matching is performed using {@link String#equals(Object)}, which means matching is case-sensitive.
     *
     * @param string the {@link String} to match
     * @return {@code true} if the pattern matches, {@code false} otherwise
     */
    public boolean follows(final String string) {
        return string != null && available() && this.input.follows(string);
    }

    /**
     * Determines if the type of value defined by an {@link Evaluator} immediately follows (occurs next).
     *
     * @param evaluator the {@link Evaluator}
     * @return {@code true} if the {@link Evaluator} matches, {@code false} otherwise
     */
    public boolean follows(final Evaluator<?> evaluator) {
        return evaluator != null && evaluator.test(this);
    }

    /**
     * Determines if the type of value known (registered) with the {@link Scanner} immediately follows (occurs next).
     *
     * @param valueClass the {@link Class} of value
     * @return {@code true} if the {@link Class} matches, {@code false} otherwise
     * @see #register(Class, Evaluator)
     */
    public boolean follows(final Class<?> valueClass) {
        final Evaluator<?> evaluator = this.evaluators.get(valueClass);
        return evaluator != null && evaluator.test(this);
    }

    /**
     * Attempts to {@link Optional}ly consume the specified regular expression {@link Pattern},
     * returning that which was consumed as an {@link Optional} or {@link Optional#empty()} if the {@link Pattern}
     * did not immediately follow.
     *
     * @param pattern the regular expression {@link Pattern}
     * @return the {@link Optional}ly matched {@link String}
     */
    public Optional<String> optionallyConsume(final Pattern pattern) {
        return pattern != null && follows(pattern) ? Optional.of(consume(pattern)) : Optional.empty();
    }

    /**
     * Attempts to {@link Optional}ly consume the specified {@link String}, returning that which was consumed as
     * an {@link Optional} or {@link Optional#empty()} if the {@link String} did not immediately follow.
     *
     * @param string the {@link String} to match
     * @return the {@link Optional}ly matched {@link String}
     */
    public Optional<String> optionallyConsume(final String string) {
        return string != null && follows(string) ? Optional.of(consume(string)) : Optional.empty();
    }

    /**
     * Attempts to {@link Optional}ly consume the type of value defined by an {@link Evaluator}.
     *
     * @param <T>       the type of value
     * @param evaluator the {@link Evaluator}
     * @return the {@link Optional}ly matched value
     */
    public <T> Optional<T> optionallyConsume(final Evaluator<T> evaluator) {
        return evaluator != null && follows(evaluator) ? Optional.of(consume(evaluator)) : Optional.empty();
    }

    /**
     * Attempts to {@link Optional}ly consume the type of value known (registered) with the {@link Scanner}.
     *
     * @param <T>        the type of value
     * @param valueClass the {@link Class} of value
     * @return the {@link Optional}ly matched value
     * @see #register(Class, Evaluator)
     */
    public <T> Optional<T> optionallyConsume(final Class<T> valueClass) {
        return follows(valueClass) ? Optional.of(consume(valueClass)) : Optional.empty();
    }

    /**
     * Skips characters one at a time until the specified regular expression {@link Pattern} matches,
     * leaving the matching content unconsumed.  Should the end of input be reached before the {@link Pattern}
     * matches, all remaining content is skipped.
     *
     * @param pattern the regular expression {@link Pattern}
     */
    public void skipUntil(final Pattern pattern) {
        while (available() && !follows(pattern)) {
            this.input.consume();
        }
    }

    /**
     * Skips characters one at a time until the specified {@link String} matches,
     * leaving the matching content unconsumed.  Should the end of input be reached before the {@link String}
     * matches, all remaining content is skipped.
     * <p>
     * Matching is performed using {@link String#equals(Object)}, which means matching is case-sensitive.
     *
     * @param string the {@link String} to match
     */
    public void skipUntil(final String string) {
        while (available() && !follows(string)) {
            this.input.consume();
        }
    }

    /**
     * Skips characters one at a time through the end of the next occurrence of the specified {@link String}, or to
     * the end of input when there is none.
     * <p>
     * Unlike {@link #skipUntil(String)}, this works on the raw input: registered {@link Filter}s are not applied,
     * so a delimiter such as a newline is found even when {@link Filter#WHITESPACE} is registered.  Advances
     * unless the input is already exhausted.
     * <p>
     * Matching is performed using {@link String#equals(Object)}, which means matching is case-sensitive.
     *
     * @param string the non-empty {@link String} to skip past
     */
    public void skipPast(final String string) {
        if (string == null || string.isEmpty()) {
            throw new IllegalArgumentException("The string must not be null or empty");
        }
        final int first = string.charAt(0);
        while (this.input.available() && !(this.input.peek() == first && this.input.follows(string))) {
            this.input.consume();
        }
        if (this.input.follows(string)) {
            this.input.consume(string.length());
        }
    }

    /**
     * Skips characters one at a time until the type of value defined by the specified {@link Evaluator} matches,
     * leaving the matching content unconsumed.  Should the end of input be reached before the {@link Evaluator}
     * matches, all remaining content is skipped.
     *
     * @param evaluator the {@link Evaluator}
     */
    public void skipUntil(final Evaluator<?> evaluator) {
        while (available() && !follows(evaluator)) {
            this.input.consume();
        }
    }

    /**
     * Skips characters one at a time until the type of value known (registered) with the {@link Scanner} matches,
     * leaving the matching content unconsumed.  Should the end of input be reached before the type matches,
     * all remaining content is skipped.
     *
     * @param valueClass the {@link Class} of value
     * @see #register(Class, Evaluator)
     */
    public void skipUntil(final Class<?> valueClass) {
        while (available() && !follows(valueClass)) {
            this.input.consume();
        }
    }

    /**
     * Repeatedly skips the specified regular expression {@link Pattern} while it matches at the current position.
     * Should the {@link Pattern} not match, nothing happens.
     *
     * @param pattern the regular expression {@link Pattern}
     */
    public void skipWhile(final Pattern pattern) {
        while (follows(pattern)) {
            skip(pattern);
        }
    }

    /**
     * Repeatedly skips the specified {@link String} while it matches at the current position.
     * Should the {@link String} not match, nothing happens.
     * <p>
     * Matching is performed using {@link String#equals(Object)}, which means matching is case-sensitive.
     *
     * @param string the {@link String} to match
     */
    public void skipWhile(final String string) {
        while (follows(string)) {
            skip(string);
        }
    }

    /**
     * Repeatedly skips the type of value defined by the specified {@link Evaluator} while it matches at
     * the current position.  Should the {@link Evaluator} not match, nothing happens.
     *
     * @param evaluator the {@link Evaluator}
     */
    public void skipWhile(final Evaluator<?> evaluator) {
        while (follows(evaluator)) {
            skip(evaluator);
        }
    }

    /**
     * Repeatedly skips the type of value known (registered) with the {@link Scanner} while it matches at
     * the current position.  Should the type not match, nothing happens.
     *
     * @param valueClass the {@link Class} of value
     * @see #register(Class, Evaluator)
     */
    public void skipWhile(final Class<?> valueClass) {
        while (follows(valueClass)) {
            skip(valueClass);
        }
    }

    /**
     * Consumes characters one at a time until the specified regular expression {@link Pattern} matches,
     * returning all consumed characters.  The matching content is left unconsumed.
     *
     * @param pattern the regular expression {@link Pattern}
     * @return the consumed characters as a {@link String}
     */
    public String consumeUntil(final Pattern pattern) {
        final var builder = new StringBuilder();

        while (available() && !follows(pattern)) {
            builder.append((char) this.input.consume());
        }

        return builder.toString();
    }

    /**
     * Consumes characters one at a time until the specified {@link String} matches,
     * returning all consumed characters.  The matching content is left unconsumed.
     * <p>
     * Matching is performed using {@link String#equals(Object)}, which means matching is case-sensitive.
     *
     * @param string the {@link String} to match
     * @return the consumed characters as a {@link String}
     */
    public String consumeUntil(final String string) {
        final var builder = new StringBuilder();

        while (available() && !follows(string)) {
            builder.append((char) this.input.consume());
        }

        return builder.toString();
    }

    /**
     * Consumes characters one at a time until the type of value defined by the specified {@link Evaluator} matches,
     * returning all consumed characters.  The matching content is left unconsumed.
     *
     * @param evaluator the {@link Evaluator}
     * @return the consumed characters as a {@link String}
     */
    public String consumeUntil(final Evaluator<?> evaluator) {
        final var builder = new StringBuilder();

        while (available() && !follows(evaluator)) {
            builder.append((char) this.input.consume());
        }

        return builder.toString();
    }

    /**
     * Consumes characters one at a time until the type of value known (registered) with the {@link Scanner} matches,
     * returning all consumed characters.  The matching content is left unconsumed.
     *
     * @param valueClass the {@link Class} of value
     * @return the consumed characters as a {@link String}
     * @see #register(Class, Evaluator)
     */
    public String consumeUntil(final Class<?> valueClass) {
        final var builder = new StringBuilder();

        while (available() && !follows(valueClass)) {
            builder.append((char) this.input.consume());
        }

        return builder.toString();
    }

    /**
     * Repeatedly consumes the specified regular expression {@link Pattern} while it matches at the current position,
     * returning all consumed characters.
     *
     * @param pattern the regular expression {@link Pattern}
     * @return the consumed characters as a {@link String}
     */
    public String consumeWhile(final Pattern pattern) {
        final var builder = new StringBuilder();

        while (follows(pattern)) {
            builder.append(consume(pattern));
        }

        return builder.toString();
    }

    /**
     * Repeatedly consumes the type of value defined by the specified {@link Evaluator} while it matches at
     * the current position, returning all consumed values as a {@link Streamable}.
     *
     * @param <T>       the type of value
     * @param evaluator the {@link Evaluator}
     * @return a {@link Streamable} of consumed values
     */
    public <T> Streamable<T> consumeWhile(final Evaluator<T> evaluator) {
        final var values = new ArrayList<T>();

        while (follows(evaluator)) {
            values.add(consume(evaluator));
        }

        return Streamable.of(values);
    }

    /**
     * Repeatedly consumes the type of value known (registered) with the {@link Scanner} while it matches at
     * the current position, returning all consumed values as a {@link Streamable}.
     *
     * @param <T>        the type of value
     * @param valueClass the {@link Class} of value
     * @return a {@link Streamable} of consumed values
     * @see #register(Class, Evaluator)
     */
    public <T> Streamable<T> consumeWhile(final Class<T> valueClass) {
        final var values = new ArrayList<T>();

        while (follows(valueClass)) {
            values.add(consume(valueClass));
        }

        return Streamable.of(values);
    }

    /**
     * Determines if there is further context to parse.
     *
     * @return {@code true} if there is further content, {@code false} otherwise
     */
    public boolean hasNext() {
        return available();
    }

    /**
     * Determines if the next character (after applying filters) matches the specified character.
     *
     * @param c the character to check
     * @return {@code true} if the next character matches, {@code false} otherwise
     */
    public boolean follows(final char c) {
        return available() && this.input.peek() == c;
    }

    /**
     * Peeks at the next character without consuming it, after applying any registered {@link Filter}s.
     * Returns {@code '\0'} if there is no further content.
     *
     * @return the next character, or {@code '\0'} if there is no further content
     */
    public char peekChar() {
        if (!available()) {
            return '\0';
        }
        final int peeked = this.input.peek();
        return peeked == -1 ? '\0' : (char) peeked;
    }

    /**
     * Consumes and returns the next character, after applying any registered {@link Filter}s.
     *
     * @return the consumed character
     * @throws ParseException if there is no further content
     */
    public char consumeChar()
        throws ParseException {
        if (!available()) {
            throw new ParseException(this.input.getLocation(), "any character", "(end of input)");
        }
        final int consumed = this.input.consume();
        if (consumed == -1) {
            throw new ParseException(this.input.getLocation(), "any character", "(end of input)");
        }
        return (char) consumed;
    }

    /**
     * Consumes balanced delimiters and returns the body (excluding both outer delimiters).
     * <p>
     * Precondition: the scanner is positioned on the opening delimiter character (after any registered filters
     * have skipped whitespace/comments).  After verifying that opening character, the body is read at the raw
     * character level — filters do <em>not</em> apply inside the body, so whitespace and comment characters are
     * preserved verbatim.
     * <p>
     * Nested {@code open}/{@code close} pairs increment and decrement an internal depth counter.  Quoted strings
     * inside the body are skipped intact — single and double quotes are both supported, with backslash escape —
     * so quote-enclosed delimiters do not affect the depth.
     *
     * @param open  the opening delimiter character
     * @param close the closing delimiter character (must differ from {@code open})
     * @return the body, excluding both outer delimiters
     * @throws ParseException if not positioned on {@code open}, or if EOF is reached before the matching close
     */
    public String consumeBalanced(final char open, final char close)
        throws ParseException {
        if (!follows(open)) {
            throw new ParseException(getLocation(), String.valueOf(open),
                hasNext() ? String.valueOf(peekChar()) : "(end of input)");
        }
        consumeChar();
        final var body = new StringBuilder();
        int depth = 1;
        boolean inString = false;
        char stringChar = 0;
        boolean escape = false;
        while (depth > 0) {
            final int next = this.input.consume();
            if (next == -1) {
                throw new ParseException(this.input.getLocation(), String.valueOf(close), "(end of input)");
            }
            final char c = (char) next;
            if (escape) {
                escape = false;
                body.append(c);
                continue;
            }
            if (inString) {
                body.append(c);
                if (c == '\\') {
                    escape = true;
                } else if (c == stringChar) {
                    inString = false;
                }
                continue;
            }
            if (c == '"' || c == '\'') {
                inString = true;
                stringChar = c;
                body.append(c);
            } else if (c == open) {
                depth++;
                body.append(c);
            } else if (c == close) {
                depth--;
                if (depth > 0) {
                    body.append(c);
                }
            } else {
                body.append(c);
            }
        }
        return body.toString();
    }

    /**
     * Determines if the next character (after applying filters) satisfies the specified {@link IntPredicate}.
     *
     * @param predicate the {@link IntPredicate}
     * @return {@code true} if the next character satisfies the predicate, {@code false} otherwise
     */
    public boolean follows(final IntPredicate predicate) {
        return predicate != null && available() && predicate.test(this.input.peek());
    }

    /**
     * Attempts to {@link Optional}ly consume the next character if it satisfies the specified {@link IntPredicate},
     * returning it as an {@link Optional} or {@link Optional#empty()} if it does not.
     *
     * @param predicate the {@link IntPredicate}
     * @return the {@link Optional}ly consumed character
     */
    public Optional<Character> optionallyConsume(final IntPredicate predicate) {
        if (!follows(predicate)) {
            return Optional.empty();
        }
        final int consumed = this.input.consume();
        return consumed == -1 ? Optional.empty() : Optional.of((char) consumed);
    }

    /**
     * Repeatedly skips characters while they satisfy the specified {@link IntPredicate}.
     * Should no character satisfy the predicate, nothing happens.
     *
     * @param predicate the {@link IntPredicate}
     */
    public void skipWhile(final IntPredicate predicate) {
        while (follows(predicate)) {
            this.input.consume();
        }
    }

    /**
     * Repeatedly consumes characters while they satisfy the specified {@link IntPredicate},
     * returning all consumed characters as a {@link String}.
     *
     * @param predicate the {@link IntPredicate}
     * @return the consumed characters
     */
    public String consumeWhile(final IntPredicate predicate) {
        final var sb = new StringBuilder();
        while (follows(predicate)) {
            final int c = this.input.consume();
            if (c == -1) {
                break;
            }
            sb.append((char) c);
        }
        return sb.toString();
    }
}
