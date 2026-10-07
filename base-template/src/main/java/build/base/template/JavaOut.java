package build.base.template;

/*-
 * #%L
 * base.build Template
 * %%
 * Copyright (C) 2026 Workday, Inc.
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

import java.io.Writer;

/**
 * An {@link Out} for templates that generate Java source. Values are written verbatim, and the template syntax uses
 * {@code %} for directives and {@code ${...}} for interpolations, so that annotations ({@code @Override}) and
 * {@code #{...}} in the output need no escaping.
 *
 * @author reed.vonredwitz
 * @since Oct-2026
 */
@OutSyntax(prefix = "%", interpolation = "${")
public final class JavaOut extends Out {

    public JavaOut() {
        super();
    }

    public JavaOut(final Writer writer) {
        super(writer);
    }

    @Override
    public void write(final Object value) {
        if (value != null) {
            raw(String.valueOf(value));
        }
    }
}
