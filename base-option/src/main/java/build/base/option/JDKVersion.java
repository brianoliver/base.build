package build.base.option;

/*-
 * #%L
 * base.build Option
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

import build.base.configuration.Default;
import build.base.configuration.Option;

import java.io.DataInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Java Development Kit (JDK) version numbers.
 * <p>
 * Modern version strings (Java 9 and later, per {@link Runtime.Version}) are handed straight to
 * {@link Runtime.Version#parse(String)}, which also provides the ordering via {@link #compareTo}.
 * Legacy strings (the pre-Java-9
 * <a href="https://www.oracle.com/technetwork/java/javase/versioning-naming-139433.html">scheme</a>,
 * e.g. {@code 1.8.0_292-b10} or {@code 1.7.0-ea-b19}) are first normalized into the modern
 * <a href="http://openjdk.java.net/jeps/223">JEP 223</a> form &mdash; the leading {@code 1.} is
 * dropped, the legacy {@code _security} update is folded into the dotted version number (so
 * {@code 1.8.0_292} becomes {@code 8.0.292}), and {@code -bNN} becomes {@code +NN}.
 * <p>
 * Note that {@link #toString()} returns this normalized, modern-scheme form rather than echoing
 * the string passed to {@link #of(String)}; use {@link #get()} to recover the raw input.
 *
 * @author brian.oliver
 * @since Nov-2019
 */
