package build.base.transport;

/*-
 * #%L
 * base.build Transport
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

import build.base.transport.transformer.EnumTransformer;
import build.base.transport.transformer.StreamTransformer;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

/**
 * An {@code abstract} {@link Transport} implementation.
 *
 * @param <T> the type of {@link Transport}
 * @author brian.oliver
 * @since Nov-2024
 */
public abstract class AbstractTransport<T extends Transport>
    implements Transport {

    /**
     * The {@link Transformer}s to use to transform/reform unmarshallable types of values.
     */
    private final CopyOnWriteArraySet<Transformer<?, ?>> transformers;

    /**
     * Constructs an {@link AbstractTransport}.
     */
    protected AbstractTransport() {
        this.transformers = new CopyOnWriteArraySet<>();

        register(new EnumTransformer<>());
        register(new StreamTransformer<>());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X, Y> T register(final Transformer<X, Y> transformer) {
        Objects.requireNonNull(transformer, "The transformer must not be null");
        this.transformers.add(transformer);
        return (T) this;
    }

    @Override
    public Stream<Transformer<?, ?>> transformers() {
        return this.transformers.stream();
    }
}
