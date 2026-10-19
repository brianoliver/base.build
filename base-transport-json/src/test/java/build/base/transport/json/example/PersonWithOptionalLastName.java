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
import java.util.Optional;

/**
 * A simple test class.
 *
 * @author brian.oliver
 * @since Nov-2024
 */
public class PersonWithOptionalLastName {

    private final String firstName;

    private final Optional<String> lastName;

    @Unmarshal
    public PersonWithOptionalLastName(final String firstName,
                                      final Optional<String> lastName) {

        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Marshal
    public void destructor(final Out<String> firstName,
                           final Out<Optional<String>> lastName) {
        firstName.set(this.firstName);
        lastName.set(this.lastName);
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof final PersonWithOptionalLastName that)) {
            return false;
        }
        return Objects.equals(this.firstName, that.firstName) && Objects.equals(this.lastName, that.lastName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.firstName, this.lastName);
    }

    static {
        Marshalling.register(PersonWithOptionalLastName.class, MethodHandles.lookup());
    }
}
