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
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.base.transport.json.JsonTransport;

import java.lang.invoke.MethodHandles;

/**
 * A marshallable class whose {@code next} reference is exposed to {@code @Unmarshal} as a {@link Lazy},
 * exercising reconstructing a genuine cycle on the way back in. {@code destructor()} exposes the raw
 * {@link Node} (as {@code Ping}/{@code Pong} do), so encoding a self-referencing {@link Node} already produces
 * an {@code @ref} back to itself; the {@code @Unmarshal} constructor asks for that neighbor as a {@link Lazy}
 * rather than a resolved {@link Node}, so {@link JsonTransport} can defer resolving the reference until
 * {@link #next()} is actually called - by which point the whole decode, including this very {@link Node}, has
 * finished constructing and is resolvable.
 */
public class Node {

    private final String name;
    private Lazy<Node> next;

    public Node(final String name) {
        this.name = name;
        this.next = Lazy.empty();
    }

    @Unmarshal
    public Node(final String name, final Lazy<Node> next) {
        this.name = name;
        this.next = next == null ? Lazy.empty() : next;
    }

    @Marshal
    public void destructor(final Out<String> name, final Out<Node> next) {
        name.set(this.name);
        next.set(this.next.getOrNull());
    }

    public void linkTo(final Node next) {
        this.next = Lazy.of(next);
    }

    public String name() {
        return this.name;
    }

    public Node next() {
        return this.next.getOrNull();
    }

    static {
        Marshalling.register(Node.class, MethodHandles.lookup());
    }
}
