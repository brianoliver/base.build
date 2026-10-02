package build.base.marshalling.example;

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

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;

import java.lang.invoke.MethodHandles;

/**
 * A marshallable class whose destructor marshals its cyclic neighbor ({@link Pong}) synchronously, via the
 * {@link Marshaller}. Used to exercise a {@code StackOverflowError} raised directly from
 * {@link Marshaller#marshal(Object)}.
 *
 * @see Pong
 */
public class Ping {

    private Pong pong;

    public Ping() {
    }

    @Unmarshal
    public Ping(final Marshaller marshaller, final Marshalled<Pong> pong) {
        this.pong = marshaller.unmarshal(pong);
    }

    @Marshal
    public void destructor(final Marshaller marshaller, final Out<Marshalled<Pong>> pong) {
        pong.set(marshaller.marshal(this.pong));
    }

    public void linkTo(final Pong pong) {
        this.pong = pong;
    }

    static {
        Marshalling.register(Ping.class, MethodHandles.lookup());
    }
}
