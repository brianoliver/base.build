package build.base.network.option;

/*-
 * #%L
 * base.build Network
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

import build.base.configuration.AbstractValueOption;
import build.base.configuration.ValueOption;

/**
 * An {@link ValueOption} used to specify a network port.
 *
 * @author graeme.campbell
 * @since May-2019
 */
public class Port
    extends AbstractValueOption<Integer> {

    /**
     * Creates a {@link Port} specifying which network port to use.
     *
     * @param port the port to connect on
     */
    private Port(final Integer port) {
        super(port);
    }

    /**
     * Creates a new {@link Port} specifying which network port to attempt a connection on
     *
     * @param port the port to attempt a connection on
     * @return a new {@link Port} specifying a connection port
     */
    public static Port of(final Integer port) {
        return new Port(port);
    }
}
