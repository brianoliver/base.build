package build.base.foundation.iterator.matching;

/*-
 * #%L
 * base.build Foundation
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

import build.base.foundation.iterator.ResettableIterator;

import java.util.Iterator;

/**
 * Provides the abstractly represent and evaluate a {@link Condition} defined as part of {@link IteratorPatternMatcher}.
 * <p>
 * Each {@link Matcher} is linked to the {@link Matcher} that possibly precedes it, together with the
 * {@link Matcher} that possibly proceeds it in the sequence of {@link Condition}s defined by an
 * {@link IteratorPatternMatcher}.  This creates a doubly-linked-list-like structure permitting
 * {@link IteratorPatternMatcher} evaluation to occur from any {@link Condition},
 * {@link Condition} lookahead and backtracking in the case of evaluation failure.
 *
 * @param <T> the type of elements evaluate
 * @author brian.oliver
 * @since Jun-2019
 */
interface Matcher<T>
    extends IteratorPatternMatcher<T>, Condition<T> {

    /**
     * Obtains the first {@link Matcher} in the sequence of {@link Matcher}s defined for
     * an {@link IteratorPatternMatcher}.
     *
     * @return the first {@link Matcher}
     */
    Matcher<T> first();

    /**
     * Obtains the next {@link Matcher} in the sequence of {@link Matcher}s defined for
     * an {@link IteratorPatternMatcher}.
     *
     * @return the next {@link Matcher} (or {@code null} if this is the last {@link Matcher})
     */
    Matcher<T> next();

    /**
     * Obtains the previous {@link Matcher} in the sequence of {@link Matcher}s defined for an
     * {@link IteratorPatternMatcher}.
     *
     * @return the previous {@link Matcher} (or {@code null} if this is the first {@link Matcher})
     */
    Matcher<T> previous();

    /**
     * Attempts to match the {@link Condition} defined by the {@link Matcher}
     * at the current position in the {@link ResettableIterator}.
     *
     * @param iterator the {@link ResettableIterator}
     * @return {@code true} if the {@link Condition} matches at the current position,
     * {@code false} otherwise
     * @throws AbortMatchingException when a {@link Matcher} determines it's impossible to match against the
     *                                provided {@link ResettableIterator}, thus allowing early termination of evaluation
     */
    boolean match(ResettableIterator<T> iterator)
        throws AbortMatchingException;

    /**
     * Describes the {@link Condition} defined by the {@link Matcher} so that it can
     * be used to construct a {@link String}.
     *
     * @return a description of the {@link Condition}
     */
    String describe();

    /**
     * Obtains the {@link Matcher} to evaluate after successfully {@link #match(ResettableIterator)}ing this
     * {@link Matcher}.
     * <p>
     * By default, the next {@link Matcher} to evaluate is the next in the sequence.  However, there are circumstances
     * where this may be customized.
     *
     * @return the next {@link Matcher} to evaluate in the {@link IteratorPatternMatcher}, or
     * {@code null} if no more matches need to occur for the {@link IteratorPatternMatcher}
     */
    default Matcher<T> step() {
        return next();
    }

    /**
     * Sequentially steps through the values provided by the {@link ResettableIterator} (ie: searches) to match
     * this {@link Matcher} together with <strong>all of the remaining</strong> {@link Matcher}s in the
     * sequence defined by the {@link IteratorPatternMatcher}.
     * <p>
     * Should this {@link Matcher} not be satisfied by the current value in the {@link ResettableIterator}, the
     * {@link ResettableIterator} is advanced to the next value.  Should this {@link Matcher} be satisfied by the
     * current value in the {@link ResettableIterator}, the next {@link Matcher} in the sequence is
     * attempted to be matched with the {@link ResettableIterator}. Should matching fail, the {@link ResettableIterator}
     * is reset to the position upon entering this method. Should matching succeed, the {@link ResettableIterator} is
     * left at the position after the match succeeded, allowing for further matching.
     *
     * @param iterator the {@link ResettableIterator}
     * @param canDrop  {@code true} when the elements including the current in the {@link ResettableIterator} can be
     *                 dropped, {@code false} when they must be retained
     * @return {@code true} when this and all remaining {@link Matcher}s are satisfied by the provided
     * {@link ResettableIterator}, {@code false} otherwise
     */
    default boolean search(final ResettableIterator<T> iterator, final boolean canDrop) {
        // we must try to match at least once
        do {
            try {
                // attempt to match this matcher at the current position in the iterator
                if (evaluate(iterator)) {
                    return true;
                }
                else {
                    // matching failed, so step to the next value in the iteration (if one exists)
                    if (iterator.hasNext()) {
                        iterator.next();
                        if (canDrop) {
                            final ResettableIterator.Position dropPosition = iterator.mark();
                            iterator.drop(dropPosition);
                        }
                    }
                }
            }
            catch (final AbortMatchingException e) {
                // search fails when matching has been aborted
                return false;
            }

            // keep searching for a match until we run out of values
        } while (iterator.hasNext());

        // failed to match
        return false;
    }

    /**
     * Attempts to evaluate this {@link AbstractMatcher} together with <strong>all of the remaining</strong>
     * {@link AbstractMatcher}s in the matcher sequence defined by the {@link IteratorPatternMatcher} with values
     * provided by and starting <strong>at the current position</strong> in the specified {@link ResettableIterator}
     * <p>
     * Should matching fail or need to be aborted, the {@link ResettableIterator} is reset to the position upon
     * entering this method.  Should matching succeed, the {@link ResettableIterator} is left at the position
     * after the match succeeded, allowing for further matching.
     *
     * @param iterator the {@link ResettableIterator}
     * @return {@code true} when this and all remaining {@link AbstractMatcher}s are satisfied by the provided
     * {@link ResettableIterator}, {@code false} otherwise
     * @throws AbortMatchingException when it has been determined matching can never be satisfied
     */
    default boolean evaluate(final ResettableIterator<T> iterator)
        throws AbortMatchingException {

        // remember the current position in the iterator
        // (so we can back-track to here should matching fail)
        final var position = iterator.mark();

        // attempt to match the entire IteratorPatternMatcher (starting from here)
        Matcher<T> pattern = this;

        try {
            // attempt to match the patterns in the sequence until we run out of patterns, or we fail to match
            while (pattern != null &&
                pattern.match(iterator)) {

                // step to the next pattern as we successfully matched the current one
                pattern = pattern.step();
            }
        }
        catch (final AbortMatchingException e) {
            // a pattern deemed it was impossible to match so it aborted
            // so we need to reset to the position to the start of where the matching started
            iterator.reset(position);

            throw e;
        }

        if (pattern == null) {
            // success! we've matched when we've past the last matcher
            return true;
        }
        else {
            // oh no! we didn't match the patterns
            // so we need to reset to the position to the start of where the matching started
            iterator.reset(position);

            return false;
        }
    }

    @Override
    default boolean test(final Iterator<T> iterator) {
        return first()
            .search(new ResettableIterator<>(iterator), true);
    }
}