public final class JDKVersion
    implements Option, Comparable<JDKVersion> {

    /**
     * Matches a legacy (pre-Java-9) version string such as {@code 1.8.0_292-b10}, {@code 1.9.0-b100},
     * or {@code 1.7.0-ea-b19}, capturing (1) feature, (2) minor, (3) security, (4) build, (5) pre-release,
     * and (6) build-after-pre-release.
     */
    private static final Pattern LEGACY = Pattern.compile(
        "1\\.(\\d+)(?:\\.(\\d+))?(?:_(\\d+))?(?:-(?:b(\\d+)|([A-Za-z0-9]+)(?:-b(\\d+))?))?");

    /**
     * The raw version {@link String} as supplied to the factory method.
     */
    private final String rawVersion;

    /**
     * The parsed, modern-scheme version this instance delegates to.
     */
    private final Runtime.Version version;

    private JDKVersion(final String rawVersion, final Runtime.Version version) {
        this.rawVersion = rawVersion;
        this.version = version;
    }

    /**
     * Obtains the raw version {@link String}.
     *
     * @return the raw version
     */
    public String get() {
        return this.rawVersion;
    }

    /**
     * Obtains the feature (major) version number, e.g. {@code 25} for {@code 25.0.4}.
     *
     * @return the feature version number
     */
    public int major() {
        return this.version.feature();
    }

    /**
     * Obtains the interim (minor) version number.
     *
     * @return the interim version number
     */
    public int minor() {
        return this.version.interim();
    }

    /**
     * Obtains the update (security) version number.
     *
     * @return the update version number
     */
    public int security() {
        return this.version.update();
    }

    /**
     * Obtains the {@link Optional} build number.
     *
     * @return the {@link Optional} build number
     */
    public Optional<Integer> build() {
        return this.version.build();
    }

    /**
     * Obtains the {@link Optional} pre-release information.
     *
     * @return the {@link Optional} pre-release information
     */
    public Optional<String> pre() {
        return this.version.pre();
    }

    /**
     * Obtains the {@link Optional} optional (additional build) information.
     *
     * @return the {@link Optional} optional information
     */
    public Optional<String> optional() {
        return this.version.optional();
    }

    /**
     * Obtains the underlying {@link Runtime.Version} this instance delegates to.
     *
     * @return the {@link Runtime.Version}
     */
    public Runtime.Version runtimeVersion() {
        return this.version;
    }

    /**
     * Obtains an unmodifiable {@link List} of the integers making up the version number (the
     * dotted sequence before any {@code -} or {@code +}). The build number is <em>not</em> included;
     * obtain it via {@link #build()}.
     *
     * @return a {@link List} of {@link Integer} numerals
     */
    public List<Integer> version() {
        return this.version.version();
    }

    /**
     * Determines if the {@link JDKVersion} supports the Module System (ie: Major Version 9+)
     *
     * @return {@code true} if the {@link JDKVersion} is modular, {@code false} otherwise
     */
    public boolean isModular() {
        return this.version.feature() >= 9;
    }

    @Override
    public boolean equals(final Object object) {
        return object instanceof JDKVersion other && compareTo(other) == 0;
    }

    @Override
    public int hashCode() {
        // consistent with equals(), which is defined as compareTo() == 0: Runtime.Version.compareTo()
        // and Runtime.Version.hashCode() take the same four components (version, pre, build, optional)
        // into account, so equal instances necessarily hash alike
        return this.version.hashCode();
    }

    @Override
    public String toString() {
        return this.version.toString();
    }

    @Override
    public int compareTo(final JDKVersion other) {
        return this.version.compareTo(other.version);
    }

    /**
     * Obtains the {@link JDKVersion} of the running JVM.
     *
     * @return the current {@link JDKVersion}
     */
    @Default
    public static JDKVersion current() {
        return new JDKVersion(Runtime.version().toString(), Runtime.version());
    }

    /**
     * Creates a {@link JDKVersion} for the specified feature (major) version number.
     * <p>
     * For example, {@code JDKVersion.of(8)} produces a {@link JDKVersion} with a feature version of 8.
     *
     * @param major the feature {@link JDKVersion} number
     * @return a {@link JDKVersion}
     */
    public static JDKVersion of(final int major) {
        final var raw = Integer.toString(major);
        return new JDKVersion(raw, Runtime.Version.parse(raw));
    }

    /**
     * Obtains the {@link JDKVersion} by parsing the specified {@link String}, accepting both the
     * modern ({@link Runtime.Version}) and legacy (pre-Java-9) schemes.
     *
     * @param version the version
     * @return a {@link JDKVersion}
     * @throws IllegalArgumentException if the version is {@code null}, blank, or cannot be parsed
     */
    public static JDKVersion of(final String version) {

        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("The specified version was empty or null");
        }

        final var rawVersion = version.trim();

        try {
            return new JDKVersion(rawVersion, Runtime.Version.parse(normalize(rawVersion)));
        }
        catch (final RuntimeException e) {
            throw new IllegalArgumentException("Failed to parse version: " + rawVersion, e);
        }
    }

    /**
     * Normalizes an arbitrary JDK version string into a form {@link Runtime.Version#parse(String)}
     * accepts: legacy (pre-Java-9) {@code 1.x} strings are remapped to the modern scheme, and
     * trailing zero elements of the version number (which {@link Runtime.Version} rejects but the
     * legacy scheme and real-world tooling emit, e.g. {@code 9.0.0}) are dropped.
     *
     * @param raw the raw version string
     * @return the normalized, modern-scheme version string
     */
    private static String normalize(final String raw) {
        return stripTrailingZeroElements(raw.startsWith("1.") ? remapLegacy(raw) : raw);
    }

    /**
     * Remaps a legacy {@code 1.x} version string to the modern scheme, dropping the leading
     * {@code 1.} and translating {@code _security} to {@code .security} and {@code -bNN} to
     * {@code +NN}. Strings that do not match the legacy shape are returned unchanged.
     */
    private static String remapLegacy(final String raw) {

        final Matcher matcher = LEGACY.matcher(raw);
        if (!matcher.matches()) {
            return raw;
        }

        final int feature = Integer.parseInt(matcher.group(1));
        final Integer minor = matcher.group(2) == null ? null : Integer.valueOf(matcher.group(2));
        final Integer security = matcher.group(3) == null ? null : Integer.valueOf(matcher.group(3));
        final String pre = matcher.group(5);
        final String build = matcher.group(4) != null ? matcher.group(4) : matcher.group(6);

        final var builder = new StringBuilder().append(feature);
        if (security != null) {
            builder.append('.').append(minor == null ? 0 : minor).append('.').append(security);
        }
        else if (minor != null && minor != 0) {
            builder.append('.').append(minor);
        }
        if (pre != null) {
            builder.append('-').append(pre);
        }
        if (build != null) {
            builder.append('+').append(Integer.parseInt(build));
        }
        return builder.toString();
    }

    /**
     * Drops trailing {@code .0} elements from the version-number portion (before any {@code -} or
     * {@code +}), keeping at least the feature element, since {@link Runtime.Version} forbids them.
     */
    private static String stripTrailingZeroElements(final String version) {

        int cut = version.length();
        for (int i = 0; i < version.length(); i++) {
            final char c = version.charAt(i);
            if (c == '-' || c == '+') {
                cut = i;
                break;
            }
        }

        final String[] elements = version.substring(0, cut).split("\\.", -1);
        int end = elements.length;
        while (end > 1 && elements[end - 1].equals("0")) {
            end--;
        }

        return String.join(".", Arrays.copyOf(elements, end)) + version.substring(cut);
    }

    /**
     * Attempts to determine the {@link JDKVersion} of the compiled {@link Class} bytecode according to the
     * <a href="https://en.wikipedia.org/wiki/Java_class_file#General_layout">Bytecode File Format</a>.
     *
     * @param targetClass the target {@link Class} from which to determine the {@link JDKVersion}
     * @return an {@link Optional} {@link JDKVersion} or
     * {@link Optional#empty()} if the {@link JDKVersion} could not be detected
     */
    public static Optional<JDKVersion> of(final Class<?> targetClass) {

        if (targetClass == null) {
            return Optional.empty();
        }

        final String className = targetClass.getName();
        final String classAsPath = className.replace('.', '/') + ".class";
        final InputStream inputStream = targetClass.getClassLoader().getResourceAsStream(classAsPath);

        if (inputStream == null) {
            return Optional.empty();
        }

        try (DataInputStream stream = new DataInputStream(inputStream)) {
            final var magicBytes = stream.readInt();

            // ensure the magicBytes are at the start of the stream
            if (magicBytes != 0xcafebabe) {
                return Optional.empty();
            }

            // we don't care about the minor bytecode version number (it's never used)
            stream.readUnsignedShort();

            // the major version is the bytecode version - 44
            final var major = stream.readUnsignedShort() - 44;

            return Optional.of(JDKVersion.of(major));
        }
        catch (final Exception e) {
            return Optional.empty();
        }
    }
}
