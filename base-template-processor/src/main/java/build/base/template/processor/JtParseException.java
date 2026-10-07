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

/**
 * Thrown when a {@code .jt} file is malformed.
 * <p>
 * The {@link #getLine() line} and {@link #getColumn() column} are one-based, or {@code 0} when unknown.
 */
final class JtParseException extends RuntimeException {

    private final String file;
    private final int line;
    private final int column;

    JtParseException(final String message) {
        this(message, null, 0, 0);
    }

    JtParseException(final String message, final String file, final int line, final int column) {
        super(message);
        this.file = file;
        this.line = line;
        this.column = column;
    }

    /** The {@code .jt} file in which the error occurred, or {@code null} when unknown. */
    String getFile() {
        return file;
    }

    int getLine() {
        return line;
    }

    int getColumn() {
        return column;
    }
}
