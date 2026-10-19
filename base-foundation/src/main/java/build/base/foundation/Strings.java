package build.base.foundation;

/*-
 * #%L
 * base.build Foundation
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Helper methods for {@link Strings}.
 *
 * @author brian.oliver
 * @since Oct-2019
 */
public class Strings {

    /**
     * An empty {@link String} array.
     */
    public static final String[] EMPTY_ARRAY = new String[0];

    /**
     * A map of parser's key'd by the type they parse to.
     */
    private static final Map<Class<?>, Function<String, ?>> PARSER_BY_CLASS;

    /**
     * Private constructor to prevent instantiation.
     */
    private Strings() {
        // empty constructor
    }

    static {
        final Map<Class<?>, Function<String, ?>> map = new IdentityHashMap<>(128);
        registerParser(map, String.class, s -> s); // must come first so it can also match Object
        registerParser(map, Number.class, s -> // must come before other numerics (short, int, long...)
            s.contains(".")
                ? endsWithIgnoreCase(s, "f") ? convert(s, Float.class) // explicit float
                : convert(s, Double.class) // default decimal type
                : endsWithIgnoreCase(s, "l") ? convert(s, Long.class) // explicit long
                    : endsWithIgnoreCase(s, "d") && !isHexadecimal(s) ? convert(s, Double.class) // explicit double
                        : endsWithIgnoreCase(s, "f") && !isHexadecimal(s) ? convert(s, Float.class) // explicit float
                            : convert(s, Integer.class)); // default whole number type
        registerParser(map, boolean.class, s ->
            s.equalsIgnoreCase("true") ||
                s.equalsIgnoreCase("yes") ||
                s.equalsIgnoreCase("on"));
        registerParser(map, byte.class, Byte::parseByte);
        registerParser(map, char.class, s -> {
            if (s.isEmpty()) {
                throw new IllegalArgumentException("a single character must be specified");
            }
            else {
                return s.charAt(0);
            }
        });
        registerParser(map, short.class, s ->
            startsWithIgnoreCase(s, "-0x") ? (short) -Short.parseShort(s.substring(3), 16) // hex
                : startsWithIgnoreCase(s, "0x") ? Short.parseShort(s.substring(2), 16) // hex
                    : s.startsWith("-0b") ? (short) -Short.parseShort(s.substring(3), 2) // binary
                        : s.startsWith("0b") ? Short.parseShort(s.substring(2), 2) // binary
                            : Short.parseShort(s)); // decimal
        registerParser(map, int.class, s ->
            startsWithIgnoreCase(s, "-0x") ? -Integer.parseInt(s.substring(3), 16) // hex
                : startsWithIgnoreCase(s, "0x") ? Integer.parseInt(s.substring(2), 16) // hex
                    : s.startsWith("-0b") ? -Integer.parseInt(s.substring(3), 2) // binary
                        : s.startsWith("0b") ? Integer.parseInt(s.substring(2), 2) // binary
                            : Integer.parseInt(s)); // decimal
        registerParser(map, long.class, s -> {
            final String s2 = endsWithIgnoreCase(s, "l") ? s.substring(0, s.length() - 1) : s;
            return startsWithIgnoreCase(s2, "-0x") ? -Long.parseLong(s2.substring(3), 16) // hex
                : startsWithIgnoreCase(s2, "0x") ? Long.parseLong(s2.substring(2), 16) // hex
                    : startsWithIgnoreCase(s2, "-0b") ? -Long.parseLong(s2.substring(3), 2) // binary
                        : startsWithIgnoreCase(s2, "0b") ? Long.parseLong(s2.substring(2), 2) // binary
                            : Long.parseLong(s2); // decimal
        });
        registerParser(map, float.class,
            s -> Float.parseFloat(endsWithIgnoreCase(s, "f") ? s.substring(0, s.length() - 1) : s));
        registerParser(map, double.class,
            s -> Double.parseDouble(endsWithIgnoreCase(s, "d") ? s.substring(0, s.length() - 1) : s));
        registerParser(map, BigDecimal.class, BigDecimal::new);
        registerParser(map, BigInteger.class, s ->
            startsWithIgnoreCase(s, "-0x") ? new BigInteger(s.substring(3), 16).negate() // hex
                : startsWithIgnoreCase(s, "0x") ? new BigInteger(s.substring(2), 16) // hex
                    : startsWithIgnoreCase(s, "-0b") ? new BigInteger(s.substring(3), 2).negate() // binary
                        : startsWithIgnoreCase(s, "0b") ? new BigInteger(s.substring(2), 2) // binary
                            : new BigInteger(s)); // decimal
        registerParser(map, AtomicInteger.class, s -> new AtomicInteger(convert(s, int.class)));
        registerParser(map, AtomicLong.class, s -> new AtomicLong(convert(s, long.class)));
        registerParser(map, AtomicBoolean.class, s -> new AtomicBoolean(convert(s, boolean.class)));
        registerParser(map, Date.class, s -> new Date(Instant.parse(s).toEpochMilli()));
        registerParser(map, Duration.class, s -> s.contains("P") ? Duration.parse(s) : parsePrettyDuration(s));
        registerParser(map, Instant.class, Instant::parse);
        registerParser(map, LocalDate.class, LocalDate::parse);
        registerParser(map, LocalDateTime.class, LocalDateTime::parse);
        registerParser(map, LocalTime.class, LocalTime::parse);
        registerParser(map, MonthDay.class, MonthDay::parse);
        registerParser(map, OffsetDateTime.class, OffsetDateTime::parse);
        registerParser(map, OffsetTime.class, OffsetTime::parse);
        registerParser(map, Period.class, Period::parse);
        registerParser(map, Year.class, Year::parse);
        registerParser(map, YearMonth.class, YearMonth::parse);
        registerParser(map, ZonedDateTime.class, ZonedDateTime::parse);
        registerParser(map, Class.class, s -> {
            try {
                return Class.forName(s);
            }
            catch (final ClassNotFoundException e) {
                throw new IllegalArgumentException("Can't convert [" + s + "] into a class");
            }
        });

        PARSER_BY_CLASS = map;
    }

