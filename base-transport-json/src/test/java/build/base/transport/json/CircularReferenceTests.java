package build.base.transport.json;

/*-
 * #%L
 * base.build Transport (JSON)
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

import build.base.json.Json;
import build.base.json.JsonValue;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshalling;
import build.base.transport.json.example.Address;
import build.base.transport.json.example.Group;
import build.base.transport.json.example.LazyObjectHolder;
import build.base.transport.json.example.Node;
import build.base.transport.json.example.Ping;
import build.base.transport.json.example.Pong;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests covering shared and lazily-discovered cyclic references encoded by {@link JsonTransport}.
 * <p>
 * {@code JsonTransport} assigns every encoded {@link Marshalled} an {@code @id} up front, before walking
 * its own values, and represents a repeat encounter of that same {@link Marshalled} (whether the cycle is still
 * being encoded, or was already fully encoded elsewhere as a merely-shared reference) as an {@code @ref} back to
 * that id, rather than re-walking it. That single mechanism resolves all of these cases without ever throwing —
 * each is discovered lazily (via a raw field or a {@code Stream<Marshalled<X>>}), late enough that a real
 * {@link Marshalled} already exists for the ancestor to be referenced by. Contrast this with
 * {@code CircularReferenceProblemTests} in {@code base-marshalling}, where a cycle discovered synchronously,
 * before any {@link Marshalled} exists yet, is rejected cleanly instead.
 *
 * @since Sep-2026
 */
class CircularReferenceTests {

    private static int number(final JsonValue value) {
        return value.asNumber().toNumber().intValue();
    }

    /**
     * An object graph without any sharing or cycles carries no identities at all, as nothing refers to them.
     */
    @Test
    void shouldNotEncodeIdentitiesNothingReferences() {

        final var parent = new Group("Parent");
        parent.addRelated(new Group("Child"));

        final var marshaller = Marshalling.newMarshaller();
        final var transport = new JsonTransport();
        final var writer = new StringWriter();
        transport.write(marshaller.marshal(parent), writer, marshaller);

        assertThat(writer.toString())
            .doesNotContain("@id")
            .doesNotContain("@ref");

        final Marshalled<Group> transported = transport.read(new StringReader(writer.toString()));
        final Group unmarshalled = marshaller.unmarshal(transported);

        assertThat(unmarshalled.related().get(0).name())
            .isEqualTo("Child");
    }

    /**
     * A destructor that exposes the raw cyclic neighbor is resolved, once {@link JsonTransport}
     * walks the raw field a second time, as a back-reference to the outermost object rather than re-walked
     * forever.
     */
    @Test
    void shouldResolveRawCyclicNeighborAsReference() {

        final var ping = new Ping();
        final var pong = new Pong();
        ping.linkTo(pong);
        pong.linkTo(ping);

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(ping);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();

        transport.write(marshalled, writer, marshaller);

        final var root = Json.parse(new StringReader(writer.toString())).asObject();
        assertThat(root.get("@type").asString().value())
            .isEqualTo(Ping.class.getName());
        assertThat(number(root.get("@id")))
            .isEqualTo(1);

        final var encodedPong = root.get("pong").asObject();
        assertThat(encodedPong.get("@type").asString().value())
            .isEqualTo(Pong.class.getName());
        // nothing refers to the Pong, so it carries no identity
        assertThat(encodedPong.has("@id"))
            .isFalse();

        // the cycle closes with a bare reference to the outermost Ping, rather than encoding it a second time
        final var back = encodedPong.get("ping").asObject();
        assertThat(back.has("@id"))
            .isFalse();
        assertThat(number(back.get("@ref")))
            .isEqualTo(1);
    }

