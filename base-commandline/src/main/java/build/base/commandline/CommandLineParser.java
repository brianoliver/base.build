package build.base.commandline;

/*-
 * #%L
 * base.build Command Line
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

import build.base.configuration.CollectedOption;
import build.base.configuration.ConfigurationBuilder;
import build.base.configuration.Option;
import build.base.foundation.Capture;
import build.base.foundation.Introspection;
import build.base.foundation.Strings;
import build.base.table.Cell;
import build.base.table.Table;
import build.base.table.option.CellSeparator;
import build.base.table.option.RowComparator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parses {@link String} arguments, typically passed into a {@code public static void main(String... arguments)} method,
 * to return {@link Option}-based representations of the said {@link String}s in a {@link ConfigurationBuilder}.
 * <p>
 * The {@link CommandLineParser#parse(String...)} method processes {@link String} arguments formatted according to
 * conventions used by <a href="https://www.gnu.org/software/libc/manual/html_node/Argument-Syntax.html">POSIX-style</a>
 * commandline applications.  For example:
 * <ul>
 *      <li>{@link String} arguments are considered {@link Option}s if they begin with a hyphen {@code -} delimiter.</li>
 *      <li>{@link Option} <i>names</i> or <i>prefixes</i> consist of one or more alphanumeric characters.</li>
 *      <li>Certain {@link Option}s may require an argument. For example, the {@code -o} command of the {@code ld}
 *          POSIX command requires an argument — an output file name.</li>
 *      <li>An {@link Option} and its argument(s) may or may not appear as separate tokens.  In other words, the
 *          whitespace separating them is optional.  Thus, {@code -o foo}, {@code -ofoo} and {@code -o=foo} are
 *          equivalent.</li>
 *      <li>{@link Option}s precede other non-{@link Option} arguments.</li>
 * </ul>
 * <p>
 * The argument {@code --} terminates all {@link Option}s; any following arguments are treated as non-{@link Option}
 * arguments, even if they begin with a hyphen.
 * <p>
 * {@link Option}s may be supplied in any order, or appear multiple times. The interpretation is left up to the
 * {@link Class} of the {@link Option}.
 * <p>
 * GNU-style long options, prefixed with {@code --} followed by a name made of alphanumeric characters and dashes is
 * supported. {@link Option} prefixes are typically one to three words long, with hyphens to separate words.
 * <p>
 * To use a {@link CommandLineParser}, it must first be configured to support a known set of {@link Option}
 * {@link Class}es, each of which defines a {@code public static} {@link Method} that acts as factory for said
 * {@link Class}es of {@link Option}.  Each factory {@link Method} must be annotated with one or more
 * {@link CommandLine} annotations, each defining the <i>name</i> or <i>prefix</i> <i>including the necessary
 * hyphens</i> the {@link Option} uses.   Multiple {@link CommandLine} annotations for a single method are permitted
 * to allow both short, long and alternative names for the same {@link Option}.
 * <p>
 * By default {@link CommandLineParser}s do not support non-{@link Option} arguments.  To enable their support,
 * the {@link CommandLineParser} must be pre-configured, prior to parsing, using the
 * {@link #setUnknownOptionConsumer(UnknownOptionConsumer)}  method to use
 * {@link #CAPTURE_UNKNOWN_OPTIONS_AS_ARGUMENTS}.  When configured, all non-{@link Option}s presented are
 * captured and represented as {@link CollectedOption} {@link CommandLine.Argument}s in the
 * returned {@link ConfigurationBuilder}.
 * <p>
 * If '{@value OPTION_CLASS_OVERRIDE} {@code <fully.qualified.class.name> <argument [params]>}' is specified, the
 * {@link CommandLineParser} will only look for a {@link CommandLine.Prefix} argument from the specified option class.
 * This can be used to disambiguate any conflicting arguments defined in different classes. If an ambiguous argument
 * is parsed without this override, an {@link IllegalArgumentException} will be thrown with more information.
 * <p>
 * NOTE: {@value HELP_ARGUMENT_SHORT}, {@value #HELP_ARGUMENT_LONG}, and {@value #OPTION_CLASS_OVERRIDE} are reserved
 * arguments for the {@link CommandLineParser} and cannot be used as
 * {@link CommandLine.Prefix}es.
 * <p>
 * Support for customizing the consumption of non-{@link Option}s may be configured by setting an
 * {@link UnknownOptionConsumer} with {@link #setUnknownOptionConsumer(UnknownOptionConsumer)}.  Provided
 * implementations include:
 * <ul>
 *     <li>{@link #CAPTURE_UNKNOWN_OPTIONS_AS_ARGUMENTS}: Captures and converts unknown {@link Option}s into
 *                                                        {@link CommandLine.Argument}s</li>
 *     <li>{@link #IGNORE_UNKNOWN_OPTIONS}: Ignores unknown {@link Option}s</li>
 * </ul>
 *
 * @author brian.oliver
 * @author spencer.firestone
 * @see Option
 * @see ConfigurationBuilder
 * @see CommandLine
 * @see CommandLine.Argument
 * @since Sep-2020
 */
