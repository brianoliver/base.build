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
import java.util.function.Predicate;

/**
 * One or more {@link VersionRange}s OR-combined into a single constraint.
 * <p>
 * This is the primary entry point for parsing the full Maven range syntax, including
 * disjoint unions such as {@code (,1.0],[1.2,)} and {@code (,1.1),(1.1,)}.
 *
 * <table>
 * <caption>Examples</caption>
 * <tr><th>Syntax</th><th>Meaning</th></tr>
 * <tr><td>{@code 1.0}</td><td>exactly 1.0</td></tr>
 * <tr><td>{@code [1.0]}</td><td>exactly 1.0</td></tr>
 * <tr><td>{@code [1.2,1.3]}</td><td>1.2 &le; v &le; 1.3</td></tr>
 * <tr><td>{@code [1.0,2.0)}</td><td>1.0 &le; v &lt; 2.0</td></tr>
 * <tr><td>{@code [1.5,)}</td><td>v &ge; 1.5</td></tr>
 * <tr><td>{@code (,1.0]}</td><td>v &le; 1.0</td></tr>
 * <tr><td>{@code (,1.0)}</td><td>v &lt; 1.0</td></tr>
 * <tr><td>{@code (1.0,2.0]}</td><td>1.0 &lt; v &le; 2.0</td></tr>
 * <tr><td>{@code (1.0,2.0)}</td><td>1.0 &lt; v &lt; 2.0</td></tr>
 * <tr><td>{@code (1.0,)}</td><td>v &gt; 1.0</td></tr>
 * <tr><td>{@code (,1.0],[1.2,)}</td><td>v &le; 1.0 or v &ge; 1.2</td></tr>
 * <tr><td>{@code (,1.1),(1.1,)}</td><td>v &ne; 1.1</td></tr>
 * </table>
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public record VersionConstraint(List<VersionRange> ranges)
    implements Predicate<Version> {

    /**
     * Constructs a {@link VersionConstraint} from a list of {@link VersionRange}s.
     * A version is contained if it satisfies at least one range.
     *
     * @param ranges the ranges (must not be null or empty)
     */
    public VersionConstraint(final List<VersionRange> ranges) {
        Objects.requireNonNull(ranges, "ranges must not be null");
        if (ranges.isEmpty()) {
            throw new IllegalArgumentException("ranges must not be empty");
        }
        this.ranges = Collections.unmodifiableList(new ArrayList<>(ranges));
    }

    /**
     * Parses a Maven range constraint string into a {@link VersionConstraint}.
     *
     * @param s the constraint string
     * @return the parsed {@link VersionConstraint}
     * @throws IllegalArgumentException if the string is null, empty, or malformed
     */
    public static VersionConstraint parse(final String s) {
        if (s == null) {
            throw new IllegalArgumentException("Constraint string must not be null");
        }
        final String trimmed = s.strip();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Constraint string must not be empty");
        }

        // bare version — exact match
        if (!trimmed.startsWith("[") && !trimmed.startsWith("(")) {
            return new VersionConstraint(List.of(VersionRange.of(Version.parse(trimmed))));
        }

        // bracket-prefixed single range: [v], [v,u], [v,u), [v,)
        if (trimmed.startsWith("[")) {
            return new VersionConstraint(List.of(VersionRange.parse(trimmed)));
        }

        // upper-only or disjoint: (,v] or (,v) — possibly followed by a second range
        if (trimmed.startsWith("(,")) {
            return parseUpperOnlyConstraint(trimmed);
        }

        // exclusive-lower-bounded single range: (v,u], (v,u), (v,)
        return new VersionConstraint(List.of(VersionRange.parse(trimmed)));
    }

    /**
     * Obtains the ranges that make up this constraint.
     *
     * @return unmodifiable list of {@link VersionRange}s
     */
    @Override
    public List<VersionRange> ranges() {
        return this.ranges;
    }

    /**
     * Determines whether the given {@link Version} satisfies this constraint.
     *
     * @param version the version to test
     * @return {@code true} if any range in this constraint contains the version
     */
    public boolean contains(final Version version) {
        return test(version);
    }

    @Override
    public boolean test(final Version version) {
        if (version == null) {
            return false;
        }
        return this.ranges.stream().anyMatch(r -> r.test(version));
    }

    @Override
    public String toString() {
        final var sb = new StringBuilder();
        for (final VersionRange range : this.ranges) {
            if (!sb.isEmpty()) {
                sb.append(',');
            }
            sb.append(range);
        }
        return sb.toString();
    }

    // ── Private parsing ───────────────────────────────────────────────────────

    /**
     * Parses a constraint that begins with {@code (,v]} or {@code (,v)}, optionally
     * followed by a second disjoint range.
     *
     * <ul>
     *   <li>{@code (,1.0]}       — single upper-bounded range</li>
     *   <li>{@code (,1.0)}       — single strictly-less-than range (upper bound set unconditionally)</li>
     *   <li>{@code (,1.0],[1.2,)} — disjoint: v &le; 1.0 or v &ge; 1.2</li>
     *   <li>{@code (,1.1),(1.1,)} — disjoint: v &ne; 1.1</li>
     * </ul>
     */
    private static VersionConstraint parseUpperOnlyConstraint(final String s) {
        if (!s.startsWith("(,")) {
            throw new IllegalArgumentException("Invalid constraint: " + s);
        }

        // find the closing bracket of the first range
        final int closeIndex = findFirstClose(s, 2);
        if (closeIndex < 0) {
            throw new IllegalArgumentException("Invalid constraint (missing ']' or ')'): " + s);
        }

        final boolean firstInclusive = s.charAt(closeIndex) == ']';
        final String firstVersionStr = s.substring(2, closeIndex).strip();

        // upper bound is set unconditionally here — no branch can omit it
        final VersionBound upperBound = VersionBound.upper(Version.parse(firstVersionStr), firstInclusive);
        final VersionRange firstRange = new VersionRange(null, upperBound);

        // check for a second disjoint range; the separator comma comes before the second range
        final String afterClose = s.substring(closeIndex + 1).strip();
        if (afterClose.isEmpty()) {
            return new VersionConstraint(List.of(firstRange));
        }

        if (!afterClose.startsWith(",")) {
            throw new IllegalArgumentException("Invalid constraint (unexpected content after first range): " + s);
        }

        final String remainder = afterClose.substring(1).strip();
        final VersionRange secondRange = parseSecondDisjointRange(remainder, s);
        return new VersionConstraint(List.of(firstRange, secondRange));
    }

    /**
     * Parses the second part of a disjoint constraint.
     * Forms: {@code [v,)}, {@code (v,)}.
     */
    private static VersionRange parseSecondDisjointRange(final String remainder, final String full) {
        if (remainder.startsWith("[")) {
            // [v,) — inclusive lower, unbounded upper
            if (!remainder.endsWith(",)")) {
                throw new IllegalArgumentException("Invalid disjoint range second part: " + full);
            }
            final String vStr = remainder.substring(1, remainder.length() - 2).strip();
            return new VersionRange(VersionBound.lower(Version.parse(vStr), true), null);
        } else if (remainder.startsWith("(")) {
            // (v,) — exclusive lower, unbounded upper
            if (!remainder.endsWith(",)")) {
                throw new IllegalArgumentException("Invalid disjoint range second part: " + full);
            }
            final String vStr = remainder.substring(1, remainder.length() - 2).strip();
            return new VersionRange(VersionBound.lower(Version.parse(vStr), false), null);
        }
        throw new IllegalArgumentException("Invalid constraint (unexpected content after first range): " + full);
    }

    /**
     * Finds the index of the first {@code ]} or {@code )} in {@code s} starting at {@code from}.
     */
    private static int findFirstClose(final String s, final int from) {
        for (int i = from; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c == ']' || c == ')') {
                return i;
            }
        }
        return -1;
    }
}
