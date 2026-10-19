package build.base.parsing;

/*-
 * #%L
 * base.build Parsing
 * %%
 * Copyright (C) 2026 The base.build Authors and Contributors
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A composable parser fragment — Layer B of base-parsing's two parser styles.
 * <p>
 * A {@code Rule<T>} attempts to parse a value of type {@code T} from a {@link Scanner}, returning
 * {@link Optional#empty()} when the input does not match (without consuming input) and a present {@link Optional}
 * when it does.  Throws {@link ParseException} for hard failures where input was partially consumed but could not
 * be completed.  Combinator methods ({@link #or}, {@link #map}, {@link #repeated}, {@link #separatedBy}, …) compose
 * rules into larger rules.
 * <p>
 * Used by {@link ExpressionParser}, {@link TemplateParser}, and any {@link AbstractParser} subclass that wants to
 * factor a sub-rule out of its imperative grammar methods.
 * <p>
 * For full grammars with domain-specific exception types and full-input consumption, see {@link Parser} and
 * {@link AbstractParser} (Layer A).
 * <p>
 * Combinators that try alternatives ({@link #or}, {@link #not}) use {@link Scanner#save()} and
 * {@link Scanner#restore(int)} and therefore require a backtracking-capable {@link Scanner} (constructed from a
 * {@link String}).
 *
 * @param <T> the type of value produced
 * @author reed.vonredwitz
 * @since Apr-2026
 */
@FunctionalInterface
public interface Rule<T> {

    /**
     * Attempts to parse a value of type {@code T} from the current position of the {@link Scanner}.
     *
     * @param scanner the {@link Scanner}
     * @return the parsed value, or {@link Optional#empty()} if the input does not match
     * @throws ParseException on a hard parse failure
     */
    Optional<T> tryParse(Scanner scanner) throws ParseException;

    /**
     * Returns a {@link Rule} that maps the result of this rule using the given function.
     *
     * @param <U> the result type
     * @param fn  the mapping function
     * @return a mapped rule
     */
    default <U> Rule<U> map(final Function<T, U> fn) {
        return scanner -> tryParse(scanner).map(fn);
    }

    /**
     * Returns a {@link Rule} that tries this rule first and, if it returns empty, tries {@code other}.
     * <p>
     * Requires a backtracking-capable {@link Scanner}.
     *
     * @param other the alternative rule
     * @return a choice rule
     * @throws UnsupportedOperationException at parse time if the scanner does not support backtracking
     */
    default Rule<T> or(final Rule<T> other) {
        return scanner -> {
            if (!scanner.supportsBacktracking()) {
                throw new UnsupportedOperationException("Rule.or() requires a backtracking-capable Scanner");
            }
            final int checkpoint = scanner.save();
            try {
                final Optional<T> result = tryParse(scanner);
                if (result.isPresent()) {
                    return result;
                }
            } catch (final ParseException ignored) {
                // first alternative failed — try the other
            }
            scanner.restore(checkpoint);
            return other.tryParse(scanner);
        };
    }

    /**
     * Returns a {@link Rule} that runs this rule for its side-effects and then returns the result of {@code next}.
     *
     * @param <U>  the result type of the next rule
     * @param next the rule to run after this one
     * @return a sequence rule
     */
    default <U> Rule<U> then(final Rule<U> next) {
        return scanner -> {
            tryParse(scanner);
            return next.tryParse(scanner);
        };
    }

    /**
     * Returns a {@link Rule} that applies this rule zero or more times, collecting results into a {@link List}.
     * <p>
     * Stops when this rule returns {@link Optional#empty()}.  Always succeeds (returns an empty list when there
     * are no matches).
     *
     * @return a repeated rule
     */
    default Rule<List<T>> repeated() {
        return scanner -> {
            final List<T> results = new ArrayList<>();
            Optional<T> item;
            while ((item = tryParse(scanner)).isPresent()) {
                results.add(item.get());
            }
            return Optional.of(results);
        };
    }

    /**
     * Returns a {@link Rule} that parses a separated list: one or more occurrences of this rule
     * interleaved with the separator rule.  Returns an empty list when not even a first element matches.
     *
     * @param sep the separator rule
     * @return a separated-list rule
     */
    default Rule<List<T>> separatedBy(final Rule<?> sep) {
        return scanner -> {
            final Optional<T> first = tryParse(scanner);
            if (first.isEmpty()) {
                return Optional.of(List.of());
            }
            final List<T> results = new ArrayList<>();
            results.add(first.get());
            while (sep.tryParse(scanner).isPresent()) {
                final Optional<T> next = tryParse(scanner);
                if (next.isEmpty()) {
                    break;
                }
                results.add(next.get());
            }
            return Optional.of(results);
        };
    }

