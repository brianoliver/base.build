package build.base.tar;

/*-
 * #%L
 * base.build Tar
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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An {@link InputStream} that reads entries from a POSIX ustar tar archive, including
 * the common extensions needed to read archives produced by other tools: the GNU
 * long-name/long-link extension ({@code L}/{@code K} typeflags) and POSIX PAX extended
 * headers ({@code x}/{@code g} typeflags).
 * <p>
 * Usage: create a {@link TarInputStream} wrapping an existing {@link InputStream},
 * then repeatedly call {@link #getNextEntry()} followed by {@link #read(byte[], int, int)}
 * to read the entry's data, until {@link #getNextEntry()} returns {@code null}.
 *
 * @author reed.vonredwitz
 * @since Jul-2026
 */
public final class TarInputStream extends InputStream {

    /**
     * The block size for tar archives.
     */
    private static final int BLOCK_SIZE = TarHeader.BLOCK_SIZE;

    /**
     * The underlying input stream.
     */
    private final InputStream in;

    /**
     * PAX extended header records set by a {@code g} (global) header block, which apply
     * to every entry until cancelled by a later {@code g} record with an empty value.
     */
    private final Map<String, String> globalPaxHeaders = new LinkedHashMap<>();

    /**
     * The number of unread data bytes remaining in the current entry.
     */
    private long bytesRemaining;

    /**
     * The number of unread padding bytes remaining after the current entry's data.
     */
    private long paddingRemaining;

    /**
     * The sparse chunk map of the current entry, or empty if it is not a GNU old-style sparse
     * entry. When non-empty, {@link #read(byte[], int, int)} reconstructs the entry's logical
     * (expanded) content by interleaving stored bytes from {@link #in} with zero-filled holes,
     * rather than reading {@link #bytesRemaining} bytes directly.
     */
    private List<TarHeader.SparseChunk> sparseChunks = List.of();

    /**
     * The index into {@link #sparseChunks} of the chunk at or after {@link #sparseLogicalPos}.
     */
    private int sparseChunkIndex;

    /**
     * The current read position within the current sparse entry's logical (expanded) content.
     */
    private long sparseLogicalPos;

    /**
     * The number of unread logical (expanded) bytes remaining in the current sparse entry.
     */
    private long sparseLogicalRemaining;

    /**
     * Constructs a {@link TarInputStream} wrapping the specified {@link InputStream}.
     *
     * @param in the underlying {@link InputStream}
     */
    public TarInputStream(final InputStream in) {
        this.in = in;
    }

    /**
     * Advances to the next entry in the archive, skipping any unread data and padding
     * remaining from the current entry.
     *
     * @return the next {@link TarEntry}, or {@code null} if the end of the archive has been reached
     * @throws IOException if an I/O error occurs
     */
    public TarEntry getNextEntry() throws IOException {
        skipFully(bytesRemaining + paddingRemaining);
        bytesRemaining = 0;
        paddingRemaining = 0;
        sparseChunks = List.of();
        sparseChunkIndex = 0;
        sparseLogicalPos = 0;
        sparseLogicalRemaining = 0;

        String longName = null;
        String longLinkName = null;
        Map<String, String> localPaxHeaders = null;
        while (true) {
            final var headerBytes = readFully(BLOCK_SIZE);
            if (headerBytes == null) {
                return null;
            }
            if (isZeroBlock(headerBytes)) {
                return atEndOfArchive() ? null : failEndOfArchive();
            }

            final var typeFlag = TarHeader.typeFlag(headerBytes);
            if (typeFlag == 'L') {
                // GNU long-name extension: the following data block holds the real entry name
                longName = readLongString(TarHeader.parseHeader(headerBytes).size());
                continue;
            }
            if (typeFlag == 'K') {
                // GNU long-link extension: the following data block holds the real link target
                longLinkName = readLongString(TarHeader.parseHeader(headerBytes).size());
                continue;
            }
            if (typeFlag == 'x') {
                // PAX extended header: applies only to the entry immediately following
                localPaxHeaders = readPaxRecords(TarHeader.parseHeader(headerBytes).size());
                continue;
            }
            if (typeFlag == 'g') {
                // PAX global extended header: applies to every subsequent entry until cancelled
                applyPaxRecords(globalPaxHeaders, readPaxRecords(TarHeader.parseHeader(headerBytes).size()));
                continue;
            }
            if (typeFlag == 'S') {
                return readSparseEntry(headerBytes, longName, longLinkName, localPaxHeaders);
            }

            var header = TarHeader.parseHeader(headerBytes);
            if (longName != null) {
                header = header.withName(longName);
            }
            if (longLinkName != null) {
                header = header.withLinkName(longLinkName);
            }
            header = applyPaxOverrides(header, localPaxHeaders);

            bytesRemaining = header.directory() ? 0 : header.size();
            paddingRemaining = computePadding(bytesRemaining);

            return new TarEntry(header);
        }
    }

