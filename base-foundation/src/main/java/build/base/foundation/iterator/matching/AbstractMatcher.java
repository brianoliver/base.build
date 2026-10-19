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

/**
 * An {@code abstract} implementation of a {@link Matcher}.
 *
 * @param <T> the type of elements evaluate
 * @author brian.oliver
 * @since Jun-2019
 */
abstract class AbstractMatcher<T>
    implements Matcher<T> {

    /**
     * The previous {@link AbstractMatcher} in the sequence of {@link AbstractMatcher}s defined for an {@link IteratorPatternMatcher}.
     * <p>
     * This will be {@code null} when the {@link AbstractMatcher} is the first defined for an {@link IteratorPatternMatcher}.
     */
    protected AbstractMatcher<T> previous;

    /**
     * The next {@link AbstractMatcher} in the sequence of {@link AbstractMatcher}s defined for an {@link IteratorPatternMatcher}.
     * <p>
     * This will be {@code null} when the {@link AbstractMatcher} is the last defined for an {@link IteratorPatternMatcher}.
     */
    protected AbstractMatcher<T> next;

    /**
     * Constructs a {@link AbstractMatcher}, linking it to the preceding {@link AbstractMatcher} (if specified).
     *
     * @param previous the previous {@link AbstractMatcher} in the sequence of {@link AbstractMatcher}s defined for
     *                 an {@link IteratorPatternMatcher}
     */
    AbstractMatcher(final AbstractMatcher<T> previous) {

        this.previous = previous;
        this.next = null;

        if (this.previous != null) {
            this.previous.next = this;
        }
    }

    /**
     * Constructs a {@link AbstractMatcher}, as the first for an {@link IteratorPatternMatcher}.
     */
    AbstractMatcher() {
        this(null);
    }

    @Override
    public Matcher<T> first() {
        Matcher<T> current = this;

        while (current.previous() != null) {
            current = current.previous();
        }

        return current;
    }

    @Override
    public Matcher<T> next() {
        return this.next;
    }

    @Override
    public Matcher<T> previous() {
        return this.previous;
    }

    @Override
    public Matcher<T> step() {
        return next();
    }

    @Override
    public String toString() {
        final var builder = new StringBuilder();

        var matcher = first();
        do {
            builder.append(matcher.describe());
            matcher = matcher.next();
            if (matcher != null) {
                builder.append(".");
            }
        } while (matcher != null);

        return builder.toString();
    }
}
