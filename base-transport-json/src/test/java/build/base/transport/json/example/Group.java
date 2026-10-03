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

import build.base.foundation.Lazy;
import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A marshallable class that destructs its related {@link Group}s as a lazily-evaluated
 * {@code Stream<Marshalled<Group>>}. Used to exercise a self-referencing {@link Group} - {@code destructor()}
 * does not recurse, neither does {@code marshal()}, but writing it via a
 * {@link build.base.transport.json.JsonTransport} does, once the transport forces the {@link Stream}. Also
 * exercises a shared, non-cyclic reference reached from two independent locations (which keeps its identity
 * across a round trip), and a small mutually-referential cluster of {@link Group}s reached from unrelated
 * locations (which is encoded with back-references rather than recursing forever).
 * <p>
 * The {@code @Unmarshal} constructor defers unmarshalling {@code related} until {@link #related()} is first
 * called, as a {@code Stream<Marshalled<Group>>} that may participate in a cycle cannot be resolved eagerly.
 */
public class Group {

    private final String name;
    private final Lazy<List<Group>> related;

    public Group(final String name) {
        this.name = name;
        this.related = Lazy.of(new ArrayList<Group>());
    }

    /**
     * The {@code related} {@link Stream} is deliberately not consumed here: unmarshalling its elements
     * during construction would require resolving any {@code @ref} back to a {@link Group} that is itself still
     * being constructed, which is impossible. Instead, it is unmarshalled on first access to {@link #related()},
     * by which point the entire decode has finished.
     */
    @Unmarshal
    public Group(final Marshaller marshaller, final String name, final Stream<Marshalled<Group>> related) {
        this.name = name;
        this.related = Lazy.of(() -> related == null
            ? new ArrayList<Group>()
            : related.map(marshaller::unmarshal).collect(Collectors.toCollection(ArrayList::new)));
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<String> name,
                           final Out<Stream<Marshalled<Group>>> related) {
        name.set(this.name);
        related.set(this.related.get().stream().map(marshaller::marshal));
    }

    public String name() {
        return this.name;
    }

    public List<Group> related() {
        return this.related.get();
    }

    public void addRelated(final Group group) {
        this.related.get().add(group);
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof final Group that)) {
            return false;
        }
        return Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.name);
    }

    static {
        Marshalling.register(Group.class, MethodHandles.lookup());
    }
}