    /**
     * Finishes parsing a GNU old-style sparse ({@code 'S'}) entry: reads any sparse extension
     * blocks the main block's {@code isextended} flag indicates follow, then sets up
     * {@link #read(byte[], int, int)} to reconstruct the entry's logical (expanded) content by
     * interleaving the stored chunk bytes with zero-filled holes.
     *
     * @param mainBlock       the already-read 512-byte sparse main header block
     * @param longName        a GNU long-name override from a preceding {@code L} block, or {@code null}
     * @param longLinkName    a GNU long-link override from a preceding {@code K} block, or {@code null}
     * @param localPaxHeaders the PAX headers set by an immediately preceding {@code x} block, or {@code null}
     * @return the parsed {@link TarEntry}
     * @throws IOException if an I/O error occurs
     */
    private TarEntry readSparseEntry(final byte[] mainBlock,
                                     final String longName,
                                     final String longLinkName,
                                     final Map<String, String> localPaxHeaders) throws IOException {
        var header = TarHeader.parseSparseMainBlock(mainBlock);

        if (TarHeader.sparseMainBlockIsExtended(mainBlock)) {
            var more = true;
            while (more) {
                final var extensionBlock = readFully(BLOCK_SIZE);
                if (extensionBlock == null) {
                    throw new IOException("Unexpected end of stream reading GNU sparse extension block");
                }
                header = header.withAdditionalSparseChunks(TarHeader.parseSparseExtensionBlock(extensionBlock));
                more = TarHeader.sparseExtensionBlockIsExtended(extensionBlock);
            }
        }

        if (longName != null) {
            header = header.withName(longName);
        }
        if (longLinkName != null) {
            header = header.withLinkName(longLinkName);
        }
        header = applyPaxOverrides(header, localPaxHeaders);

        final var storedSize = header.sparseChunks().stream().mapToLong(TarHeader.SparseChunk::length).sum();
        bytesRemaining = storedSize;
        paddingRemaining = computePadding(storedSize);
        sparseChunks = header.sparseChunks();
        sparseChunkIndex = 0;
        sparseLogicalPos = 0;
        sparseLogicalRemaining = header.size();

        return new TarEntry(header);
    }

    /**
     * Applies any applicable PAX overrides (global headers merged with, and overridden
     * by, this entry's local headers) to a parsed header.
     *
     * @param header          the header parsed from the entry's own block
     * @param localPaxHeaders the PAX headers set by an immediately preceding {@code x} block, or
     *                        {@code null} if none
     * @return the header with PAX overrides applied
     */
    private TarHeader applyPaxOverrides(final TarHeader header, final Map<String, String> localPaxHeaders) {
        if (globalPaxHeaders.isEmpty() && (localPaxHeaders == null || localPaxHeaders.isEmpty())) {
            return header;
        }
        final var effective = new LinkedHashMap<>(globalPaxHeaders);
        if (localPaxHeaders != null) {
            effective.putAll(localPaxHeaders);
        }

        var result = header;
        if (effective.containsKey("path")) {
            result = result.withName(effective.get("path"));
        }
        if (effective.containsKey("linkpath")) {
            result = result.withLinkName(effective.get("linkpath"));
        }
        if (effective.containsKey("size")) {
            result = result.withSize(Long.parseLong(effective.get("size")));
        }
        if (effective.containsKey("mtime")) {
            result = result.withModTime((long) Double.parseDouble(effective.get("mtime")));
        }
        if (effective.containsKey("uid") || effective.containsKey("gid")
            || effective.containsKey("uname") || effective.containsKey("gname")) {
            final var uid = effective.containsKey("uid") ? Integer.parseInt(effective.get("uid")) : result.uid();
            final var gid = effective.containsKey("gid") ? Integer.parseInt(effective.get("gid")) : result.gid();
            final var userName = effective.getOrDefault("uname", result.userName());
            final var groupName = effective.getOrDefault("gname", result.groupName());
            result = result.withOwner(uid, gid, userName, groupName);
        }
        return result;
    }

