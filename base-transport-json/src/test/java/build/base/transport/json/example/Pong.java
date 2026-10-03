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
 * The cyclic neighbor of {@link Ping}. See {@link Ping} for details.
 *
 * @see Ping
 */
public class Pong {

    private Ping ping;

    public Pong() {
    }

    @Unmarshal
    public Pong(final Ping ping) {
        this.ping = ping;
    }

    @Marshal
    public void destructor(final Out<Ping> ping) {
        ping.set(this.ping);
    }

    public void linkTo(final Ping ping) {
        this.ping = ping;
    }

    static {
        Marshalling.register(Pong.class, MethodHandles.lookup());
    }
}
