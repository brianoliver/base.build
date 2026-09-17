package build.base.transport.json.example;

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

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Exercises a value nested two generic levels deep - {@code Optional<Stream<Integer>>} and
 * {@code Stream<Optional<String>>} - where each level is handled by a distinct {@code Codec}
 * ({@code OptionalCodec}/{@code StreamableCodec}) rather than a single one. A {@code Codec} that re-derives
 * its own operand type from the constructor parameter's declared type, instead of the local type it was
 * actually invoked with, resolves the wrong type past the first level of nesting. {@code numbers} also
 * exercises the {@code Stream}-to-{@code Streamable} {@code Transformer} hop nested inside an {@code Optional},
 * which has the same failure mode if the {@code Transformer}'s target type isn't reparameterized with the
 * original type argument.
 */
public class NestedGenerics {

    private final Optional<List<Integer>> numbers;
    private final List<Optional<String>> names;

    public NestedGenerics(final Optional<List<Integer>> numbers, final List<Optional<String>> names) {
        this.numbers = numbers;
        this.names = names;
    }

    @Unmarshal
    public NestedGenerics(final Optional<Stream<Integer>> numbers, final Stream<Optional<String>> names) {
        this.numbers = numbers.map(stream -> stream.collect(Collectors.toList()));
        this.names = names.collect(Collectors.toList());
    }

    @Marshal
    public void destructor(final Out<Optional<Stream<Integer>>> numbers, final Out<Stream<Optional<String>>> names) {
        numbers.set(this.numbers.map(List::stream));
        names.set(this.names.stream());
    }

    public Optional<List<Integer>> numbers() {
        return this.numbers;
    }

    public List<Optional<String>> names() {
        return this.names;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof final NestedGenerics that)) {
            return false;
        }
        return Objects.equals(this.numbers, that.numbers)
            && Objects.equals(this.names, that.names);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.numbers, this.names);
    }

    static {
        Marshalling.register(NestedGenerics.class, MethodHandles.lookup());
    }
}