    /**
     * Return {@code true} if the string starts with a prefix indicating it is a hexadecimal value.
     *
     * @param s the string
     * @return {@code true} if the string starts with a prefix indicating it is a hexadecimal value
     */
    private static boolean isHexadecimal(final String s) {
        return startsWithIgnoreCase(s, "0x") || s.regionMatches(true, 0, "-0x", 0, 3);
    }

    /**
     * Return {@code true} if the string starts with a prefix indicating it is a binary value.
     *
     * @param s the string
     * @return {@code true} if the string starts with a prefix indicating it is a binary value
     */
    private static boolean isBinary(final String s) {
        return startsWithIgnoreCase(s, "0b") || s.regionMatches(true, 0, "-0b", 0, 3);
    }

    /**
     * Return {@code true} iff the specified string starts with the prefix regardless of case.
     *
     * @param target the string to check
     * @param prefix the prefix to check for
     * @return {@code true} iff the specified string starts with the prefix regardless of case
     */
    public static boolean startsWithIgnoreCase(final String target, final String prefix) {
        return target.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
     * Return {@code true} iff the specified string ends with the suffix regardless of case.
     *
     * @param target the string to check
     * @param suffix the suffix to check for
     * @return {@code true} iff the specified string starts with the suffix regardless of case
     */
    public static boolean endsWithIgnoreCase(final String target, final String suffix) {
        final int ct = target.length();
        final int cs = suffix.length();
        return ct > cs && target.regionMatches(true, ct - cs, suffix, 0, cs);
    }

    /**
     * Register a parser for the specified class and all its super classes and interfaces so long as they are not
     * already registered.
     * <p>
     * If the specified class represents a primitive then it's boxed type is registered as well.
     * <p>
     * The underlying {@link #PARSER_BY_CLASS} is not thread safe and thus the map
     *
     * @param clz    the class to parse to
     * @param parser the parser
     * @param <T>    the parser's result type
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static <T> void registerParser(final Map<Class<?>, Function<String, ?>> map,
                                           final Class<? super T> clz,
                                           final Function<String, ? extends T> parser) {
        map.putIfAbsent(clz, parser);
        if (clz.isPrimitive()) {
            final Class<?> clzBoxed =
                clz == boolean.class ? Boolean.class
                    : clz == byte.class ? Byte.class
                        : clz == short.class ? Short.class
                            : clz == char.class ? Character.class
                                : clz == int.class ? Integer.class
                                    : clz == float.class ? Float.class
                                        : clz == long.class ? Long.class
                                            : clz == double.class ? Double.class
                                                : null;

            if (clzBoxed == null) {
                throw new IllegalStateException();
            }

            registerParser(map, (Class) clzBoxed, parser);
        }

        for (Class<?> iface : clz.getInterfaces()) {
            registerParser(map, (Class) iface, parser);
        }

        final Class<? super T> superClz = clz.getSuperclass();
        if (superClz != null) {
            registerParser(map, superClz, parser);
        }
    }

    /**
     * Attempts to convert the specified {@link String} into a desired class.
     *
     * @param <T>          the desired type
     * @param desiredClass the desired {@link Class}
     * @param string       the {@link String} to convert
     * @return the value in the desired type
     * @throws IllegalArgumentException when the provided {@link String} can't be converted into the desired type
     * @throws NullPointerException     when the desired class is null
     */
    @SuppressWarnings("unchecked")
    public static <T> T convert(final String string, final Class<T> desiredClass)
        throws IllegalArgumentException, NullPointerException {

        if (desiredClass == null) {
            throw new NullPointerException("Desired class can not be null");
        }
        else if (string == null) {

            if (desiredClass.isPrimitive()) {
                throw new NullPointerException(
                    "Specified value can not be null for the desired class [" + desiredClass + "]");
            }
            else {
                return null;
            }
        }

        try {
            final Function<String, ?> parser = PARSER_BY_CLASS.get(desiredClass);
            if (parser == null) {
                if (desiredClass.isEnum()) {
                    final String value = string.trim();

                    // we search to allow finding values without matching precisely!
                    for (Enum<?> enumValue : (Enum<?>[]) desiredClass.getEnumConstants()) {
                        if (enumValue.name().equalsIgnoreCase(value)) {
                            return (T) enumValue;
                        }
                    }

                    throw new IllegalArgumentException(
                        "Can't locate the enum value [" + string + "] in the [" + desiredClass + "]");
                }

                final Constructor<T> constructor = desiredClass.getDeclaredConstructor(String.class);
                constructor.setAccessible(true);
                return constructor.newInstance(string);
            }

            return (T) parser.apply(string);
        }
        catch (final Exception e) {
            throw new IllegalArgumentException("error converting [" + string + "] into " + desiredClass, e);
        }
    }

    /**
     * Returns the provided {@link String} with all trailing white space removed.
     *
     * @param string the {@link String}
     * @return the {@link String} with trailing whitespace removed
     */
    public static String trimTrailingWhiteSpace(final String string) {

        if (string == null || string.isEmpty()) {
            return string;
        }
        else {
            int index = string.length() - 1;
            while ((index >= 0) && Character.isWhitespace(string.charAt(index))) {
                index--;
            }
            index++;
            return (index < string.length()) ? string.substring(0, index) : string;
        }
    }

    /**
     * Constructs a new {@link String} consisting of the specified {@link String} repeated a
     * required number of times.
     *
     * @param string the {@link String} to repeat
     * @param times  the number of times to repeat the string
     * @return a {@link String}
     */
    public static String repeat(final String string, final int times) {

        return string == null || times <= 0
            ? ""
            : string.repeat(times);
    }

    /**
     * Determines if the specified {@code null}able {@link String} is empty.
     *
     * @param string the possibly {@code null} {@link String} to check
     * @return {@code true} if the given {@link String} is {@code null} or empty
     */
    public static boolean isEmpty(final String string) {
        return string == null || string.isEmpty();
    }

    /**
     * Returns a {@link String} with whitespace removed from the specified {@code null}able {@link String}.
     *
     * @param string the {@link String} to trim
     * @return the trimmed {@link String} or {@code null} if the given string is {@code null}
     */
    public static String trim(final String string) {
        return string == null ? null : string.trim();
    }

    /**
     * Return the Object.toString for a given object, that is the String which would be returned if the implementing
     * class of the supplied object did not override {@link Object#toString}.
     *
     * @param o the object to obtain the default toString for
     * @return the string.
     */
    public static String defaultOf(final Object o) {
        return o == null ? null : o.getClass().getName() + '@' + Integer.toHexString(o.hashCode());
    }

    /**
     * Return an identity String for a given object using the format of Object.toString.
     *
     * @param o the object to obtain the default toString for
     * @return the string, or {@code null} if {@code null} is supplied
     */
    public static String identityOf(final Object o) {
        return o == null ? null : o.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(o));
    }

