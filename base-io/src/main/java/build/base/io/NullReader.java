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
import java.io.Reader;

/**
 * A {@link Reader} that always returns end-of-file when attempting to read from it.
 *
 * @author brian.oliver
 * @since Oct-2018
 */
public class NullReader
    extends Reader {

    /**
     * The global {@link NullReader} instance.
     */
    private static final NullReader READER = new NullReader();

    /**
     * Obtains the global {@link NullReader} instance.
     *
     * @return the {@link NullReader}
     */
    public static NullReader get() {
        return READER;
    }

    @Override
    public int read(final char[] buffer, final int offset, final int length)
        throws IOException {
        return -1;
    }

    @Override
    public void close()
        throws IOException {
        // there's nothing to do when closing
    }
}
