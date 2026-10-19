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

import build.base.io.LookaheadReader;

import java.util.function.Predicate;

/**
 * The character-level input abstraction used by {@link Scanner}.
 * <p>
 * Implementations that hold a fixed char buffer (e.g. {@link StringInput}) may support backtracking via
 * {@link #save()} and {@link #restore(int)}; stream-backed implementations (e.g. {@link LookaheadReaderInput})
 * typically do not.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public interface ScannerInput
    extends AutoCloseable {

    /**
     * Returns {@code true} if there are characters remaining in the input.
     *
     * @return {@code true} if input is available
     */
    boolean available();

    /**
     * Returns the next character without consuming it, or {@code -1} at end-of-input.
     *
     * @return the next character as an {@code int}, or {@code -1}
     */
    int peek();

    /**
     * Returns up to {@code size} next characters without consuming them.
     *
     * @param size the maximum number of characters to peek
     * @return the peeked characters
     */
    CharSequence peek(int size);

    /**
     * Returns all currently buffered characters without consuming them.
     *
     * @return the peeked characters
     */
    CharSequence peekMaximum();

    /**
     * Consumes and returns the next character, or {@code -1} at end-of-input.
     *
     * @return the consumed character as an {@code int}, or {@code -1}
     */
    int consume();

    /**
     * Consumes and returns up to {@code size} characters.
     *
     * @param size the number of characters to consume
     * @return the consumed characters
     */
    String consume(int size);

    /**
     * Returns the current location in the input stream.
     *
     * @return the current {@link LookaheadReader.Location}
     */
    LookaheadReader.Location getLocation();

    @Override
    void close() throws Exception;

    /**
     * Returns {@code true} if the specified string occurs next in the input.
     *
     * @param string the string to check
     * @return {@code true} if the string follows
     */
    default boolean follows(final String string) {
        return string != null && peek(string.length()).equals(string);
    }

    /**
     * Returns {@code true} if the next character satisfies the predicate.
     *
     * @param predicate the predicate to test
     * @return {@code true} if the predicate is satisfied
     */
    default boolean follows(final Predicate<? super Integer> predicate) {
        return predicate != null && available() && predicate.test(peek());
    }

    /**
     * Skips {@code count} characters.
     *
     * @param count the number of characters to skip
     */
    default void skip(final int count) {
        consume(count);
    }

    /**
     * Skips characters while the predicate is satisfied.
     *
     * @param predicate the predicate
     */
    default void skipWhile(final Predicate<? super Character> predicate) {
        if (predicate != null) {
            while (available() && predicate.test((char) peek())) {
                consume();
            }
        }
    }

    /**
     * Returns {@code true} if this input supports backtracking via {@link #save()} and {@link #restore(int)}.
     *
     * @return {@code true} if backtracking is supported
     */
    default boolean supportsBacktracking() {
        return false;
    }

    /**
     * Saves the current position and returns a checkpoint that can be passed to {@link #restore(int)}.
     *
     * @return an opaque checkpoint value
     * @throws UnsupportedOperationException if backtracking is not supported
     */
    default int save() {
        throw new UnsupportedOperationException("This ScannerInput does not support backtracking");
    }

    /**
     * Restores the input position to the given checkpoint.
     *
     * @param checkpoint a value previously returned by {@link #save()}
     * @throws UnsupportedOperationException if backtracking is not supported
     */
    default void restore(final int checkpoint) {
        throw new UnsupportedOperationException("This ScannerInput does not support backtracking");
    }
}
