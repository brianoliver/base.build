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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * An {@link InputStream} adapter that completes a {@link CompletableFuture} when input has been exhausted, closed or
 * an exception occurs during reading.
 *
 * @author brian.oliver
 * @since Jul-2021
 */
public class CompletableInputStream
    extends InputStream {

    /**
     * The adapted {@link InputStream}.
     */
    private final InputStream stream;

    /**
     * The {@link CompletableFuture} for the {@link InputStream}.
     */
    private final CompletableFuture<InputStream> completableFuture;

    /**
     * Constructs a {@link CompletableFuture}.
     *
     * @param stream the {@link InputStream} to adapt
     */
    public CompletableInputStream(final InputStream stream) {
        this.stream = Objects.requireNonNull(stream, "The InputStream must not be null");
        this.completableFuture = new CompletableFuture<>();
    }

    /**
     * Obtains the {@link CompletableFuture} indicating when the {@link InputStream} has been terminated.
     *
     * @return the {@link CompletableFuture} for the {@link InputStream}
     */
    public CompletableFuture<InputStream> onTerminated() {
        return this.completableFuture;
    }

    /**
     * Handles when an {@link IOException} occurs during reading from the underlying {@link InputStream}.
     * <p>
     * Should the {@link IOException} be due to the {@link InputStream} being closed, the {@link #onTerminated()}
     * {@link CompletableFuture} is completed normally.  Otherwise, the {@link CompletableFuture} is completed
     * exceptionally with the {@link IOException}, and the exception is re-thrown.
     *
     * @param e the {@link IOException}
     * @throws IOException should the {@link IOException}
     */
    private void handle(final IOException e)
        throws IOException {

        if (e != null && !e.getMessage().contains("closed")) {
            this.completableFuture.completeExceptionally(e);
            throw e;
        }

        this.completableFuture.complete(this);
    }

    @Override
    public int read(final byte[] b)
        throws IOException {

        try {
            final int count = this.stream.read(b);
            if (count == -1) {
                this.completableFuture.complete(this);
            }
            return count;
        }
        catch (final IOException e) {
            handle(e);
            return -1;
        }
    }

    @Override
    public int read(final byte[] b, final int off, final int len)
        throws IOException {

        try {
            final int count = this.stream.read(b, off, len);
            if (count == -1) {
                this.completableFuture.complete(this);
            }
            return count;
        }
        catch (final IOException e) {
            handle(e);
            return -1;
        }
    }

    @Override
    public long skip(final long n)
        throws IOException {

        try {
            return this.stream.skip(n);
        }
        catch (final IOException e) {
            handle(e);
            return 0;
        }
    }

    @Override
    public int available()
        throws IOException {

        try {
            return this.stream.available();
        }
        catch (final IOException e) {
            this.completableFuture.completeExceptionally(e);
            throw e;
        }
    }

    @Override
    public void close()
        throws IOException {

        try {
            this.stream.close();
            this.completableFuture.complete(this);
        }
        catch (final IOException e) {
            this.completableFuture.completeExceptionally(e);
            throw e;
        }

    }

    @Override
    public synchronized void mark(final int readlimit) {
        this.stream.mark(readlimit);
    }

    @Override
    public synchronized void reset()
        throws IOException {

        try {
            this.stream.reset();
        }
        catch (final IOException e) {
            this.completableFuture.completeExceptionally(e);
            throw e;
        }
    }

    @Override
    public boolean markSupported() {
        return this.stream.markSupported();
    }

    @Override
    public int read()
        throws IOException {

        try {
            final int result = this.stream.read();
            if (result == -1) {
                this.completableFuture.complete(this);
            }
            return result;
        }
        catch (final IOException e) {
            handle(e);
            return -1;
        }
    }
}
