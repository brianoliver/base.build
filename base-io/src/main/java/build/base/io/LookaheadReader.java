package build.base.io;

/*-
 * #%L
 * base.build IO
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

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A {@link FilterReader} permitting lookahead with read {@link Location} tracking.
 *
 * @author brian.oliver
 * @since Aug-2019
 */
public class LookaheadReader
    extends FilterReader {

    /**
     * The default preferred number of characters to lookahead (4k)
     */
    public static final int DEFAULT_LOOKAHEAD = 4096;

    /**
     * The lookahead buffer.
     */
    private char[] buffer;

    /**
     * The current index position in the buffer.
     */
    private int index;

    /**
     * The current number of characters available in the buffer.
     */
    private int length;

    /**
     * Has the end of the stream been reached?
     */
    private boolean hasReachedEndOfStream;

    /**
     * The line number for the current index position in the buffer.
     */
    private int line;

    /**
     * The column number of the current index position in the buffer.
     */
    private int column;

    /**
     * The zero-based character offset of the current index position in the stream.
     */
    private long offset;

    /**
     * Constructs a {@link LookaheadReader} based on the provided {@link Reader} with a desired lookahead character
     * buffer size.
     *
     * @param reader    the {@link Reader}
     * @param lookahead the desired lookahead character buffer size
     */
    public LookaheadReader(final Reader reader, final int lookahead) {
        super(Objects.requireNonNull(reader, "The Reader must not be null"));

        this.buffer = new char[lookahead <= 0 ? DEFAULT_LOOKAHEAD : lookahead];

        this.hasReachedEndOfStream = false;
        this.index = 0;
        this.length = 0;

        this.line = 1;
        this.column = 1;
        this.offset = 0;
    }

    /**
     * Constructs a {@link LookaheadReader} based on the provided {@link Reader} using the {@link #DEFAULT_LOOKAHEAD}
     * as the desired lookahead character buffer size.
     *
     * @param reader the {@link Reader}
     */
    public LookaheadReader(final Reader reader) {
        this(reader, DEFAULT_LOOKAHEAD);
    }

    /**
     * Attempts to fill the internal buffer from the underlying {@link Reader} with the desired number
     * of lookahead {@link Character}s. Should the existing internal buffer be too small to
     * hold the desired number of {@link Character}s, a new internal buffer is suitably allocated.
     * <p>
     * Should an {@link IOException} occur when attempting read from the underlying {@link Reader}, the buffer will
     * remain untouched, but the {@link LookaheadReader} will assume an end-of-stream has occurred.
     *
     * @param desired the desired number of {@link Character}s in the buffer
     * @return {@code true} if the internal buffer has some content for reading, {@code false} otherwise
     */
    private boolean prepare(final int desired) {

        // determine the remaining number of characters in the lookahead buffer
        final int remaining = this.length - this.index;

        // only prepare the buffer if there's a need and there's content
        if (remaining < desired && !this.hasReachedEndOfStream) {

            // reallocate the buffer when it's not big enough to hold the desired number of characters
            if (desired > this.buffer.length) {
                final var newBuffer = new char[desired];
                System.arraycopy(this.buffer, this.index, newBuffer, 0, remaining);
                this.buffer = newBuffer;
                this.index = 0;
                this.length = remaining;
            }

            // shuffle the characters down when there's not enough space for the desired characters
            if (this.index + desired > this.buffer.length) {
                System.arraycopy(this.buffer, this.index, this.buffer, 0, remaining);
                this.index = 0;
                this.length = remaining;
            }

            // top up the buffer
            try {
                final int size = super.read(this.buffer, this.length, this.buffer.length - this.length);

                if (size == -1) {
                    this.hasReachedEndOfStream = true;
                }
                else {
                    this.length += size;
                }
            }
            catch (final IOException e) {
                // we leave the buffer untouched upon exception
                this.hasReachedEndOfStream = true;
            }
        }

        return this.index < this.length;
    }

    /**
     * Attempts to fill the existing internal buffer with {@link Character}s for reading from the underlying
     * {@link Reader}.
     * <p>
     * Should an {@link IOException} occur when attempting read from the underlying {@link Reader}, the buffer will
     * remain untouched, but the {@link LookaheadReader} will assume an end-of-stream has occurred.
     *
     * @return {@code true} if the internal buffer has some content for reading, {@code false} when there is no content
     * available
     */
    private boolean prepare() {
        return prepare(1);
    }

    /**
     * Obtains the current {@link Location} in the {@link LookaheadReader}.
     *
     * @return the current {@link Location}
     */
    public Location getLocation() {
        return Location.of(this.line, this.column, this.offset);
    }

    /**
     * Determines if there is content available to be read from the {@link LookaheadReader}.
     *
     * @return {@code true} if there is content available to be read, {@code false} otherwise
     */
    public boolean available() {
        return prepare();
    }

    /**
     * Obtains the next character in the {@link LookaheadReader} without consuming it, returning {@code -1} if the
     * end of the stream has been reached or can no longer be read.
     *
     * @return the next character in the {@link LookaheadReader}, or {@code -1}
     */
    public int peek() {
        if (prepare()) {
            return this.buffer[this.index];
        }
        else {
            return -1;
        }
    }

    /**
     * Obtains a {@link CharSequence} representing the specified number of next characters in the
     * {@link LookaheadReader} without consuming said characters.  Should there not be enough characters remaining in
     * the {@link LookaheadReader}, those that are remaining are returned.  Should there be no characters remaining
     * in the {@link LookaheadReader}, or it can no longer be read, an empty {@link CharSequence} is returned.
     *
     * @param size the desired number of characters
     * @return a {@link String} with at most the desired number of characters
     */
    public CharSequence peek(final int size) {
        if (prepare(size)) {
            return new String(this.buffer, this.index, Math.min(size, this.length - this.index));
        }
        else {
            return "";
        }
    }

    /**
     * Obtains a {@link CharSequence} representing an entire buffer of characters available to the
     * {@link LookaheadReader}.  Should there not be enough characters remaining in the {@link LookaheadReader}, those
     * that are remaining are returned.  Should there be no characters remaining in the {@link LookaheadReader}, or it
     * can no longer be read, an empty {@link CharSequence} is returned.
     *
     * @return a {@link CharSequence}
     */
    public CharSequence peekMaximum() {
        if (prepare(this.buffer.length)) {
            return new String(this.buffer, this.index, this.length - this.index);
        }
        else {
            return "";
        }
    }

    /**
     * Determines if the specified {@link Predicate} is satisfied by the next character in the {@link LookaheadReader}.
     *
     * @param predicate the {@link Predicate}
     * @return {@code true} if the {@link Predicate} is satisfied, {@code false} otherwise
     */
    public boolean follows(final Predicate<? super Integer> predicate) {
        return predicate != null && prepare() && predicate.test(peek());
    }

    /**
     * Determines if the specified {@link String} occurs next in the {@link LookaheadReader},
     * using {@link String#equals(Object)} for comparison.
     *
     * @param string the {@link String}
     * @return {@code true} when the specified {@link String} occurs next, otherwise {@code false}
     */
    public boolean follows(final String string) {
        return string != null && peek(string.length()).equals(string);
    }

    /**
     * Consumes the next character in the {@link LookaheadReader}, returning {@code -1} if the
     * end of the stream has been reached, or it can no longer be read.
     *
     * @return the next character in the {@link LookaheadReader}, or {@code -1}
     */
    public int consume() {
        if (prepare()) {
            final int value = this.buffer[this.index++];

            if ((value == '\r' && peek() != '\n') || value == '\n') {
                this.line++;
                this.column = 1;
            }
            else {
                this.column++;
            }

            this.offset++;
            return value;
        }
        else {
            return -1;
        }
    }

    /**
     * Obtains a {@link String} containing the specified number of next available characters, including whitespace,
     * consumed from the {@link LookaheadReader}.
     * <p>
     * Should there not be enough characters remaining in the {@link LookaheadReader}, those that are remaining are
     * consumed and returned.  Should there be no characters remaining in the {@link LookaheadReader}, or it can no
     * longer be read, an empty {@link String} is returned.
     *
     * @param size the desired number of characters to consume
     * @return a {@link String} with at most the desired number of characters
     */
    public String consume(final int size) {

        // attempt to build a String with the specified number of characters
        final StringBuilder builder = new StringBuilder(size);

        int remaining = size;
        while (remaining > 0 && prepare()) {
            // determine how may characters we read from the buffer (this iteration)
            final int count = Math.min(remaining, this.length - this.index);

            // place the characters into the builder to post-process and later return
            builder.append(this.buffer, this.index, count);

            // consume the characters in the buffer
            this.index += count;

            // there's now less characters to consume
            remaining = remaining - count;
        }

        //

        // update the current location based on the content of the string
        for (int index = 0; index < builder.length(); index++) {
            final int value = builder.charAt(index);

            if ((value == '\r' && index + 1 < builder.length() && builder.charAt(index + 1) != '\n')
                || value == '\n') {
                this.line++;
                this.column = 1;
            }
            else {
                this.column++;
            }
        }

        this.offset += builder.length();
        return builder.toString();
    }

    /**
     * Skips the content in the {@link LookaheadReader} while the specified {@link Predicate} is {@code true}.
     *
     * @param predicate the {@link Predicate}
     */
    public void skipWhile(final Predicate<? super Character> predicate) {
        if (predicate != null) {
            while (available() && predicate.test((char) peek())) {
                consume();
            }
        }
    }

    @Override
    public int read()
        throws IOException {

        return consume();
    }

    @Override
    public int read(final char[] buffer, final int offset, final int length) {
        final String string = consume(length);

        // copy the consumed string into the buffer
        for (int i = 0; i < string.length(); i++) {
            buffer[offset + i] = string.charAt(i);
        }

        return string.isEmpty() && this.hasReachedEndOfStream ? -1 : string.length();
    }

    @Override
    public boolean ready()
        throws IOException {
        // the LookaheadReader is ready if there's content in the buffer or the FilterReader is ready
        return this.index < this.length || super.ready();
    }

    @Override
    public long skip(final long count) {
        prepare();
        return consume((int) count).length();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void mark(final int readAheadLimit)
        throws IOException {
        throw new IOException("mark(int) not supported");
    }

    @Override
    public void reset()
        throws IOException {
        throw new IOException("reset() not supported");
    }

    @Override
    public void close()
        throws IOException {
        super.close();
    }

    /**
     * Defines a location in the {@link LookaheadReader}.
     */
    public interface Location {

        /**
         * The starting {@link Location}.
         */
        Location START = Location.of(1, 1);

        /**
         * Obtains the line number, the first being 1.
         *
         * @return the line number
         */
        int getLine();

        /**
         * Obtains the column, the first being 1.
         *
         * @return the column
         */
        int getColumn();

        /**
         * Obtains the zero-based character offset from the start of the stream.
         *
         * @return the character offset
         */
        default long getOffset() {
            return 0;
        }

        /**
         * Creates a {@link Location} given the specified line, column, and character offset.
         *
         * @param line   the line
         * @param column the column
         * @param offset the zero-based character offset
         * @return a new {@link Location}
         */
        static Location of(final int line, final int column, final long offset) {
            return new Location() {
                @Override
                public int getLine() {
                    return line;
                }

                @Override
                public int getColumn() {
                    return column;
                }

                @Override
                public long getOffset() {
                    return offset;
                }

                @Override
                public int hashCode() {
                    return line * 13 + column * 7;
                }

                @Override
                public boolean equals(final Object object) {
                    if (object instanceof Location) {
                        final Location other = (Location) object;
                        return getLine() == other.getLine() && getColumn() == other.getColumn();
                    }

                    return false;
                }

                @Override
                public String toString() {
                    return "Location " + getLine() + ":" + getColumn();
                }
            };
        }

        /**
         * Creates a {@link Location} given the specified line and column positions.
         *
         * @param line   the line
         * @param column the column
         * @return a new {@link Location}
         */
        static Location of(final int line, final int column) {
            return of(line, column, 0);
        }
    }
}
