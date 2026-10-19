package build.base.io;

/*-
 * #%L
 * base.build IO
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

import java.io.IOException;
import java.io.Writer;

/**
 * A {@link Writer} that drops all content written to it.
 *
 * @author brian.oliver
 * @since Oct-2018
 */
public class NullWriter
    extends Writer {

    /**
     * The global {@link NullWriter} instance.
     */
    private static final NullWriter WRITER = new NullWriter();

    /**
     * Obtains the global {@link NullWriter} instance.
     *
     * @return the {@link NullWriter}
     */
    public static NullWriter get() {
        return WRITER;
    }

    @Override
    public void write(final char[] buffer, final int offset, final int length) {
        // there's nothing to do when writing
    }

    @Override
    public void flush() {
        // there's nothing to do when flushing
    }

    @Override
    public void close()
        throws IOException {
        // there's nothing to do when closing
    }
}
