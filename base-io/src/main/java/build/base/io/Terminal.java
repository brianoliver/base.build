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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.CompletableFuture;

/**
 * Provides {@link Reader}s and {@link Writer}s for interacting with the {@code stdout}, {@code stderr} and
 * {@code stdin} of an external {@code Process}, {@code Container} or {@code Virtual Machine}.
 *
 * @author brian.oliver
 * @since Jul-2021
 */
public interface Terminal
    extends AutoCloseable {

    /**
     * Obtains a {@link Reader} to read output from the {@link Terminal}, typically from the output
     * sent to {@code stdout}.
     *
     * @return a {@link Reader}
     */
    Reader getOutputReader();

    /**
     * Obtains a {@link Reader} to read error output from the {@link Terminal}, typically from the output
     * sent to {@code stdout}.
     *
     * @return a {@link Reader}
     */
    Reader getErrorReader();

    /**
     * Obtains a {@link Writer} to write input to the {@link Terminal}, typically input to be consumed by {@code stdin}.
     *
     * @return a {@link Writer}
     */
    Writer getInputWriter();

    /**
     * Obtains a {@link CompletableFuture} indicating that the {@link Terminal} has been closed.
     *
     * @return a {@link CompletableFuture} completed when the {@link Terminal} is closed
     */
    CompletableFuture<?> onClosed();

    @Override
    void close();

    /**
     * Constructs a {@link Terminal} for the specified {@link InputStream} and {@link OutputStream}s.
     *
     * @param stdin  the {@link OutputStream} to write to Standard Input
     * @param stdout the {@link InputStream} to read from Standard Output
     * @param stderr the {@link InputStream} to read from Standard Error
     * @return a new {@link Terminal}
     */
    static Terminal of(final OutputStream stdin,
                       final InputStream stdout,
                       final InputStream stderr) {

        // establish Completable Streams for the Process
        final CompletableInputStream completableStdOut = new CompletableInputStream(stdout);
        final CompletableInputStream completableStdErr = new CompletableInputStream(stderr);

        // establish Readers and Writers for the Process
        final OutputStreamWriter stdinWriter = new OutputStreamWriter(new BufferedOutputStream(stdin));
        final InputStreamReader stdoutReader = new InputStreamReader(new BufferedInputStream(completableStdOut));
        final InputStreamReader stderrReader = new InputStreamReader(new BufferedInputStream(completableStdErr));

        // establish a CompletableFuture for the Terminal that is completed when the InputStreams are terminated
        final CompletableFuture<?> onClosed = CompletableFuture.allOf(
            completableStdOut.onTerminated(),
            completableStdErr.onTerminated());

        return new Terminal() {
            @Override
            public Reader getOutputReader() {
                return stdoutReader;
            }

            @Override
            public Reader getErrorReader() {
                return stderrReader;
            }

            @Override
            public Writer getInputWriter() {
                return stdinWriter;
            }

            @Override
            public CompletableFuture<?> onClosed() {
                return onClosed;
            }

            @Override
            public void close() {
                try {
                    stdinWriter.close();
                }
                catch (final IOException __) {
                    // we safely ignore exceptions on closing
                }

                try {
                    stdoutReader.close();
                }
                catch (final IOException __) {
                    // we safely ignore exceptions on closing
                }

                try {
                    stderrReader.close();
                }
                catch (final IOException __) {
                    // we safely ignore exceptions on closing
                }
            }
        };
    }

    /**
     * Constructs a {@link Terminal} for the specified {@link Process}.
     *
     * @param process a {@link Process}
     * @return a new {@link Terminal}
     */
    static Terminal of(final Process process) {
        return of(process.getOutputStream(), process.getInputStream(), process.getErrorStream());
    }
}
