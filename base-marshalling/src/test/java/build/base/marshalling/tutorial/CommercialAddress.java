package build.base.marshalling.tutorial;

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
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;

import java.lang.invoke.MethodHandles;

/**
 * A commercial {@link PostalAddress}.
 *
 * @author brian.oliver
 * @since Jan-2025
 */
public class CommercialAddress
    extends AbstractPostalAddress {

    @Unmarshal
    public CommercialAddress(final String street,
                             final String city) {

        super(street, city);
    }

    @Marshal
    public void destructor(final Out<String> street,
                           final Out<String> city) {

        super.destructor(street, city);
    }

    static {
        // self-register this type when it's initialized
        Marshalling.register(CommercialAddress.class, MethodHandles.lookup());
    }
}
