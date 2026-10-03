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

/**
 * A marshallable class exposing its cyclic neighbor ({@link Pong}) as a raw field. Marshalling this class alone
 * does not recurse — the destructor just hands back the raw {@link Pong} reference — but writing it via a
 * {@link build.base.transport.json.JsonTransport} does, because the transport marshals the raw neighbor once it
 * walks the field.
 *
 * @see Pong
 */
public class Ping {

    private Pong pong;

    public Ping() {
    }

    @Unmarshal
    public Ping(final Pong pong) {
        this.pong = pong;
    }

    @Marshal
    public void destructor(final Out<Pong> pong) {
        pong.set(this.pong);
    }

    public void linkTo(final Pong pong) {
        this.pong = pong;
    }

    static {
        Marshalling.register(Ping.class, MethodHandles.lookup());
    }
}
