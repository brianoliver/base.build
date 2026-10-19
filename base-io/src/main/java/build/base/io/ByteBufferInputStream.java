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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * An {@link InputStream} based on a {@link ByteBuffer}.
 *
 * @author brian.oliver
 * @since Aug-2024
 */
public class ByteBufferInputStream
    extends InputStream {

    /**
     * The underlying {@link ByteBuffer}.
     */
    private final ByteBuffer byteBuffer;

    /**
     * Constructs a {@link ByteBufferInputStream} for the specified {@link ByteBuffer}.
     *
     * @param byteBuffer the {@link ByteBuffer}
     */
    public ByteBufferInputStream(final ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    @Override
    public int available() {
        return this.byteBuffer.remaining();
    }

    @Override
    public int read()
        throws IOException {

        return this.byteBuffer.hasRemaining()
            ? (this.byteBuffer.get() & 0xFF)
            : -1;
    }

    @Override
    public int read(final byte[] bytes, final int off, final int length)
        throws IOException {

        if (!this.byteBuffer.hasRemaining()) {
            return -1;
        }

        final var size = Math.min(length, this.byteBuffer.remaining());
        this.byteBuffer.get(bytes, off, size);
        return size;
    }
}
