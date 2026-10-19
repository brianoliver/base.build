package build.base.foundation;

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

import java.lang.ref.Reference;

/**
 * An abstract {@link Reference}-like wrapper for a key which uses a {@link Hasher} to compute its hash and equality.
 *
 * @param <T> the type
 * @author mark.falco
 * @author brian.oliver
 * @since May.2022
 */
public abstract class AbstractHasherReference<T> {

    /**
     * The key.
     */
    protected final T key;

    /**
     * Cached hash of the key.
     */
    protected final int hash;

    /**
     * Constructs an {@link AbstractHasherReference}.
     *
     * @param referent the object being referred to
     * @param hasher   the hasher to hash with
     */
    protected AbstractHasherReference(final T referent,
                                      final Hasher<? super T> hasher) {

        this.key = referent;
        this.hash = hasher.hashCode(referent);
    }

    /**
     * @return the underlying key.
     */
    public T get() {
        return this.key;
    }

    /**
     * Return the hasher associated with this {@link Hasher}
     *
     * @return the hasher
     */
    abstract protected Hasher<? super T> getHasher();

    @Override
    public int hashCode() {
        return this.hash;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof AbstractHasherReference<?>
            && getHasher().equals(((AbstractHasherReference<T>) obj).get(), get()));
    }
}
