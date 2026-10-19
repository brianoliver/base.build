package build.base.version;

/*-
 * #%L
 * base.build Version
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An immutable, structured version of the form {@code sequence [-prerelease] [+build]}.
 * <p>
 * The <em>sequence</em> is one or more elements separated by {@code .} or by transitions between
 * numeric and alphabetic characters (e.g. {@code 1.0RC1} → {@code [1, 0, RC, 1]}).
 * <p>
 * The optional <em>prerelease</em> segment follows the first {@code -} and precedes any {@code +}.
 * A version without a prerelease segment compares as greater than one with the same sequence but
 * a prerelease segment (e.g. {@code 1.0 > 1.0-SNAPSHOT}).
 * <p>
 * The optional <em>build</em> segment follows {@code +} and is compared last.
 * <p>
 * No qualifier policy is applied: string elements are compared lexicographically.
 * Callers that need qualifier ordering (e.g. {@code alpha < beta}) should use
 * {@link VersionOrder#MAVEN}; callers that need SemVer-compatible ordering should use
 * {@link VersionOrder#SEMVER}.
 * <p>
 * The format is inspired by <a href="https://semver.org">Semantic Versioning 2.0.0</a> but
 * intentionally relaxed: the sequence is not restricted to three dot-separated integers,
 * build metadata is included in {@link #compareTo} (SemVer §10 requires it to be ignored —
 * use {@link VersionOrder#SEMVER} for spec-compliant comparison), and alpha/digit transitions
 * within a segment create implicit element boundaries (e.g. {@code 1.0RC1} → {@code [1, 0, RC, 1]}).
 * These relaxations make the type suitable for Maven-ecosystem versions such as {@code 2.13},
 * {@code 1.0RC2}, and {@code 4.5.1.Final}.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public final class Version
    implements Comparable<Version> {

    /**
     * The raw version string as supplied to {@link #parse(String)}.
     */
    private final String raw;

    /**
     * The sequence elements (before {@code -} or {@code +}).
     */
    private final List<Element> sequence;

    /**
     * The pre-release elements (between the first {@code -} and any {@code +}).
     */
    private final List<Element> prerelease;

    /**
     * The build-metadata elements (after {@code +}).
     */
    private final List<Element> build;

    /**
     * An immutable element of a {@link Version}, holding either an {@link Integer} or a {@link String} value.
     * <p>
     * Two elements of the same type are compared naturally; mixed-type pairs fall back to string comparison.
     */
    public static final class Element
        implements Comparable<Element> {

        private final Object value;

        private Element(final int value) {
            this.value = value;
        }

        private Element(final String value) {
            this.value = value;
        }

        /**
         * Obtains the value of this element, either an {@link Integer} or a {@link String}.
         *
         * @return the value
         */
        public Object getValue() {
            return this.value;
        }

        @Override
        public int compareTo(final Element other) {
            if (this.value instanceof Integer i && other.value instanceof Integer j) {
                return Integer.compare(i, j);
            }
            if (this.value instanceof String s && other.value instanceof String t) {
                return s.compareTo(t);
            }
            return this.value.toString().compareTo(other.value.toString());
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Element)) {
                return false;
            }
            return compareTo((Element) obj) == 0;
        }

        @Override
        public int hashCode() {
            // Use the string representation so that mixed-type pairs that compare equal
            // (e.g. Integer(1) and String("1") via the toString fallback in compareTo)
            // produce the same hash code, satisfying the Object.hashCode contract.
            return this.value.toString().hashCode();
        }

        @Override
        public String toString() {
            return this.value.toString();
        }
    }

    private Version(final String raw,
                    final List<Element> sequence,
                    final List<Element> prerelease,
                    final List<Element> build) {

        this.raw = raw;
        this.sequence = Collections.unmodifiableList(sequence);
        this.prerelease = Collections.unmodifiableList(prerelease);
        this.build = Collections.unmodifiableList(build);
    }

    /**
     * Parses a version string into a {@link Version}, returning an empty {@link Optional}
     * instead of throwing for any invalid input.
     *
     * @param raw the version string
     * @return an {@link Optional} containing the parsed version, or empty if the input is invalid
     */
    public static Optional<Version> tryParse(final String raw) {
        try {
            return Optional.of(parse(raw));
        } catch (final IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Parses a version string into a {@link Version}.
     *
     * @param raw the version string
     * @return the parsed {@link Version}
     * @throws IllegalArgumentException if the string is null, empty, or malformed
     */
    public static Version parse(final String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Version string must not be null");
        }
        if (raw.isBlank()) {
            throw new IllegalArgumentException("Version string must not be empty");
        }

        // split off build metadata first (after '+')
        final int plusIndex = raw.indexOf('+');
        final String versionAndPrerelease = plusIndex >= 0 ? raw.substring(0, plusIndex) : raw;
        final String buildString = plusIndex >= 0 ? raw.substring(plusIndex + 1) : "";

        // split sequence from prerelease (at first '-')
        final int dashIndex = versionAndPrerelease.indexOf('-');
        final String sequenceString = dashIndex >= 0
            ? versionAndPrerelease.substring(0, dashIndex)
            : versionAndPrerelease;
        final String prereleaseString = dashIndex >= 0
            ? versionAndPrerelease.substring(dashIndex + 1)
            : "";

        if (sequenceString.isEmpty()) {
            throw new IllegalArgumentException("Version sequence must not be empty: " + raw);
        }

        final List<Element> sequence = parseElements(sequenceString, true);
        final List<Element> prerelease = prereleaseString.isEmpty()
            ? Collections.emptyList()
            : parseElements(prereleaseString, false);
        final List<Element> build = buildString.isEmpty()
            ? Collections.emptyList()
            : parseElements(buildString, false);

        return new Version(raw, sequence, prerelease, build);
    }

    /**
     * Parses a dot-delimited (and optionally alpha/digit-transition-delimited) segment string
     * into a list of {@link Element}s.
     *
     * @param s              the segment string
     * @param requireNumeric whether the first element must begin with a digit
     * @return the parsed elements
     */
    private static List<Element> parseElements(final String s, final boolean requireNumeric) {
        if (requireNumeric && !Character.isDigit(s.charAt(0))) {
            throw new IllegalArgumentException("Version sequence must begin with a digit: " + s);
        }
        final List<Element> elements = new ArrayList<>();
        int start = 0;

        for (int i = 0; i <= s.length(); i++) {
            final boolean end = i == s.length();
            final char c = end ? 0 : s.charAt(i);

            if (end || c == '.' || c == '-') {
                if (i > start) {
                    elements.add(toElement(s.substring(start, i)));
                }
                start = i + 1;
            } else if (i > start) {
                final char prev = s.charAt(i - 1);
                if (Character.isDigit(prev) != Character.isDigit(c)) {
                    elements.add(toElement(s.substring(start, i)));
                    start = i;
                }
            }
        }

        return elements;
    }

    private static Element toElement(final String s) {
        // strip leading zeros for numeric elements
        if (!s.isEmpty() && Character.isDigit(s.charAt(0))) {
            try {
                return new Element(Integer.parseInt(s));
            } catch (final NumberFormatException e) {
                // too large for int — treat as string (rare for real versions)
                return new Element(s);
            }
        }
        return new Element(s);
    }

    /**
     * Obtains the raw version string.
     *
     * @return the raw version string
     */
    public String get() {
        return this.raw;
    }

    /**
     * Obtains the sequence elements (the dotted-numeric part before any {@code -} or {@code +}).
     *
     * @return unmodifiable list of sequence elements
     */
    public List<Element> sequence() {
        return this.sequence;
    }

    /**
     * Obtains the pre-release elements (the part after the first {@code -} and before any {@code +}).
     *
     * @return unmodifiable list of pre-release elements; empty if no pre-release segment
     */
    public List<Element> prerelease() {
        return this.prerelease;
    }

    /**
     * Obtains the build-metadata elements (the part after {@code +}).
     *
     * @return unmodifiable list of build-metadata elements; empty if no build segment
     */
    public List<Element> build() {
        return this.build;
    }

    @Override
    public int compareTo(final Version other) {
        // 1. compare sequences, treating trailing missing elements as zero
        int cmp = compareElementLists(this.sequence, other.sequence);
        if (cmp != 0) {
            return cmp;
        }

        // 2. no prerelease > has prerelease (structural rule, not a qualifier policy)
        final boolean thisHasPre = !this.prerelease.isEmpty();
        final boolean otherHasPre = !other.prerelease.isEmpty();
        if (thisHasPre != otherHasPre) {
            return thisHasPre ? -1 : 1;
        }

        // 3. compare prerelease elements
        cmp = compareElementLists(this.prerelease, other.prerelease);
        if (cmp != 0) {
            return cmp;
        }

        // 4. compare build metadata
        return compareElementLists(this.build, other.build);
    }

    private static int compareElementLists(final List<Element> a, final List<Element> b) {
        final int common = Math.min(a.size(), b.size());
        for (int i = 0; i < common; i++) {
            final int c = a.get(i).compareTo(b.get(i));
            if (c != 0) {
                return c;
            }
        }
        // trailing elements compared against zero
        if (a.size() != b.size()) {
            final List<Element> longer = a.size() > b.size() ? a : b;
            final int sign = a.size() > b.size() ? 1 : -1;
            for (int i = common; i < longer.size(); i++) {
                final Element e = longer.get(i);
                if (e.value instanceof Integer n && n != 0) {
                    return sign;
                }
                if (e.value instanceof String s && !s.isEmpty()) {
                    return sign;
                }
            }
        }
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Version)) {
            return false;
        }
        return compareTo((Version) obj) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(normalize(this.sequence), normalize(this.prerelease), normalize(this.build));
    }

    /**
     * Returns the sublist of elements with trailing zero integers removed,
     * matching the equality semantics of {@link #compareElementLists}.
     */
    private static List<Element> normalize(final List<Element> elements) {
        int last = elements.size();
        while (last > 0) {
            final Element e = elements.get(last - 1);
            if (e.value instanceof Integer i && i == 0) {
                last--;
            } else {
                break;
            }
        }
        return elements.subList(0, last);
    }

    @Override
    public String toString() {
        return this.raw;
    }
}
