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

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * A marshallable class with multiple parameters including a {@link Country} enum.
 *
 * @author brian.oliver
 * @since Nov-2024
 */
public class AddressWithCountry {

    private final String street;
    private final String city;
    private final Country country;

    @Unmarshal
    public AddressWithCountry(final String street,
                              final String city,
                              final Country country) {
        this.street = street;
        this.city = city;
        this.country = country;
    }

    @Marshal
    public void destructor(final Out<String> street,
                           final Out<String> city,
                           final Out<Country> country) {

        street.set(this.street);
        city.set(this.city);
        country.set(this.country);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof final AddressWithCountry that)) {
            return false;
        }
        return Objects.equals(street, that.street) && Objects.equals(city, that.city)
            && country == that.country;
    }

    @Override
    public int hashCode() {
        return Objects.hash(street, city, country);
    }

    static {
        Marshalling.register(AddressWithCountry.class, MethodHandles.lookup());
    }
}
