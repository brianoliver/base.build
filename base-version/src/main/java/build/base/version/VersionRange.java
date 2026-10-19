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

import java.util.Objects;
import java.util.function.Predicate;

/**
 * A single contiguous interval of {@link Version}s, defined by an optional lower
 * {@link VersionBound} and an optional upper {@link VersionBound}.
 * <p>
 * A {@code null} lower bound means unbounded below; a {@code null} upper bound means
 * unbounded above.
 * <p>
 * To parse or evaluate disjoint union ranges (e.g. {@code (,1.0],[1.2,)}),
 * use {@link VersionConstraint} instead.
 *
 * @param lower The lower bound, or {@code null} for unbounded-below.
 * @param upper The upper bound, or {@code null} for unbounded-above.
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public record VersionRange(VersionBound lower, VersionBound upper)
    implements Predicate<Version> {

    /**
     * Constructs a {@link VersionRange} with the given bounds.
     *
     * @param lower the lower {@link VersionBound}, or {@code null} for unbounded-below
     * @param upper the upper {@link VersionBound}, or {@code null} for unbounded-above
     */
    public VersionRange {
    }

    /**
     * Creates an exact-match range ({@code [version,version]}).
     *
     * @param version the exact version
     * @return a new exact-match {@link VersionRange}
     */
    public static VersionRange of(final Version version) {
        Objects.requireNonNull(version, "version must not be null");
        return new VersionRange(
            VersionBound.lower(version, true),
            VersionBound.upper(version, true));
    }

    /**
     * Parses a single-interval Maven range expression.
     *
     * <ul>
     *   <li>{@code 1.0}         — exactly 1.0</li>
     *   <li>{@code [1.0]}       — exactly 1.0</li>
     *   <li>{@code [1.2,1.3]}   — 1.2 &le; v &le; 1.3</li>
     *   <li>{@code [1.0,2.0)}   — 1.0 &le; v &lt; 2.0</li>
     *   <li>{@code [1.5,)}      — v &ge; 1.5</li>
     *   <li>{@code (,1.0]}      — v &le; 1.0</li>
     *   <li>{@code (,1.0)}      — v &lt; 1.0</li>
     *   <li>{@code (1.0,2.0]}   — 1.0 &lt; v &le; 2.0</li>
     *   <li>{@code (1.0,2.0)}   — 1.0 &lt; v &lt; 2.0</li>
     *   <li>{@code (1.0,)}      — v &gt; 1.0</li>
     * </ul>
     * <p>
     * Disjoint ranges (e.g. {@code (,1.0],[1.2,)}) are not accepted here;
     * use {@link VersionConstraint#parse(String)} for those.
     *
     * @param s the range string
     * @return the parsed {@link VersionRange}
     * @throws IllegalArgumentException if the string is null, empty, or not a single-interval expression
     */
    public static VersionRange parse(final String s) {
        if (s == null) {
            throw new IllegalArgumentException("Range string must not be null");
        }
        final String trimmed = s.strip();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Range string must not be empty");
        }

        if (trimmed.startsWith("[")) {
            return parseLowerBounded(trimmed);
        }
        if (trimmed.startsWith("(")) {
            return trimmed.startsWith("(,")
                ? parseUpperOnly(trimmed)
                : parseExclusiveLowerBounded(trimmed);
        }

        // bare version — exact match
        return VersionRange.of(Version.parse(trimmed));
    }

    /**
     * Obtains the lower {@link VersionBound}, or {@code null} if unbounded below.
     *
     * @return the lower bound
     */
    @Override
    public VersionBound lower() {
        return this.lower;
    }

    /**
     * Obtains the upper {@link VersionBound}, or {@code null} if unbounded above.
     *
     * @return the upper bound
     */
    @Override
    public VersionBound upper() {
        return this.upper;
    }

    /**
     * Determines whether the given {@link Version} is contained in this range.
     *
     * @param version the version to test
     * @return {@code true} if contained
     */
    public boolean contains(final Version version) {
        return test(version);
    }

    @Override
    public boolean test(final Version version) {
        if (version == null) {
            return false;
        }
        return (this.lower == null || this.lower.test(version))
            && (this.upper == null || this.upper.test(version));
    }

    @Override
    public String toString() {
        if (this.lower != null && this.upper != null
            && this.lower.version().equals(this.upper.version())
            && this.lower.isInclusive() && this.upper.isInclusive()) {
            return "[" + this.lower.version() + "]";
        }
        final String lo = this.lower == null ? "(" : (this.lower.isInclusive() ? "[" : "(") + this.lower.version();
        final String hi = this.upper == null ? ")" : this.upper.version() + (this.upper.isInclusive() ? "]" : ")");
        return lo + "," + hi;
    }

    // ── Private parsing helpers ───────────────────────────────────────────────

    /**
     * Parses a lower-bounded range that starts with {@code [}.
     * Forms: {@code [v]}, {@code [v,u]}, {@code [v,u)}, {@code [v,)}.
     */
    private static VersionRange parseLowerBounded(final String s) {
        // strip leading '['
        final String inner = s.substring(1);

        // [v] — exact match
        if (inner.endsWith("]") && !inner.contains(",")) {
            final Version v = Version.parse(inner.substring(0, inner.length() - 1).strip());
            return VersionRange.of(v);
        }

        // [v,...]
        final int comma = inner.indexOf(',');
        if (comma < 0) {
            throw new IllegalArgumentException("Invalid range (missing ',' or ']'): " + s);
        }

        final Version lowerVersion = Version.parse(inner.substring(0, comma).strip());
        final String rest = inner.substring(comma + 1).strip();

        // [v,) — unbounded upper
        if (rest.equals(")")) {
            return new VersionRange(VersionBound.lower(lowerVersion, true), null);
        }

        // [v,u] or [v,u)
        final boolean upperInclusive;
        final String upperStr;
        if (rest.endsWith("]")) {
            upperInclusive = true;
            upperStr = rest.substring(0, rest.length() - 1).strip();
        } else if (rest.endsWith(")")) {
            upperInclusive = false;
            upperStr = rest.substring(0, rest.length() - 1).strip();
        } else {
            throw new IllegalArgumentException("Invalid range (missing closing ']' or ')'): " + s);
        }

        final Version upperVersion = Version.parse(upperStr);
        return new VersionRange(
            VersionBound.lower(lowerVersion, true),
            VersionBound.upper(upperVersion, upperInclusive));
    }

    /**
     * Parses a range with an exclusive lower bound that starts with {@code (} (but not {@code (,}).
     * Forms: {@code (v,u]}, {@code (v,u)}, {@code (v,)}.
     */
    private static VersionRange parseExclusiveLowerBounded(final String s) {
        // strip leading '('
        final String inner = s.substring(1);

        final int comma = inner.indexOf(',');
        if (comma < 0) {
            throw new IllegalArgumentException("Invalid range (missing ','): " + s);
        }

        final Version lowerVersion = Version.parse(inner.substring(0, comma).strip());
        final String rest = inner.substring(comma + 1).strip();

        // (v,) — exclusive lower, unbounded upper
        if (rest.equals(")")) {
            return new VersionRange(VersionBound.lower(lowerVersion, false), null);
        }

        // (v,u] or (v,u)
        final boolean upperInclusive;
        final String upperStr;
        if (rest.endsWith("]")) {
            upperInclusive = true;
            upperStr = rest.substring(0, rest.length() - 1).strip();
        } else if (rest.endsWith(")")) {
            upperInclusive = false;
            upperStr = rest.substring(0, rest.length() - 1).strip();
        } else {
            throw new IllegalArgumentException("Invalid range (missing closing ']' or ')'): " + s);
        }

        return new VersionRange(
            VersionBound.lower(lowerVersion, false),
            VersionBound.upper(Version.parse(upperStr), upperInclusive));
    }

    /**
     * Parses an upper-only range that starts with {@code (,}.
     * Forms: {@code (,v]}, {@code (,v)}.
     * Disjoint continuations (e.g. {@code (,1.0],[1.2,)}) are rejected.
     */
    private static VersionRange parseUpperOnly(final String s) {
        if (!s.startsWith("(,")) {
            throw new IllegalArgumentException("Invalid range: " + s);
        }
        final String rest = s.substring(2).strip();

        final boolean inclusive;
        final String versionStr;
        if (rest.endsWith("]")) {
            inclusive = true;
            versionStr = rest.substring(0, rest.length() - 1).strip();
        } else if (rest.endsWith(")")) {
            inclusive = false;
            versionStr = rest.substring(0, rest.length() - 1).strip();
        } else {
            throw new IllegalArgumentException("Invalid range (missing closing ']' or ')'): " + s);
        }

        // reject disjoint ranges — they contain a comma after the closing bracket
        if (versionStr.contains(",") || versionStr.contains("[") || versionStr.contains("(")) {
            throw new IllegalArgumentException("Disjoint ranges must be parsed with VersionConstraint: " + s);
        }

        // upper bound is set unconditionally here — no branch can skip it
        final VersionBound upperBound = VersionBound.upper(Version.parse(versionStr), inclusive);
        return new VersionRange(null, upperBound);
    }
}
