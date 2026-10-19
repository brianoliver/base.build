package build.base.foundation.tuple;

/*-
 * #%L
 * base.build Foundation
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

/**
 * An immutable 3-<a href="https://en.wikipedia.org/wiki/Tuple">Tuple</a>.
 *
 * @param <A> the first value of the {@link Triple}
 * @param <B> the second value of the {@link Triple}
 * @param <C> the third value of the {@link Triple}
 * @author brian.oliver
 * @since Jun-2018
 */
public final class Triple<A, B, C>
    extends AbstractTuple {

    /**
     * Constructs a {@link Triple} with the specified values.
     *
     * @param a the first value
     * @param b the second value
     * @param c the third value
     */
    private Triple(final A a, final B b, final C c) {
        super(a, b, c);
    }

    /**
     * Obtains the first value of the {@link Triple}.
     *
     * @return the first value.
     */
    @SuppressWarnings("unchecked")
    public A first() {
        return (A) get(0);
    }

    /**
     * Obtains the second value of the {@link Triple}.
     *
     * @return the second value.
     */
    @SuppressWarnings("unchecked")
    public B second() {
        return (B) get(1);
    }

    /**
     * Obtains the third value of the {@link Triple}.
     *
     * @return the third value.
     */
    @SuppressWarnings("unchecked")
    public C third() {
        return (C) get(2);
    }

    /**
     * Creates a {@link Triple} with the specified values.
     *
     * @param <A> the first value of the {@link Triple}
     * @param <B> the second value of the {@link Triple}
     * @param <C> the third value of the {@link Triple}
     * @param a   the first value
     * @param b   the second value
     * @param c   the third value
     * @return a new {@link Triple}
     */
    public static <A, B, C> Triple<A, B, C> of(final A a, final B b, final C c) {
        return new Triple<>(a, b, c);
    }
}