    /**
     * A lazily-evaluated {@code Stream<Marshalled<X>>} collection is resolved, once
     * {@link JsonTransport} forces the {@link java.util.stream.Stream} during {@code write()} and discovers the
     * same {@link Marshalled} again, as a back-reference rather than re-walked forever.
     */
    @Test
    void shouldResolveSelfReferentialStreamOfMarshalledsAsReference() {

        final var retention = new Group("Retention");
        retention.addRelated(retention);

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(retention);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();

        transport.write(marshalled, writer);

        final var root = Json.parse(new StringReader(writer.toString())).asObject();
        assertThat(number(root.get("@id")))
            .isEqualTo(1);

        final var related = root.get("related").asArray().values();
        assertThat(related)
            .hasSize(1);

        // the group's only relation is itself: a bare reference to the outermost group, not a second encoding
        final var back = related.get(0).asObject();
        assertThat(back.has("@id"))
            .isFalse();
        assertThat(number(back.get("@ref")))
            .isEqualTo(1);
    }

    /**
     * A self-referential {@code Stream<Marshalled<X>>} collection survives a full round trip. {@link Group}'s
     * {@code @Unmarshal} constructor must not consume the stream during construction (the {@code @ref} back to
     * itself cannot be resolved until construction finishes), and instead defers until {@link Group#related()}.
     */
    @Test
    void shouldRoundTripSelfReferentialStreamOfMarshalleds() {

        final var retention = new Group("Retention");
        retention.addRelated(retention);

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(retention);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();
        transport.write(marshalled, writer);

        final Marshalled<Group> transported = transport.read(new StringReader(writer.toString()));
        final Group unmarshalled = marshaller.unmarshal(transported);

        assertThat(unmarshalled.name())
            .isEqualTo("Retention");
        assertThat(unmarshalled.related())
            .hasSize(1);
        assertThat(unmarshalled.related().get(0).name())
            .isEqualTo("Retention");
    }

    /**
     * A shared, non-cyclic reference reached from two independent, non-nested locations is recognized as
     * shared and represented as a single {@code @id} plus an {@code @ref} back to it, so the two round-tripped
     * instances are the very same object.
     */
    @Test
    void shouldPreserveIdentityOfSharedNonCyclicReference() {

        final var shared = new Group("Shared");
        final var a = new Group("A");
        a.addRelated(shared);
        final var b = new Group("B");
        b.addRelated(shared);
        final var root = new Group("Root");
        root.addRelated(a);
        root.addRelated(b);

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(root);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();
        transport.write(marshalled, writer);

        final Marshalled<Group> transported = transport.read(new StringReader(writer.toString()));
        final Group unmarshalledRoot = marshaller.unmarshal(transported);

        final var unmarshalledA = unmarshalledRoot.related().get(0);
        final var unmarshalledB = unmarshalledRoot.related().get(1);

        assertThat(unmarshalledA.related().get(0))
            .isSameAs(unmarshalledB.related().get(0));
    }

    /**
     * A minimal two-node mutually-referential cluster ({@code X} and {@code Y}) reached from two otherwise
     * unrelated, non-nested root objects. Both are encoded once, with back-references closing the cycle, and
     * decode to the very same instances.
     */
    @Test
    void shouldResolveMutuallyReferentialSharedClusterAsReference() {

        final var x = new Group("X");
        final var y = new Group("Y");
        x.addRelated(y);
        y.addRelated(x);

        final var rootA = new Group("RootA");
        rootA.addRelated(x);
        final var rootB = new Group("RootB");
        rootB.addRelated(y);

        final var universe = new Group("Universe");
        universe.addRelated(rootA);
        universe.addRelated(rootB);

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(universe);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();

        transport.write(marshalled, writer);

        assertThat(writer.toString())
            .contains("\"@ref\"");

        final Marshalled<Group> transported = transport.read(new StringReader(writer.toString()));
        final Group unmarshalled = marshaller.unmarshal(transported);

        final var unmarshalledX = unmarshalled.related().get(0).related().get(0);
        final var unmarshalledY = unmarshalled.related().get(1).related().get(0);

        assertThat(unmarshalledX.name()).isEqualTo("X");
        assertThat(unmarshalledY.name()).isEqualTo("Y");
        assertThat(unmarshalledX.related().get(0)).isSameAs(unmarshalledY);
        assertThat(unmarshalledY.related().get(0)).isSameAs(unmarshalledX);
    }