public class CommandLineParser {

    /**
     * An argument that will trigger a {@link HelpException}.
     */
    public static final String HELP_ARGUMENT_SHORT = "-h";

    /**
     * An argument that will trigger a {@link HelpException}.
     */
    public static final String HELP_ARGUMENT_LONG = "--help";

    /**
     * An argument that will allow a user to specify that the following argument should only be applied to a prefix
     * on the given {@link build.base.configuration.Option} class.
     */
    public static final String OPTION_CLASS_OVERRIDE = "--option-class";

    /**
     * An {@link UnknownOptionConsumer} that ignores unknown command-line arguments.
     */
    public static final UnknownOptionConsumer IGNORE_UNKNOWN_OPTIONS = new UnknownOptionConsumer() {
        @Override
        public boolean consume(final String argument, final ConfigurationBuilder configurationBuilder) {
            return true;
        }
    };

    /**
     * An {@link UnknownOptionConsumer} that captures and converts unknown {@link Option}s into
     * {@link CommandLine.Argument}s.
     */
    public static final UnknownOptionConsumer CAPTURE_UNKNOWN_OPTIONS_AS_ARGUMENTS = new UnknownOptionConsumer() {

        @Override
        public boolean consume(final String argument, final ConfigurationBuilder configurationBuilder) {

            final var canonical = Strings.trim(argument);

            // ignore empty arguments
            if (Strings.isEmpty(canonical)) {
                return true;
            }

            // ensure the unknown command-line argument doesn't appear to be an Option
            if (argument.startsWith("-")) {
                return false;
            }

            // add the unknown command-line argument as an Argument
            configurationBuilder.add(CommandLine.Argument.of(canonical));

            return true;
        }
    };

    /**
     * A {@link Comparator} that evaluates hyphens after alphanumeric characters, so that single-hyphen prefixes
     * will sort before double-hyphen prefixes.
     */
    private static final Comparator<String> PREFIX_COMPARATOR = new Comparator<>() {
        @Override
        public int compare(final String prefix1, final String prefix2) {
            if (prefix1.isEmpty() && prefix2.isEmpty()) {
                return 0;
            }

            if (prefix1.isEmpty()) {
                return -1;
            }

            if (prefix2.isEmpty()) {
                return 1;
            }

            if (prefix1.charAt(0) == prefix2.charAt(0)) {
                return compare(prefix1.substring(1), prefix2.substring(1));
            }
            if (prefix1.charAt(0) == '-') {
                return 1;
            }
            return prefix1.compareTo(prefix2);
        }
    };

    /**
     * {@link Method}s used to construct each {@link Option}, grouped by help section name.
     * <p>
     * Insertion order is preserved for both sections and methods within each section. The empty string
     * key represents the default (unsectioned) group, which renders without a section header.
     */
    private final LinkedHashMap<String, LinkedHashSet<Method>> sectionedMethods;

    /**
     * All registered prefixes per {@link Method}, used for help rendering.
     * <p>
     * Populated by every call to {@link #add(String, Class, Method)} so that both annotation-driven
     * and explicit-prefix registrations appear correctly in help output.
     */
    private final LinkedHashMap<Method, LinkedHashSet<String>> methodPrefixes;

    /**
     * Explicitly supplied descriptions per {@link Method}, used for help rendering when no
     * {@link CommandLine.Description} annotation is present on the method.
     */
    private final LinkedHashMap<Method, String> methodDescriptions;

    /**
     * The current help section name applied to subsequently registered {@link Option}s.
     * <p>
     * Defaults to {@code ""} (no section header). Changed via {@link #setHelpSection(String)}.
     */
    private String currentHelpSection;

    /**
     * A {@link Map} of construction {@link Method}s by {@link CommandLine.Prefix} value.
     */
    private final Map<String, Set<Method>> prefixMethods;

    /**
     * The {@link Capture}d {@link UnknownOptionConsumer} for the {@link CommandLineParser}.
     */
    private final Capture<UnknownOptionConsumer> unknownOptionConsumer;

    /**
     * The {@link Capture}d positional argument {@link Consumer} for the {@link CommandLineParser}.
     * <p>
     * When set, non-hyphen-prefixed arguments that do not match any registered {@link Option} prefix are
     * routed here rather than to the {@link #unknownOptionConsumer}.
     */
    private final Capture<Consumer<String>> positionalArgumentConsumer;

    /**
     * Should {@link #HELP_ARGUMENT_SHORT} and {@link #HELP_ARGUMENT_LONG} arguments be ignored.
     */
    private boolean suppressHelp;

    /**
     * Help text usage program name.
     */
    private String helpUsageProgramName;

    /**
     * Help text usage positional arguments label (e.g. {@code "<task>..."}).
     */
    private String helpUsageArguments;

