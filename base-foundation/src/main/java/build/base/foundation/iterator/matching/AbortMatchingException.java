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
 * An {@link Exception} thrown to immediately abort (short-cut) further attempts to evaluate an
 * {@link IteratorPatternMatcher}.
 *
 * @author brian.oliver
 * @since Jun-2019
 */
public class AbortMatchingException
    extends Exception {

    /**
     * The {@link Matcher} causing the evaluation to abort.
     */
    private final Matcher<?> matcher;

    /**
     * Constructs an {@link AbortMatchingException}.
     *
     * @param matcher the {@link Matcher} causing the evaluation to abort.
     */
    AbortMatchingException(final Matcher<?> matcher) {
        this.matcher = matcher;
    }

    /**
     * Obtains the {@link Matcher} that caused the evaluation to abort.
     *
     * @return the {@link Matcher}
     */
    public Matcher<?> getMatcher() {
        return this.matcher;
    }
}
