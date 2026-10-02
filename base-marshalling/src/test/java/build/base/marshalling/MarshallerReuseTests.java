package build.base.marshalling;

/*-
 * #%L
 * base.build Marshalling
 * %%
 * Copyright (C) 2026 Workday Inc
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

import build.base.marshalling.example.MutablePoint;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests covering a {@link Marshaller} - as returned by {@link Marshalling#newMarshaller()} - being reused, exactly
 * as its documentation intends, for many unrelated {@link Marshaller#marshal(Object)}/
 * {@link Marshaller#unmarshal(Marshalled)} calls over its lifetime, rather than being discarded after one.
 *
 * @since Sep-2026
 */
class MarshallerReuseTests {

    /**
     * A {@link Marshaller} deliberately never forgets an {@link Object} identity it has already marshalled,
     * for the life of that {@link Marshaller} - even across separate, top-level {@link Marshaller#marshal(Object)}
     * calls made well apart from each other. This is not incidental: it is what lets a {@link Transport}
     * (e.g. {@code JsonTransport}) marshal a raw, previously-unmarshalled field it discovers while walking an
     * already-produced {@link Marshalled} - potentially long after the original {@link Marshaller#marshal(Object)}
     * call returned - and recognize it as the very same {@link Marshalled} it already has, closing a shared or
     * cyclic reference instead of re-destructing (or, for a genuine cycle, infinitely recursing on) the
     * {@link Object}.
     * <p>
     * One consequence: mutating an {@link Object} and marshalling it again with the <i>same</i> {@link Marshaller}
     * returns the {@link Marshalled} from the first call, not one reflecting the new state. Callers who marshal
     * a series of unrelated, potentially-mutated {@link Object} graphs are expected to use a fresh
     * {@link Marshaller} (via {@link Marshalling#newMarshaller()}) per graph, exactly as every other test in this
     * module does.
     */
    @Test
    void shouldReuseCachedMarshalledForSameObjectIdentityAcrossCalls() {

        final var point = new MutablePoint(1, 1);

        final var marshaller = Marshalling.newMarshaller();

        final var firstMarshalled = marshaller.marshal(point);
        assertThat(firstMarshalled.values().stream()).containsExactly(1, 1);

        point.moveTo(2, 2);

        final var secondMarshalled = marshaller.marshal(point);
        assertThat(secondMarshalled)
            .isSameAs(firstMarshalled);
        assertThat(secondMarshalled.values().stream()).containsExactly(1, 1);
    }

    /**
     * A {@link Marshaller} returned by {@link Marshalling#newMarshaller()} is a single, shared {@link Object} -
     * concurrent, independent {@link Marshaller#marshal(Object)}/{@link Marshaller#unmarshal(Marshalled)} calls
     * from different threads must not corrupt each other's cycle-detection/dedup state or throw.
     */
    @Test
    void shouldSupportConcurrentUseFromMultipleThreads() throws InterruptedException {

        final var marshaller = Marshalling.newMarshaller();

        final var threadCount = 8;
        final var iterationsPerThread = 2_000;

        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final var failures = new CopyOnWriteArrayList<Throwable>();
        final var latch = new CountDownLatch(threadCount);

        try {
            for (var t = 0; t < threadCount; t++) {
                final var threadIndex = t;
                executor.submit(() -> {
                    try {
                        for (var i = 0; i < iterationsPerThread; i++) {
                            final var point = new MutablePoint(threadIndex, i);
                            final var marshalled = marshaller.marshal(point);
                            final MutablePoint unmarshalled = marshaller.unmarshal(marshalled);
                            if (!unmarshalled.equals(point)) {
                                throw new AssertionError("Round-trip mismatch for " + point);
                            }
                        }
                    } catch (final Throwable failure) {
                        failures.add(failure);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdown();
        }

        assertThat(failures).isEmpty();
    }
}
