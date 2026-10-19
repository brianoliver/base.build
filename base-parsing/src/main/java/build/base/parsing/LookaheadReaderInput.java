package build.base.parsing;

/*-
 * #%L
 * base.build Parsing
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

import build.base.io.LookaheadReader;

import java.io.IOException;
import java.io.Reader;

/**
 * A {@link ScannerInput} backed by a {@link LookaheadReader}.  Does not support backtracking.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public final class LookaheadReaderInput
    implements ScannerInput {

    private final LookaheadReader reader;

    /**
     * Constructs a {@link LookaheadReaderInput} for the given {@link Reader}.
     *
     * @param reader the reader
     */
    public LookaheadReaderInput(final Reader reader) {
        this.reader = reader instanceof LookaheadReader lr ? lr : new LookaheadReader(reader);
    }

    @Override
    public boolean available() {
        return reader.available();
    }

    @Override
    public int peek() {
        return reader.peek();
    }

    @Override
    public CharSequence peek(final int size) {
        return reader.peek(size);
    }

    @Override
    public CharSequence peekMaximum() {
        return reader.peekMaximum();
    }

    @Override
    public int consume() {
        return reader.consume();
    }

    @Override
    public String consume(final int size) {
        return reader.consume(size);
    }

    @Override
    public LookaheadReader.Location getLocation() {
        return reader.getLocation();
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
