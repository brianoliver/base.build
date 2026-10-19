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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A marshallable class that is comprises mutliple other marshallable classes.
 *
 * @author brian.oliver
 * @since Nov-2024
 */
public class Company {

    private final ArrayList<Address> addresses;

    public Company() {
        this.addresses = new ArrayList<>();
    }

    @Unmarshal
    public Company(final Stream<Address> addresses) {
        this.addresses = addresses == null
            ? new ArrayList<>()
            : addresses.collect(Collectors.toCollection(ArrayList::new));
    }

    @Marshal
    public void destructor(final Out<Stream<Address>> addresses) {
        addresses.set(this.addresses.stream());
    }

    public Stream<Address> addresses() {
        return this.addresses.stream();
    }

    public Company add(final Address address) {
        if (address != null) {
            this.addresses.add(address);
        }
        return this;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof final Company that)) {
            return false;
        }
        return Streams.equals(this.addresses(), that.addresses());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.addresses);
    }

    static {
        Marshalling.register(Company.class, MethodHandles.lookup());
    }
}