    /**
     * Decoding a genuine cycle back into constructed objects. A {@code @Unmarshal} constructor is
     * strictly bottom-up - it needs its arguments fully resolved before it can be invoked - so a reference
     * that points back to an object still in the middle of being constructed has nothing to resolve to yet.
     * {@link Node} sidesteps this by asking for its self-reference as a {@link build.base.foundation.Lazy}
     * rather than a resolved {@link Node}: {@link JsonTransport} defers resolving it until something actually
     * calls {@link Node#next()}, by which point the whole decode - including this very {@link Node} - has
     * finished constructing.
     */
    @Test
    void shouldReconstructSelfReferentialLazyReferenceOnDecode() {

        final var node = new Node("self");
        node.linkTo(node);

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(node);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();
        // the Marshaller that produced the Marshalled is supplied, so the raw cyclic field is recognized as
        // that same Marshalled and the "@ref" closes onto the outermost "@id"
        transport.write(marshalled, writer, marshaller);

        final Marshalled<Node> transported = transport.read(new StringReader(writer.toString()));
        final var unmarshalled = marshaller.unmarshal(transported);

        assertThat(unmarshalled.name())
            .isEqualTo("self");
        assertThat(unmarshalled.next())
            .isSameAs(unmarshalled);
    }

    /**
     * A {@code Lazy<Object>} parameter is encoded by the abstract-type {@code @type}/{@code value} wrapper, and
     * decoded by unwrapping it to the structural {@link Marshalled} - deferred, rather than resolved eagerly.
     */
    @Test
    void shouldRoundTripLazyOfAbstractType() {

        final var holder = new LazyObjectHolder(new Address("1 Main St", "Springfield"));

        final var marshaller = Marshalling.newMarshaller();
        final var transport = new JsonTransport();
        final var writer = new StringWriter();
        transport.write(marshaller.marshal(holder), writer, marshaller);

        final Marshalled<LazyObjectHolder> transported = transport.read(new StringReader(writer.toString()));

        assertThat(marshaller.unmarshal(transported).content())
            .isEqualTo(new Address("1 Main St", "Springfield"));
    }

    /**
     * A {@code Lazy<Object>} parameter whose abstract-type wrapper lacks a {@code value} member is rejected.
     */
    @Test
    void shouldRejectLazyOfAbstractTypeWithoutValueMember() {

        final var json = "{\"@type\":\"" + LazyObjectHolder.class.getName() + "\","
            + "\"content\":{\"@type\":\"" + Address.class.getName() + "\"}}";

        final var transport = new JsonTransport();

        assertThatThrownBy(() -> transport.<LazyObjectHolder>read(new StringReader(json)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("value");
    }

    /**
     * An {@code @id} is only unique within the one document it was assigned in, so a single, reused
     * {@link build.base.marshalling.Marshaller} unmarshalling several documents must not resolve an {@code @id}
     * (or {@code @ref}) in a later document to an {@link Object} from an earlier one.
     */
    @Test
    void shouldNotConfuseIdentitiesAcrossDocumentsUnmarshalledByTheSameMarshaller() {

        final var marshaller = Marshalling.newMarshaller();
        final var transport = new JsonTransport();

        final var a = new Group("A");
        a.addRelated(a);
        final var b = new Group("B");
        b.addRelated(b);

        final var writerA = new StringWriter();
        transport.write(marshaller.marshal(a), writerA);
        final var writerB = new StringWriter();
        transport.write(marshaller.marshal(b), writerB);

        final Group unmarshalledA = marshaller.unmarshal(
            transport.<Group>read(new StringReader(writerA.toString())));
        final Group unmarshalledB = marshaller.unmarshal(
            transport.<Group>read(new StringReader(writerB.toString())));

        assertThat(unmarshalledA.name())
            .isEqualTo("A");
        assertThat(unmarshalledB.name())
            .isEqualTo("B");
        assertThat(unmarshalledB.related().get(0))
            .isSameAs(unmarshalledB);
        assertThat(unmarshalledA.related().get(0))
            .isSameAs(unmarshalledA);
    }
}
