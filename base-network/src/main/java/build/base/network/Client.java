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
import build.base.flow.Publisher;
import build.base.flow.Subscriber;
import build.base.io.SerializableCallable;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * A client that will connect to a {@link Server}, allowing the sending, receiving and processing of
 * {@link SerializableCallable}s.
 *
 * @author brian.oliver
 * @author lina.xu
 * @since Nov-2018
 */
public class Client
    implements AutoCloseable {

    /**
     * The {@link Connection} to the {@link Server}.
     */
    private final Connection connection;

    /**
     * Constructs a {@link Client} with the specified identity to the {@link Server} operating
     * on the provided {@link URI}.
     *
     * @param identity the {@link Client} identity
     * @param uri      the {@link URI}
     * @throws IOException should creating a {@link Client} fail
     */
    public Client(final int identity, final URI uri)
        throws IOException {

        Objects.requireNonNull(uri, "The URI of the Server must not be null");

        // establish socket connection
        final var socket = new Socket(uri.getHost(), uri.getPort());
        socket.setKeepAlive(true);
        socket.setTcpNoDelay(true);

        // establish the connection
        this.connection = new Connection(identity, socket);
        this.connection.start();
    }

    @Override
    public void close() {
        this.connection.close();
    }

    /**
     * Obtains a {@link CompletableFuture} that is completed when the {@link Client} has been started and is ready
     * to accept {@link #submit(SerializableCallable)}d requests.
     *
     * @return a {@link CompletableFuture}
     */
    public CompletableFuture<Client> onStarted() {
        return this.connection.onStarted().thenApply(c -> this);
    }

    /**
     * Obtains a {@link CompletableFuture} that is completed when the {@link Client} has is no longer able to
     * accept {@link #submit(SerializableCallable)}d requests.  This {@link CompletableFuture} is completed
     * automatically upon {@link Client} {@link Connection} failure with the appropriate {@link Throwable} and when
     * the {@link Client} is {@link #close()}d.
     *
     * @return a {@link CompletableFuture}
     */
    public CompletableFuture<Client> onStopped() {
        return this.connection.onClosed().handle((c, t) -> this);
    }

    /**
     * Submits the specified {@link SerializableCallable} for execution on the other end of the {@link Connection}.
     *
     * @param <T>      the type of result of the {@link SerializableCallable}
     * @param callable the {@link SerializableCallable}
     * @return a {@link CompletableFuture} for the result of the {@link SerializableCallable}
     */
    public synchronized <T extends Serializable> CompletableFuture<T> submit(final SerializableCallable<T> callable) {
        return onStarted().thenCompose(_ -> this.connection.submit(callable));
    }

    /**
     * Attempts to create a {@link Producer} with the specified name and item {@link Class} for the {@link Client}.
     *
     * @param <T>       the type of the value
     * @param name      the name of the {@link Producer}
     * @param itemClass the {@link Class} of items that will be published
     * @return an {@link Optional} containing the new {@link Producer} or {@link Optional#empty()}
     * when {@link Producer} could not be created
     */
    public synchronized <T extends Serializable> Optional<Producer<T>> createProducer(final String name,
                                                                                      final Class<? extends T> itemClass) {

        return this.connection.createProducer(name, itemClass);
    }

    /**
     * Subscribes for items published by a {@link Publisher} with the specified name and {@link Class} of items.
     *
     * @param <T>        the type of items
     * @param name       the name of the {@link Publisher} to which to subscribe
     * @param itemClass  the {@link Class} of items for the {@link Publisher}
     * @param subscriber the {@link Subscriber}
     */
    public synchronized <T extends Serializable> void subscribe(final String name,
                                                                final Class<? extends T> itemClass,
                                                                final Subscriber<T> subscriber) {

        this.connection.subscribe(name, itemClass, subscriber);
    }
}
