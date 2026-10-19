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

import build.base.foundation.Closeables;
import build.base.io.SerializableCallable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * A server to which {@link Client}s may connect, allowing the sending, receiving and processing of
 * {@link build.base.io.SerializableCallable}s.
 *
 * @author brian.oliver
 * @since Nov-2018
 */
public class Server
    implements AutoCloseable {

    /**
     * The identity of the {@link Server}.
     */
    private final int identity;

    /**
     * The {@link ServerSocket} to which {@link Client}s will connect.
     */
    private final ServerSocket serverSocket;

    /**
     * The {@link Connection}s to the {@link Server}, organized by remote identity.
     */
    private final ConcurrentHashMap<Long, CompletableFuture<Connection>> connections;

    /**
     * A {@link CompletableFuture} that is completed when the {@link Server} is started.
     */
    private final CompletableFuture<Server> onStarted;

    /**
     * A {@link CompletableFuture} that is completed when the {@link Server} is stopped.
     */
    private final CompletableFuture<Server> onStopped;

    /**
     * Constructs a {@link Server}.
     *
     * @param identity the identity of the {@link Server}
     * @throws IOException when the {@link Server} could not be established
     */
    public Server(final int identity)
        throws IOException {

        this.identity = identity;
        this.connections = new ConcurrentHashMap<>();

        this.onStarted = new CompletableFuture<>();
        this.onStopped = new CompletableFuture<>();

        // establish the ServerSocket
        this.serverSocket = new ServerSocket(0);
    }

    /**
     * Starts the {@link Server}, allowing {@link Client}s to connect to the {@link Server}.
     * <p>
     * If the {@link Server} was previously started, the request is ignored.
     *
     * @return the {@link Server} once started
     */
    public synchronized Server start() {
        if (this.onStarted.isDone()) {
            return this;
        }

        // establish a Thread to accept connections
        final Thread acceptThread = new Thread(() -> {
            try {
                // signal that the server is started
                this.onStarted.complete(this);

                while (!this.onStopped.isDone()) {
                    // wait for a client to connect
                    final Socket socket = this.serverSocket.accept();

                    // establish a connection to send/receive requests to/from the client
                    final Connection connection = new Connection(this.identity, socket);
                    connection.start();

                    this.connections.compute(connection.getRemoteIdentity(), (remoteIdentity, future) -> {
                        if (future == null) {
                            return CompletableFuture.completedFuture(connection);
                        }
                        else if (future.complete(connection)) {
                            return future;
                        }
                        // the future is already completed
                        // (which means there's an existing connection or it is was already completed)

                        try {
                            if (future.get().onClosed().isDone()) {
                                // the existing connection is closed, so we can replace it with the new connection
                                return CompletableFuture.completedFuture(connection);
                            }
                            else {
                                // the existing connection is still in use, so close the new connection
                                connection.close();

                                return future;
                            }
                        }
                        catch (final InterruptedException ignored) {
                            Thread.interrupted();
                            return future;
                        }
                        catch (final ExecutionException ignored) {
                            return future;
                        }
                    });

                    // when the Connection is closed, clean up the connection
                    connection.onClosed().whenComplete((c, t) -> this.connections.remove(c.getRemoteIdentity()));
                }
            }
            catch (final IOException e) {
                // attempt to signal that the server is stopped
                // (it may have been stopped externally, in which case the exception is expected)
                if (this.onStopped.completeExceptionally(e)) {
                    System.out.println("[build.base.network] Failed to accept a connection");
                    e.printStackTrace();
                }
            }
            finally {
                // close all of the Connections
                this.connections.values().stream().
                    forEach(future -> {
                        // attempt to cancel the completion
                        // (this will cause the connection to automatically be closed post creation)
                        future.cancel(true);

                        // or should the connection is available, close it now
                        future.whenComplete((connection, throwable) -> connection.close());
                    });
            }
        });
        acceptThread.setDaemon(true);
        acceptThread.setName("build.base.network.Server");
        acceptThread.start();

        return this;
    }

    /**
     * Obtains a {@link CompletableFuture} that is completed when the {@link Server} has been started and is ready
     * accept {@link Connection}s from {@link Client}s, and process {@link SerializableCallable} requests.
     *
     * @return a {@link CompletableFuture}
     */
    public CompletableFuture<Server> onStarted() {
        return this.onStarted;
    }

    /**
     * Obtains a {@link CompletableFuture} that is completed when the {@link Server} has is no longer able to
     * accept {@link Connection}s or process {@link SerializableCallable} requests and all the {@link Connection}s are
     * closed.
     *
     * @return a {@link CompletableFuture}
     */
    public CompletableFuture<Server> onStopped() {
        return CompletableFuture.allOf(this.connections.values().stream()
            .map(connectionFuture -> connectionFuture.thenCompose(Connection::onClosed))
            .toArray(CompletableFuture[]::new)).thenCompose(__ -> this.onStopped);
    }

    /**
     * Obtains a {@link CompletableFuture} that is completed with and when a {@link Connection} from the specified
     * remotely identified {@link Client} is made.
     * <p>
     * Should the {@link Server} be closed, the returned {@link CompletableFuture} will cancelled.  Should it be
     * determined that a {@link Connection} will never be made, the returned {@link CompletableFuture} should be
     * {@link CompletableFuture#cancel(boolean)}ed by the caller to prevent the {@link Server} maintaining the
     * {@link CompletableFuture} forever, thus leaking resources.
     *
     * @param remoteIdentity the identity of the {@link Client}.
     * @return a {@link CompletableFuture}
     */
    public synchronized CompletableFuture<Connection> onConnection(final long remoteIdentity) {
        if (this.onStopped.isDone()) {
            final CompletableFuture<Connection> future = new CompletableFuture<>();
            future.cancel(true);
            return future;
        }
        else {
            return this.onStarted.thenCompose(server -> this.connections.compute(remoteIdentity, (entryId, future) -> {
                if (future == null) {
                    final CompletableFuture<Connection> connection = new CompletableFuture<>();

                    // when cancelled (completed exceptionally) automatically remove tracking the connection from the
                    // server to prevent holding onto connections that will never be made
                    connection.whenComplete((c, t) -> {
                        if (t != null) {
                            this.connections.remove(entryId);
                        }
                    });

                    return connection;
                }
                else {
                    return future;
                }
            }));
        }
    }

    /**
     * Obtains the local {@link InetAddress} on which the {@link Server} is listening for {@link Client}
     * {@link Connection}s.
     *
     * @return the local {@link InetAddress}
     */
    public InetAddress getLocalAddress() {
        return this.serverSocket.getInetAddress();
    }

    /**
     * Obtains the local port on which the {@link Server} is listening for {@link Client} {@link Connection}s.
     *
     * @return the local port
     */
    public int getLocalPort() {
        return this.serverSocket.getLocalPort();
    }

    @Override
    public synchronized void close() {

        if (!this.onStopped.isDone()) {
            // signal that the server is stopped
            this.onStopped.complete(this);

            Closeables.close(this.serverSocket);
        }
    }
}
