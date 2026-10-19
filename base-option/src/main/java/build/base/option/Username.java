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
import build.base.configuration.Default;
import build.base.configuration.ValueOption;

/**
 * {@link ValueOption} used to specify a username when communicating across networks.
 *
 * @author graeme.campbell
 * @since May-2019
 */
public class Username
    extends AbstractValueOption<String> {

    /**
     * Creates a new {@link Username} from a {@link String}.
     *
     * @param username the username being created.
     */
    private Username(final String username) {
        super(username);
    }

    /**
     * Creates a new {@link Username}.
     *
     * @param username the username to be used
     * @return a {@link Username} containing the username provided
     */
    public static Username of(final String username) {
        return new Username(username);
    }

    /**
     * Create a default {@link Username} based on the current user for this session.
     *
     * @return a {@link Username} with the username of the current user based {@code user.name}
     */
    @Default
    @SuppressWarnings("unused")
    public static Username ofCurrentUser() {
        return Username.of(System.getProperty("user.name"));
    }
}