    /**
     * Merges freshly-parsed PAX records into a running set of global headers, honoring the
     * PAX convention that a record with an empty value cancels (removes) that key.
     *
     * @param target  the global headers accumulated so far, updated in place
     * @param records the records parsed from a {@code g} header block
     */
    private static void applyPaxRecords(final Map<String, String> target, final Map<String, String> records) {
        records.forEach((key, value) -> {
            if (value.isEmpty()) {
                target.remove(key);
            } else {
                target.put(key, value);
            }
        });
    }

    /**
     * Checks whether a zero block just read marks the end of the archive, i.e. it is
     * followed by a second zero block or immediately by the end of the stream.
     *
     * @return {@code true} if this is a valid end-of-archive marker
     * @throws IOException if an I/O error occurs
     */
    private boolean atEndOfArchive() throws IOException {
        final var next = readFully(BLOCK_SIZE);
        // POSIX requires two consecutive zero-filled records; well-formed archives always
        // write both. Immediate EOF after only one is accepted too, as a deliberate leniency
        // for archives that omit trailing padding, rather than failing on an otherwise-valid stream.
        return next == null || isZeroBlock(next);
    }

    /**
     * Throws to report a zero block that was not followed by a second zero block or
     * the end of the stream, indicating a malformed archive.
     *
     * @return never returns
     * @throws IOException always
     */
    private TarEntry failEndOfArchive() throws IOException {
        throw new IOException("Malformed tar archive: zero block not followed by a second zero block or end of stream");
    }

    /**
     * Reads a GNU long-name or long-link data block: {@code size} bytes of NUL-terminated
     * string data, followed by padding to the next block boundary.
     *
     * @param size the length of the data, as recorded in the preceding 'L' or 'K' header
     * @return the long string value
     * @throws IOException if an I/O error occurs
     */
    private String readLongString(final long size) throws IOException {
        final var bytes = readFully((int) size);
        if (bytes == null) {
            throw new IOException("Unexpected end of stream reading GNU long name/link data");
        }
        skipFully(computePadding(size));

        int end = 0;
        while (end < bytes.length && bytes[end] != 0) {
            end++;
        }
        return new String(bytes, 0, end, StandardCharsets.UTF_8);
    }

    /**
     * Reads and parses a PAX extended header data block into its key/value records.
     * <p>
     * Each record has the form {@code "<length> <key>=<value>\n"}, where {@code length} is
     * the decimal ASCII length of the entire record, including itself, the space, and the
     * trailing newline.
     *
     * @param size the length of the PAX data, as recorded in the preceding 'x' or 'g' header
     * @return the parsed records, in encounter order
     * @throws IOException if an I/O error occurs
     */
    private Map<String, String> readPaxRecords(final long size) throws IOException {
        final var data = readFully((int) size);
        if (data == null) {
            throw new IOException("Unexpected end of stream reading PAX extended header");
        }
        skipFully(computePadding(size));

        final var records = new LinkedHashMap<String, String>();
        var offset = 0;
        while (offset < data.length) {
            var spaceIndex = offset;
            while (spaceIndex < data.length && data[spaceIndex] != ' ') {
                spaceIndex++;
            }
            if (spaceIndex >= data.length) {
                break;
            }
            final var recordLength = Integer.parseInt(new String(data, offset, spaceIndex - offset, StandardCharsets.US_ASCII));
            final var recordEnd = offset + recordLength;

            final var kvStart = spaceIndex + 1;
            var equalsIndex = kvStart;
            while (equalsIndex < recordEnd && data[equalsIndex] != '=') {
                equalsIndex++;
            }
            final var key = new String(data, kvStart, equalsIndex - kvStart, StandardCharsets.UTF_8);
            final var valueStart = equalsIndex + 1;
            final var valueEnd = recordEnd - 1; // exclude trailing '\n'
            final var value = new String(data, valueStart, valueEnd - valueStart, StandardCharsets.UTF_8);
            records.put(key, value);

            offset = recordEnd;
        }
        return records;
    }

