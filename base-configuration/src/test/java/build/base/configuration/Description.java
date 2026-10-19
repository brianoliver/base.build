package build.base.configuration;

/*-
 * #%L
 * base.build Configuration
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
 * An {@link Option} defining a {@link Description}.
 *
 * @author brian.oliver
 * @since Jan-2019
 */
public class Description
    extends AbstractValueOption<String> {

    /**
     * Constructs a {@link Description}.
     *
     * @param description the description
     */
    private Description(final String description) {
        super(description);
    }

    /**
     * Creates a {@link Description} given a specified {@link String}
     *
     * @param description the description
     * @return a new {@link Description}
     */
    public static Description of(final String description) {
        return new Description(description);
    }
}
