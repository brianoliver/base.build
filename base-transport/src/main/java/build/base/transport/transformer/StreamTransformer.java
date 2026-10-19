package build.base.transport.transformer;

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

import build.base.foundation.Introspection;
import build.base.foundation.stream.Streamable;
import build.base.marshalling.Marshaller;
import build.base.transport.Transformer;

import java.lang.reflect.Type;
import java.util.stream.Stream;

/**
 * A {@link Transformer} for {@link Stream} types, transforming them to and from {@link Streamable}s.
 *
 * @param <T> the type of {@link Stream} value
 * @author tim.berston
 * @since Feb-2025
 */
public class StreamTransformer<T>
    implements Transformer<Stream<T>, Streamable<T>> {

    @Override
    public Class<?> sourceClass() {
       return Stream.class;
    }

    @Override
    public Class<?> targetClass() {
        return Streamable.class;
    }

    @Override
    public Streamable<T> transform(final Marshaller marshaller,
                                   final Stream<T> value) {

        return value == null ? null : Streamable.of(value);
    }

    @Override
    public Stream<T> reform(final Marshaller marshaller,
                            final Type type,
                            final Streamable<T> streamable) {

        Introspection.getClassFromType(type)
            .filter(Stream.class::isAssignableFrom)
            .orElseThrow(() -> new ClassCastException("The specified type [" + type + "] is not a Stream<?>"));

        return streamable.stream();
    }
}
