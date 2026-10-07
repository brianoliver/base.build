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

import build.base.io.LookaheadReader;

import java.util.Objects;
import java.util.Optional;

/**
 * A {@link RuntimeException} thrown when expected content is not found while scanning, tokenizing or parsing text.
 *
 * @author brian.oliver
 * @see Scanner
 * @since Nov-2019
 */
public class ParseException
    extends RuntimeException {

    /**
     * The {@link Optional} {@link LookaheadReader.Location} at which the content was expected.
     */
    private final Optional<LookaheadReader.Location> location;

    /**
     * The expected content or pattern.
     */
    private final String expected;

    /**
     * The found content.
     */
    private final CharSequence found;

    /**
     * Constructs a {@link ParseException}.
     *
     * @param location  the optional {@link LookaheadReader.Location} where the content was expected (may be {@code null})
     * @param expected  the expected content
     * @param found     the found content
     * @param throwable the optional {@link Throwable} associated with the {@link ParseException} (may be {@code null})
     */
    public ParseException(final LookaheadReader.Location location,
                          final String expected,
                          final CharSequence found,
                          final Throwable throwable) {

        super(throwable);

        this.location = Optional.ofNullable(location);
        this.expected = Objects.requireNonNull(expected, "The expected content must not be null");
        this.found = Objects.requireNonNull(found, "The found content must not be null");
    }

    /**
     * Constructs a {@link ParseException}.
     *
     * @param location the {@link LookaheadReader.Location} where the content was expected
     * @param expected the expected content
     * @param found    the found content
     */
    public ParseException(final LookaheadReader.Location location,
                          final String expected,
                          final CharSequence found) {

        this(location, expected, found, null);
    }

    /**
     * Obtains the {@link Optional} @link LookaheadReader.Location} at which the content was expected.
     *
     * @return the {@link LookaheadReader.Location}
     */
    public Optional<LookaheadReader.Location> getLocation() {
        return this.location;
    }

    /**
     * Obtains the expected content or pattern.
     *
     * @return the expected content
     */
    public String getExpected() {
        return this.expected;
    }

    /**
     * Obtains the content that was found.
     *
     * @return the found content
     */
    public CharSequence getFound() {
        return this.found;
    }

    /**
     * Obtains a description of what was expected, what was found and, when known, where, for example
     * {@code Expected [;] but found [template] at line 2, column 1}.
     *
     * @return the message, never {@code null}
     */
    @Override
    public String getMessage() {
        return "Expected [" + this.expected + "] but found [" + this.found + "]"
            + this.location.map(l -> " at line " + l.getLine() + ", column " + l.getColumn()).orElse("");
    }

    @Override
    public String toString() {
        return "Parser Exception: " + getMessage();
    }
}
