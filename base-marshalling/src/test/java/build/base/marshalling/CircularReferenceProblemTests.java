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

import build.base.marshalling.example.Ping;
import build.base.marshalling.example.Pong;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests covering a destructor that marshals its cyclic neighbor synchronously, before any {@link Marshalled}
 * exists to represent it, and so can't be resolved into a back-reference the way a lazily-discovered cycle can.
 * {@code HierarchicalMarshaller.marshal()} detects this and fails cleanly with a
 * {@link CyclicMarshallingException}, rather than recursing until the stack overflows.
 *
 * @since Sep-2026
 */
class CircularReferenceProblemTests {

    /**
     * A destructor that marshals its cyclic neighbor synchronously is rejected cleanly.
     */
    @Test
    void shouldRejectSynchronousCycleCleanly() {

        final var ping = new Ping();
        final var pong = new Pong();
        ping.linkTo(pong);
        pong.linkTo(ping);

        final var marshaller = Marshalling.newMarshaller();

        // the destructor is invoked reflectively, so the CyclicMarshallingException arrives wrapped in
        // InvocationTargetException/RuntimeException, not as the top-level thrown type - walk to the bottom
        // of whatever cause chain results
        assertThatThrownBy(() -> marshaller.marshal(ping))
            .satisfies(thrown -> {
                var cause = thrown;
                while (cause.getCause() != null && cause.getCause() != cause) {
                    cause = cause.getCause();
                }
                assertThat(cause).isInstanceOf(CyclicMarshallingException.class);
            });
    }
}
