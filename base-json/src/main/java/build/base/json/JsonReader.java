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

import build.base.parsing.AbstractParser;
import build.base.parsing.ParseException;
import build.base.parsing.Rule;
import build.base.parsing.Scanner;

import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class JsonReader extends AbstractParser<JsonValue> {

    private static final int MAX_DEPTH = 500;

    private int depth;
    private final Deque<String> pathSegments = new ArrayDeque<>();

    // Self-referential value rule — the one place Rule.define() earns its keep.
    private final Rule<JsonValue> value = Rule.define(self -> s -> {
        skipWs(s);
        if (!s.hasNext()) {
            throw new ParseException(s.getLocation(), "JSON value", "(end of input)");
        }
        final char c = s.peekChar();
        return switch (c) {
            case '"' -> Optional.of(parseString());
            case '{' -> Optional.of(parseObject(self));
            case '[' -> Optional.of(parseArray(self));
            case 't' -> {
                s.consume("true");
                yield Optional.of(JsonBoolean.TRUE);
            }
            case 'f' -> {
                s.consume("false");
                yield Optional.of(JsonBoolean.FALSE);
            }
            case 'n' -> {
                s.consume("null");
                yield Optional.of(JsonNull.INSTANCE);
            }
            default -> {
                if (c == '-' || isAsciiDigit(c)) {
                    yield Optional.of(parseNumber());
                }
                yield Optional.empty();
            }
        };
    });

    private JsonReader(final String input) {
        super(input);
    }

    private JsonReader(final Reader reader) {
        super(reader);
    }

    static JsonValue read(final String input) {
        return new JsonReader(input).run();
    }

    static JsonValue read(final Reader reader) {
        return new JsonReader(reader).run();
    }

    @Override
    protected void registerFilters(final Scanner s) {
        // JSON whitespace is significant inside strings; skip it explicitly at token boundaries.
    }

    @Override
    protected JsonValue parse() {
        final var result = value.require("JSON value").tryParse(scanner).get();
        skipWs(scanner);
        return result;
    }

    @Override
    protected RuntimeException translate(final ParseException cause) {
        final var loc = cause.getLocation().orElse(null);
        final long line = loc != null ? loc.getLine() : 0;
        final long column = loc != null ? loc.getColumn() : 0;
        final long offset = loc != null ? loc.getOffset() : 0;
        final var msg = "Expected " + cause.getExpected() + " but found " + cause.getFound();
        return new JsonParseException(msg, line, column, offset, currentPath());
    }

    // -------------------------------------------------------------------------
    // Grammar methods
    // -------------------------------------------------------------------------

    private JsonString parseString() {
        scanner.consume("\"");
        final var sb = new StringBuilder();
        while (scanner.hasNext()) {
            final char c = scanner.consumeChar();
            if (c == '"') {
                return JsonString.of(sb.toString());
            }
            if (c == '\\') {
                if (!scanner.hasNext()) {
                    throw new ParseException(scanner.getLocation(), "escape character", "(end of input)");
                }
                final char esc = scanner.consumeChar();
                switch (esc) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> sb.append(parseUnicodeEscape());
                    default -> throw new ParseException(scanner.getLocation(),
                        "valid escape character", String.valueOf(esc));
                }
            } else if (c < 0x20) {
                throw new ParseException(scanner.getLocation(),
                    "printable character", "control character U+" + Integer.toHexString(c));
            } else {
                sb.append(c);
            }
        }
        throw new ParseException(scanner.getLocation(), "'\"'", "(end of input)");
    }

    private char parseUnicodeEscape() {
        final String hex = scanner.consume(4);
        try {
            return (char) Integer.parseInt(hex, 16);
        } catch (final NumberFormatException e) {
            throw new ParseException(scanner.getLocation(), "4 hex digits", hex);
        }
    }

    private JsonNumber parseNumber() {
        final var sb = new StringBuilder();
        scanner.optionallyConsume(c -> c == '-').ifPresent(sb::append);
        if (!scanner.follows(JsonReader::isAsciiDigit)) {
            throw new ParseException(scanner.getLocation(), "digit",
                scanner.hasNext() ? String.valueOf(scanner.peekChar()) : "(end of input)");
        }
        boolean hasDecimalOrExp = false;
        final boolean startsWithZero = scanner.peekChar() == '0';
        sb.append(scanner.consumeChar());
        if (startsWithZero && scanner.follows(JsonReader::isAsciiDigit)) {
            throw new ParseException(scanner.getLocation(), "digit after zero", "leading zero");
        }
        sb.append(scanner.consumeWhile(JsonReader::isAsciiDigit));
        if (scanner.follows('.')) {
            hasDecimalOrExp = true;
            sb.append(scanner.consumeChar());
            if (!scanner.follows(JsonReader::isAsciiDigit)) {
                throw new ParseException(scanner.getLocation(), "digit after decimal point",
                    scanner.hasNext() ? String.valueOf(scanner.peekChar()) : "(end of input)");
            }
            sb.append(scanner.consumeWhile(JsonReader::isAsciiDigit));
        }
        if (scanner.follows(c -> c == 'e' || c == 'E')) {
            hasDecimalOrExp = true;
            sb.append(scanner.consumeChar());
            scanner.optionallyConsume(c -> c == '+' || c == '-').ifPresent(sb::append);
            if (!scanner.follows(JsonReader::isAsciiDigit)) {
                throw new ParseException(scanner.getLocation(), "digit in exponent",
                    scanner.hasNext() ? String.valueOf(scanner.peekChar()) : "(end of input)");
            }
            sb.append(scanner.consumeWhile(JsonReader::isAsciiDigit));
        }
        final var numStr = sb.toString();
        try {
            if (hasDecimalOrExp) {
                return JsonNumber.of(new BigDecimal(numStr));
            }
            try {
                return JsonNumber.of(Long.parseLong(numStr));
            } catch (final NumberFormatException e) {
                return JsonNumber.of(new BigInteger(numStr));
            }
        } catch (final NumberFormatException e) {
            throw new ParseException(scanner.getLocation(), "valid number", numStr);
        }
    }

    private JsonObject parseObject(final Rule<JsonValue> valueRule) {
        if (++depth > MAX_DEPTH) {
            throw new ParseException(scanner.getLocation(), "JSON value", "maximum nesting depth exceeded");
        }
        try {
            scanner.consume("{");
            skipWs(scanner);
            if (scanner.follows('}')) {
                scanner.consumeChar();
                return new JsonObjectImpl(Map.of());
            }
            final var members = new LinkedHashMap<String, JsonValue>();
            boolean first = true;
            while (scanner.hasNext()) {
                if (!first) {
                    skipWs(scanner);
                    scanner.consume(",");
                }
                skipWs(scanner);
                final var key = parseString().value();
                skipWs(scanner);
                scanner.consume(":");
                pathSegments.addLast(key);
                members.put(key, valueRule.require("JSON value").tryParse(scanner).get());
                pathSegments.removeLast();
                first = false;
                skipWs(scanner);
                if (scanner.follows('}')) {
                    scanner.consumeChar();
                    return new JsonObjectImpl(Collections.unmodifiableMap(new LinkedHashMap<>(members)));
                }
            }
            throw new ParseException(scanner.getLocation(), "'}'", "(end of input)");
        } finally {
            depth--;
        }
    }

    private JsonArray parseArray(final Rule<JsonValue> valueRule) {
        if (++depth > MAX_DEPTH) {
            throw new ParseException(scanner.getLocation(), "JSON value", "maximum nesting depth exceeded");
        }
        try {
            scanner.consume("[");
            skipWs(scanner);
            if (scanner.follows(']')) {
                scanner.consumeChar();
                return new JsonArrayImpl(List.of());
            }
            final var values = new ArrayList<JsonValue>();
            boolean first = true;
            int index = 0;
            while (scanner.hasNext()) {
                if (!first) {
                    skipWs(scanner);
                    scanner.consume(",");
                }
                pathSegments.addLast(String.valueOf(index));
                values.add(valueRule.require("JSON value").tryParse(scanner).get());
                pathSegments.removeLast();
                first = false;
                index++;
                skipWs(scanner);
                if (scanner.follows(']')) {
                    scanner.consumeChar();
                    return new JsonArrayImpl(List.copyOf(values));
                }
            }
            throw new ParseException(scanner.getLocation(), "']'", "(end of input)");
        } finally {
            depth--;
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void skipWs(final Scanner s) {
        s.skipWhile(c -> c == ' ' || c == '\t' || c == '\r' || c == '\n');
    }

    private static boolean isAsciiDigit(final int c) {
        return c >= '0' && c <= '9';
    }

    private String currentPath() {
        if (pathSegments.isEmpty()) {
            return null;
        }
        final var sb = new StringBuilder();
        for (final var segment : pathSegments) {
            sb.append('/').append(segment);
        }
        return sb.toString();
    }
}
