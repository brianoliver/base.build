package build.base.json;

/*-
 * #%L
 * base.build JSON
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
 * Thrown when JSON input cannot be parsed.
 * <p>
 * Carries the exact position (line, column, offset) and a JSON-pointer-style path to the value being parsed when
 * the error occurred.
 */
public final class JsonParseException extends RuntimeException {

    private final long line;
    private final long column;
    private final long offset;
    private final String path;

    JsonParseException(final String message,
                       final long line,
                       final long column,
                       final long offset,
                       final String path) {

        super(buildMessage(message, line, column, offset, path));
        this.line = line;
        this.column = column;
        this.offset = offset;
        this.path = path;
    }

    private static String buildMessage(final String message,
                                       final long line,
                                       final long column,
                                       final long offset,
                                       final String path) {

        final var sb = new StringBuilder(message)
            .append(" (line ").append(line)
            .append(", column ").append(column)
            .append(", offset ").append(offset)
            .append(')');
        if (path != null) {
            sb.append(" at ").append(path);
        }
        return sb.toString();
    }

    /**
     * 1-based line number where the error occurred.
     */
    public long line() {
        return line;
    }

    /**
     * 1-based column number where the error occurred.
     */
    public long column() {
        return column;
    }

    /**
     * 0-based character offset from the start of input where the error occurred.
     */
    public long offset() {
        return offset;
    }

    /**
     * JSON-pointer-style path to the value being parsed, or {@code null} at the root.
     */
    public String path() {
        return path;
    }
}
