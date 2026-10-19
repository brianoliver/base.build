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
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassWithArrayOfClassWithObjectProperty {

    private final ArrayList<ClassWithObjectProperty> values;

    public ClassWithArrayOfClassWithObjectProperty(ArrayList<ClassWithObjectProperty> values) {
        this.values = values == null
            ? new ArrayList<>()
            : values;
    }

    @Unmarshal
    public ClassWithArrayOfClassWithObjectProperty(final Stream<ClassWithObjectProperty> values) {
        this.values = values.collect(Collectors.toCollection(ArrayList::new));
    }

    @Marshal
    public void destructor(final Out<Stream<ClassWithObjectProperty>> values) {
        values.set(this.values.stream());
    }

    public ArrayList<ClassWithObjectProperty> values() {
        return this.values;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof final ClassWithArrayOfClassWithObjectProperty other)) {
            return false;
        }
        if (this.values.size() != other.values.size()) {
            return false;
        }
        for (int i = 0; i < this.values.size(); i++) {
            if (!Objects.equals(this.values.get(i), other.values.get(i))) {
                return false;
            }
        }
        return true;
    }

    static {
        Marshalling.register(ClassWithArrayOfClassWithObjectProperty.class, MethodHandles.lookup());
    }

}
