package build.base.compile.testing;

/*-
 * #%L
 * base.build Compile Testing
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

import java.net.URI;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

/**
 * Factory methods for in-memory {@link JavaFileObject} instances.
 */
public final class JavaFileObjects {

    private JavaFileObjects() {
    }

    /**
     * Returns an in-memory source file with the given qualified class name and source text.
     *
     * @param qualifiedName fully-qualified class name, e.g. {@code com.example.Foo}
     * @param source        the Java source text
     */
    public static JavaFileObject forSourceString(final String qualifiedName,
                                                 final String source) {
        final URI uri = URI.create("mem:///" + qualifiedName.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension);
        return new SourceStringFileObject(uri, source);
    }

    /**
     * Returns an in-memory source file built by joining the given lines with newlines.
     *
     * @param qualifiedName fully-qualified class name, e.g. {@code com.example.Foo}
     * @param lines         lines of source text
     */
    public static JavaFileObject forSourceLines(final String qualifiedName,
                                                final String... lines) {
        return forSourceString(qualifiedName, String.join("\n", lines));
    }

    private static final class SourceStringFileObject extends SimpleJavaFileObject {

        private final String source;

        SourceStringFileObject(final URI uri, final String source) {
            super(uri, Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
            return source;
        }
    }
}
