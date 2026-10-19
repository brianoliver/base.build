package build.base.option;

/*-
 * #%L
 * base.build Option
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
 * A {@link ValueOption} representing a host name.
 *
 * @author patrick.peralta
 * @since Jan-2019
 */
public class HostName
    extends AbstractValueOption<String> {

    /**
     * Constructs a {@link HostName}.
     *
     * @param value the non-{@code null} value
     */
    protected HostName(final String value) {
        super(value);
    }

    /**
     * Return an instance of {@link HostName} for the provided host string.
     *
     * @param host the host name
     * @return instance of {@link HostName} for the host
     */
    public static HostName of(final String host) {
        return new HostName(host);
    }

    /**
     * Return an instance of {@link HostName} for localhost.
     *
     * @return instance of {@link HostName} for localhost
     */
    public static HostName localhost() {
        return HostName.of("localhost");
    }
}
