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

import build.base.configuration.Configuration;
import build.base.configuration.Option;
import build.base.foundation.Strings;
import build.base.foundation.predicate.Predicates;
import build.base.network.option.KeepAlive;
import build.base.network.option.SocketTimeout;
import build.base.network.option.TCPNoDelay;
import build.base.option.Timeout;
import build.base.retryable.BlockingRetry;
import build.base.retryable.EphemeralFailureException;
import build.base.retryable.Retryable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Utilities for working with networks.
 *
 * @author brian.oliver
 * @since Dec-2018
 */
public class Network {

    /**
     * This host's wildcard {@link InetAddress}.
     */
    private static final InetAddress WILDCARD_ADDRESS = new InetSocketAddress(0).getAddress();

    /**
     * A {@link ConcurrentHashMap} of the {@link InetAddress}es that have been tested for reachability.
     */
    private static final ConcurrentHashMap<InetAddress, Boolean> REACHABILITY = new ConcurrentHashMap<>();

    /**
     * A private constructor to prevent creation.
     */
    private Network() {
        // prevent creation
    }

    /**
     * Determines if the specified {@link InetAddress}, define by a local {@link java.net.InterfaceAddress} is
     * reachable.
     * <p>
     * The reachability of an {@link InetAddress} is defined as the ability to successfully;
     * <ol>
     *     <li>establish a {@link ServerSocket} for the {@link InetAddress}, and</li>
     *     <li>connect to the established {@link ServerSocket} using a {@link Socket}</li>
     * </ol>
     * <p>
     * Once the reachability of an {@link InetAddress} has been determined, it is not rechecked for future invocations
     * of this method.  The previously determined reachability is returned.
     *
     * @param inetAddress the {@link InetAddress}
     * @return {@code true} if reachable, {@code false} otherwise
     */
    public static boolean isReachable(final InetAddress inetAddress) {

        return REACHABILITY.compute(inetAddress, (__, existing) -> {
            // don't check reachability when it's already known!
            if (existing != null) {
                return existing;
            }

            // attempt to establish a ServerSocket on an ephemeral port
            try (ServerSocket serverSocket = new ServerSocket(0, 1, inetAddress)) {

                // attempt to connect a client to the ServerSocket
                final Socket clientSocket = new Socket();

                clientSocket.connect(new InetSocketAddress(inetAddress, serverSocket.getLocalPort()), 1000);

                clientSocket.close();

                return true;
            }
            catch (final IOException exception) {
                return false;
            }
        });
    }

    /**
     * Obtains a {@link Predicate} that is satisfied if and only if a provided {@link InetAddress} is
     * an instance of the {@link Class} of specified {@link InetAddress}.
     *
     * @param inetAddress the {@link InetAddress}
     * @return a {@link Predicate}
     */
    public static Predicate<InetAddress> isOfClass(final InetAddress inetAddress) {
        if (inetAddress == null) {
            return Predicates.never();
        }
        else {
            return address -> inetAddress.getClass().isInstance(address);
        }
    }

    /**
     * The reachable non-virtual {@link InetAddress}es for the local machine.
     *
     * @return a {@link Stream} containing all reachable addresses for the local machine
     * @see Network#isReachable(InetAddress)
     */
    public static Stream<InetAddress> reachableLocalAddresses() {
        // determine the reachable addresses
        try {
            // obtain the locally available NetworkInterfaces
            final List<NetworkInterface> networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());

            // obtain the reachable InetAddresses of the NetworkInterfaces
            return networkInterfaces.stream()
                .filter(networkInterface -> {
                    try {
                        return !networkInterface.isPointToPoint();
                    }
                    catch (final SocketException e) {
                        return false;
                    }
                })
                .filter(networkInterface -> !networkInterface.isVirtual())
                .flatMap(networkInterface -> Collections.list(networkInterface.getInetAddresses())
                    .stream()
                    .filter(Network::isReachable));
        }
        catch (final SocketException e) {
            throw new NetworkException("Failed to determine network interfaces for the local machine");
        }
    }

    /**
     * Obtains the local host wildcard {@link InetAddress}.
     *
     * @return the local host wildcard {@link InetAddress}
     */
    public static InetAddress localWildcardAddress() {
        return WILDCARD_ADDRESS;
    }

    /**
     * Determines if wildcard reservations are required, typically when running on Mac OS.
     *
     * @return true if wildcard reservations are required
     */
    public static boolean hostRequiresWildcardReservations() {
        final String osName = System.getProperty("os.name");

        return !Strings.isEmpty(osName) && osName.startsWith("Mac");
    }

    /**
     * Attempts to connect to a {@link Socket} and retries until the {@link Timeout} is reached.
     *
     * @param address the {@link InetSocketAddress} at which the {@link Socket} can be reached
     * @param options the {@link Option}s passed
     * @return a {@link CompletableFuture} which will eventually be completed with a {@link Socket}
     * <p>
     * {@link Option}s for how frequently and how long to retry the connection:
     * @see build.base.option.Timeout
     * @see build.base.retryable.option.Delay
     * @see build.base.retryable.option.RetryFrequency
     * <p>
     * {@link Option}s for the connection attempt itself:
     * @see SocketTimeout
     * @see KeepAlive
     * @see TCPNoDelay
     */
    public static CompletableFuture<Socket> connect(final InetSocketAddress address,
                                                    final Option... options) {

        final Configuration configuration = Configuration.of(options);

        final var timeout = configuration.getValue(SocketTimeout.class);
        final var keepAlive = configuration.getOrDefault(KeepAlive.class, () -> KeepAlive.YES) == KeepAlive.YES;
        final var tcpNoDelay = configuration.getOrDefault(TCPNoDelay.class, () -> TCPNoDelay.YES) == TCPNoDelay.YES;

        final Retryable<Socket> supplySocket = () -> {
            try {
                final Socket socket = new Socket();
                socket.setKeepAlive(keepAlive);
                socket.setTcpNoDelay(tcpNoDelay);
                socket.connect(address, (int) timeout.toMillis());
                return socket;
            }
            catch (final IOException e) {
                throw new EphemeralFailureException(e);
            }
        };

        final CompletableFuture<Socket> future = new CompletableFuture<>();
        Thread.ofVirtual()
            .name("Connecting to: " + address)
            .start(() -> {
                try {
                    future.complete(BlockingRetry.of(supplySocket, options).get());
                }
                catch (final Throwable throwable) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }
}
