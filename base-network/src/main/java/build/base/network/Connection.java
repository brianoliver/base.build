package build.base.network;

/*-
 * #%L
 * base.build Network
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

import build.base.flow.Producer;
import build.base.flow.Subscriber;
import build.base.flow.SubscriberRegistry;
import build.base.foundation.tuple.Pair;
import build.base.io.SerializableCallable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A bidirectional connection between a {@link Client} and a {@link Server}, capable of executing received
 * {@link SerializableCallable} requests and sending {@link SerializableCallable} responses.
 *
 * @author brian.oliver
 * @author lina.xu
 * @since Nov-2018
 */
public class Connection
    implements AutoCloseable {

    /**
     * The local identity, typically the pid, (this side) of the {@link Connection}.
     */
    private final long localIdentity;

    /**
     * The remote identity, typically the pid, (other side) of the {@link Connection}.
     */
    private final long remoteIdentity;

    /**
     * The {@link Socket} representing the {@link Connection}.
     */
    private final Socket socket;

    /**
     * The {@link DataInputStream} for reading responses from the {@link Connection}.
     */
    private final DataInputStream inputStream;

    /**
     * The {@link DataOutputStream} for writing requests to the {@link Connection}.
     */
    private final DataOutputStream outputStream;

    /**
     * The next request id to allocate for {@link SerializableCallable}s request sent by this {@link Connection}.
     */
    private final AtomicInteger nextRequestId;

    /**
     * The pending {@link CompletableFuture}s for {@link SerializableCallable}s request sent by this
     * {@link Connection}, organized by request id.
     */
    private final ConcurrentHashMap<Integer, CompletableFuture<?>> requestFutures;

    /**
     * A {@link CompletableFuture} that is completed when the {@link Connection} is started.
     */
    private final CompletableFuture<Connection> onStarted;

    /**
     * A {@link CompletableFuture} that is completed when the {@link Connection} is closed.
     */
    private final CompletableFuture<Connection> onClosed;

    /**
     * The currently registered {@link Producer}s by name and {@link Class} of item.
     */
    private final ConcurrentHashMap<Pair<String, Class<?>>, Producer<?>> producers;

    /**
     * The currently registered {@link SubscriberRegistry}s by name and {@link Class} of item.
     */
    private final ConcurrentHashMap<Pair<String, Class<?>>, SubscriberRegistry<?>> subscribers;

    /**
     * An empty response.
     */
    private static final byte[] EMPTY = new byte[0];

    /**
     * Constructs the {@link Connection}.
     *
     * @param localIdentity the local identity (this side) of the {@link Connection}
     * @param socket        the {@link Socket}
     * @throws IOException should establishing the {@link Connection} fail
     */
    public Connection(final long localIdentity, final Socket socket)
        throws IOException {

        this.localIdentity = localIdentity;
        this.socket = socket;

        this.onStarted = new CompletableFuture<>();
        this.onClosed = new CompletableFuture<>();

        this.inputStream = new DataInputStream(socket.getInputStream());
        this.outputStream = new DataOutputStream(socket.getOutputStream());

        this.nextRequestId = new AtomicInteger(1);
        this.requestFutures = new ConcurrentHashMap<>();

        this.producers = new ConcurrentHashMap<>();
        this.subscribers = new ConcurrentHashMap<>();

        // send the local identity
        this.outputStream.writeLong(this.localIdentity);

        // read the remote identity
        this.remoteIdentity = this.inputStream.readLong();
    }

    /**
     * Starts the {@link Connection}, allowing {@link SerializableCallable}s to be sent and received.
     * <p>
     * If the {@link Connection} was previously started, the request is ignored.
     *
     * @return the {@link Connection} once started
     */
    public synchronized Connection start() {

        if (!this.onStarted.isDone()) {
            // establish a Virtual Thread for reading requests from the connection
            Thread.ofVirtual()
                .name("Server.Connection[" + this.remoteIdentity + "]")
                .start(this::processRequests);

            // the connection has been started
            this.onStarted.complete(this);
        }

        return this;
    }

    /**
     * Attempts to create a {@link Producer} to publish items with the specified name and item {@link Class}.
     *
     * @param <T>       the type of the value
     * @param name      the name of the {@link Producer}
     * @param itemClass the {@link Class} of items that will be published
     * @return an {@link Optional} containing the new {@link Producer} or {@link Optional#empty()} when
     * {@link Producer} could not be created.
     */
    @SuppressWarnings("unchecked")
    public synchronized <T extends Serializable> Optional<Producer<T>> createProducer(final String name,
                                                                                      final Class<? extends T> itemClass) {

        // return an empty Optional if the connection is not ready or closed
        if (!onStarted().isDone() || onClosed().isDone()) {
            return Optional.empty();
        }

        // attempt to create an instance of the Producer if it doesn't exist
        final var pair = Pair.<String, Class<?>>of(name, itemClass);

        final var isOperational = new AtomicBoolean(true);

        // create the Producer if it doesn't exist, otherwise return the one that exists
        return Optional.of((Producer<T>) this.producers.computeIfAbsent(pair, __ -> new Producer<T>() {
            @Override
            public synchronized void publish(final T item) {
                if (item == null) {
                    // we're no longer operational
                    isOperational.set(false);
                    Connection.this.producers.remove(pair);
                }

                if (!onStarted().isDone() || onClosed().isDone() || !isOperational.get()) {
                    // no publishing will occur if the connection is not started or it's closed
                    return;
                }

                // establish a request id
                final var requestId = Connection.this.nextRequestId.getAndIncrement();

                // serialize the request
                final byte[] bytes;
                try (var byteOutputStream = new ByteArrayOutputStream();
                    var objectOutputStream = new ObjectOutputStream(byteOutputStream)) {

                    objectOutputStream.writeUTF(name);
                    objectOutputStream.writeUTF(itemClass.getName());
                    objectOutputStream.writeObject(item);

                    bytes = byteOutputStream.toByteArray();

                    // send the request
                    try {
                        writeToOutputStream(requestId, bytes, Operation.PUBLISH);
                    }
                    catch (final IOException e) {
                        // this is fatal, so close the connection
                        close();
                    }
                }
                catch (final IOException e) {
                    // this is not fatal
                }
            }

            @Override
            public void close() {
                // publish a null to indicate that we're no longer publishing
                publish(null);
            }
        }));
    }

    /**
     * Subscribes for items published by a {@link Producer} with the specified name and {@link Class} of items.
     *
     * @param <T>        the type of items
     * @param name       the name of the {@link Producer} to which to subscribe
     * @param itemClass  the {@link Class} of items for the {@link Producer}
     * @param subscriber the {@link Subscriber}
     */
    @SuppressWarnings("unchecked")
    public synchronized <T extends Serializable> void subscribe(final String name,
                                                                final Class<? extends T> itemClass,
                                                                final Subscriber<T> subscriber) {

        Objects.requireNonNull(name, "The Producer name to observe must not be null");
        Objects.requireNonNull(itemClass, "The Producer item class to observe must not be null");
        Objects.requireNonNull(subscriber, "The Observer must not be null");

        final var pair = Pair.<String, Class<?>>of(name, itemClass);
        this.subscribers.compute(pair, (_, existing) -> {
            final var registry = existing == null
                ? new SubscriberRegistry<T>()
                : (SubscriberRegistry<T>) existing;

            registry.subscribe(subscriber);

            return registry;
        });
    }

    /**
     * Process inbound requests from the {@link #inputStream}.
     */
    private void processRequests() {
        try {
            while (!this.onClosed.isDone()) {

                // read the request id
                final var requestId = this.inputStream.readInt();

                // determine the operation to perform
                final var operation = Operation.values()[this.inputStream.readInt()];

                // read the message length
                final var messageLength = this.inputStream.readInt();

                // read the expected message length into it's own buffer
                // (we use a separate buffer so we don't corrupt the stream if the message is corrupted)
                final var messageBytes = new byte[messageLength];

                int bytesRead = 0;
                int readLength;

                while (bytesRead < messageLength && (readLength = this.inputStream.read(messageBytes, bytesRead,
                    messageLength - bytesRead)) != -1) {
                    bytesRead += readLength;
                }

                if (bytesRead == messageLength) {
                    handleOperation(requestId, operation, messageBytes);
                }
                else {
                    // the entire message could not be read
                    // (because the stream was probably closed early)
                    this.onClosed.completeExceptionally(
                        new IOException("Failed to read expected number of bytes.  Expected "
                            + messageLength + " (bytes), read " + bytesRead + " (bytes)"));
                }
            }
        }
        catch (final IOException e) {
            // any IOException that is caught here is due to the connection closing
            // (there's no reason to log this)
        }

        // cancel all pending request CompletableFutures
        // (there will be no further results)
        this.requestFutures.forEach((id, future) -> future.cancel(true));

        // the connection has been closed
        this.onClosed.complete(this);
    }

    /**
     * Handle an inbound {@link Operation}.
     *
     * @param requestId    the request ID for the {@link Operation}
     * @param operation    the type of {@link Operation}
     * @param messageBytes the serialized message containing the {@link Operation}
     */
    @SuppressWarnings("unchecked")
    private void handleOperation(final int requestId, final Operation operation, final byte[] messageBytes) {
        try {
            final var objectInputStream = new ObjectInputStream(new ByteArrayInputStream(messageBytes));

            switch (operation) {
            case REQUEST:
                final var request = (SerializableCallable<?>) objectInputStream.readObject();

                // create a Virtual Thread to handle the request
                Thread.ofVirtual()
                    .name("Server.Request[" + requestId + "]")
                    .start(() -> handleRequest(requestId, request));

                break;

            case RESPONSE:
                final var response = (SerializableCallable<?>) objectInputStream.readObject();

                // obtain the CompletableFuture to complete with the response
                final var completableFuture = (CompletableFuture<Object>) this.requestFutures.remove(requestId);

                if (completableFuture == null) {
                    // unknown request responses are ignored
                    System.err.println("Missing request future (internal error)");
                }
                else {
                    try {
                        completableFuture.complete(response.call());
                    }
                    catch (final Exception e) {
                        completableFuture.completeExceptionally(e);
                    }
                }

                break;

            case PUBLISH:
                // read the name of the Producer
                final var name = objectInputStream.readUTF();

                // read the Class name of the Producer
                final var itemClassName = objectInputStream.readUTF();

                // read the item to publish
                final var item = objectInputStream.readObject();

                final var itemClass = Class.forName(itemClassName);
                final var pair = Pair.<String, Class<?>>of(name, itemClass);

                // attempt to obtain the SubscriberRegistry
                final var registry = (SubscriberRegistry<Object>) this.subscribers.get(pair);

                if (registry != null) {
                    if (item == null) {
                        this.subscribers.remove(pair);
                        registry.complete();
                    }
                    else {
                        registry.publish(item);
                    }
                }

                break;

            default:
                // unknown object types are ignored
            }
        }
        catch (final IOException | ClassNotFoundException e) {
            // de-serialization failure or the lack of a class for a request/response isn't fatal
            e.printStackTrace(System.err);
        }
    }

    /**
     * Handle an inbound {@link Operation#REQUEST request}. The request is executed and a response
     * is generated and sent via {@link #outputStream}.
     *
     * @param requestId the ID of the request
     * @param request   the request
     */
    private void handleRequest(final int requestId, final SerializableCallable<?> request) {
        // execute the request, establish a response to return
        SerializableCallable<?> response;
        try {
            final Serializable result = request.call();
            response = () -> result;
        }
        catch (final Throwable t) {
            response = () -> {
                throw t;
            };
        }

        // serialize the response to send back
        byte[] bytes;

        try {
            bytes = serialize(response);
        }
        catch (final IOException e) {
            try {
                bytes = serialize(e);
            }
            catch (final IOException fatal) {
                bytes = EMPTY;
            }
        }

        // send the response for the request
        try {
            writeToOutputStream(requestId, bytes, Operation.RESPONSE);
        }
        catch (final IOException e) {
            // this is fatal for the connection as the stream may now be corrupted
            this.onClosed.completeExceptionally(e);
        }
    }

    /**
     * Serializes the specified {@link Serializable} into a byte array.
     *
     * @param serializable the {@link Serializable}
     * @return the non-null byte array
     * @throws IOException should serialization fail
     */
    private byte[] serialize(final Serializable serializable)
        throws IOException {

        // create a buffer containing the response
        try (var byteOutputStream = new ByteArrayOutputStream();
            var objectOutputStream = new ObjectOutputStream(byteOutputStream)) {

            objectOutputStream.writeObject(serializable);

            return byteOutputStream.toByteArray();
        }
    }

    @Override
    public synchronized void close() {
        if (!this.onClosed.isDone()) {
            // drop the Producers
            this.producers.clear();

            // notify Subscribers that we're no longer processing events
            this.subscribers.forEach((_, registry) -> registry.complete());

            // close the socket
            // (this also closes the underlying input and output streams)
            try {
                this.socket.close();
            }
            catch (final IOException e) {
                // we don't care about the exception as when closing
            }
        }
    }

    /**
     * Obtains the local identity of the {@link Connection}.
     *
     * @return the local identity
     */
    public long getLocalIdentity() {
        return this.localIdentity;
    }

    /**
     * Obtains the remote identity of the {@link Connection}.
     *
     * @return the remote identity
     */
    public long getRemoteIdentity() {
        return this.remoteIdentity;
    }

    /**
     * Obtains a {@link CompletableFuture} that is completed when the {@link Connection} has successfully been started
     * and is ready to accept {@link #submit(SerializableCallable)}ed requests.
     *
     * @return a {@link CompletableFuture}
     */
    public CompletableFuture<Connection> onStarted() {
        return this.onStarted;
    }

    /**
     * Obtains a {@link CompletableFuture} that is completed when the {@link Connection} is closed and will no longer
     * accept {@link #submit(SerializableCallable)}ed requests.
     *
     * @return a {@link CompletableFuture}
     */
    public CompletableFuture<Connection> onClosed() {
        return this.onClosed;
    }

    /**
     * Submits the specified {@link SerializableCallable} for execution on the other end of the {@link Connection}.
     *
     * @param <T>      the type of result of the {@link SerializableCallable}
     * @param callable the {@link SerializableCallable}
     * @return a {@link CompletableFuture} for the result of the {@link SerializableCallable}
     */
    public synchronized <T extends Serializable> CompletableFuture<T> submit(final SerializableCallable<T> callable) {

        if (callable == null) {
            // null requests as completed immediately with null
            return CompletableFuture.completedFuture(null);
        }

        // establish a CompletableFuture for the result
        final var completableFuture = new CompletableFuture<T>();

        if (onClosed().isDone()) {
            // when closed, complete with cancellation
            completableFuture.cancel(true);
            return completableFuture;
        }

        // serialize and send the request

        // establish a request id
        final var requestId = this.nextRequestId.getAndIncrement();
        this.requestFutures.put(requestId, completableFuture);

        final byte[] bytes;
        try {
            // create a buffer containing the request
            bytes = serialize(callable);
        }
        catch (final IOException e) {
            // this is not fatal
            completableFuture.completeExceptionally(e);
            return completableFuture;
        }

        try {
            writeToOutputStream(requestId, bytes, Operation.REQUEST);
        }
        catch (final IOException e) {
            // this is fatal, so close the connection
            close();

            completableFuture.completeExceptionally(e);
        }

        return completableFuture;
    }

    /**
     * Write a request to the {@link #outputStream}.
     *
     * @param requestId the ID of the request
     * @param bytes     the request data
     * @param operation the type of operation
     * @throws IOException if an exception occurs
     */
    private synchronized void writeToOutputStream(final int requestId, final byte[] bytes,
                                                  final Operation operation)
        throws IOException {

        this.outputStream.writeInt(requestId);
        this.outputStream.writeInt(operation.ordinal());
        this.outputStream.writeInt(bytes.length);
        this.outputStream.write(bytes);
        this.outputStream.flush();
    }

    /**
     * Defines the {@link Operation} to be performed by a {@link Connection}.
     */
    private enum Operation {
        /**
         * Execute a {@link SerializableCallable} request, returning the result with a
         * {@link SerializableCallable} {@link #RESPONSE}.
         */
        REQUEST,

        /**
         * Evaluate a {@link SerializableCallable} response, using the produced value or {@link Throwable} to complete
         * a previously created {@link CompletableFuture}.
         */
        RESPONSE,

        /**
         * Publishes a named and strongly typed item on a {@link Connection}.
         */
        PUBLISH;
    }
}