    /**
     * Returns a {@link String} that is double-quoted if it contains white-space, otherwise returns the
     * {@link String} as is.
     *
     * @param string the {@link String}
     * @return a {@link String}
     */
    public static String doubleQuoteIfContainsWhiteSpace(final String string) {
        if (string == null || string.matches("^\"(.*\\s+.*)|([^\\s]*)\"$") || string.matches("[^\\s]*")) {
            return string;
        }
        else {
            return "\"" + string + "\"";
        }
    }

    /**
     * Escape or Double quote special CharSequence in the given string.
     *
     * @param s the string to replace
     * @return the resulting string
     */
    public static String escape(final String s) {
        return s.replace("\\", "\\\\")
            .replace("*", "\"*\"");
    }

    /**
     * Obtains a {@link String} containing the consecutive digits appearing at the end of the specified {@link String}.
     *
     * @param string the {@link String}
     * @return the {@link Optional} {@link String} containing the digits, {@link Optional#empty()} when none exist
     */
    public static Optional<String> lastDigitsOf(final String string) {
        if (Strings.isEmpty(string)) {
            return Optional.empty();
        }

        int i = string.length() - 1;

        while (i >= 0 && Character.isDigit(string.charAt(i))) {
            i--;
        }

        return i < 0
            ? Optional.of(string)
            : i < string.length() - 1
                ? Optional.of(string.substring(i + 1))
                : Optional.empty();
    }

