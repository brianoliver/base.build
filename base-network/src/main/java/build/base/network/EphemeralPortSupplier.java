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

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link PortSupplier} which lazily creates and holds leases on ports allocated by the OS in the ephemeral port
 * range. The {@link EphemeralPortSupplier} then yields a leased port while giving up the lease when asked for a
 * port.
 *
 * @author graeme.campbell
 * @since Aug-2019
 */
public class EphemeralPortSupplier
    implements PortSupplier {

    /**
     * The port used to request that the OS provide a free ephemeral port.
     */
    private static final int EPHEMERAL_PORT = 0;

    /**
     * The set of {@link InetAddress}es on which to scan to determine port availability.
     */
    private final Set<InetAddress> leasableAddresses;

    /**
     * Constructs a new {@link EphemeralPortSupplier} which will hold leases on the supplied addresses for each
     * port it leases.
     *
     * @param leasableAddresses the {@link InetAddress}es on this host which may be leased
     */
    private EphemeralPortSupplier(final Stream<InetAddress> leasableAddresses) {
        this.leasableAddresses = leasableAddresses.collect(Collectors.toSet());
    }

    /**
     * Creates a new {@link EphemeralPortSupplier} which leases ports across the entire ephemeral port range for
     * this host and reserves sockets on the all of the host's reachable local addresses.
     *
     * @return a new {@link EphemeralPortSupplier}
     */
    public static EphemeralPortSupplier create() {
        return new EphemeralPortSupplier(
            Network.hostRequiresWildcardReservations() ? Stream.concat(Network.reachableLocalAddresses(),
                Stream.of(Network.localWildcardAddress())) : Network.reachableLocalAddresses());
    }

    @Override
    public int getAsInt() {

        final var incompleteLeases = new HashSet<Lease>();
        try {
            while (true) {
                final var lease = Lease.acquire(this.leasableAddresses);

                if (lease.complete()) {
                    lease.close();
                    return lease.port;
                }

                incompleteLeases.add(lease);
            }
        }
        finally {
            incompleteLeases.forEach(Lease::close);
        }
    }

    /**
     * A {@link Lease} on a port which has reserved sockets on all {@link InetAddress}es included in this
     * {@link EphemeralPortSupplier}.
     */
    private static class Lease
        implements AutoCloseable {

        /**
         * The {@link ServerSocket}s for this {@link Lease}.
         */
        private final Set<ServerSocket> serverSockets;

        /**
         * The {@link DatagramSocket}s for this {@link Lease}.
         */
        private final Set<DatagramSocket> datagramSockets;

        /**
         * The {@link InetAddress}es to create this {@link Lease} on.
         */
        private final Set<InetAddress> addressesToLease;

        /**
         * The port reserved by this {@link Lease}.
         */
        private int port;

        /**
         * Create a {@link Lease} on a port for the provided {@link InetAddress}es.
         *
         * @param port             the port to reserve sockets on
         * @param addressesToLease the {@link InetAddress}es to reserve sockets on
         */
        private Lease(final int port, final Set<InetAddress> addressesToLease) {
            this.port = port;
            this.serverSockets = new HashSet<>();
            this.datagramSockets = new HashSet<>();
            this.addressesToLease = addressesToLease;
            this.addressesToLease.forEach(this::reserveSockets);
        }

        /**
         * Attempt to acquire a {@link Lease} on the provided port and {@link InetAddress}es. The result is empty if
         * one or more of the {@link InetAddress}es were occupied and the port could not be completely reserved.
         *
         * @param addressesToLease the {@link InetAddress}es to reserve
         * @return an {@link Optional} {@link Lease} on the requested port and {@link InetAddress}es
         */
        static Lease acquire(final Set<InetAddress> addressesToLease) {
            return new Lease(EPHEMERAL_PORT, addressesToLease);
        }

        @Override
        public void close() {
            this.serverSockets.forEach(Closeables::close);
            this.datagramSockets.forEach(Closeables::close);
        }

        /**
         * Get the port that this {@link Lease} is holding.
         *
         * @return this {@link Lease}'s port
         */
        public int getPort() {
            return this.port;
        }

        /**
         * Determine whether this {@link Lease} holds sockets on the complete set of {@link InetAddress}es it is
         * supposed to.
         *
         * @return whether this {@link Lease} is complete
         */
        public boolean complete() {
            return (this.serverSockets.size() == this.addressesToLease.size());
        }

        /**
         * Reserves a {@link ServerSocket} and {@link DatagramSocket} for the provided {@link InetAddress} and this
         * {@link Lease}'s port.
         *
         * @param address the {@link InetAddress} to bind the sockets to
         */
        private void reserveSockets(final InetAddress address) {

            ServerSocket serverSocket = null;

            try {
                serverSocket = new ServerSocket();
                serverSocket.bind(new InetSocketAddress(address, this.port));

                if (this.port == 0) {
                    this.port = serverSocket.getLocalPort();
                }

                // because datagram sockets cannot be acquired once the local wildcard address has a datagram socket created we
                // only attempt to create a datagram socket if this address is the local wildcard address
                if ((!address.equals(Network.localWildcardAddress()) && Network.hostRequiresWildcardReservations())) {
                    this.serverSockets.add(serverSocket);
                }
                else {
                    // if any other local addresses have a datagram socket created for this port we will fail to acquire this
                    // wildcard address's datagram socket for this port
                    try {
                        final DatagramSocket datagramSocket = new DatagramSocket(this.port, address);

                        this.serverSockets.add(serverSocket);
                        this.datagramSockets.add(datagramSocket);
                    }
                    catch (final IOException e) {
                        Closeables.close(serverSocket);
                    }
                }
            }
            catch (final IOException e) {
                Closeables.close(serverSocket);

                if (this.port == 0) {
                    throw new NetworkException("OS has run out of ephemeral ports so no further ports can be supplied");
                }
            }
        }
    }
}
