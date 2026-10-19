package build.base.parsing;

/*-
 * #%L
 * base.build Parsing
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
 * A full-grammar parser — Layer A of base-parsing's two parser styles.
 * <p>
 * A {@code Parser<T>} consumes its entire input and produces a {@code T}, or throws a domain-specific
 * exception.  Used by full grammars such as ELParser, JtParser, ModuleInfoParser, AbcParser, and JsonParser.
 * The standard implementation extends {@link AbstractParser}, which provides {@link Scanner} construction,
 * full-input-consumption assertion, exception translation, and grammar helpers.
 * <p>
 * For composable fragments (combinator-style, soft no-match via {@link java.util.Optional#empty()}), see
 * {@link Rule} (Layer B).
 *
 * @param <T> the type of value produced
 * @author reed.vonredwitz
 * @since Apr-2026
 */
@FunctionalInterface
public interface Parser<T> {

    /**
     * Parses the configured input in full and returns the resulting value, throwing a domain-specific
     * exception on failure.
     *
     * @return the parsed value
     */
    T run();
}
