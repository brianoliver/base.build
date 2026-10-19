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

import java.util.ArrayList;

/**
 * A {@link ScannerInput} backed by a {@link String}.  Supports full backtracking via {@link #save()} and
 * {@link #restore(int)}.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public final class StringInput
    implements ScannerInput {

    private record Snapshot(int index, int line, int column, long offset) {
    }

    private final char[] chars;
    private final ArrayList<Snapshot> snapshots = new ArrayList<>();
    private int index;
    private int line;
    private int column;
    private long offset;

    /**
     * Constructs a {@link StringInput} for the given string.
     *
     * @param string the input string
     */
    public StringInput(final String string) {
        this.chars = string.toCharArray();
        this.index = 0;
        this.line = 1;
        this.column = 1;
        this.offset = 0;
    }

    @Override
    public boolean available() {
        return index < chars.length;
    }

    @Override
    public int peek() {
        return available() ? chars[index] : -1;
    }

    @Override
    public CharSequence peek(final int size) {
        if (!available()) {
            return "";
        }
        return new String(chars, index, Math.min(size, chars.length - index));
    }

    @Override
    public CharSequence peekMaximum() {
        return available() ? new String(chars, index, chars.length - index) : "";
    }

    @Override
    public int consume() {
        if (!available()) {
            return -1;
        }
        final int c = chars[index++];
        if ((c == '\r' && (index >= chars.length || chars[index] != '\n')) || c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        offset++;
        return c;
    }

    @Override
    public String consume(final int size) {
        final var sb = new StringBuilder(size);
        for (int i = 0; i < size && available(); i++) {
            sb.append((char) consume());
        }
        return sb.toString();
    }

    @Override
    public LookaheadReader.Location getLocation() {
        return LookaheadReader.Location.of(line, column, offset);
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public boolean supportsBacktracking() {
        return true;
    }

    @Override
    public int save() {
        final int id = snapshots.size();
        snapshots.add(new Snapshot(index, line, column, offset));
        return id;
    }

    @Override
    public void restore(final int checkpoint) {
        final Snapshot snap = snapshots.get(checkpoint);
        this.index = snap.index();
        this.line = snap.line();
        this.column = snap.column();
        this.offset = snap.offset();
        snapshots.subList(checkpoint + 1, snapshots.size()).clear();
    }
}
