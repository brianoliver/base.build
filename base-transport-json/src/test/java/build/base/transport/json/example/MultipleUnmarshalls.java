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

public class MultipleUnmarshalls {

    private final String aValue;
    private final Optional<String> anOptional;

    public MultipleUnmarshalls() {
        this.aValue = "";
        this.anOptional = Optional.empty();
    }

    @Unmarshal
    public MultipleUnmarshalls(final String aValue,
                               final Optional<String> anOptional) {

        this.aValue = aValue;
        this.anOptional = anOptional;
    }

    @Unmarshal
    public MultipleUnmarshalls(final String aValue) {

        this.aValue = aValue;
        this.anOptional = Optional.of("missing");
    }

    @Marshal
    public void destructor(final Out<String> aValue,
                           final Out<Optional<String>> anOptional) {
        aValue.set(this.aValue);
        anOptional.set(this.anOptional);
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof final MultipleUnmarshalls that)) {
            return false;
        }

        return this.aValue.equals(that.aValue) &&
            Objects.equals(this.anOptional, that.anOptional);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.aValue, this.anOptional);
    }

    static {
        Marshalling.register(MultipleUnmarshalls.class, MethodHandles.lookup());
    }
}
