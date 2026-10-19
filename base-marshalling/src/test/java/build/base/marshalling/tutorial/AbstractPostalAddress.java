package build.base.marshalling.tutorial;

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

import build.base.marshalling.Out;

import java.util.Objects;

/**
 * An abstract {@link PostalAddress}
 *
 * @author brian.oliver
 * @since Jan-2025
 */
public abstract class AbstractPostalAddress
    implements PostalAddress {

    private final String street;
    private final String city;

    protected AbstractPostalAddress(final String street,
                                    final String city) {

        this.street = street;
        this.city = city;
    }

    public void destructor(final Out<String> street,
                           final Out<String> city) {

        street.set(this.street);
        city.set(this.city);
    }

    @Override
    public String street() {
        return this.street;
    }

    @Override
    public String city() {
        return this.city;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof final AbstractPostalAddress address)) {
            return false;
        }
        return Objects.equals(this.street, address.street)
            && Objects.equals(this.city, address.city);
    }
}
