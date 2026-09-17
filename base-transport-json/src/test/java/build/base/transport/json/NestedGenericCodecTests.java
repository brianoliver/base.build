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

import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Parameter;
import build.base.transport.Transformer;
import build.base.transport.json.codec.OptionalCodec;
import build.base.transport.json.codec.StreamableCodec;
import build.base.transport.json.example.NestedGenerics;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers a {@link Codec} being invoked on a type nested two generic levels deep, e.g. the {@code Integer} of
 * {@code Optional<Stream<Integer>>}. Each {@link Codec} only receives {@link Parameter}, not the local
 * {@link java.lang.reflect.Type} it's being invoked with, so a {@link Codec} that (incorrectly) re-derives its
 * own operand type from {@code Parameter.type()} - instead of the {@link java.lang.reflect.Type} it was
 * actually invoked with - gets the wrong answer past the first level of nesting: it would resolve
 * {@code Stream<Integer>} where it should resolve {@code Integer}.
 *
 * @see NestedGenerics
 */
class NestedGenericCodecTests {

    /**
     * {@code Optional<Stream<Integer>>}: {@link OptionalCodec} wraps a value whose own type needs a
     * {@code Stream}-to-{@code Streamable} {@link Transformer} hop before {@link StreamableCodec} can handle
     * it - exercising both the {@code Codec}-to-{@code Codec} nesting and the {@link Transformer} target-type
     * reparameterization ({@code JsonTransport.retarget}) in the same field.
     */
    @Test
    void shouldRoundTripPresentOptionalOfStreamOfIntegers() {

        final var nested = new NestedGenerics(Optional.of(List.of(1, 2, 3)), List.of());

        assertThat(roundTrip(nested))
            .isEqualTo(nested);
    }

    /**
     * The empty-{@link Optional} half of the same field - a {@link Codec} that resolved the wrong element type
     * would still pass this one (nothing to decode), so it's here only to pin down that the {@code ignore}/
     * {@code defaultValue} path is unaffected by the deeper fix.
     */
    @Test
    void shouldRoundTripEmptyOptionalOfStreamOfIntegers() {

        final var nested = new NestedGenerics(Optional.empty(), List.of());

        assertThat(roundTrip(nested))
            .isEqualTo(nested);
    }

    /**
     * {@code Stream<Optional<String>>}: the nesting the other way around - {@link StreamableCodec} wraps
     * elements each handled by {@link OptionalCodec}, including a present and an empty {@link Optional} in the
     * same {@link java.util.stream.Stream} so both {@code OptionalCodec} branches run past the outer nesting.
     */
    @Test
    void shouldRoundTripStreamOfOptionalStrings() {

        final var nested = new NestedGenerics(
            Optional.empty(),
            Arrays.asList(Optional.of("first"), Optional.empty(), Optional.of("third")));

        assertThat(roundTrip(nested))
            .isEqualTo(nested);
    }

    private static NestedGenerics roundTrip(final NestedGenerics nested) {

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(nested);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();
        transport.write(marshalled, writer);

        final Marshalled<NestedGenerics> transported = transport.read(new StringReader(writer.toString()));

        return marshaller.unmarshal(transported);
    }
}