    /**
     * An optional {@link Consumer} to call with the help text instead of throwing a {@link HelpException}.
     */
    private Consumer<String> helpHandler;

    /**
     * A {@link Function} used to resolve environment variable values by name.
     * <p>
     * Defaults to {@link System#getenv(String)} wrapped in {@link Optional#ofNullable(Object)}.
     * Set to {@code null} to disable environment variable resolution entirely.
     */
    private Function<String, Optional<String>> envVarResolver;

    /**
     * Registered environment variable mappings: environment variable name to command-line prefix.
     */
    private final LinkedHashMap<String, String> envVarMappings;

    /**
     * Registered commands: name to description. Insertion order preserved for help output.
     */
    private final LinkedHashMap<String, String> commands;

    /**
     * Registered categories: name to description. Insertion order preserved for help output.
     */
    private final LinkedHashMap<String, String> categories;

    /**
     * The section heading for the commands block in help output. Defaults to {@code "Commands:"}.
     */
    private String commandsSectionName;

    /**
     * The section heading for the categories block in help output. Defaults to {@code "Categories:"}.
     */
    private String categoriesSectionName;

    /**
     * Constructs a {@link CommandLineParser}.
     */
    public CommandLineParser() {
        // establish the map of methods by prefix, ordering them in by their prefix longest to shortest,
        // and alphabetically when they are the same, so that we can parse/match in that order (longest first)
        this.prefixMethods = new TreeMap<>((x, y) -> x.length() == y.length()
            ? x.compareTo(y)
            : y.length() - x.length());

        this.sectionedMethods = new LinkedHashMap<>();
        this.methodPrefixes = new LinkedHashMap<>();
        this.methodDescriptions = new LinkedHashMap<>();
        this.currentHelpSection = "";
        this.unknownOptionConsumer = Capture.empty();
        this.positionalArgumentConsumer = Capture.empty();
        this.envVarResolver = name -> Optional.ofNullable(System.getenv(name));
        this.envVarMappings = new LinkedHashMap<>();
        this.commands = new LinkedHashMap<>();
        this.categories = new LinkedHashMap<>();
        this.commandsSectionName = "Commands:";
        this.categoriesSectionName = "Categories:";
    }

    /**
     * Sets the program name for the {@link CommandLineParser}.
     *
     * @param programName what to use for the help text usage program name
     * @return this {@link CommandLineParser} to permit fluent-style method invocation
     */
    public CommandLineParser setHelpUsageProgramName(final String programName) {
        this.helpUsageProgramName = programName;

        return this;
    }

    /**
     * Sets the positional arguments label for the {@link CommandLineParser} help usage line.
     * <p>
     * When set alongside {@link #setHelpUsageProgramName(String)}, the usage line becomes:
     * {@code Usage: <programName> [options] <arguments>}.
     *
     * @param arguments the positional arguments label (e.g. {@code "<task>..."})
     * @return this {@link CommandLineParser} to permit fluent-style method invocation
     */
    public CommandLineParser setHelpUsageArguments(final String arguments) {
        this.helpUsageArguments = arguments;

        return this;
    }

    /**
     * Sets a {@link Consumer} to call with the help text instead of throwing a {@link HelpException}.
     * <p>
     * When set, {@link #parse(String...)} will invoke the handler and return an empty
     * {@link ConfigurationBuilder} rather than propagating a {@link HelpException}.
     *
     * @param handler the {@link Consumer} to receive the help text, or {@code null} to revert to exception behaviour
     * @return this {@link CommandLineParser} to permit fluent-style method invocation
     */
    public CommandLineParser setHelpHandler(final Consumer<String> handler) {
        this.helpHandler = handler;

        return this;
    }

    /**
     * Sets the current help section name for subsequently registered {@link Option}s.
     * <p>
     * All {@link Option}s added after this call will appear under the specified section heading in the
     * help output, until this method is called again with a different name. Pass {@code null} or an
     * empty string to return to the default (unsectioned) group.
     * <p>
     * Example output with sections:
     * <pre>
     * Options:
     *   Global options
     *     -w    ...
     *   Task options
     *     -j    ...
     * </pre>
     *
     * @param section the section heading, or {@code null} / empty to clear
     * @return this {@link CommandLineParser} to permit fluent-style method invocation
     */
    public CommandLineParser setHelpSection(final String section) {
        this.currentHelpSection = section == null ? "" : section;

        return this;
    }

    /**
     * Sets the environment variable resolver used to look up registered environment variables.
     * <p>
     * Defaults to {@link System#getenv(String)} wrapped in {@link Optional#ofNullable(Object)}.
     * Pass {@code null} to disable environment variable resolution entirely.
     *
     * @param resolver a function mapping an environment variable name to its {@link Optional} value,
     *                 or {@code null} to disable
     * @return this {@link CommandLineParser} to permit fluent-style method invocation
     */
    public CommandLineParser setEnvironmentVariableResolver(final Function<String, Optional<String>> resolver) {
        this.envVarResolver = resolver;

        return this;
    }

