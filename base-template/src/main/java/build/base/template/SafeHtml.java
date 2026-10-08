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

import java.util.Objects;

/**
 * HTML that the caller vouches for, so {@link HtmlOut#write(Object)} emits it without escaping.
 * <p>
 * Only wrap markup that was produced by trusted code or has already been escaped. Never wrap user input.
 *
 * @param html the trusted markup
 * @author reed.vonredwitz
 * @since Oct-2026
 */
public record SafeHtml(String html) {

    public SafeHtml {
        Objects.requireNonNull(html, "html");
    }

    /**
     * Marks markup as trusted.
     *
     * @param html the markup, which must already be safe to emit verbatim
     * @return a {@link SafeHtml} for the markup
     */
    public static SafeHtml trusted(final String html) {
        return new SafeHtml(html);
    }

    @Override
    public String toString() {
        return html;
    }
}
