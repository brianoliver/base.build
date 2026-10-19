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
 * One end of a {@link VersionRange} interval — either a lower bound ({@code >=} or {@code >})
 * or an upper bound ({@code <=} or {@code <}).
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public final class VersionBound
    implements Predicate<Version> {

    private enum End {LOWER, UPPER}

    private final Version version;
    private final End end;
    private final boolean inclusive;

    private VersionBound(final Version version, final End end, final boolean inclusive) {
        this.version = Objects.requireNonNull(version, "version must not be null");
        this.end = Objects.requireNonNull(end, "end must not be null");
        this.inclusive = inclusive;
    }

    /**
     * Creates a lower bound ({@code >= version} when inclusive, {@code > version} otherwise).
     *
     * @param version   the bound version
     * @param inclusive whether the bound is inclusive
     * @return a new lower {@link VersionBound}
     */
    public static VersionBound lower(final Version version, final boolean inclusive) {
        return new VersionBound(version, End.LOWER, inclusive);
    }

    /**
     * Creates an upper bound ({@code <= version} when inclusive, {@code < version} otherwise).
     *
     * @param version   the bound version
     * @param inclusive whether the bound is inclusive
     * @return a new upper {@link VersionBound}
     */
    public static VersionBound upper(final Version version, final boolean inclusive) {
        return new VersionBound(version, End.UPPER, inclusive);
    }

    /**
     * Obtains the {@link Version} at this bound.
     *
     * @return the version
     */
    public Version version() {
        return this.version;
    }

    /**
     * Returns {@code true} if this is a lower bound.
     *
     * @return whether this is a lower bound
     */
    public boolean isLower() {
        return this.end == End.LOWER;
    }

    /**
     * Returns {@code true} if this is an upper bound.
     *
     * @return whether this is an upper bound
     */
    public boolean isUpper() {
        return this.end == End.UPPER;
    }

    /**
     * Returns {@code true} if this bound is inclusive.
     *
     * @return whether this bound is inclusive
     */
    public boolean isInclusive() {
        return this.inclusive;
    }

    @Override
    public boolean test(final Version v) {
        if (v == null) {
            return false;
        }
        final int cmp = v.compareTo(this.version);
        if (this.end == End.LOWER) {
            return this.inclusive ? cmp >= 0 : cmp > 0;
        } else {
            return this.inclusive ? cmp <= 0 : cmp < 0;
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VersionBound other)) {
            return false;
        }
        return this.version.equals(other.version)
            && this.end == other.end
            && this.inclusive == other.inclusive;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.version, this.end, this.inclusive);
    }

    @Override
    public String toString() {
        return this.end == End.LOWER
            ? (this.inclusive ? "[" : "(") + this.version
            : this.version + (this.inclusive ? "]" : ")");
    }
}
