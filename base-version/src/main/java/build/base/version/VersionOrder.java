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

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pre-built {@link Comparator}s for {@link Version}.
 *
 * <p>Two orderings are provided:
 *
 * <ul>
 *   <li>{@link #NATURAL} — delegates to {@link Version#compareTo(Version)}. String elements in all
 *       segments are compared lexicographically. This is the default ordering used by
 *       {@link VersionRange} and {@link VersionConstraint}.</li>
 *   <li>{@link #SEMVER} — Semantic Versioning 2.0.0-compatible ordering. Identical to
 *       {@link #NATURAL} except that build metadata is ignored when determining precedence,
 *       as required by SemVer §10.</li>
 *   <li>{@link #MAVEN} — applies Maven-compatible qualifier ordering to string elements in the
 *       prerelease segment. The recognized qualifier chain is:
 *       {@code alpha} (= {@code a}) &lt; {@code beta} (= {@code b}) &lt; {@code milestone} (= {@code m})
 *       &lt; {@code rc} (= {@code cr}) &lt; {@code snapshot} &lt; <em>no prerelease</em>.
 *       Matching is case-insensitive. Unknown qualifiers sort after all known ones,
 *       lexicographically among themselves.
 *       <p>Note: the {@code sp} (service pack) qualifier, which in Maven ranks <em>above</em> a
 *       bare release, is not yet supported — it requires overriding the structural
 *       "no-prerelease &gt; has-prerelease" rule.</li>
 * </ul>
 *
 * <p>Both orderings are consistent with {@link java.util.Collections#sort}, {@link List#sort},
 * and {@link java.util.stream.Stream#sorted}.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public enum VersionOrder
    implements Comparator<Version> {

    /**
     * Natural ordering: delegates to {@link Version#compareTo(Version)}.
     * String prerelease elements are compared lexicographically.
     */
    NATURAL {
        @Override
        public int compare(final Version a, final Version b) {
            return a.compareTo(b);
        }
    },

    /**
     * Semantic Versioning 2.0.0-compatible ordering.
     * Identical to {@link #NATURAL} except that build metadata is ignored when determining
     * version precedence, as required by
     * <a href="https://semver.org/#spec-item-10">SemVer §10</a>.
     */
    SEMVER {
        @Override
        public int compare(final Version a, final Version b) {
            // 1. compare sequences
            final int seqCmp = compareElementLists(a.sequence(), b.sequence(), false);
            if (seqCmp != 0) {
                return seqCmp;
            }

            // 2. structural rule: no prerelease > has prerelease
            final boolean aHasPre = !a.prerelease().isEmpty();
            final boolean bHasPre = !b.prerelease().isEmpty();
            if (aHasPre != bHasPre) {
                return aHasPre ? -1 : 1;
            }

            // 3. compare prerelease (lexicographic — SemVer §11.4)
            // build metadata intentionally omitted (SemVer §10)
            return compareElementLists(a.prerelease(), b.prerelease(), false);
        }
    },

    /**
     * Maven-compatible qualifier ordering.
     * See {@link VersionOrder} class Javadoc for the full qualifier chain.
     */
    MAVEN {
        @Override
        public int compare(final Version a, final Version b) {
            // 1. compare sequences (same as natural — numeric elements, trailing-zero equivalence)
            int cmp = compareElementLists(a.sequence(), b.sequence(), false);
            if (cmp != 0) {
                return cmp;
            }

            // 2. structural rule: no prerelease > has prerelease
            final boolean aHasPre = !a.prerelease().isEmpty();
            final boolean bHasPre = !b.prerelease().isEmpty();
            if (aHasPre != bHasPre) {
                return aHasPre ? -1 : 1;
            }

            // 3. compare prerelease with qualifier-aware string ordering
            cmp = compareElementLists(a.prerelease(), b.prerelease(), true);
            if (cmp != 0) {
                return cmp;
            }

            // 4. compare build (same as natural)
            return compareElementLists(a.build(), b.build(), false);
        }
    };

    // ── Qualifier rank table ──────────────────────────────────────────────────

    private static final Map<String, Integer> QUALIFIER_RANK = Map.of(
        "alpha", 1, "a", 1,
        "beta",  2, "b", 2,
        "milestone", 3, "m", 3,
        "rc", 4, "cr", 4,
        "snapshot", 5
    );

    /** Returns the Maven qualifier rank for {@code s} (case-insensitive), or MAX_VALUE if unknown. */
    private static int qualifierRank(final String s) {
        return QUALIFIER_RANK.getOrDefault(s.toLowerCase(Locale.ROOT), Integer.MAX_VALUE);
    }

    // ── Element-list comparison ───────────────────────────────────────────────

    private static int compareElementLists(final List<Version.Element> a,
                                           final List<Version.Element> b,
                                           final boolean mavenQualifiers) {
        final int common = Math.min(a.size(), b.size());
        for (int i = 0; i < common; i++) {
            final int c = compareElements(a.get(i), b.get(i), mavenQualifiers);
            if (c != 0) {
                return c;
            }
        }

        // trailing elements compared against zero — mirrors Version.compareElementLists
        if (a.size() != b.size()) {
            final List<Version.Element> longer = a.size() > b.size() ? a : b;
            final int sign = a.size() > b.size() ? 1 : -1;
            for (int i = common; i < longer.size(); i++) {
                final Object val = longer.get(i).getValue();
                if (val instanceof Integer n && n != 0) {
                    return sign;
                }
                if (val instanceof String s && !s.isEmpty()) {
                    return sign;
                }
            }
        }
        return 0;
    }

    private static int compareElements(final Version.Element a,
                                       final Version.Element b,
                                       final boolean mavenQualifiers) {
        if (mavenQualifiers
            && a.getValue() instanceof String sa
            && b.getValue() instanceof String sb) {
            return compareQualifiers(sa, sb);
        }
        return a.compareTo(b);
    }

    private static int compareQualifiers(final String a, final String b) {
        final int rankA = qualifierRank(a);
        final int rankB = qualifierRank(b);
        if (rankA != rankB) {
            return Integer.compare(rankA, rankB);
        }
        // both unknown — lexicographic, case-insensitive
        if (rankA == Integer.MAX_VALUE) {
            return a.compareToIgnoreCase(b);
        }
        return 0;
    }
}