    /**
     * Returns a {@link Rule} that applies this rule and then filters the result by the given predicate.
     * Returns {@link Optional#empty()} when the predicate is not satisfied.
     *
     * @param predicate the predicate
     * @return a filtered rule
     */
    default Rule<T> filter(final Predicate<T> predicate) {
        return scanner -> tryParse(scanner).filter(predicate);
    }

    /**
     * Returns a {@link Rule} that runs this rule and {@code other} in sequence, combining their results with
     * {@code fn}.
     * <p>
     * Returns {@link Optional#empty()} if either rule returns empty.  Note that if this rule succeeds but
     * {@code other} returns empty, any input consumed by this rule is not restored — use {@link #or} with a
     * checkpoint if you need that guarantee.
     *
     * @param <U>   the result type of the second rule
     * @param <R>   the combined result type
     * @param other the second rule
     * @param fn    the combining function
     * @return a zip rule
     */
    default <U, R> Rule<R> zip(final Rule<U> other, final BiFunction<T, U, R> fn) {
        return scanner -> {
            final Optional<T> left = tryParse(scanner);
            if (left.isEmpty()) {
                return Optional.empty();
            }
            final Optional<U> right = other.tryParse(scanner);
            if (right.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(fn.apply(left.get(), right.get()));
        };
    }

    /**
     * Returns a {@link Rule} that always succeeds, wrapping the result of this rule in an {@link Optional}.
     * Returns {@link Optional#of}{@code (Optional.empty())} when this rule does not match.
     *
     * @return an optional rule
     */
    default Rule<Optional<T>> optional() {
        return scanner -> Optional.of(tryParse(scanner));
    }

    /**
     * Returns a {@link Rule} that applies this rule one or more times, collecting results into a {@link List}.
     * Returns {@link Optional#empty()} when there are no matches.
     *
     * @return a one-or-more repeated rule
     */
    default Rule<List<T>> repeated1() {
        return scanner -> {
            final Optional<T> first = tryParse(scanner);
            if (first.isEmpty()) {
                return Optional.empty();
            }
            final List<T> results = new ArrayList<>();
            results.add(first.get());
            Optional<T> item;
            while ((item = tryParse(scanner)).isPresent()) {
                results.add(item.get());
            }
            return Optional.of(results);
        };
    }

    /**
     * Returns a {@link Rule} that parses {@code open}, then this rule, then {@code close}, returning only
     * this rule's result.
     * <p>
     * Returns {@link Optional#empty()} if {@code open} does not match.  The {@code close} rule is always
     * attempted when the content rule succeeds; its result is discarded.
     *
     * @param open  the opening delimiter rule
     * @param close the closing delimiter rule
     * @return a between rule
     */
    default Rule<T> between(final Rule<?> open, final Rule<?> close) {
        return scanner -> {
            if (open.tryParse(scanner).isEmpty()) {
                return Optional.empty();
            }
            final Optional<T> result = tryParse(scanner);
            close.tryParse(scanner);
            return result;
        };
    }

    /**
     * Returns a {@link Rule} that applies this rule exactly {@code n} times, collecting results into a
     * {@link List}.  Returns {@link Optional#empty()} if the first application does not match.
     * <p>
     * Note that if one or more (but fewer than {@code n}) applications match, any consumed input is not restored.
     *
     * @param n the exact number of times to apply this rule
     * @return a fixed-count rule
     * @throws IllegalArgumentException if {@code n} is negative
     */
    default Rule<List<T>> times(final int n) {
        if (n < 0) {
            throw new IllegalArgumentException("times() count must not be negative");
        }
        return scanner -> {
            final List<T> results = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                final Optional<T> item = tryParse(scanner);
                if (item.isEmpty()) {
                    return i == 0 ? Optional.empty() : Optional.of(results);
                }
                results.add(item.get());
            }
            return Optional.of(results);
        };
    }

    /**
     * Returns a {@link Rule} that applies this rule at least {@code min} times, collecting results into a
     * {@link List}.  Returns {@link Optional#empty()} if fewer than {@code min} matches are found.
     * <p>
     * Continues matching beyond {@code min} until the rule returns empty.
     *
     * @param min the minimum number of matches required
     * @return an at-least rule
     * @throws IllegalArgumentException if {@code min} is negative
     */
    default Rule<List<T>> atLeast(final int min) {
        if (min < 0) {
            throw new IllegalArgumentException("atLeast() min must not be negative");
        }
        return scanner -> {
            final List<T> results = new ArrayList<>();
            Optional<T> item;
            while ((item = tryParse(scanner)).isPresent()) {
                results.add(item.get());
            }
            return results.size() >= min ? Optional.of(results) : Optional.empty();
        };
    }

