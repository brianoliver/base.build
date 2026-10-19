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

import build.base.configuration.AbstractValueOption;
import build.base.configuration.CollectedOption;
import build.base.configuration.Option;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

/**
 * An {@link Option} that can be converted to a {@link Stream} of {@link String}s for use on a command line.
 *
 * @author brian.oliver
 * @author spencer.firestone
 * @since Sep-2020
 */
public interface CommandLine
    extends Option {

    /**
     * Indicates a {@code public} {@code static} annotated {@link Method} returning an {@link Option} defined on the
     * said {@link Class} {@link Option}, may be used for parsing command line arguments with a
     * {@link CommandLineParser}.
     * <p>
     * For example, the following defines an {@link Option} that takes a single boolean parameter, thus supporting
     * being used with a {@link CommandLineParser}.
     * <pre>
     * <code>
     * public class Enabled implements Option {
     *
     *      ...
     *
     *      &#x40;Prefix("-enabled")
     *      &#x40;Prefix("-e")
     *      public static Enabled create(boolean value) {
     *          return new Enabled(value);
     *      }
     * }
     * </code>
     * </pre>
     * <p>
     * NOTE: {@link CommandLineParser#HELP_ARGUMENT_SHORT} and {@link CommandLineParser#HELP_ARGUMENT_LONG} are
     * reserved arguments for the {@link CommandLineParser} and cannot be used as {@link Prefix}es.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD })
    @Repeatable(Prefixes.class)
    @interface Prefix {

        /**
         * The <i>name</i> or <i>prefix</i> of the {@link Prefix} option.
         * <p>
         * To support POSIX-style commandline options, the name should be prefixed with one or more hyphens.
         * For example:
         * <p>
         * {@code -size} or {@code --size}
         *
         * @return the name of the {@link Prefix} option
         */
        String value();
    }

    /**
     * Defines zero or more {@link Prefix}es.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD })
    @interface Prefixes {

        /**
         * The repeated {@link Prefix}es.
         *
         * @return the {@link Prefix}es
         */
        Prefix[] value();
    }

    /**
     * For a given {@link Prefix}ed method, the help text to be displayed.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD })
    @interface Description {

        /**
         * The help text to be displayed for a given {@link Prefix}ed method.
         *
         * @return the help text
         */
        String value();
    }

    /**
     * Converts a {@link CommandLine} into a {@link Stream} of {@link String}s representing a prefixed option.
     *
     * @return a {@link Stream} of {@link String}s
     */
    Stream<String> arguments();

    /**
     * An argument appearing after the parsing of any {@link Prefix}es.
     */
    class Argument
        extends AbstractValueOption<String>
        implements CollectedOption<List> {

        /**
         * Constructs an {@link Argument}.
         *
         * @param string the argument
         */
        private Argument(final String string) {
            super(string);
        }

        /**
         * Constructs an {@link Argument}.
         *
         * @param string the argument
         * @return an {@link Argument}
         */
        public static Argument of(final String string) {
            return new Argument(string);
        }
    }
}