    /**
     * Return a {@link String} which contains the stringified form of each of the supplied objects.
     *
     * @param delim  the delimiter to include between values
     * @param values the values to include in the returned {@link String}
     * @return the stringified contents
     */
    public static String of(final String delim, final Object... values) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; ++i) {
            if (i > 0) {
                sb.append(delim);
            }
            sb.append(values[i]);
        }
        return sb.toString();
    }

    /**
     * Return a {@link String} containing the {@link Throwable#printStackTrace()} of the supplied exception.
     *
     * @param throwable the exception to return as a {@link String}
     * @return the stringified form of the exception
     */
    public static String of(final Throwable throwable) {
        final StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    /**
     * Return a pretty-printed representation of a {@link Duration}.
     *
     * @param duration the duration to stringify
     * @return the stringified duration
     */
    public static String of(final Duration duration) {
        // strip away "tiny" durations which is likely to be irrelevant when the total duration is significantly longer
        // basically for time values larger than seconds we include up to two units, i.e. d/h, or h/m, or m/s, for
        // seconds and below we include between two and three significant digits of information avoiding trailing zeros

        final long nanos = duration.toNanos();
        final long micros = TimeUnit.NANOSECONDS.toMicros(nanos);
        final long millis = TimeUnit.NANOSECONDS.toMillis(nanos);
        final long seconds = TimeUnit.NANOSECONDS.toSeconds(nanos);
        final long minutes = TimeUnit.NANOSECONDS.toMinutes(nanos);
        final long hours = TimeUnit.NANOSECONDS.toHours(nanos);
        final long days = TimeUnit.NANOSECONDS.toDays(nanos);

        if (days != 0) {
            // strip minutes and below
            final long remainder = Math.abs(hours % TimeUnit.DAYS.toHours(1));
            return remainder == 0 ? days + "d" : String.format("%dd%dh", days, remainder);
        }
        else if (hours != 0) {
            // strip seconds and below
            final long remainder = Math.abs(minutes % TimeUnit.HOURS.toMinutes(1));
            return remainder == 0 ? hours + "h" : String.format("%dh%dm", hours, remainder);
        }
        else if (minutes != 0) {
            // strip millis and below
            final long remainder = Math.abs(seconds % TimeUnit.MINUTES.toSeconds(1));
            return remainder == 0 ? minutes + "m" : String.format("%dm%ds", minutes, remainder);
        }
        else if (seconds != 0) {
            // strip micros and possibly millis
            final long tenths = (Math.abs(millis) % TimeUnit.SECONDS.toMillis(1)) / 100;
            return Math.abs(seconds) >= 10 || tenths == 0 ? seconds + "s" : String.format("%d.%ds", seconds, tenths);
        }
        else if (millis != 0) {
            // strip nanos and possibly micros
            final long tenths = (Math.abs(micros) % TimeUnit.MILLISECONDS.toMicros(1)) / 100;
            return Math.abs(millis) >= 10 || tenths == 0 ? millis + "ms" : String.format("%d.%dms", millis, tenths);
        }
        else if (micros != 0) {
            // possibly strip nanos
            final long tenths = (Math.abs(nanos) % TimeUnit.MICROSECONDS.toNanos(1)) / 100;
            return Math.abs(micros) >= 10 || tenths == 0 ? micros + "us" : String.format("%d.%dus", micros, tenths);
        }
        else {
            return nanos + "ns";
        }
    }

    /**
     * Parse a {@link #of(Duration) pretty printed formatted} string into a {@link Duration}, the string may be
     * formatted with decimal values with suffixes of (d, h, m, s, us, ns) and may contain multiple such decimal and
     * suffix pairs, i.e. "1m2.3s". This parser supports a super set of what is output by {@link Strings#of(Duration)}.
     * <p>
     * For public access use {@link Strings#convert(String, Class)} with a class of {@link Duration}.
     *
     * @param duration the duration
     * @return the {@link Duration}
     */
    private static Duration parsePrettyDuration(final String duration) {
        final String dur = duration.toLowerCase();

        int i = Integer.MAX_VALUE;
        final int ns = dur.lastIndexOf("ns");
        final int us = dur.lastIndexOf("us", i = ns < 0 ? i : ns - 1);
        final int ms = dur.lastIndexOf("ms", i = us < 0 ? i : us - 1);
        final int s = dur.lastIndexOf('s', i = ms < 0 ? i : ms - 1);
        final int m = dur.lastIndexOf('m', i = s < 0 ? i : s - 1);
        final int h = dur.lastIndexOf('h', i = m < 0 ? i : m - 1);
        final int d = dur.lastIndexOf('d', h < 0 ? i : h - 1);

        long nanos = 0;
        long sign = 1;
        i = 0;

        if (dur.startsWith("-")) {
            sign = -1;
            i = 1;
        }
        else if (dur.startsWith("+")) {
            i = 1;
        }

        try {
            if (d > 0) {
                nanos = TimeUnit.HOURS.toNanos((long) (Double.parseDouble(dur.substring(i, d)) * 24));
                i = d + 1;
            }

            if (h > 0) {
                nanos += TimeUnit.MINUTES.toNanos((long) (Double.parseDouble(dur.substring(i, h)) * 60));
                i = h + 1;
            }

            if (m > 0) {
                nanos += TimeUnit.SECONDS.toNanos((long) (Double.parseDouble(dur.substring(i, m)) * 60));
                i = m + 1;
            }

            if (s > 0) {
                nanos += TimeUnit.MILLISECONDS.toNanos((long) (Double.parseDouble(dur.substring(i, s)) * 1000));
                i = s + 1;
            }

            if (ms > 0) {
                nanos += TimeUnit.MICROSECONDS.toNanos((long) (Double.parseDouble(dur.substring(i, ms)) * 1000));
                i = ms + 2;
            }

            if (us > 0) {
                nanos += TimeUnit.NANOSECONDS.toNanos((long) (Double.parseDouble(dur.substring(i, us)) * 1000));
                i = us + 2;
            }

            if (ns > 0) {
                nanos += TimeUnit.NANOSECONDS.toNanos(Integer.parseInt(dur.substring(i, ns)));
                i = ns + 2;
            }
        }
        catch (final NumberFormatException e) {
            throw new IllegalArgumentException("malformed duration: " + duration, e);
        }

        if (i != dur.length()) {
            // there is more content after our final suffix
            throw new IllegalArgumentException("malformed duration: " + duration);
        }

        return Duration.ofNanos(nanos * sign);
    }

    /**
     * Collects {@link Character}s in the specified {@link String} while they satisfy the provided {@link Predicate},
     * starting at the specified index position
     *
     * @param source           the source {@link String}
     * @param startingPosition the starting index position
     * @param predicate        the {@link Character} {@link Predicate}
     * @return an {@link Optional} {@link String} containing the {@link Character}s that satisfy the {@link Predicate},
     * or {@link Optional#empty()} should there be no {@link Character}s or the {@link Predicate} is {@code null}
     */
    public static Optional<String> collectWhile(final String source,
                                                final int startingPosition,
                                                final Predicate<? super Character> predicate) {

        if (source == null || predicate == null || source.isEmpty()
            || startingPosition < 0 || startingPosition >= source.length()) {
            return Optional.empty();
        }

        int i = startingPosition;
        while (i < source.length() && predicate.test(source.charAt(i))) {
            i++;
        }

        return Optional.of(source.substring(startingPosition, i));
    }

    /**
     * Collects {@link Character}s in the specified {@link String} while they satisfy the provided {@link Predicate}.
     *
     * @param source    the source {@link String}
     * @param predicate the {@link Character} {@link Predicate}
     * @return an {@link Optional} {@link String} containing the {@link Character}s that satisfy the {@link Predicate},
     * or {@link Optional#empty()} should there be no {@link Character}s or the {@link Predicate} is {@code null}
     */
    public static Optional<String> collectWhile(final String source,
                                                final Predicate<? super Character> predicate) {

        return collectWhile(source, 0, predicate);
    }

    /**
     * Determines if a {@link String} contains the specified {@link String} prefix at the specified position.
     *
     * @param source   the {@link String}
     * @param position the position with in the {@link String}
     * @param prefix   the prefix {@link String}
     * @return {@code true} if the prefix {@link String} is present, otherwise {@code false}
     */
    public static boolean follows(final String source,
                                  final int position,
                                  final String prefix) {

        return source.startsWith(prefix, position);
    }

    /**
     * Determines if the {@link Character} at the specified position in the provided {@link String}
     * satisfies the {@link Predicate}.
     *
     * @param source    the {@link String}
     * @param position  the position with in the {@link String}
     * @param predicate the {@link Character} {@link Predicate}
     * @return {@code true} if the {@link Character} {@link Predicate} is satisfied, {@code false} otherwise
     */
    public static boolean follows(final String source,
                                  final int position,
                                  final Predicate<? super Character> predicate) {

        return source != null && !source.isEmpty()
            && predicate != null
            && position >= 0 && position < source.length()
            && predicate.test(source.charAt(position));
    }

    /**
     * Returns a new {@link String} with the {@link Character#isWhitespace(char)} {@link Character}s removed
     * from the provided input {@link String}. This includes spaces, tabs, newlines, and other whitespace characters.
     *
     * @param input the input {@link String} to process
     * @return the input {@link String} with {@link Character#isWhitespace(char)} {@link Character}s removed
     */
    public static String stripWhiteSpace(final String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        return input.replaceAll("\\s+", "").trim();
    }

    /**
     * Returns a new {@link String} with the {@link Character#isWhitespace(char)} {@link Character}s removed
     * from the provided input {@link String}, and the first letter of each word capitalized.
     *
     * @param input the input {@link String} to process
     * @return the processed {@link String} with {@link Character#isWhitespace(char)} {@link Character}s removed and
     * first letters capitalized
     */
    public static String stripWhiteSpaceAndCapitalizeFirstLetterOfEachWord(final String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        final var result = new StringBuilder();
        boolean capitalizeNext = true;

        for (int i = 0; i < input.length(); i++) {
            final var current = input.charAt(i);
            if (Character.isWhitespace(current)) {
                capitalizeNext = true;
            }
            else {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(current));
                    capitalizeNext = false;
                }
                else {
                    result.append(current);
                }
            }
        }
        return result.toString();
    }
}