    @Override
    public int read() throws IOException {
        final var buffer = new byte[1];
        final var n = read(buffer, 0, 1);
        return n < 0 ? -1 : buffer[0] & 0xFF;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (!sparseChunks.isEmpty()) {
            return readSparse(b, off, len);
        }
        if (bytesRemaining <= 0) {
            return -1;
        }
        final int toRead = (int) Math.min(len, bytesRemaining);
        final int n = in.read(b, off, toRead);
        if (n < 0) {
            throw new IOException("Unexpected end of stream in tar entry data");
        }
        bytesRemaining -= n;
        return n;
    }

    /**
     * Reads from a GNU old-style sparse entry, reconstructing its logical (expanded) content
     * by returning zero-filled bytes for holes and, for stored ranges, bytes read from
     * {@link #in} (decrementing {@link #bytesRemaining} by exactly what was consumed).
     *
     * @param b   the destination buffer
     * @param off the offset into {@code b} to start writing at
     * @param len the maximum number of bytes to write
     * @return the number of bytes written, or {@code -1} at the end of the entry's logical content
     * @throws IOException if an I/O error occurs
     */
    private int readSparse(final byte[] b, final int off, final int len) throws IOException {
        if (sparseLogicalRemaining <= 0) {
            return -1;
        }
        while (sparseChunkIndex < sparseChunks.size()
            && sparseLogicalPos >= sparseChunks.get(sparseChunkIndex).offset() + sparseChunks.get(sparseChunkIndex).length()) {
            sparseChunkIndex++;
        }

        final var holeEnd = sparseChunkIndex < sparseChunks.size()
            ? sparseChunks.get(sparseChunkIndex).offset()
            : sparseLogicalPos + sparseLogicalRemaining;
        if (sparseLogicalPos < holeEnd) {
            final int n = (int) Math.min(len, holeEnd - sparseLogicalPos);
            Arrays.fill(b, off, off + n, (byte) 0);
            sparseLogicalPos += n;
            sparseLogicalRemaining -= n;
            return n;
        }

        final var chunk = sparseChunks.get(sparseChunkIndex);
        final var chunkRemaining = chunk.offset() + chunk.length() - sparseLogicalPos;
        final int toRead = (int) Math.min(Math.min(len, chunkRemaining), bytesRemaining);
        final int n = in.read(b, off, toRead);
        if (n < 0) {
            throw new IOException("Unexpected end of stream in sparse tar entry data");
        }
        bytesRemaining -= n;
        sparseLogicalPos += n;
        sparseLogicalRemaining -= n;
        return n;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    /**
     * Skips exactly the specified number of bytes from the underlying stream.
     *
     * @param count the number of bytes to skip
     * @throws IOException if an I/O error occurs or the stream ends prematurely
     */
    private void skipFully(final long count) throws IOException {
        long remaining = count;
        final var buffer = new byte[BLOCK_SIZE];
        while (remaining > 0) {
            final int toRead = (int) Math.min(buffer.length, remaining);
            final int n = in.read(buffer, 0, toRead);
            if (n < 0) {
                throw new IOException("Unexpected end of stream while skipping tar entry data");
            }
            remaining -= n;
        }
    }

    /**
     * Reads exactly the specified number of bytes from the underlying stream.
     *
     * @param count the number of bytes to read
     * @return the bytes read, or {@code null} if the stream was already at end of file
     * @throws IOException if an I/O error occurs or the stream ends prematurely after starting to read
     */
    private byte[] readFully(final int count) throws IOException {
        final var buffer = new byte[count];
        int offset = 0;
        while (offset < count) {
            final int n = in.read(buffer, offset, count - offset);
            if (n < 0) {
                if (offset == 0) {
                    return null;
                }
                throw new IOException("Unexpected end of stream reading tar header");
            }
            offset += n;
        }
        return buffer;
    }

    /**
     * Determines whether a block consists entirely of zero bytes.
     *
     * @param block the block to check
     * @return {@code true} if every byte is zero
     */
    private static boolean isZeroBlock(final byte[] block) {
        for (final byte b : block) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Computes the number of padding bytes needed after {@code size} bytes of data
     * to reach a 512-byte block boundary.
     *
     * @param size the number of data bytes
     * @return the number of padding bytes
     */
    private static long computePadding(final long size) {
        final var remainder = size % BLOCK_SIZE;
        return remainder == 0 ? 0 : BLOCK_SIZE - remainder;
    }
}
