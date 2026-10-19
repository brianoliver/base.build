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

import java.util.Set;


/**
 * An {@code abstract} {@link Set} which stores its entries in another {@link Set} in a converted form.
 *
 * @param <T> the set entry type
 * @param <U> the underlying entry type
 * @author mark.falco
 * @author brian.oliver
 * @since March-2022
 */
public abstract class AbstractConverterSet<T, U>
    extends AbstractConverterCollection<T, U>
    implements Set<T> {

    /**
     * Constructs an {@link AbstractConverterSet}.
     *
     * @param underlying the underlying {@link Set}
     */
    protected AbstractConverterSet(final Set<U> underlying) {
        super(underlying);
    }
}
