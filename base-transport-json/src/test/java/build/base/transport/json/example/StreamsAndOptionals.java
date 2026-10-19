package build.base.transport.json.example;

/*-
 * #%L
 * base.build Transport (JSON)
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

import build.base.foundation.stream.Streams;
import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamsAndOptionals {

    private final ArrayList<String> nullStream;
    private final ArrayList<String> emptyStream;
    private final Optional<String> nullOptional;
    private final Optional<String> emptyOptional;

    public StreamsAndOptionals() {
        this.nullStream = null;
        this.emptyStream = new ArrayList<>();
        this.nullOptional = null;
        this.emptyOptional = Optional.empty();
    }

    @Unmarshal
    public StreamsAndOptionals(final Stream<String> nullStream,
                               final Stream<String> emptyStream,
                               final Optional<String> nullOptional,
                               final Optional<String> emptyOptional) {

        this.nullStream = nullStream == null
            ? null
            : nullStream.collect(Collectors.toCollection(ArrayList::new));

        this.emptyStream = emptyStream == null
            ? null
            : emptyStream.collect(Collectors.toCollection(ArrayList::new));

        this.nullOptional = nullOptional == null ? null : nullOptional;
        this.emptyOptional = emptyOptional == null ? Optional.empty() : emptyOptional;
    }

    @Marshal
    public void destructor(final Out<Stream<String>> nullStream,
                           final Out<Stream<String>> emptyStream,
                           final Out<Optional<String>> nullOptional,
                           final Out<Optional<String>> emptyOptional) {

        nullStream.set(this.nullStream == null ? null : this.nullStream.stream());
        emptyStream.set(this.emptyStream == null ? null : this.emptyStream.stream());
        nullOptional.set(this.nullOptional);
        emptyOptional.set(this.emptyOptional);
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof final StreamsAndOptionals that)) {
            return false;
        }

        return Objects.equals(this.emptyOptional, that.emptyOptional)
            && Objects.equals(this.nullOptional, that.nullOptional)
            && ((this.nullStream == null && that.nullStream == null) || Streams.equals(this.nullStream.stream(),
            that.nullStream.stream()))
            && ((this.emptyStream == null && that.emptyStream == null) || Streams.equals(this.emptyStream.stream(),
            that.emptyStream.stream()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.nullStream, this.emptyStream, this.nullOptional, this.emptyOptional);
    }

    static {
        Marshalling.register(StreamsAndOptionals.class, MethodHandles.lookup());
    }
}