    /**
     * Returns a {@link Rule} that applies this rule at most {@code max} times, collecting results into a
     * {@link List}.  Always succeeds (returns an empty list when there are no matches).
     *
     * @param max the maximum number of matches to collect
     * @return an at-most rule
     * @throws IllegalArgumentException if {@code max} is negative
     */
    default Rule<List<T>> atMost(final int max) {
        if (max < 0) {
            throw new IllegalArgumentException("atMost() max must not be negative");
        }
        return scanner -> {
            final List<T> results = new ArrayList<>();
            while (results.size() < max) {
                final Optional<T> item = tryParse(scanner);
                if (item.isEmpty()) {
                    break;
                }
                results.add(item.get());
            }
            return Optional.of(results);
        };
    }

    /**
     * Returns a {@link Rule} that succeeds (returning {@code true}) when this rule does <em>not</em> match at the
     * current position, and fails when it does.  The scanner position is never advanced.
     * <p>
     * Requires a backtracking-capable {@link Scanner} so the attempted parse can be undone.
     *
     * @return a negative-lookahead rule
     * @throws UnsupportedOperationException at parse time if the scanner does not support backtracking
     */
    default Rule<Boolean> not() {
        return scanner -> {
            if (!scanner.supportsBacktracking()) {
                throw new UnsupportedOperationException("Rule.not() requires a backtracking-capable Scanner");
            }
            final int checkpoint = scanner.save();
            final boolean matched;
            try {
                matched = tryParse(scanner).isPresent();
            } catch (final ParseException ignored) {
                scanner.restore(checkpoint);
                return Optional.of(true);
            }
            scanner.restore(checkpoint);
            return matched ? Optional.empty() : Optional.of(true);
        };
    }

    /**
     * Returns a {@link Rule} that succeeds (returning {@code true}) only when the scanner has no remaining input,
     * and fails otherwise.
     *
     * @return an end-of-input rule
     */
    static Rule<Boolean> endOfInput() {
        return scanner -> scanner.hasNext() ? Optional.empty() : Optional.of(true);
    }

    /**
     * Defines a recursive {@link Rule} by providing a self-reference placeholder.
     * <p>
     * The {@code definition} function receives a placeholder that forwards all parse calls to the
     * fully-constructed rule.  This enables rules that reference themselves, such as nested structures and
     * recursive grammars.
     * <p>
     * Direct left recursion (a rule whose first action is to call itself) will cause a {@link StackOverflowError}
     * at parse time, as with any recursive-descent parser.  Use iteration ({@link #repeated},
     * {@link #separatedBy}) for left-recursive patterns instead.
     *
     * <pre>{@code
     * // Nested parentheses: '(' (word | nested)* ')'
     * Rule<String> nested = Rule.define(self ->
     *     self.or(WORD).repeated().map(parts -> String.join("", parts))
     *         .between(OPEN_PAREN, CLOSE_PAREN));
     * }</pre>
     *
     * @param <T>        the result type
     * @param definition a function that receives a self-reference and returns the rule definition
     * @return the recursive rule
     */
    static <T> Rule<T> define(final Function<Rule<T>, Rule<T>> definition) {
        final var ref = new AtomicReference<Rule<T>>();
        final Rule<T> placeholder = scanner -> ref.get().tryParse(scanner);
        ref.set(definition.apply(placeholder));
        return placeholder;
    }

    /**
     * Returns a {@link Rule} that behaves identically to this rule but relabels any {@link ParseException} it
     * throws, replacing the expected description with {@code description}.  Soft failures
     * ({@link Optional#empty()} returns) are passed through unchanged.
     *
     * @param description the description to use in place of the original expected text
     * @return a labelled rule
     */
    default Rule<T> label(final String description) {
        return scanner -> {
            try {
                return tryParse(scanner);
            } catch (final ParseException e) {
                throw new ParseException(e.getLocation().orElse(null), description, e.getFound(), e);
            }
        };
    }

    /**
     * Returns a {@link Rule} that throws a {@link ParseException} with the given description when this rule
     * returns {@link Optional#empty()}, converting a soft failure into a hard one.  If this rule throws a
     * {@link ParseException}, it is relabelled with {@code description}.
     *
     * @param description the description of what was expected
     * @return a requiring rule
     */
    default Rule<T> require(final String description) {
        return scanner -> {
            try {
                final Optional<T> result = tryParse(scanner);
                if (result.isEmpty()) {
                    throw new ParseException(scanner.getLocation(), description, scanner.hasNext()
                        ? scanner.peekChar() + ""
                        : "(end of input)");
                }
                return result;
            } catch (final ParseException e) {
                if (e.getExpected().equals(description)) {
                    throw e;
                }
                throw new ParseException(e.getLocation().orElse(null), description, e.getFound(), e);
            }
        };
    }
}