    /**
     * Registers an environment variable fallback for the specified command-line prefix.
     * <p>
     * When the registered environment variable is set and no explicit command-line value for the corresponding
     * option was provided, the environment variable value is injected as if the user had passed
     * {@code <prefix> <value>} on the command line. Command-line arguments always take precedence because
     * env-var-derived arguments are prepended before the actual arguments (last write wins).
     *
     * @param envVarName the name of the environment variable (e.g. {@code "MY_WORKING_DIR"})
     * @param prefix     the command-line prefix this variable maps to (e.g. {@code "-w"})
     * @return this {@link CommandLineParser} to permit fluent-style method invocation
     */
    public CommandLineParser registerEnvVar(final String envVarName, final String prefix) {
        Objects.requireNonNull(envVarName, "The env var name must not be null");
        Objects.requireNonNull(prefix, "The prefix must not be null");

        this.envVarMappings.put(envVarName, prefix);

        return this;
    }

    /**
     * Sets a {@link Consumer} that receives non-hyphen-prefixed arguments that do not match any registered
     * {@link Option} prefix.
     * <p>
     * This is the preferred way to declare first-class positional arguments (e.g. task names) rather than
     * relying on {@link #CAPTURE_UNKNOWN_OPTIONS_AS_ARGUMENTS}. When a positional consumer is set, hyphen-prefixed
     * unknowns still fall through to the {@link UnknownOptionConsumer}.
     *
     * @param consumer the {@link Consumer} to receive positional arguments, or {@code null} to clear
     * @return this {@link CommandLineParser} to permit fluent-style method invocation
     */
    public CommandLineParser setPositionalArgumentConsumer(final Consumer<String> consumer) {
        if (consumer == null) {
            this.positionalArgumentConsumer.clear();
        }
        else {
            this.positionalArgumentConsumer.set(consumer);
        }

        return this;
    }

    /**
     * Configures the {@link CommandLineParser} to ignore {@link #HELP_ARGUMENT_SHORT} and {@link #HELP_ARGUMENT_LONG}
     * arguments.
     *
     * @param suppressHelp {@code true} to ignore the help arguments, {@code false} to process them
     * @return this {@link CommandLineParser} to permit fluent-style method invocation
     */
    public CommandLineParser setSuppressHelp(final boolean suppressHelp) {
        this.suppressHelp = suppressHelp;

        return this;
    }

    /**
     * Sets the specified {@link UnknownOptionConsumer} for the {@link CommandLineParser}.
     *
     * @param consumer the {@link UnknownOptionConsumer}
     * @return this {@link CommandLineParser} to permit fluent-style method invocation
     */
    public CommandLineParser setUnknownOptionConsumer(final UnknownOptionConsumer consumer) {
        if (consumer == null) {
            this.unknownOptionConsumer.clear();
        }
        else {
            this.unknownOptionConsumer.set(consumer);
        }

        return this;
    }

    /**
     * Adds the specified {@link Class} of {@link Option} to the {@link CommandLineParser} for the provided prefix.
     * <p>
     * Unlike {@link #add(Class)}, the specified {@link Option} {@link Class} does not need a {@link Method} to be
     * annotated with the {@link CommandLine.Prefix}.
     *
     * @param prefix         the {@link Option} prefix
     * @param optionClass    the {@link Class} of {@link Option}
     * @param methodName     the name of the {@code static} {@link Method} to create the {@link Option}
     * @param parameterTypes the types of parameters for the {@link Method}
     * @return this {@link CommandLineParser} to permit fluent-style method invocation
     * @throws NoSuchMethodException when the specified {@link Method} can't be located on the {@link Class}
     */
    public CommandLineParser add(final String prefix,
                                 final Class<? extends Option> optionClass,
                                 final String methodName,
                                 final Class<?>... parameterTypes)
        throws NoSuchMethodException {

        Objects.requireNonNull(prefix, "The prefix must not be null");
        Objects.requireNonNull(optionClass, "The class of option must not be null");
        Objects.requireNonNull(methodName, "The method name must not be null");

        return add(prefix, optionClass, optionClass.getMethod(methodName, parameterTypes));
    }

