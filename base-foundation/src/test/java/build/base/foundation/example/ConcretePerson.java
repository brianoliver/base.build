package build.base.foundation.example;

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

import build.base.foundation.Introspection;

/**
 * A non-abstract person for testing {@link Introspection}.
 *
 * @author brian.oliver
 * @since Jul-2018
 */
public class ConcretePerson
    extends AbstractPerson {

    /**
     * Was the {@link #onNonAbstractPostInject()} invoked?
     */
    private boolean onNonAbstractPostInjectInvoked;

    /**
     * Constructs a person.
     *
     * @param firstName the first name of the person
     * @param lastName  the last name of the person
     */
    public ConcretePerson(final String firstName, final String lastName) {
        super(firstName, lastName);

        this.onNonAbstractPostInjectInvoked = false;
    }

    /**
     * Invoked after injection of the {@link ConcretePerson}.
     */
    public void onNonAbstractPostInject() {
        this.onNonAbstractPostInjectInvoked = true;
    }

    /**
     * Determines if {@link #onNonAbstractPostInject()} was been invoked.
     *
     * @return {@code true} when invoked, {@code false} otherwise
     */
    public boolean isNonAbstractPostInjectInvoked() {
        return this.onNonAbstractPostInjectInvoked;
    }
}
