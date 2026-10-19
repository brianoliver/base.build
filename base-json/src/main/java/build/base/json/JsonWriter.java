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

import java.io.IOException;
import java.io.UncheckedIOException;

final class JsonWriter {

    private JsonWriter() {
    }

    static void write(final JsonValue value, final Appendable out, final JsonFormat format) {
        try {
            write(value, out, format, 0);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(final JsonValue value,
                              final Appendable out,
                              final JsonFormat format,
                              final int depth)
        throws IOException {

        switch (value) {
            case JsonString s -> writeString(s.value(), out);
            case JsonNumber n -> writeNumber(n.toNumber(), out);
            case JsonBoolean b -> out.append(b.value() ? "true" : "false");
            case JsonNull _ -> out.append("null");
            case JsonObject o -> writeObject(o, out, format, depth);
            case JsonArray a -> writeArray(a, out, format, depth);
        }
    }

    private static void writeString(final String value, final Appendable out)
        throws IOException {

        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    private static void writeNumber(final Number number, final Appendable out)
        throws IOException {

        out.append(number.toString());
    }

    private static void writeObject(final JsonObject object,
                                    final Appendable out,
                                    final JsonFormat format,
                                    final int depth)
        throws IOException {

        final var members = object.members();
        if (members.isEmpty()) {
            out.append("{}");
            return;
        }
        final boolean pretty = format == JsonFormat.PRETTY;
        out.append('{');
        boolean first = true;
        for (final var entry : members.entrySet()) {
            if (!first) {
                out.append(',');
            }
            if (pretty) {
                out.append('\n');
                appendIndent(out, depth + 1);
            }
            writeString(entry.getKey(), out);
            out.append(':');
            if (pretty) {
                out.append(' ');
            }
            write(entry.getValue(), out, format, depth + 1);
            first = false;
        }
        if (pretty) {
            out.append('\n');
            appendIndent(out, depth);
        }
        out.append('}');
    }

    private static void writeArray(final JsonArray array,
                                   final Appendable out,
                                   final JsonFormat format,
                                   final int depth)
        throws IOException {

        final var values = array.values();
        if (values.isEmpty()) {
            out.append("[]");
            return;
        }
        final boolean pretty = format == JsonFormat.PRETTY;
        out.append('[');
        boolean first = true;
        for (final var value : values) {
            if (!first) {
                out.append(',');
            }
            if (pretty) {
                out.append('\n');
                appendIndent(out, depth + 1);
            }
            write(value, out, format, depth + 1);
            first = false;
        }
        if (pretty) {
            out.append('\n');
            appendIndent(out, depth);
        }
        out.append(']');
    }

    private static void appendIndent(final Appendable out, final int depth)
        throws IOException {

        for (int i = 0; i < depth; i++) {
            out.append("  ");
        }
    }
}
