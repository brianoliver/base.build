package build.base.io;

/*-
 * #%L
 * base.build IO
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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A non-thread-safe, un-synchronized and lock-free alternative to the {@link java.io.BufferedInputStream}
 * provided by the Java Platform.
 * <p>
 * <strong>WARNING:</strong> This implementation is only suitable for use by a single-thread.
 *
 * @author brian.oliver
 * @since Sep-2024
 */
public final class NonThreadSafeBufferedInputStream
    extends FilterInputStream {

    /**
     * The default buffer size is 8k.
     */
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * The initial size for the buffer.
     */
    private final int initialSize;

    /**
     * The currently held bytes read from the underlying {@link InputStream}.  When {@code null} the
     * {@link InputStream} is considered closed.
     */
    private final byte[] buffer;

    /**
     * The number of bytes in the buffer that may be read.
     */
    private int size;

    /**
     * The index position in the buffer indicating the next byte to read,
     * in the range {@code 0} to {@code size}.
     */
    private int position;

    /**
     * Constructs a {@link NonThreadSafeBufferedInputStream} for the specified
     * {@link InputStream} with the default buffer size.
     *
     * @param inputStream the underlying input stream
     */
    public NonThreadSafeBufferedInputStream(final InputStream inputStream) {
        this(inputStream, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Constructs a {@link NonThreadSafeBufferedInputStream} for the specified
     * {@link InputStream} with the provided buffer size.
     *
     * @param inputStream the underlying input stream
     * @param bufferSize  the buffer size
     */
    public NonThreadSafeBufferedInputStream(final InputStream inputStream,
                                            final int bufferSize) {
        super(inputStream);

        this.initialSize = bufferSize <= 0 ? DEFAULT_BUFFER_SIZE : bufferSize;
        this.buffer = new byte[initialSize];
    }

    /**
     * Attempts to obtain the underlying {@link InputStream}, checking first if the {@link InputStream} is closed.
     *
     * @throws IOException if the {@link InputStream} is closed
     */
    private InputStream getInputStream()
        throws IOException {

        final var input = super.in;

        if (input == null) {
            throw new IOException("InputStream closed");
        }

        return input;
    }

    /**
     * Attempts to obtain the internal buffer, checking first if the {@link InputStream} is closed.
     *
     * @throws IOException if the {@link InputStream} is closed
     */
    private byte[] getBuffer()
        throws IOException {

        final var buffer = this.buffer;

        if (buffer == null) {
            throw new IOException("InputStream closed");
        }

        return buffer;
    }

    /**
     * Ensures the {@link InputStream} is open.
     *
     * @throws IOException if the {@link InputStream} is closed
     */
    private void ensureOpen()
        throws IOException {

        if (this.buffer == null) {
            throw new IOException("InputStream closed");
        }
    }

    /**
     * Attempts to fill the {@link #buffer} from the underlying {@link InputStream}.
     *
     * @throws IOException if the {@link InputStream} is closed
     */
    private void fill()
        throws IOException {

        // obtain the buffer to fill
        final var buffer = getBuffer();

        // reset the position to the start of the buffer
        this.position = 0;

        // attempt to fill the buffer
        final var readSize = getInputStream().read(buffer, this.position, buffer.length);

        // adjust the size based on the amount read
        this.size = Math.max(readSize, 0);
    }

    @Override
    public int read()
        throws IOException {

        if (this.position >= this.size) {
            fill();

            if (this.position >= this.size) {
                return -1;
            }
        }

        return getBuffer()[this.position++] & 0xff;
    }

    @Override
    public int read(final byte[] target,
                    final int offset,
                    final int length)
        throws IOException {

        ensureOpen();

        if ((offset | length | (offset + length) | (target.length - (offset + length))) < 0) {
            throw new IndexOutOfBoundsException();
        }
        else if (length == 0) {
            return 0;
        }

        int bytesRead = 0;

        while (true) {
            final var count = readPortion(target, offset + bytesRead, length - bytesRead);

            if (count <= 0) {
                return (bytesRead == 0) ? count : bytesRead;
            }

            bytesRead += count;

            if (bytesRead >= length) {
                return bytesRead;
            }

            final var input = super.in;
            if (input != null && input.available() <= 0) {
                return bytesRead;
            }
        }
    }

    /**
     * Read a portion into the specified target.
     *
     * @param target the target array
     * @param offset the offset into which to read
     * @param length the length to read
     */
    private int readPortion(final byte[] target,
                            final int offset,
                            final int length)
        throws IOException {

        int availableInBuffer = this.size - this.position;

        if (availableInBuffer <= 0) {
            final var size = Math.max(getBuffer().length, this.initialSize);

            // should the requested length be greater than the buffer (which is currently empty)
            // don't bother copying it into the buffer first
            if (length >= size) {
                return getInputStream().read(target, offset, length);
            }

            fill();

            availableInBuffer = this.size - this.position;

            if (availableInBuffer <= 0) {
                return -1;
            }
        }

        final var count = Math.min(availableInBuffer, length);
        System.arraycopy(getBuffer(), this.position, target, offset, count);

        this.position += count;

        return count;
    }

    @Override
    public long skip(final long count)
        throws IOException {

        ensureOpen();

        if (count <= 0) {
            return 0;
        }

        final var available = this.size - this.position;

        if (available <= 0) {
            return getInputStream().skip(count);
        }

        final var skipped = Math.min(available, count);
        this.position += (int) skipped;

        return skipped;
    }

    @Override
    public int available()
        throws IOException {

        final var availableInBuffer = this.size - this.position;
        final var availableInStream = getInputStream().available();

        return availableInBuffer > (Integer.MAX_VALUE - availableInStream)
            ? Integer.MAX_VALUE
            : availableInBuffer + availableInStream;
    }

    @Override
    public void close()
        throws IOException {

        if (super.in != null) {
            super.in.close();
            super.in = null;
        }
    }
}

