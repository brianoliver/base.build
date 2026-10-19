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
 * An immutable 2-<a href="https://en.wikipedia.org/wiki/Tuple">Tuple</a>.
 *
 * @param <A> the first value of the {@link Pair}
 * @param <B> the second value of the {@link Pair}
 * @author brian.oliver
 * @since Jun-2018
 */
public final class Pair<A, B>
    extends AbstractTuple {

    /**
     * Constructs a {@link Pair} with the specified values.
     *
     * @param a the first value
     * @param b the second value
     */
    private Pair(final A a, final B b) {
        super(a, b);
    }

    /**
     * Obtains the first value of the {@link Pair}.
     *
     * @return the first value.
     */
    public A first() {
        return get(0);
    }

    /**
     * Obtains the second value of the {@link Pair}.
     *
     * @return the second value.
     */
    public B second() {
        return get(1);
    }

    /**
     * Creates a {@link Pair} with the specified values.
     *
     * @param <A> the first value of the {@link Pair}
     * @param <B> the second value of the {@link Pair}
     * @param a   the first value
     * @param b   the second value
     * @return a new {@link Pair}
     */
    public static <A, B> Pair<A, B> of(final A a, final B b) {
        return new Pair<>(a, b);
    }
}
