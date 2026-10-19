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

import build.base.configuration.AbstractValueOption;
import build.base.configuration.Default;
import build.base.configuration.Option;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

/**
 * An {@link Option} to define the working directory, which by default will be the {@code user.dir}.
 *
 * @author brian.oliver
 * @since Oct-2018
 */
public class WorkingDirectory
    extends AbstractValueOption<String> {

    /**
     * Constructs a {@link WorkingDirectory}.
     *
     * @param directory the directory
     */
    private WorkingDirectory(final String directory) {
        super(directory);
    }

    /**
     * Obtains a {@link Path} representation of the {@link WorkingDirectory} using the specified {@link FileSystem}.
     *
     * @param fileSystem the {@link FileSystem}
     * @return a {@link Path}
     */
    public Path path(final FileSystem fileSystem) {
        return fileSystem.getPath(get());
    }

    /**
     * Obtains a {@link Path} representation of the {@link WorkingDirectory} using the {@link FileSystems#getDefault()}.
     *
     * @return a {@link Path}
     */
    public Path path() {
        return path(FileSystems.getDefault());
    }

    /**
     * Creates a {@link WorkingDirectory}.
     *
     * @param directory the directory
     * @return a {@link WorkingDirectory}
     */
    public static WorkingDirectory of(final String directory) {
        return new WorkingDirectory(directory);
    }

    /**
     * Obtains the current {@link WorkingDirectory} for the user, which is defined by the system property
     * {@code user.dir}.
     *
     * @return a {@link WorkingDirectory}
     */
    @Default
    public static WorkingDirectory current() {
        return WorkingDirectory.of(System.getProperty("user.dir"));
    }
}
