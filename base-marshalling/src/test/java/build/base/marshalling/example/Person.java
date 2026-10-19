package build.base.marshalling.example;

/*-
 * #%L
 * base.build Marshalling
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

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.base.marshalling.tutorial.Address;

import java.util.Objects;

/**
 * A marshallable class that is comprises another marshallable class, the former explicitly using
 * {@link Marshalled}s and {@link Marshaller}s for marshalling.
 *
 * @author brian.oliver
 * @since Nov-2024
 */
public class Person {

    private final Address address;

    public Person(final Address address) {
        this.address = address;
    }

    @Unmarshal
    public Person(final Marshaller marshaller,
                  final Marshalled<Address> address) {

        this.address = marshaller.unmarshal(address);
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Marshalled<Address>> address) {

        address.set(marshaller.marshal(this.address));
    }

    public Address address() {
        return this.address;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof final Person person)) {
            return false;
        }
        return Objects.equals(this.address, person.address);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.address);
    }
}
