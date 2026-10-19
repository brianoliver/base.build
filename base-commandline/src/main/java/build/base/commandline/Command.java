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

import build.base.commandline.CommandLineParser.UnknownOptionConsumer;
import build.base.configuration.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A fluent builder for constructing a {@link CommandLineParser}, providing a chainable API for registering
 * {@link Option}s, positional argument labels, help handlers, help section grouping, and environment variable
 * fallbacks.
 * <p>
 * All {@link Option} registrations are deferred until {@link #build()} is called. Any {@link NoSuchMethodException}
 * raised during registration is wrapped in an {@link IllegalArgumentException}, eliminating the need for callers
 * to handle checked exceptions.
 * <p>
 * Example:
 * <pre>{@code
 * CommandLineParser parser = Command.create("spin")
 *     .argument("<task>...")
 *     .helpHandler(help -> { System.out.println(help); System.exit(0); })
 *     .unknownOption(CommandLineParser.IGNORE_UNKNOWN_OPTIONS)
 *     .section("Global options")
 *     .option("-w", WorkingDirectory.class, "of", String.class)
 *     .option(NetworkAccess.class)
 *     .envVar("SPIN_WORKING_DIR", "-w")
 *     .section("Task options")
 *     .option(JavaTask.class)
 *     .build();
 * }</pre>
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 * @see CommandLineParser
 */
public final class Command {

    /**
     * The program name, used in the help usage line.
     */
    private final String name;

    /**
     * The positional arguments label for the help usage line (e.g. {@code "<task>..."}).
     */
    private String usageArguments;

    /**
     * An optional {@link Consumer} to call with help text instead of throwing a {@link CommandLineParser.HelpException}.
     */
    private Consumer<String> helpHandler;

    /**
     * The {@link UnknownOptionConsumer} for unrecognised arguments.
     */
    private UnknownOptionConsumer unknownOptionConsumer;

    /**
     * Deferred registration actions applied to the {@link CommandLineParser} during {@link #build()}.
     */
    private final List<Consumer<CommandLineParser>> registrations;

    /**
     * Constructs a {@link Command} for the specified program name.
     *
     * @param name the program name
     */
    private Command(final String name) {
        this.name = name;
        this.registrations = new ArrayList<>();
    }

    /**
     * Creates a {@link Command} for the specified program name.
     *
     * @param name the program name (used in the help usage line)
     * @return a new {@link Command}
     */
    public static Command create(final String name) {
        return new Command(name);
    }

    /**
     * Sets the positional arguments label for the help usage line.
     * <p>
     * When set alongside a program name, the usage line becomes:
     * {@code Usage: <name> [options] <arguments>}.
     *
     * @param label the positional arguments label (e.g. {@code "<task>..."})
     * @return this {@link Command}
     */
    public Command argument(final String label) {
        this.usageArguments = label;

        return this;
    }

    /**
     * Sets a {@link Consumer} to call with help text instead of throwing a {@link CommandLineParser.HelpException}.
     *
     * @param handler the handler to receive the formatted help text
     * @return this {@link Command}
     */
    public Command helpHandler(final Consumer<String> handler) {
        this.helpHandler = handler;

        return this;
    }

    /**
     * Sets the {@link UnknownOptionConsumer} for arguments that do not match any registered {@link Option} prefix.
     *
     * @param consumer the consumer, or {@code null} to clear
     * @return this {@link Command}
     */
    public Command unknownOption(final UnknownOptionConsumer consumer) {
        this.unknownOptionConsumer = consumer;

        return this;
    }

    /**
     * Registers a named command for display in the help output.
     * <p>
     * Commands appear under a {@code Commands:} section in {@code --help}, sorted alphabetically by name.
     * Registration is purely declarative — it does not affect parsing or validation.
     *
     * @param name        the command name as the user would type it (e.g. {@code "compile"})
     * @param description a short description shown alongside the name in help output
     * @return this {@link Command}
     */
    public Command command(final String name, final String description) {
        this.registrations.add(parser -> parser.registerCommand(name, description));

        return this;
    }

    /**
     * Registers a named category for display in the help output.
     * <p>
     * Categories are aggregate aliases that match multiple tasks at once (e.g. {@code "build"} runs
     * compile, test, and package). They appear under a {@code Categories:} section in {@code --help},
     * sorted alphabetically by name. Registration is purely declarative — it does not affect parsing.
     *
     * @param name        the category name as the user would type it (e.g. {@code "build"})
     * @param description a short description shown alongside the name in help output
     * @return this {@link Command}
     */
    public Command category(final String name, final String description) {
        this.registrations.add(parser -> parser.registerCategory(name, description));

        return this;
    }

    /**
     * Sets the section heading used for the commands block in help output.
     * <p>
     * Defaults to {@code "Commands:"}. Use this to override the heading — for example, a build
     * tool might prefer {@code "Tasks:"}.
     *
     * @param name the section heading
     * @return this {@link Command}
     */
    public Command commandsSectionName(final String name) {
        this.registrations.add(parser -> parser.setCommandsSectionName(name));

        return this;
    }

    /**
     * Sets the section heading used for the categories block in help output.
     * <p>
     * Defaults to {@code "Categories:"}. Use this to override the heading — for example, a build
     * tool might prefer {@code "Aliases:"}.
     *
     * @param name the section heading
     * @return this {@link Command}
     */
    public Command categoriesSectionName(final String name) {
        this.registrations.add(parser -> parser.setCategoriesSectionName(name));

        return this;
    }

    /**
     * Sets the current help section name for subsequently registered {@link Option}s.
     * <p>
     * All options added after this call appear under the specified heading in the help output,
     * mirroring cliffy's {@code .group()} API. Call again with a different name to start a new
     * section, or with {@code null} to return to the default (unsectioned) group.
     *
     * @param name the section heading (e.g. {@code "Global options"}), or {@code null} to clear
     * @return this {@link Command}
     */
    public Command section(final String name) {
        this.registrations.add(parser -> parser.setHelpSection(name));

        return this;
    }

    /**
     * Registers a {@link CommandLine}-annotated {@link Option} class.
     * <p>
     * The class must have at least one {@code static} method annotated with {@link CommandLine.Prefix}.
     *
     * @param optionClass the {@link Class} of {@link Option} to register
     * @return this {@link Command}
     */
    public Command option(final Class<? extends Option> optionClass) {
        this.registrations.add(parser -> parser.add(optionClass));

        return this;
    }

    /**
     * Registers an {@link Option} class with an explicit prefix and factory method.
     * <p>
     * The {@link Option} class does not need a {@link CommandLine.Prefix}-annotated method; the factory method is
     * located by name and parameter types at {@link #build()} time. Any {@link NoSuchMethodException} is wrapped
     * in an {@link IllegalArgumentException}.
     *
     * @param prefix         the command-line prefix (e.g. {@code "-w"} or {@code "--working-dir"})
     * @param optionClass    the {@link Class} of {@link Option}
     * @param methodName     the name of the {@code static} factory method
     * @param parameterTypes the parameter types of the factory method
     * @return this {@link Command}
     */
    public Command option(final String prefix,
                          final Class<? extends Option> optionClass,
                          final String methodName,
                          final Class<?>... parameterTypes) {

        this.registrations.add(parser -> {
            try {
                parser.add(prefix, optionClass, methodName, parameterTypes);
            }
            catch (final NoSuchMethodException e) {
                throw new IllegalArgumentException(
                    "No method [" + methodName + "] with the given parameter types on ["
                        + optionClass.getName() + "]", e);
            }
        });

        return this;
    }

    /**
     * Registers an {@link Option} class with multiple prefixes, a factory method, and an explicit description
     * for help output.
     * <p>
     * All prefixes are registered for parsing and appear together on a single help row. Use this when the
     * {@link Option} class cannot carry {@link CommandLine.Prefix} or {@link CommandLine.Description} annotations
     * (e.g. because it lives in a module that does not depend on {@code base-commandline}).
     *
     * @param prefixes       the command-line prefixes (e.g. {@code List.of("--working-dir", "-w")})
     * @param optionClass    the {@link Class} of {@link Option}
     * @param methodName     the name of the {@code static} factory method
     * @param description    the description shown in help output
     * @param parameterTypes the parameter types of the factory method
     * @return this {@link Command}
     */
    public Command option(final List<String> prefixes,
                          final Class<? extends Option> optionClass,
                          final String methodName,
                          final String description,
                          final Class<?>... parameterTypes) {

        this.registrations.add(parser -> {
            try {
                parser.add(prefixes, optionClass, methodName, description, parameterTypes);
            }
            catch (final NoSuchMethodException e) {
                throw new IllegalArgumentException(
                    "No method [" + methodName + "] with the given parameter types on ["
                        + optionClass.getName() + "]", e);
            }
        });

        return this;
    }

    /**
     * Registers an environment variable fallback for the specified command-line prefix.
     * <p>
     * When the registered environment variable is set and the option has not been provided on the command line,
     * the environment variable value is injected as if the user had passed {@code <prefix> <value>}.
     * Command-line arguments always take precedence.
     * <p>
     * The default resolver uses {@link System#getenv(String)}. To use a custom resolver
     * (e.g. for testing), call {@link #envVarResolver(Function)}.
     *
     * @param envVarName the name of the environment variable (e.g. {@code "SPIN_WORKING_DIR"})
     * @param prefix     the command-line prefix this variable maps to (e.g. {@code "-w"})
     * @return this {@link Command}
     */
    public Command envVar(final String envVarName, final String prefix) {
        this.registrations.add(parser -> parser.registerEnvVar(envVarName, prefix));

        return this;
    }

    /**
     * Sets a custom environment variable resolver used to look up registered environment variables.
     * <p>
     * Defaults to {@link System#getenv(String)} wrapped in {@link Optional#ofNullable(Object)}.
     * Pass {@code null} to disable environment variable resolution entirely.
     *
     * @param resolver a function that maps an environment variable name to its value, or {@code null} to disable
     * @return this {@link Command}
     */
    public Command envVarResolver(final Function<String, Optional<String>> resolver) {
        this.registrations.add(parser -> parser.setEnvironmentVariableResolver(resolver));

        return this;
    }

    /**
     * Sets a {@link Consumer} to receive positional (non-option) arguments — arguments that do not begin with
     * {@code -} and do not match any registered {@link Option} prefix.
     * <p>
     * This is the preferred way to declare first-class positional arguments (e.g. task names) rather than relying
     * on {@link CommandLineParser#CAPTURE_UNKNOWN_OPTIONS_AS_ARGUMENTS}. Hyphen-prefixed unknowns still fall
     * through to the {@link CommandLineParser.UnknownOptionConsumer}.
     *
     * @param consumer the {@link Consumer} to receive each positional argument value, or {@code null} to clear
     * @return this {@link Command}
     */
    public Command positionalArgument(final Consumer<String> consumer) {
        this.registrations.add(parser -> parser.setPositionalArgumentConsumer(consumer));

        return this;
    }

    /**
     * Builds and returns a configured {@link CommandLineParser}.
     *
     * @return a new {@link CommandLineParser} configured according to this {@link Command}
     * @throws IllegalArgumentException if any registered option method cannot be found
     */
    public CommandLineParser build() {
        final var parser = new CommandLineParser()
            .setHelpUsageProgramName(this.name);

        if (this.usageArguments != null) {
            parser.setHelpUsageArguments(this.usageArguments);
        }

        if (this.helpHandler != null) {
            parser.setHelpHandler(this.helpHandler);
        }

        if (this.unknownOptionConsumer != null) {
            parser.setUnknownOptionConsumer(this.unknownOptionConsumer);
        }

        this.registrations.forEach(registration -> registration.accept(parser));

        return parser;
    }
}