    /**
     * Adds the specified {@link Class} of {@link Option} to the {@link CommandLineParser} for multiple prefixes,
     * with an explicit description for help output.
     * <p>
     * All prefixes are registered for parsing and appear together on a single help row. The description
     * is used in help output when no {@link CommandLine.Description} annotation is present on the method.
     *
     * @param prefixes       the {@link Option} prefixes (e.g. {@code List.of("--working-dir", "-w")})
     * @param optionClass    the {@link Class} of {@link Option}
     * @param methodName     the name of the {@code static} {@link Method} to create the {@link Option}
     * @param description    the description shown in help output
     * @param parameterTypes the types of parameters for the {@link Method}
     * @return this {@link CommandLineParser} to permit fluent-style method invocation
     * @throws NoSuchMethodException when the specified {@link Method} can't be located on the {@link Class}
     */
    public CommandLineParser add(final List<String> prefixes,
                                 final Class<? extends Option> optionClass,
                                 final String methodName,
                                 final String description,
                                 final Class<?>... parameterTypes)
        throws NoSuchMethodException {

        Objects.requireNonNull(prefixes, "The prefixes must not be null");
        Objects.requireNonNull(optionClass, "The class of option must not be null");
        Objects.requireNonNull(methodName, "The method name must not be null");

        final var method = optionClass.getMethod(methodName, parameterTypes);

        for (final String prefix : prefixes) {
            add(prefix, optionClass, method);
        }

        if (description != null && !description.isBlank()) {
            this.methodDescriptions.put(method, description);
        }

        return this;
    }

    /**
     * Adds the specified {@link Class} of {@link Option} to the {@link CommandLineParser} for the provided prefix.
     * <p>
     * Unlike {@link #add(Class)}, the specified {@link Option} {@link Class} does not need a {@link Method} to be
     * annotated with the {@link CommandLine.Prefix}.  Furthermore, the {@link Method} does not need to be defined
     * on the {@link Option} {@link Class}, it simply needs to be {@code static} and accessible.
     *
     * @param prefix      the {@link Option} prefix
     * @param optionClass the {@link Class} of {@link Option}
     * @param method      the {@code static} {@link Method} to create the {@link Option}
     * @return this {@link CommandLineParser} to permit fluent-style method invocation
     */
    public CommandLineParser add(final String prefix,
                                 final Class<? extends Option> optionClass,
                                 final Method method) {

        Objects.requireNonNull(prefix, "The prefix must not be null");
        Objects.requireNonNull(optionClass, "The class of option must not be null");
        Objects.requireNonNull(method, "The method must not be null");

        if (prefix.equals(CommandLineParser.HELP_ARGUMENT_SHORT)
            || prefix.equals(CommandLineParser.HELP_ARGUMENT_LONG)
            || prefix.equals(CommandLineParser.OPTION_CLASS_OVERRIDE)) {
            //            LOG.warn("'{}' is a reserved argument for the CommandLineParser. This prefix for the {} "
            //                + "option will be ignored.", prefix, optionClass.getName());
        }
        else {
            this.sectionedMethods
                .computeIfAbsent(this.currentHelpSection, k -> new LinkedHashSet<>())
                .add(method);
            this.methodPrefixes
                .computeIfAbsent(method, k -> new LinkedHashSet<>())
                .add(prefix);
            this.prefixMethods.compute(prefix, (__, methods) -> {
                if (methods == null) {
                    return new HashSet<>();
                }
                else {
                    //                    LOG.warn("An argument conflict has been detected for [{}] when parsing [{}]. "
                    //                            + "This argument has already been registered with the following classes: "
                    //                            + "[{}]. Use of this argument will require disambiguation by prefixing it "
                    //                            + "with '{} <fully.qualified.option.class>'.",
                    //                        prefix,
                    //                        method.getDeclaringClass(),
                    //                        methods.stream()
                    //                            .map(Method::getDeclaringClass)
                    //                            .map(Class::getName)
                    //                            .collect(Collectors.joining(", ")),
                    //                        OPTION_CLASS_OVERRIDE);
                    return methods;
                }
            }).add(method);
        }

        return this;
    }

    /**
     * Adds the specified {@link Class} of {@link CommandLine} annotated {@link Option} to the {@link CommandLineParser}.
     *
     * @param optionClass the {@link Class} of {@link CommandLine} {@link Option}
     * @return this {@link CommandLineParser} to permit fluent-style method invocation
     */
    public CommandLineParser add(final Class<? extends Option> optionClass) {
        if (optionClass == null) {
            return this;
        }

        // Find all static construction methods annotated with CommandLine.Prefix and register them
        final var foundMethod = Capture.of(false);

        Introspection.getVisibleMethods(optionClass, method ->
                (method.isAnnotationPresent(CommandLine.Prefix.class)
                    || method.isAnnotationPresent(CommandLine.Prefixes.class))
                    && method.getReturnType() == optionClass
                    && Modifier.isStatic(method.getModifiers()))
            .forEach(method ->
                Arrays.stream(method.getAnnotationsByType(CommandLine.Prefix.class))
                    .map(CommandLine.Prefix::value)
                    .forEach(prefix -> {
                        foundMethod.set(true);
                        add(prefix, optionClass, method);
                    }));

        // if no methods were found for this class, log a warning
        if (!foundMethod.get()) {
            //            LOG.warn("[{}] does not contain a static method annotated with @CommandLine.Prefix", describe(optionClass));
        }

        return this;
    }

