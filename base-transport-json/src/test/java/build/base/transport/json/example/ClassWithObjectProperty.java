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

public class ClassWithObjectProperty {

    private final Object object;

    @Unmarshal
    public ClassWithObjectProperty(final Object object) {
        this.object = object;
    }

    @Marshal
    public void destructor(final Out<Object> object) {

        object.set(this.object);
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof final ClassWithObjectProperty classWithObjectProperty)) {
            return false;
        }
        return Objects.equals(this.object, classWithObjectProperty.object);
    }

    @Override
    public int hashCode() {
        return this.object.hashCode();
    }

    static {
        Marshalling.register(ClassWithObjectProperty.class, MethodHandles.lookup());
    }

}
