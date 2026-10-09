package build.base.template.processor;

/*-
 * #%L
 * base.build Template Processor
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The delimiters of a template: the prefix that introduces a directive at the start of a line, and the opening
 * delimiter of an interpolation (closed by a right brace).
 * <p>
 * Either can be escaped by doubling: the prefix doubled at the start of a line emits the prefix as text ({@code @@}
 * emits a literal {@code @}), and doubling the first character of the interpolation opener emits the opener as text
 * (<code>##&#123;</code> emits a literal <code>#&#123;</code>).
 *
 * @param prefix        the directive prefix, for example {@code @}
 * @param interpolation the interpolation opener, which ends with a left brace, for example <code>#&#123;</code>
 * @param contexts      the named output contexts of the out type, as context name to the name of the method of the
 *                      out type that implements it; the opener without its brace, a context name and a left brace
 *                      (<code>#url&#123;</code>) writes to a context, and {@code raw} is always one
 * @author reed.vonredwitz
 * @since Oct-2026
 */
record Syntax(String prefix, String interpolation, Map<String, String> contexts) {

    static final Syntax DEFAULT = new Syntax("@", "#{");

    /**
     * The name of the built-in context that writes its value verbatim; it cannot be declared by an out type.
     */
    static final String RAW = "raw";

    /**
     * Names an out type cannot use for a context.
     */
    static final Set<String> RESERVED_CONTEXTS = Set.of("raw", "write", "include");

    /**
     * The directives recognised after the prefix; any other line starting with the prefix is text.
     */
    static final Set<String> DIRECTIVES = Set.of(
        "for", "if", "else", "while", "switch", "case", "default", "do", "try", "catch", "finally",
        "var", "final", "include", "fragment", "endfragment", "slot", "flush", "end", "java");

    /**
     * Directives that are reserved but not implemented yet.
     */
    static final Set<String> RESERVED = Set.of("slot", "flush");

    Syntax(final String prefix, final String interpolation) {
        this(prefix, interpolation, Map.of());
    }

    Syntax {
        final var all = new LinkedHashMap<>(contexts);
        all.put(RAW, RAW);
        contexts = Collections.unmodifiableMap(all);
        if (prefix.isEmpty() || prefix.chars().anyMatch(c -> Character.isWhitespace(c))
            || Character.isJavaIdentifierPart(prefix.charAt(0))) {
            throw new IllegalArgumentException(
                "prefix \"" + prefix
                    + "\" must be non-empty, contain no whitespace and not start with an identifier character");
        }
        if (interpolation.length() < 2 || !interpolation.endsWith("{")
            || interpolation.chars().anyMatch(c -> Character.isWhitespace(c))) {
            throw new IllegalArgumentException(
                "interpolation \"" + interpolation
                    + "\" must end with '{', have at least one other character and contain no whitespace");
        }
    }

    /**
     * The text that, at the start of a line, stands for a literal {@link #prefix()}: the prefix doubled.
     */
    String escapedPrefix() {
        return prefix + prefix;
    }

    /**
     * The text that stands for a literal {@link #interpolation()}.
     */
    String escapedInterpolation() {
        return interpolation.charAt(0) + interpolation;
    }

    /**
     * The same syntax with different delimiters, keeping the contexts.
     */
    Syntax withDelimiters(final String newPrefix, final String newInterpolation) {
        return new Syntax(newPrefix, newInterpolation, contexts);
    }

    /**
     * The text that stands for a literal named opener: the first character of the opener doubled in front of it.
     */
    String escapedContextOpener(final String context) {
        return interpolation.charAt(0) + contextOpener(context);
    }

    /**
     * The opener of a named write to a context, for example <code>#url&#123;</code>.
     */
    String contextOpener(final String context) {
        return interpolationLead() + context + "{";
    }

    /**
     * The opener without its trailing brace, which the expression parser consumes separately.
     */
    String interpolationLead() {
        return interpolation.substring(0, interpolation.length() - 1);
    }
}