    /**
     * Registers a named command for display in the help output.
     * <p>
     * Commands are purely declarative — registration does not affect parsing. They appear under a
     * {@code Commands:} section in help, sorted alphabetically by name.
     *
     * @param name        the command name as the user would type it (e.g. {@code "compile"})
     * @param description a short description shown alongside the name in help output
     * @return this {@link CommandLineParser} to permit fluent-style method invocation
     */
    public CommandLineParser registerCommand(final String name, final String description) {
        Objects.requireNonNull(name, "The command name must not be null");
        Objects.requireNonNull(description, "The command description must not be null");

        this.commands.put(name, description);

        return this;
    }

    /**
     * Registers a named category for display in the help output.
     * <p>
     * Categories are aggregate aliases that match multiple tasks at once (e.g. {@code "build"} runs
     * compile, test, and package). Registration is purely declarative and does not affect parsing.
     * They appear under a {@code Categories:} section in help, sorted alphabetically by name.
     *
     * @param name        the category name as the user would type it (e.g. {@code "build"})
     * @param description a short description shown alongside the name in help output
     * @return this {@link CommandLineParser} to permit fluent-style method invocation
     */
    public CommandLineParser registerCategory(final String name, final String description) {
        Objects.requireNonNull(name, "The category name must not be null");
        Objects.requireNonNull(description, "The category description must not be null");

        this.categories.put(name, description);

        return this;
    }

    /**
     * Sets the section heading used for the commands block in help output.
     *
     * @param name the section heading (e.g. {@code "Tasks:"})
     * @return this {@link CommandLineParser} to permit fluent-style method invocation
     */
    public CommandLineParser setCommandsSectionName(final String name) {
        this.commandsSectionName = Objects.requireNonNull(name, "The commands section name must not be null");

        return this;
    }

    /**
     * Sets the section heading used for the categories block in help output.
     *
     * @param name the section heading (e.g. {@code "Aliases:"})
     * @return this {@link CommandLineParser} to permit fluent-style method invocation
     */
    public CommandLineParser setCategoriesSectionName(final String name) {
        this.categoriesSectionName = Objects.requireNonNull(name, "The categories section name must not be null");

        return this;
    }

    /**
     * Scans the registered option classes and constructs and throws a {@link HelpException} using reflection on
     * {@link CommandLine} annotations.
     */
    private void throwHelpException(final String reason) {
        final var helpTable = Table.create();

        if (!Strings.isEmpty(reason)) {
            helpTable.addRow(reason);
        }

        if (this.helpUsageProgramName != null) {
            final var usageLine = new StringBuilder("Usage: ").append(this.helpUsageProgramName).append(" [options]");
            if (this.helpUsageArguments != null) {
                usageLine.append(" ").append(this.helpUsageArguments);
            }
            helpTable.addRow(usageLine.toString());
        }

        if (!this.commands.isEmpty()) {
            helpTable.addRow(this.commandsSectionName);

            final var commandsTable = Table.create();
            commandsTable.options().add(CellSeparator.of("    "));
            this.commands.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> commandsTable.addRow(Cell.of(entry.getKey()), Cell.of(entry.getValue())));

            helpTable.addRow(commandsTable.toString());
            helpTable.addRow("");
        }

        if (!this.categories.isEmpty()) {
            helpTable.addRow(this.categoriesSectionName);

            final var categoriesTable = Table.create();
            categoriesTable.options().add(CellSeparator.of("    "));
            this.categories.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> categoriesTable.addRow(Cell.of(entry.getKey()), Cell.of(entry.getValue())));

