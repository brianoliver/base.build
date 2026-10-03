package build.base.transport.json.example;

/*-
 * #%L
 * base.build Transport (JSON)
 * %%
 * Copyright (C) 2026 Workday Inc
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

import build.base.foundation.Lazy;
import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;

import java.lang.invoke.MethodHandles;

/**
 * A marshallable class whose {@code content} is exposed to {@code @Marshal} as a raw {@link Object} (so it is
 * encoded in the {@code @type}/{@code value} wrapper used for abstract types) but requested by
 * {@code @Unmarshal} as a {@link Lazy}.
 */
public class LazyObjectHolder {

    private final Lazy<Object> content;

    public LazyObjectHolder(final Object content) {
        this.content = Lazy.of(content);
    }

    @Unmarshal
    public LazyObjectHolder(final Lazy<Object> content) {
        this.content = content == null ? Lazy.empty() : content;
    }

    @Marshal
    public void destructor(final Out<Object> content) {
        content.set(this.content.getOrNull());
    }

    public Object content() {
        return this.content.getOrNull();
    }

    static {
        Marshalling.register(LazyObjectHolder.class, MethodHandles.lookup());
    }
}
