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

/**
 * A Point that that relies upon dynamic reflection to determine {@link Marshal} and {@link Unmarshal} methods.
 *
 * @author brian.oliver
 * @since Nov-2024
 */
public class Point {

    private final int x;
    private final int y;

    @Unmarshal
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Marshal
    public void destructor(final Out<Integer> x,
                           final Out<Integer> y) {
        x.set(this.x);
        y.set(this.y);
    }

    public int x() {
        return this.x;
    }

    public int y() {
        return this.y;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof final Point that)) {
            return false;
        }
        return this.x == that.x && this.y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.x, this.y);
    }
}
