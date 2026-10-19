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

import build.base.configuration.Default;
import build.base.configuration.Option;

/**
 * An {@link Option} specifying whether TCP-NoDelay is required.
 *
 * @author graeme.campbell
 * @see <a href="https://en.wikipedia.org/wiki/Nagle%27s_algorithm">Nagles Algorithm</a>
 * @since Mar-2019
 */
public enum TCPNoDelay
    implements Option {
    /**
     * Socket option TCP_NODELAY is on.
     */
    YES,

    /**
     * Socket option TCP_NODELAY is off.
     */
    @Default
    NO
}
