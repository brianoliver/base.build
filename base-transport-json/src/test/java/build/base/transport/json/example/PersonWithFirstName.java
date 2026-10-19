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
 * A simple test class.
 *
 * @author brian.oliver
 * @since Nov-2024
 */
public class PersonWithFirstName {

    private final String name;

    @Unmarshal
    public PersonWithFirstName(final String name) {
        this.name = name;
    }

    @Marshal
    public void destructor(final Out<String> name) {
        name.set(this.name);
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof final PersonWithFirstName that)) {
            return false;
        }
        return Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.name);
    }

    static {
        Marshalling.register(PersonWithFirstName.class, MethodHandles.lookup());
    }
}
