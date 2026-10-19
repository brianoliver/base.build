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
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;

import java.util.Objects;
import java.util.Optional;

/**
 * A {@code null}able value.
 *
 * @author brian.oliver
 * @since Nov-2024
 */
public class Wrapper<T> {

    private final T value;

    @Unmarshal
    public Wrapper(final T value) {
        this.value = value;
    }

    @Marshal
    public void destructor(final Out<T> value) {
        value.set(this.value);
    }

    public Optional<T> get() {
        return Optional.ofNullable(this.value);
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof final Wrapper<?> wrapper)) {
            return false;
        }
        return Objects.equals(this.value, wrapper.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.value);
    }
}
