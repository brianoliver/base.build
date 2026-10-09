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

import build.base.json.Json;
import build.base.json.JsonFormat;
import build.base.json.JsonValue;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class HtmlOut extends Out {

    public HtmlOut() {
        super();
    }

    public HtmlOut(final Writer writer) {
        super(writer);
    }

    /**
     * Writes a value: a {@link SafeHtml} is emitted verbatim, a {@link Template} is rendered into this {@link HtmlOut},
     * {@code null} writes nothing and anything else is HTML-escaped.
     * <p>
     * A {@link Template} must accept an {@link HtmlOut} (a {@code Template<? super HtmlOut>}); any other kind fails
     * with a {@link ClassCastException}.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void write(final Object value) {
        switch (value) {
            case null -> {
            }
            case SafeHtml safe -> raw(safe.html());
            case Template<?> template -> ((Template<? super HtmlOut>) template).render(this);
            default -> raw(escape(String.valueOf(value)));
        }
    }

    /**
     * Writes a value inside a <em>quoted</em> attribute value (or as text): HTML-escapes it. {@code null} writes
     * nothing. Unlike {@link #write(Object)}, a {@link SafeHtml} or {@link Template} is not treated specially: its
     * {@link Object#toString()} is escaped.
     * <p>
     * Attribute values must be quoted in the template; an unquoted attribute cannot be made safe by escaping.
     *
     * @param value the value
     */
    @OutContext("attr")
    public void writeAttr(final Object value) {
        if (value != null) {
            raw(escape(String.valueOf(value)));
        }
    }

    /**
     * Writes a value as a URL inside a quoted attribute ({@code href}, {@code src}, {@code hx-get}, ...).
     * <p>
     * A URL with a scheme other than {@code http}, {@code https} or {@code mailto} (for example {@code javascript:}
     * or {@code data:}), or containing control characters, is replaced by {@value #UNSAFE_URL}. So is a
     * protocol-relative URL (one starting with {@code //}), which would send the browser to another host. Other
     * relative URLs are allowed. Characters that may not appear in a URL are percent-encoded (existing {@code %}
     * escapes are kept), and the result is attribute-escaped. {@code null} writes nothing.
     * <p>
     * The whole value is treated as a URL. To build a URL from parts, percent-encode each part before concatenating.
     *
     * @param value the URL
     */
    @OutContext("url")
    public void writeUrl(final Object value) {
        if (value != null) {
            raw(escape(sanitizeUrl(String.valueOf(value))));
        }
    }

    /**
     * Writes a value as JSON inside a quoted attribute ({@code hx-vals}, {@code hx-headers}, ...): JSON-encodes it,
     * then attribute-escapes the result.
     * <p>
     * This is also the context for a value inside an attribute that holds JavaScript, such as an event handler
     * ({@code onclick}), Alpine's {@code x-data} or {@code @click}, or htmx's {@code hx-on}: a JSON literal is a
     * JavaScript literal, and the attribute escaping keeps it inside the quotes. Use it rather than
     * {@link #writeJs(Object)}, which is for the body of a {@code <script>} element only.
     * <p>
     * Accepts a {@link JsonValue}, {@code null}, {@link String}, {@link Number}, {@link Boolean}, {@link Map} with
     * {@link String} keys, {@link Iterable} and object array, the last three of which may nest any of these.
     *
     * @param value the value
     * @throws IllegalArgumentException if the value (or a nested value) cannot be represented as JSON
     */
    @OutContext("json")
    public void writeJson(final Object value) {
        raw(escape(toJson(value).toJsonString(JsonFormat.COMPACT)));
    }

    /**
     * Writes a value as a JavaScript literal (the JSON encoding) for use inside an inline {@code <script>}.
     * <p>
     * {@code <}, {@code >} and {@code &} are written as {@code \}{@code u} escapes, so the value cannot close the
     * script element or open a comment ({@code </script}, {@code <!--}); U+2028 and U+2029 are escaped too. Accepts
     * the same values as {@link #writeJson(Object)}. Do not use inside quotes: the output carries its own.
     * <p>
     * Only for the body of a {@code <script>} element. The output is not attribute-escaped and a JSON string contains
     * {@code "}, so inside an attribute (an event handler, {@code x-data}, {@code hx-on}) it would end the attribute
     * value early: use {@link #writeJson(Object)} there.
     *
     * @param value the value
     * @throws IllegalArgumentException if the value (or a nested value) cannot be represented as JSON
     */
    @OutContext("js")
    public void writeJs(final Object value) {
        final var json = toJson(value).toJsonString(JsonFormat.COMPACT);
        final var result = new StringBuilder(json.length() + 8);
        for (int i = 0; i < json.length(); i++) {
            final char c = json.charAt(i);
            switch (c) {
                case '<', '>', '&', ' ', ' ' -> result.append(String.format("\\u%04x", (int) c));
                default -> result.append(c);
            }
        }
        raw(result.toString());
    }

    /**
     * Writes a value as the <em>contents</em> of a CSS string (the surrounding quotes are the template's), in a
     * {@code <style>} element or a {@code style} attribute. Every character other than an ASCII letter, digit, space,
     * {@code -} or {@code _} is written as a CSS hex escape, so the value can neither end the string nor the
     * element, nor break out of the attribute. {@code null} writes nothing.
     *
     * @param value the value
     */
    @OutContext("css")
    public void writeCss(final Object value) {
        if (value == null) {
            return;
        }
        final var s = String.valueOf(value);
        final var result = new StringBuilder(s.length() + 8);
        s.codePoints().forEach(cp -> {
            if (cp < 0x80 && (Character.isLetterOrDigit(cp) || cp == ' ' || cp == '-' || cp == '_')) {
                result.append((char) cp);
            } else {
                result.append('\\').append(Integer.toHexString(cp)).append(' ');
            }
        });
        raw(result.toString());
    }

    /**
     * The replacement for a URL that is not allowed.
     */
    static final String UNSAFE_URL = "#unsafe-url";

    private static String sanitizeUrl(final String url) {
        final var s = url.strip();
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) < 0x20 || s.charAt(i) == 0x7f) {
                return UNSAFE_URL;
            }
        }
        // a protocol-relative URL names another host
        if (s.startsWith("//")) {
            return UNSAFE_URL;
        }
        // a scheme is what precedes a ':' that comes before any '/', '?' or '#'
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                break;
            }
            if (c == ':') {
                final var scheme = s.substring(0, i).toLowerCase(Locale.ROOT);
                if (!scheme.equals("http") && !scheme.equals("https") && !scheme.equals("mailto")) {
                    return UNSAFE_URL;
                }
                break;
            }
        }
        return percentEncode(s);
    }

    private static String percentEncode(final String s) {
        final var result = new StringBuilder(s.length() + 8);
        for (final byte b : s.getBytes(StandardCharsets.UTF_8)) {
            final int c = b & 0xff;
            if (c < 0x80 && (Character.isLetterOrDigit(c) || "-._~:/?#[]@!$&()*+,;=%".indexOf(c) >= 0)) {
                result.append((char) c);
            } else {
                result.append('%').append(String.format("%02X", c));
            }
        }
        return result.toString();
    }

    private static JsonValue toJson(final Object value) {
        return switch (value) {
            case JsonValue json -> json;
            case Map<?, ?> map -> {
                final var members = new LinkedHashMap<String, JsonValue>();
                map.forEach((k, v) -> {
                    if (!(k instanceof String name)) {
                        throw new IllegalArgumentException("JSON object keys must be strings, not " + k);
                    }
                    members.put(name, toJson(v));
                });
                yield Json.object(members);
            }
            case Iterable<?> items -> {
                final var elements = new ArrayList<JsonValue>();
                items.forEach(item -> elements.add(toJson(item)));
                yield Json.array(elements);
            }
            case Object[] items -> toJson(Arrays.asList(items));
            case null, default -> Json.of(value);
        };
    }

    private static String escape(final String s) {
        StringBuilder result = null;
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            final String replacement = switch (c) {
                case '&' -> "&amp;";
                case '<' -> "&lt;";
                case '>' -> "&gt;";
                case '"' -> "&quot;";
                case '\'' -> "&#39;";
                default -> null;
            };
            if (replacement != null) {
                if (result == null) {
                    result = new StringBuilder(s.length() + 8);
                    result.append(s, 0, i);
                }
                result.append(replacement);
            } else if (result != null) {
                result.append(c);
            }
        }
        return result != null ? result.toString() : s;
    }
}
