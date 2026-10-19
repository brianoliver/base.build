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

import java.util.List;

/**
 * An {@link CollectedOption} {@link Option} representing an {@link Argument}.
 *
 * @author brian.oliver
 * @since Nov-2017
 */
public class Argument
    extends AbstractValueOption<String>
    implements CollectedOption<List> {

    /**
     * Constructs an {@link Argument} for a specific value.
     *
     * @param value the value
     */
    private Argument(final String value) {
        super(value);
    }

    /**
     * Creates an {@link Argument} with a specified value.
     *
     * @param value the value for the {@link Argument}
     * @return the new {@link Argument}
     */
    public static Argument of(final String value) {
        return new Argument(value);
    }
}
