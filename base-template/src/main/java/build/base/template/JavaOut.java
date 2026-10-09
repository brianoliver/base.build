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
import java.util.Set;

/**
 * An {@link Out} for templates that generate Java source. The template syntax uses {@code %} for directives and
 * {@code ${...}} for interpolations, so that annotations ({@code @Override}) and {@code #{...}} in the output need no
 * escaping.
 * <p>
 * {@code ${expr}} ({@link #write(Object)}) writes its value <em>verbatim</em>: it is for code the template author
 * trusts (a type name, an expression). The named contexts are the safe forms for data: <code>$str&#123;expr&#125;</code>,
 * <code>$id&#123;expr&#125;</code> and <code>$javadoc&#123;expr&#125;</code>.
 *
 * @author reed.vonredwitz
 * @since Oct-2026
 */
@OutSyntax(prefix = "%", interpolation = "${")
public final class JavaOut extends Out {

    /**
     * The words that cannot be used as an identifier.
     */
    private static final Set<String> RESERVED = Set.of(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue",
        "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if",
        "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private",
        "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
        "throw", "throws", "transient", "try", "void", "volatile", "while", "_", "true", "false", "null");

    public JavaOut() {
        super();
    }

    public JavaOut(final Writer writer) {
        super(writer);
    }

    /**
     * Writes a value verbatim; {@code null} writes nothing. Use the named contexts for data that is not trusted code.
     */
    @Override
    public void write(final Object value) {
        if (value != null) {
            raw(String.valueOf(value));
        }
    }

    /**
     * Writes a value as the <em>contents</em> of a string or character literal (the surrounding quotes are the
     * template's). {@code \}, {@code "} and {@code '} are backslash-escaped, line breaks and tabs use their named
     * escapes, and other control characters are written as octal escapes, which (unlike {@code \}{@code u} escapes) are
     * not translated before the source is tokenised, so no value can end the literal or the line. U+2028, U+2029 and
     * unpaired surrogates are written as {@code \}{@code u} escapes. {@code null} writes nothing.
     * <p>
     * Not for text blocks, which have different rules.
     *
     * @param value the value
     */
    @OutContext("str")
    public void writeStr(final Object value) {
        if (value == null) {
            return;
        }
        final var s = String.valueOf(value);
        final var result = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            switch (c) {
                case '\\' -> result.append("\\\\");
                case '"' -> result.append("\\\"");
                case '\'' -> result.append("\\'");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                default -> {
                    if (c < 0x20 || c == 0x7f) {
                        result.append(String.format("\\%03o", (int) c));
                    } else if (c == ' ' || c == ' ') {
                        result.append(String.format("\\u%04x", (int) c));
                    } else if (Character.isHighSurrogate(c) && i + 1 < s.length()
                        && Character.isLowSurrogate(s.charAt(i + 1))) {
                        result.append(c).append(s.charAt(++i));
                    } else if (Character.isSurrogate(c)) {
                        result.append(String.format("\\u%04x", (int) c));
                    } else {
                        result.append(c);
                    }
                }
            }
        }
        raw(result.toString());
    }

    /**
     * Writes a value as a Java identifier, which it must already be: non-empty, a valid identifier, and not a
     * reserved word, literal or {@code _}. An identifier is never altered, because two different inputs mapped to the
     * same name would silently produce wrong code.
     *
     * @param value the identifier
     * @throws IllegalArgumentException if the value is {@code null} or not a valid identifier
     */
    @OutContext("id")
    public void writeId(final Object value) {
        final var s = value == null ? null : String.valueOf(value);
        if (!isIdentifier(s)) {
            throw new IllegalArgumentException("not a valid Java identifier: " + s);
        }
        raw(s);
    }

    private static boolean isIdentifier(final String s) {
        if (s == null || s.isEmpty() || RESERVED.contains(s)) {
            return false;
        }
        final int first = s.codePointAt(0);
        if (!Character.isJavaIdentifierStart(first) || Character.isIdentifierIgnorable(first)) {
            return false;
        }
        return s.codePoints().skip(1).allMatch(cp ->
            Character.isJavaIdentifierPart(cp) && !Character.isIdentifierIgnorable(cp));
    }

    /**
     * Writes a value as text inside a Javadoc or block comment. The value is HTML-escaped (Javadoc is HTML), and
     * {@code @}, {@code \} and a {@code /} that follows {@code *} are written as character references, so the value can
     * neither close the comment, start a tag or inline tag, nor smuggle in a {@code \}{@code u} escape (which Java
     * translates inside comments too, so {@code \}{@code u002a/} would end one). A {@code /} that starts the value is
     * escaped too, as the template text before it may end in {@code *} and the output cannot see that.
     * {@code null} writes nothing.
     * <p>
     * Only for a block or Javadoc comment. In a {@code //} comment a line break in the value would end the comment
     * and leave the rest as code.
     * <p>
     * Line prefixes (<code> * </code>) are the template's concern; a multi-line value is written as it is.
     *
     * @param value the value
     */
    @OutContext("javadoc")
    public void writeJavadoc(final Object value) {
        if (value == null) {
            return;
        }
        final var s = String.valueOf(value);
        final var result = new StringBuilder(s.length() + 8);
        // The text before the value is the template's and may end in '*', so a leading '/' is escaped too
        char previous = '*';
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            switch (c) {
                case '&' -> result.append("&amp;");
                case '<' -> result.append("&lt;");
                case '>' -> result.append("&gt;");
                case '@' -> result.append("&#64;");
                case '\\' -> result.append("&#92;");
                case '/' -> result.append(previous == '*' ? "&#47;" : "/");
                default -> result.append(c);
            }
            previous = c;
        }
        raw(result.toString());
    }
}