            helpTable.addRow(categoriesTable.toString());
            helpTable.addRow("");
        }

        helpTable.addRow("Options:");

        this.sectionedMethods.forEach((section, methods) -> {
            if (!section.isEmpty()) {
                helpTable.addRow("  " + section);
            }

            final var optionsTable = Table.create();
            optionsTable.options()
                .add(CellSeparator.of("    "))
                .add((RowComparator) (row1, row2) -> {
                    final String row1Prefix = row1.getCell(1).getLine(0).orElse("");
                    final String row2Prefix = row2.getCell(1).getLine(0).orElse("");
                    return PREFIX_COMPARATOR.compare(row1Prefix, row2Prefix);
                });

            methods.forEach(method -> {
                // create an internal table for the option information that orders rows by the prefix for that row
                final var optionTable = Table.create();

                // add the prefixes to the option table
                optionTable.addRow(String.join(", ",
                    Optional.ofNullable(this.methodPrefixes.get(method))
                        .orElseGet(LinkedHashSet::new)
                        .stream()
                        .collect(Collectors.toCollection(() -> new TreeSet<>(PREFIX_COMPARATOR)))));

                // create a new table for the description
                final var infoTable = Table.create();
                infoTable.options().add(CellSeparator.of("    "));

                // add the description if it exists (annotation takes precedence over explicit)
                Optional.ofNullable(method.getAnnotation(CommandLine.Description.class))
                    .map(CommandLine.Description::value)
                    .or(() -> Optional.ofNullable(this.methodDescriptions.get(method)))
                    .ifPresent(description -> infoTable.addRow(Cell.create(), Cell.of(description)));

                // add the info table to the optionTable
                optionTable.addRow(infoTable.toString());

                // add the table as a new row to the optionsTable
                optionsTable.addRow(Cell.create(), Cell.of(optionTable.toString()));
            });

            helpTable.addRow(optionsTable.toString());
        });

        throw new HelpException(helpTable.toString());
    }

    /**
     * Parses the specified command line arguments based on the configured {@link Option}s.
     *
     * @param arguments the command line arguments
     * @return a new {@link ConfigurationBuilder} containing the extracted {@link Option}s
     * @throws IllegalArgumentException should parsing fail
     * @see #add(Class)
     * @see #add(String, Class, Method)
     * @see #add(String, Class, String, Class[])
     */
    public ConfigurationBuilder parse(final String... arguments)
        throws IllegalArgumentException {

        try {
            return doParse(arguments);
        }
        catch (final HelpException e) {
            if (this.helpHandler != null) {
                this.helpHandler.accept(e.getMessage());
                return ConfigurationBuilder.create();
            }
            throw e;
        }
    }

    /**
     * Synthesizes command-line arguments derived from registered environment variables.
     * <p>
     * For each registered env var that has a value, injects {@code prefix} followed by the value
     * (for options with parameters) or just {@code prefix} (for zero-parameter flags).
     * Returns an empty list when no resolver is set or no env vars are registered.
     *
     * @return a list of synthesized arguments to prepend before the actual command-line arguments
     */
    private List<String> synthesizeEnvVarArgs() {
        if (this.envVarResolver == null || this.envVarMappings.isEmpty()) {
            return List.of();
        }

        final var args = new ArrayList<String>();

        this.envVarMappings.forEach((envVarName, prefix) ->
            this.envVarResolver.apply(envVarName).ifPresent(value -> {
                final var methods = this.prefixMethods.get(prefix);
                if (methods != null && !methods.isEmpty()) {
                    final var method = methods.iterator().next();
                    args.add(prefix);
                    if (method.getParameterCount() > 0) {
                        args.add(value);
                    }
                }
            }));

        return args;
    }

    /**
     * Internal implementation of {@link #parse(String...)}.
     */
    private ConfigurationBuilder doParse(final String... arguments) {

        final var configurationBuilder = ConfigurationBuilder.create();

        // prepend env-var-derived args; command-line args come after and override (last write wins)
        final var envVarArgs = synthesizeEnvVarArgs();
        final String[] effectiveArguments = envVarArgs.isEmpty()
            ? arguments
            : Stream.concat(envVarArgs.stream(),
                arguments != null ? Arrays.stream(arguments) : Stream.empty())
                .toArray(String[]::new);

        final var canonical = effectiveArguments == null || effectiveArguments.length == 0
            ? effectiveArguments
            : Arrays.stream(effectiveArguments)
                .map(Strings::trim)
                .filter(string -> !Strings.isEmpty(string))
                .flatMap(string -> Arrays.stream(string.split("=")))
                .peek(argument -> {
                    if (!this.suppressHelp
                        && (argument.equals(HELP_ARGUMENT_SHORT) || argument.equals(HELP_ARGUMENT_LONG))) {
                        throwHelpException(null);
                    }
                })
                .toArray(String[]::new);

        if (canonical == null || canonical.length == 0) {
            return configurationBuilder;
        }

        // parse each of the Options from the arguments
        final var argumentIndex = new AtomicInteger(0);

        while (argumentIndex.get() < canonical.length) {

            var argument = canonical[argumentIndex.getAndIncrement()];

            // ignore help arguments
            if (argument.equals(HELP_ARGUMENT_SHORT) || argument.equals(HELP_ARGUMENT_LONG)) {
                continue;
            }

            if (argument.equals("--")) {
                // terminate processing the options (as no more follow)
                break;
            }

            final var optionClassName = Capture.<String>empty();

            // process any --option-class arguments
            if (argument.equals(OPTION_CLASS_OVERRIDE)) {

                // --option-class needs at least two more arguments: FQN and the disambiguated argument
                if (canonical.length - argumentIndex.get() < 2) {
                    throwHelpException("Missing expected parameters for [" + OPTION_CLASS_OVERRIDE + "]. Expecting "
                        + "<fully.qualified.option.class> and the argument to be disambiguated");
                }

                // store the option class and advance the current argument
                optionClassName.set(canonical[argumentIndex.getAndIncrement()]);
                argument = canonical[argumentIndex.getAndIncrement()];
            }

            final var current = argument;

            if (!this.prefixMethods.keySet().stream()
                .filter(current::startsWith)
                .findFirst()
                .map(matched -> {

                    // determine the Method to construct the CommandLine Option
                    final Method method;
                    final var methods = this.prefixMethods.get(matched);
                    if (optionClassName.isPresent()) {
                        // --option-class was specified, attempt to match one of the methods
                        method = methods.stream()
                            .filter(m -> m.getDeclaringClass().getName().equals(optionClassName.get()))
                            .findFirst()
                            .orElseThrow(() ->
                                new IllegalArgumentException(String.format("A conflict was found for disambiguated "
                                        + "argument '%s %s %s'. However, no CommandLine.Prefix annotation for that "
                                        + "argument was found on that option class.",
                                    OPTION_CLASS_OVERRIDE, optionClassName.get(), matched)));
                    }
                    else if (methods.size() != 1) {
                        // --option-class was not specified and the argument is ambiguous. Throw an exception
                        // indicating that the --option-class argument must be used
                        throw new IllegalArgumentException(String.format("A conflict was found for argument %s. "
                                + "Please prefix this argument with '%s <fully.qualified.option.class>' to "
                                + "disambiguate.",
                            matched, OPTION_CLASS_OVERRIDE));
                    }
                    else {

                        // --option-class was not specified and the argument is not ambiguous
                        method = methods.iterator().next();
                    }

                    // determine the string remaining after the prefix is removed eg: -Dkey=value -> key=value
                    // (and remove the = if it immediately follows the prefix)
                    final var matchedLength = matched.length();
                    final var remaining = matchedLength < current.length()
                        ? current.substring(current.charAt(matchedLength) == '='
                        ? matchedLength + 1
                        : matchedLength)
                        : "";

                    // ensure when there's no parameters required, there's no additional text
                    final var parameterCount = method.getParameterCount();
                    if (parameterCount == 0 && remaining.length() > 0) {
                        throw new IllegalArgumentException("Expected [" + matched + "] but found [" + current + "]");
                    }

                    // ensure that if there are parameters required, there are at least that many arguments left
                    if (parameterCount > canonical.length - argumentIndex.get()) {
                        throwHelpException("Missing expected parameter for [" + matched + "]");
                    }

                    try {
                        // establish the formal parameters for the Method

                        // (there may be a first value we need to use from the option)
                        final var first = remaining.isEmpty()
                            ? Capture.<String>empty()
                            : Capture.of(remaining);

                        final var parameters = Arrays.stream(method.getParameterTypes())
                            .map(parameterClass -> Strings.convert(
                                    first.isPresent()
                                        ? first.consume()
                                        : canonical[argumentIndex.getAndIncrement()],
                                    parameterClass)
                                )
                            .toArray();

                        // create the Option by invoking the Method
                        method.setAccessible(true);
                        return (Option) method.invoke(null, parameters);

                    }
                    catch (final IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
                        throw new IllegalArgumentException(
                            "Failed to create [" + method.getDeclaringClass() + "] for [" + current + "]", e);
                    }
                })
                .map(configurationBuilder::add)
                .isPresent()) {

                // route non-hyphen-prefixed arguments to the positional consumer when set
                if (this.positionalArgumentConsumer.isPresent() && !current.startsWith("-")) {
                    this.positionalArgumentConsumer.map(consumer -> {
                        consumer.accept(current);
                        return null;
                    });
                }
                // otherwise attempt to consume as an unknown option
                else if (!this.unknownOptionConsumer
                    .map(consumer -> consumer.consume(current, configurationBuilder))
                    .orElse(false)) {

                    throwHelpException(
                        "Unknown option [" + current + "] at position [" + (argumentIndex.get() + 1) + "]");
                }
            }
        }

        return configurationBuilder;
    }

    /**
     * Indicates that a help request was made of the {@link CommandLineParser}. {@link #getMessage()} will contain the
     * help text.
     */
    public static class HelpException
        extends RuntimeException {

        /**
         * Constructs a {@link HelpException}.
         *
         * @param message the help text
         */
        private HelpException(final String message) {
            super(message);
        }
    }

    /**
     * Provides a mechanism to consume command-line arguments for which an {@link Option} could not be automatically
     * resolved when parsing with {@link CommandLineParser#parse(String...)}.
     */
    @FunctionalInterface
    public interface UnknownOptionConsumer {

        /**
         * Invoked by a {@link CommandLineParser} when a command-line argument is unknown and/or can't be resolved
         * into an {@link Option}.
         * <p>
         * Should the {@link UnknownOptionConsumer} be capable of consuming the specified command-line argument, this
         * method must return {@code true}, indicating that parsing may continue.  Should the specified command-line
         * argument be unknown to the {@link UnknownOptionConsumer} and/or not parsable, {@code false} must be returned.
         * <p>
         * Implementations of this method are free to mutate the provided {@link ConfigurationBuilder} during
         * processing. When parsing has been completed, the provided {@link ConfigurationBuilder} by type will be
         * returned to the application.
         *
         * @param argument             the unknown command-line argument
         * @param configurationBuilder the  {@link ConfigurationBuilder}
         * @return {@code true} when the command-line argument was consumed and thus parsing should continue,
         * {@code false} when the command-line argument could not be consumed
         */
        boolean consume(String argument, ConfigurationBuilder configurationBuilder);
    }
}
