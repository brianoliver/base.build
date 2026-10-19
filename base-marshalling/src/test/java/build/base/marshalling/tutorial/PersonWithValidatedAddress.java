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

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * A marshallable person with an {@link ValidatedAddress}.
 *
 * @author brian.oliver
 * @since Jan-2025
 */
public class PersonWithValidatedAddress {

    private String firstName;
    private String lastName;
    private ValidatedAddress address;

    /**
     * Constructs a {@link PersonWithValidatedAddress}.
     *
     * @param firstName the first name
     * @param lastName  the last name
     * @param address   the {@link ValidatedAddress}
     */
    @Unmarshal
    public PersonWithValidatedAddress(final String firstName,
                                      final String lastName,
                                      final ValidatedAddress address) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
    }

    /**
     * Destructs a {@link PersonWithValidatedAddress}
     *
     * @param firstName the first name
     * @param lastName  the last name
     */
    @Marshal
    public void destructor(final Out<String> firstName,
                           final Out<String> lastName,
                           final Out<ValidatedAddress> middleName) {

        firstName.set(this.firstName);
        lastName.set(this.lastName);
        middleName.set(this.address);
    }

    public String firstName() {
        return this.firstName;
    }

    public String lastName() {
        return this.lastName;
    }

    public ValidatedAddress address() {
        return this.address;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof final PersonWithValidatedAddress that)) {
            return false;
        }
        return Objects.equals(this.firstName, that.firstName)
            && Objects.equals(this.lastName, that.lastName)
            && Objects.equals(this.address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.firstName, this.lastName, this.address);
    }

    static {
        // self-register this type when it's initialized
        Marshalling.register(PersonWithValidatedAddress.class, MethodHandles.lookup());
    }
}
