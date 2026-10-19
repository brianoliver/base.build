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

import build.base.flow.Publicist;
import build.base.flow.Publisher;
import build.base.flow.Subscriber;
import build.base.flow.SubscriberRegistry;
import build.base.flow.Subscription;
import build.base.foundation.AtomicEnum;
import build.base.foundation.CompletableFutures;
import build.base.foundation.Lazy;
import build.base.foundation.Strings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Asynchronously pipes a stream of characters read from a {@link Reader}, one line at a time, to a {@link Writer}
 * using a <i>Virtual Thread</i>, until either:
 * <ol>
 *     <li>the end of the stream of input characters is reached,</li>
 *     <li>either {@link Reader} or {@link Writer} is closed, or</li>
 *     <li>termination is requested.</li>
 * </ol>
 * <p>
 * Unlike {@link java.io.PipedReader}s and {@link java.io.PipedWriter}s, {@link Pipe}s additionally allow:
 * <ol>
 *     <li>transformation of lines read from a {@link Reader} prior to writing them to a {@link Writer}, and</li>
 *     <li>publishing of the <i>un-transformed</i> lines to {@link Subscriber}s</li>
 * </ol>
 * <p>
 * {@link Pipe}s are intended to be used once and only once for a given {@link Reader}.  Once {@link #open()}ed, a
 * {@link Pipe} may not be opened again.  Attempting to reconfigure or subscribe to an opened {@link Pipe} will be
 * ignored.
 *
 * @author brian.oliver
 * @since Jul-2021
 */
public class Pipe
    implements AutoCloseable {

    /**
     * The {@link Reader} from which to read the character stream.
     */
    private final Reader reader;

    /**
     * The {@link Writer} to which characters will be written.
     */
    private final Writer writer;

    /**
     * The {@link Publicist}s for the {@link Pipe} subscribers
     */
    private final Publicist<String> publicist;

    /**
     * The {@link State} of the {@link Pipe}.
     */
    private enum State {
        /**
         * The {@link Pipe} is new and hasn't been opened.
         */
        NEW,

        /**
         * The {@link Pipe} is open and is actively reading and writing characters.
         */
        OPEN,

        /**
         * The {@link Pipe} has been closed.
         */
        CLOSED
    }

    /**
     * The optional ({@code null}able) name of the stream being piped (for diagnostic purposes).
     */
    private String name;

    /**
     * An optional ({@code null}able) {@link Function} allowing transformation of a line of characters prior to being written.
     */
    private Function<String, String> transformer;

    /**
     * The {@link Duration} used for {@link Thread#join(long) joining} the {@link Thread} in {@link #close()}.
     */
    private Duration timeout;

    /**
     * The {@link State} of the {@link Pipe}.
     */
    private final AtomicEnum<State> state;

    /**
     * The {@link Lazy}ily initialized {@link Thread}.
     */
    private final Lazy<Thread> lazyThread;

    /**
     * Constructs a {@link Pipe}.
     *
     * @param reader the {@link Reader}
     * @param writer the {@link Writer}
     */
    public Pipe(final Reader reader,
                final Writer writer) {

        Objects.requireNonNull(reader, "The reader must not be null");
        Objects.requireNonNull(writer, "The writer must not be null");

        this.reader = reader;
        this.writer = writer;
        this.publicist = new SubscriberRegistry<>();

        this.name = "(unknown)";
        this.transformer = null;
        this.timeout = Duration.ofSeconds(60);

        this.state = AtomicEnum.of(State.NEW);
        this.lazyThread = Lazy.empty();
    }

    /**
     * Attempts to subscribe the specified {@link Subscriber} if possible, when publication has not already commenced.
     * If already subscribed, or the attempt to subscribe fails due to policy violations or errors, the
     * {@link Subscriber}'s {@link Subscriber#onError(Throwable)} method is invoked with an
     * {@link IllegalStateException}.  Otherwise, the {@link Subscriber}'s {@link Subscriber#onSubscribe} method is
     * invoked with a new {@link Subscription}.  {@link Subscriber}s enable receiving items by invoking the
     * {@link Subscription#request(long)} method of their {@link Subscription}, and may unsubscribe from receiving items
     * invoking {@link Subscription#cancel()}.
     *
     * @param subscriber the subscriber
     */
    public Pipe subscribe(final Subscriber<? super String> subscriber) {
        Objects.requireNonNull(subscriber, "The subscriber must not be null");

        this.state.consumeIf(State.NEW,
            _ -> this.publicist.subscribe(subscriber),
            _ -> subscriber.onError(
                new IllegalStateException("Subscription to the Pipe is not permitted as it is :" + this.state)));

        return this;
    }

    /**
     * Obtains a {@link Publisher} for the {@link Pipe}.
     *
     * @return a {@link Publisher}
     */
    public Publisher<String> publisher() {
        return Pipe.this::subscribe;
    }

    /**
     * Sets the name of the {@link Pipe}.
     *
     * @param name the name
     * @return the {@link Pipe} to allow fluent-style method invocation
     */
    public Pipe setName(final String name) {
        this.state.consumeIf(State.NEW, __ -> this.name = Strings.isEmpty(name) ? "(unknown)" : name);

        return this;
    }

    /**
     * Sets the timeout {@link Duration} for closing the {@link Pipe}.
     *
     * @param timeout the {@link Duration}
     * @return the {@link Pipe} to allow fluent-style method invocation
     */
    public Pipe setTimeout(final Duration timeout) {
        Objects.requireNonNull(timeout, "The timeout must not be null");

        this.state.consumeIf(State.NEW, __ -> this.timeout = timeout);

        return this;
    }

    /**
     * Set the {@link Function} to transform lines read from the {@link Reader}, before them being written to the
     * {@link Writer} and published to {@link Subscriber}s.
     *
     * @param transformer the {@link Function}
     * @return the {@link Pipe} to allow fluent-style method invocation
     */
    public Pipe setTransformer(final Function<String, String> transformer) {
        this.state.consumeIf(State.NEW, __ -> this.transformer = transformer == null ? line -> line : transformer);

        return this;
    }

    /**
     * Opens the {@link Pipe} to asynchronously read from the configured {@link Reader} and subsequently write to the
     * configured {@link Writer}.
     * <p>
     * Upon opening the {@link Pipe}, a Virtual {@link Thread} will be created.
     * <p>
     * To cancel asynchronous piping at any time, simply invoke {@link CompletableFuture#cancel(boolean)} on the
     * returned {@link CompletableFuture}.
     *
     * @return a {@link CompletableFuture} indicating when the piping has completed or failed
     */
    public CompletableFuture<?> open() {

        // establish the name and transformer
        final var name = Strings.isEmpty(this.name) ? "(unknown)" : this.name;
        final Function<String, String> transformer = this.transformer == null ? line -> line : this.transformer;

        // only allow opening if it's not already open
        if (this.state.compareAndSet(State.NEW, State.OPEN)) {
            // establish a CompletableFuture to return allowing notification and external control of piping
            final CompletableFuture<Void> future = new CompletableFuture<>();

            // establish a Runnable to perform the piping
            final Runnable runnable = () -> {
                // retain the started thread
                this.lazyThread.set(Thread.currentThread());

                // establish buffered readers and writers for piping
                // (we don't do this in the try-with-resources as that will close the local reader/writer
                // which will subsequently close the underlying reader/writers, which may corrupt those that
                // are passed in! - for example we don't want System.out closed if we're piping to it!)
                final BufferedReader reader = new BufferedReader(Pipe.this.reader);
                final PrintWriter writer;

                if (Pipe.this.writer instanceof PrintWriter) {
                    writer = (PrintWriter) Pipe.this.writer;
                }
                else {
                    writer = new PrintWriter(new BufferedWriter(Pipe.this.writer));
                }

                try {
                    String line;
                    while (!future.isDone() && (line = reader.readLine()) != null) {

                        // notify the subscribers of the line (without transformation)
                        Pipe.this.publicist.publish(line);

                        // transform the line prior to writing
                        line = transformer.apply(line);

                        // write the line
                        writer.println(line);
                        writer.flush();

                        //TODO: LOG.trace("Pipe [{}]: {}", name, line);
                    }

                    // notify the subscribers that piping is complete
                    Pipe.this.publicist.complete();

                    //TODO: LOG.debug("Pipe [{}]: (terminated normally)", name);
                }
                catch (final Exception e) {
                    // notify the subscribers of the exception
                    Pipe.this.publicist.error(e);

                    // any exception occurring reading or writing automatically terminates piping
                    //TODO: LOG.debug("Pipe [{}]: (terminated abnormally)", name, e);
                }
                finally {
                    // the piping has been terminated
                    future.complete(null);

                    // the Pipe is now closed
                    Pipe.this.state.set(State.CLOSED);

                    // ensure all output is written
                    writer.flush();
                }
            };

            // establish a Thread to perform the asynchronous piping
            final Thread thread = Thread.ofVirtual()
                .name(name)
                .start(runnable);

            // we'll attempt to interrupt the Thread when we're cancelled
            future.whenComplete((_, _) -> thread.interrupt());

            return future;
        }
        else {
            return CompletableFutures.completeExceptionally(
                new IllegalStateException("Attempted to open a Pipe that was previously opened"));
        }
    }

    @Override
    public void close() {
        // attempt to close iff we're not already closed
        if (this.state.set(State.CLOSED) != State.CLOSED) {
            try {
                // attempt to close the reader
                try {
                    this.reader.close();
                }
                catch (final IOException e) {
                    //TODO: LOG.warn("Pipe [{}]: (failed to close reader)", this.name, e);
                }

                // attempt to wait for the piping thread to terminate
                this.lazyThread.get().join(this.timeout.toMillis());

                if (this.lazyThread.get().isAlive()) {
                    //TODO: LOG.warn("Pipe [{}]: (thread still alive after join)", this.name);
                }
                else {
                    //TODO: LOG.debug("Pipe [{}]: (closed normally)", this.name);
                }
            }
            catch (final InterruptedException e) {
                //TODO: LOG.debug("Pipe [{}]: (closed abnormally)", this.name, e);

                Thread.currentThread().interrupt();
            }
        }
    }
}
