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

import build.base.io.LookaheadReader;
import build.base.parsing.tokenparsers.TokenParser;

/**
 * Represents a token extracted from an expression string.
 *
 * @param <N>         the node type produced by the parser
 * @param tokenParser the {@link TokenParser} that produced this token
 * @param location    the location of the token in the source expression
 * @param value       the raw text of the token
 *
 * @author tim.berston
 * @since Nov-2024
 */
public record Token<N>(TokenParser<N> tokenParser, LookaheadReader.Location location, String value) {

    /**
     * Returns the token value with the first and last characters removed (useful for stripping quotes).
     *
     * @return the trimmed value
     */
    public String trimFirstAndLast() {
        return this.value.substring(1, this.value.length() - 1);
    }
}
