package build.base.foundation.iterator.matching;

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

import build.base.foundation.iterator.ResettableIterator;

import java.util.Iterator;

/**
 * A {@link Matcher} to match the start of an {@link Iterator}.
 *
 * @param <T> the type of elements evaluate
 */
public class StartsThen<T>
    extends AbstractTerm<T> {

    @Override
    public boolean match(final ResettableIterator<T> iterator)
        throws AbortMatchingException {

        if (iterator.mark().isStart()) {
            return true;
        }
        else {
            throw new AbortMatchingException(this);
        }
    }

    @Override
    public String describe() {
        return "starts()";
    }
}
