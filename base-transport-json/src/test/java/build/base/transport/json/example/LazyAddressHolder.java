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

import java.lang.invoke.MethodHandles;

/**
 * A marshallable class exposing a {@code Lazy<Address>} symmetrically - unlike {@link Node}, both
 * {@code destructor()} and the {@code @Unmarshal} constructor declare it as {@code Lazy<Address>}.
 */
public class LazyAddressHolder {

    private final Lazy<Address> address;

    @Unmarshal
    public LazyAddressHolder(final Lazy<Address> address) {
        this.address = address == null ? Lazy.empty() : address;
    }

    @Marshal
    public void destructor(final Out<Lazy<Address>> address) {
        address.set(this.address);
    }

    public Address address() {
        return this.address.getOrNull();
    }

    static {
        Marshalling.register(LazyAddressHolder.class, MethodHandles.lookup());
    }
}
