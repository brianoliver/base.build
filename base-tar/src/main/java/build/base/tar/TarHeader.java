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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a POSIX ustar tar entry header (512 bytes).
 * <p>
 * {@link #sparseChunks()} is non-empty only for entries read from, or intended to be written
 * as, a GNU old-style sparse ({@code 'S'} typeflag) entry: {@link #size()} is then the entry's
 * full logical (expanded) size, and the chunks describe which byte ranges of that logical size
 * are actually stored in the archive, with the remaining ranges implicitly zero-filled holes.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public record TarHeader(String name, long size, long modTime, boolean directory, int permissions, String linkName,
                        int uid, int gid, String userName, String groupName, boolean hardlink,
                        List<SparseChunk> sparseChunks) {

    /**
     * The size of a tar header/block in bytes.
     */
    public static final int BLOCK_SIZE = 512;

    /**
     * The ustar magic string.
     */
    private static final String USTAR_MAGIC = "ustar";

    /**
     * The ustar version.
     */
    private static final String USTAR_VERSION = "00";

    /**
     * The number of sparse (offset, length) chunk entries that fit directly in a GNU
     * old-style sparse main header block.
     */
    private static final int SPARSE_CHUNKS_IN_MAIN_BLOCK = 4;

    /**
     * The number of sparse (offset, length) chunk entries that fit in each GNU old-style
     * sparse extension block.
     */
    private static final int SPARSE_CHUNKS_PER_EXTENSION_BLOCK = 21;

    /**
     * Canonical constructor; defensively copies {@link #sparseChunks()}.
     */
    public TarHeader {
        sparseChunks = List.copyOf(sparseChunks);
    }

    /**
     * A contiguous range of a GNU old-style sparse entry's logical (expanded) content that is
     * actually stored in the archive.
     * <p>
     * Nothing in the format requires {@link #offset()}/{@link #length()} to be aligned to any
     * boundary, but GNU tar's own extractor has been observed to silently drop chunk data at
     * arbitrary byte offsets it did not write itself, while chunks aligned to a filesystem block
     * size (e.g. 4096) round-trip correctly. Since any real sparse-file detector (POSIX
     * {@code SEEK_HOLE}/{@code SEEK_DATA}, {@code FIEMAP}, etc.) already reports block- or
     * extent-aligned ranges, this is not a practical limitation, but hand-built chunks for tests
     * or synthetic archives should stick to block-aligned offsets and lengths to extract cleanly
     * with real tar.
     *
     * @param offset the byte offset of this chunk within the entry's logical content
     * @param length the number of stored bytes in this chunk
     */
    public record SparseChunk(long offset, long length) {
    }

    /**
     * Creates a {@link TarHeader} for the specified entry.
     *
     * @param name        the entry name
     * @param size        the entry size in bytes (0 for directories)
     * @param modTime     the modification time in seconds since epoch
     * @param directory   whether the entry is a directory
     * @param permissions the POSIX permissions (e.g. 0755)
     * @return a new {@link TarHeader}
     */
    public static TarHeader createHeader(final String name,
                                         final long size,
                                         final long modTime,
                                         final boolean directory,
                                         final int permissions) {
        return new TarHeader(name, size, modTime, directory, permissions, "", 0, 0, "", "", false, List.of());
    }

    /**
     * Creates a {@link TarHeader} for the specified entry, with explicit ownership.
     *
     * @param name        the entry name
     * @param size        the entry size in bytes (0 for directories)
     * @param modTime     the modification time in seconds since epoch
     * @param directory   whether the entry is a directory
     * @param permissions the POSIX permissions (e.g. 0755)
     * @param uid         the numeric owner id
     * @param gid         the numeric group id
     * @param userName    the owner name
     * @param groupName   the group name
     * @return a new {@link TarHeader}
     */
    public static TarHeader createHeader(final String name,
                                         final long size,
                                         final long modTime,
                                         final boolean directory,
                                         final int permissions,
                                         final int uid,
                                         final int gid,
                                         final String userName,
                                         final String groupName) {
        return new TarHeader(name, size, modTime, directory, permissions, "", uid, gid, userName, groupName, false, List.of());
    }

    /**
     * Creates a {@link TarHeader} for a symbolic link entry.
     *
     * @param name        the entry name
     * @param linkTarget  the path the symlink points to
     * @param modTime     the modification time in seconds since epoch
     * @param permissions the POSIX permissions (e.g. 0777)
     * @return a new {@link TarHeader}
     */
    public static TarHeader createSymlinkHeader(final String name,
                                                final String linkTarget,
                                                final long modTime,
                                                final int permissions) {
        return new TarHeader(name, 0, modTime, false, permissions, linkTarget, 0, 0, "", "", false, List.of());
    }

    /**
     * Creates a {@link TarHeader} for a hard link entry.
     *
     * @param name        the entry name
     * @param linkTarget  the archive-relative path of the file this entry is linked to
     * @param modTime     the modification time in seconds since epoch
     * @param permissions the POSIX permissions (e.g. 0644)
     * @return a new {@link TarHeader}
     */
    public static TarHeader createHardlinkHeader(final String name,
                                                 final String linkTarget,
                                                 final long modTime,
                                                 final int permissions) {
        return new TarHeader(name, 0, modTime, false, permissions, linkTarget, 0, 0, "", "", true, List.of());
    }

    /**
     * Creates a {@link TarHeader} for a GNU old-style sparse file entry.
     *
     * @param name         the entry name
     * @param realSize     the full logical (expanded) size of the file
     * @param sparseChunks the stored byte ranges within the logical content; any range not
     *                     covered is an implicit zero-filled hole
     * @param modTime      the modification time in seconds since epoch
     * @param permissions  the POSIX permissions (e.g. 0644)
     * @return a new {@link TarHeader}
     */
    public static TarHeader createSparseHeader(final String name,
                                               final long realSize,
                                               final List<SparseChunk> sparseChunks,
                                               final long modTime,
                                               final int permissions) {
        return new TarHeader(name, realSize, modTime, false, permissions, "", 0, 0, "", "", false, sparseChunks);
    }

    /**
     * Whether this entry is a GNU old-style sparse file, i.e. has a non-empty
     * {@link #sparseChunks()}.
     *
     * @return {@code true} if this entry is sparse
     */
    boolean isSparse() {
        return !sparseChunks.isEmpty();
    }

    /**
     * Serializes this header into a 512-byte ustar (or, for sparse entries, GNU old-style
     * sparse) header block.
     *
     * @return the 512-byte header block
     * @throws IllegalArgumentException if {@link #name()} exceeds 100 bytes with no valid
     *                                  prefix/name split point (see {@link #nameNeedsPaxExtension()})
     */
    byte[] toBytes() {
        if (isSparse()) {
            final var storedSize = sparseChunks.stream().mapToLong(SparseChunk::length).sum();
            return buildSparseBlock(name, permissions, uid, gid, storedSize, modTime, userName, groupName,
                chunksWithTerminator(), size);
        }
        final var typeFlag = directory ? '5' : hardlink ? '1' : linkName.isEmpty() ? '0' : '2';
        return buildBlock(name, permissions, uid, gid, directory ? 0 : size, modTime, typeFlag, linkName,
            userName, groupName);
    }

    /**
     * Returns {@link #sparseChunks()} with a trailing zero-length terminator entry appended at
     * {@link #size()} (the logical end of the entry). GNU tar writes this terminator itself and
     * some extractors (including GNU tar) rely on it, rather than the {@code realsize} field
     * alone, to know to truncate the extracted file out to its full logical size when the last
     * real chunk does not already reach the end.
     *
     * @return the sparse chunks to serialize, including the terminator
     */
    private List<SparseChunk> chunksWithTerminator() {
        final var augmented = new ArrayList<>(sparseChunks);
        augmented.add(new SparseChunk(size, 0));
        return augmented;
    }

    /**
     * Returns the GNU old-style sparse extension blocks needed to convey this entry's sparse
     * map (including its terminator entry, see {@link #chunksWithTerminator()}) beyond the
     * first {@value #SPARSE_CHUNKS_IN_MAIN_BLOCK} entries embedded in the main header block.
     *
     * @return the extension blocks, in the order they must be written after the main header block
     */
    List<byte[]> sparseExtensionBlocks() {
        final var augmented = chunksWithTerminator();
        if (augmented.size() <= SPARSE_CHUNKS_IN_MAIN_BLOCK) {
            return List.of();
        }
        final var remaining = augmented.subList(SPARSE_CHUNKS_IN_MAIN_BLOCK, augmented.size());
        final var blocks = new ArrayList<byte[]>();
        var offset = 0;
        while (offset < remaining.size()) {
            final var block = new byte[BLOCK_SIZE];
            Arrays.fill(block, (byte) 0);
            final var count = Math.min(SPARSE_CHUNKS_PER_EXTENSION_BLOCK, remaining.size() - offset);
            for (int i = 0; i < count; i++) {
                final var chunk = remaining.get(offset + i);
                final var base = i * 24;
                writeOctal(block, base, 12, chunk.offset());
                writeOctal(block, base + 12, 12, chunk.length());
            }
            offset += count;
            final var hasMore = offset < remaining.size();
            block[SPARSE_CHUNKS_PER_EXTENSION_BLOCK * 24] = (byte) (hasMore ? 1 : 0);
            blocks.add(block);
        }
        return blocks;
    }

    /**
     * Whether {@link #name()} is too long to fit in the ustar name/prefix fields, requiring
     * a PAX {@code path} extended header record (or GNU {@code L} long-name record) to convey
     * the full name.
     *
     * @return {@code true} if no ustar-compatible prefix/name split exists
     */
    boolean nameNeedsPaxExtension() {
        return utf8Length(name) > 100 && findPrefixSplitIndex(name) < 0;
    }

    /**
     * Whether {@link #linkName()} is too long to fit in the ustar linkname field, requiring
     * a PAX {@code linkpath} extended header record (or GNU {@code K} long-link record) to
     * convey the full target.
     *
     * @return {@code true} if the link name exceeds 100 bytes
     */
    boolean linkNameNeedsPaxExtension() {
        return utf8Length(linkName) > 100;
    }

    /**
     * Returns a copy of this header with {@link #name()} and/or {@link #linkName()} truncated
     * to fit the ustar fixed-width fields, for use as the placeholder ustar header that
     * accompanies a PAX extended header carrying the real values.
     *
     * @return the truncated header
     */
    TarHeader toUstarSafeHeader() {
        final var safeName = nameNeedsPaxExtension() ? truncateToUtf8Bytes(name, 100) : name;
        final var safeLinkName = linkNameNeedsPaxExtension() ? truncateToUtf8Bytes(linkName, 100) : linkName;
        return new TarHeader(safeName, size, modTime, directory, permissions, safeLinkName, uid, gid, userName, groupName, hardlink, sparseChunks);
    }

    /**
     * Builds a 512-byte ustar header block for a PAX ({@code x}/{@code g}) or GNU
     * long-name/long-link ({@code L}/{@code K}) extension record, whose data is the
     * following {@code dataSize} bytes in the stream.
     *
     * @param name     the extension entry's own name (conventionally ignored by readers)
     * @param dataSize the size of the extension data that follows this block
     * @param typeFlag the extension typeflag ({@code 'x'}, {@code 'g'}, {@code 'L'}, or {@code 'K'})
     * @return the 512-byte header block
     */
    static byte[] createExtensionHeaderBlock(final String name, final long dataSize, final char typeFlag) {
        return buildBlock(name, 0, 0, 0, dataSize, 0, typeFlag, "", "", "");
    }

    private static byte[] buildBlock(final String name,
                                     final int permissions,
                                     final int uid,
                                     final int gid,
                                     final long size,
                                     final long modTime,
                                     final char typeFlag,
                                     final String linkName,
                                     final String userName,
                                     final String groupName) {
        final var header = new byte[BLOCK_SIZE];
        Arrays.fill(header, (byte) 0);

        // determine name and prefix
        final String entryName;
        final String prefix;

        if (utf8Length(name) > 100) {
            // POSIX ustar: prefix and name are joined by an implicit '/' that is not
            // itself stored, so the split must land exactly on a '/' in the original path.
            final int splitIndex = prefixSplitIndex(name);
            prefix = name.substring(0, splitIndex);
            entryName = name.substring(splitIndex + 1);
        } else {
            entryName = name;
            prefix = "";
        }

        // name (offset 0, 100 bytes)
        writeString(header, 0, 100, entryName);

        // mode (offset 100, 8 bytes)
        writeOctal(header, 100, 8, permissions);

        // uid (offset 108, 8 bytes)
        writeOctal(header, 108, 8, uid);

        // gid (offset 116, 8 bytes)
        writeOctal(header, 116, 8, gid);

        // size (offset 124, 12 bytes)
        writeOctal(header, 124, 12, size);

        // mtime (offset 136, 12 bytes)
        writeOctal(header, 136, 12, modTime);

        // checksum placeholder (offset 148, 8 bytes) - fill with spaces for calculation
        Arrays.fill(header, 148, 156, (byte) ' ');

        // typeflag (offset 156, 1 byte)
        header[156] = (byte) typeFlag;

        // linkname (offset 157, 100 bytes)
        writeString(header, 157, 100, linkName);

        // magic (offset 257, 6 bytes)
        writeString(header, 257, 6, USTAR_MAGIC);

        // version (offset 263, 2 bytes)
        writeString(header, 263, 2, USTAR_VERSION);

        // uname (offset 265, 32 bytes)
        writeString(header, 265, 32, userName);

        // gname (offset 297, 32 bytes)
        writeString(header, 297, 32, groupName);

        // devmajor (offset 329, 8 bytes)
        writeOctal(header, 329, 8, 0);

        // devminor (offset 337, 8 bytes)
        writeOctal(header, 337, 8, 0);

        // prefix (offset 345, 155 bytes)
        writeString(header, 345, 155, prefix);

        // compute checksum
        long checksum = 0;
        for (final byte b : header) {
            checksum += (b & 0xFF);
        }
        // write checksum (offset 148, 8 bytes) as 6 octal digits + null + space
        final var checksumStr = String.format("%06o\0 ", checksum);
        final var checksumBytes = checksumStr.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(checksumBytes, 0, header, 148, Math.min(checksumBytes.length, 8));

        return header;
    }

    /**
     * Builds a 512-byte GNU old-style sparse ({@code 'S'}) main header block, embedding up to
     * {@value #SPARSE_CHUNKS_IN_MAIN_BLOCK} sparse chunks directly and setting the extended
     * flag if more follow in {@link #sparseExtensionBlocks()}.
     *
     * @param name         the entry name (already known to fit the 100-byte ustar name field)
     * @param permissions  the POSIX permissions
     * @param uid          the numeric owner id
     * @param gid          the numeric group id
     * @param storedSize   the sum of all chunk lengths, i.e. the number of data bytes actually
     *                     written to the archive for this entry
     * @param modTime      the modification time in seconds since epoch
     * @param userName     the owner name
     * @param groupName    the group name
     * @param sparseChunks the full sparse map
     * @param realSize     the entry's full logical (expanded) size
     * @return the 512-byte header block
     */
    private static byte[] buildSparseBlock(final String name,
                                           final int permissions,
                                           final int uid,
                                           final int gid,
                                           final long storedSize,
                                           final long modTime,
                                           final String userName,
                                           final String groupName,
                                           final List<SparseChunk> sparseChunks,
                                           final long realSize) {
        final var header = new byte[BLOCK_SIZE];
        Arrays.fill(header, (byte) 0);

        // name (offset 0, 100 bytes)
        writeString(header, 0, 100, name);

        // mode (offset 100, 8 bytes)
        writeOctal(header, 100, 8, permissions);

        // uid (offset 108, 8 bytes)
        writeOctal(header, 108, 8, uid);

        // gid (offset 116, 8 bytes)
        writeOctal(header, 116, 8, gid);

        // size (offset 124, 12 bytes) - bytes actually stored in the archive, not the real size
        writeOctal(header, 124, 12, storedSize);

        // mtime (offset 136, 12 bytes)
        writeOctal(header, 136, 12, modTime);

        // checksum placeholder (offset 148, 8 bytes) - fill with spaces for calculation
        Arrays.fill(header, 148, 156, (byte) ' ');

        // typeflag (offset 156, 1 byte)
        header[156] = (byte) 'S';

        // linkname (offset 157, 100 bytes) - unused for sparse entries, left zero

        // GNU magic (offset 257, 6 bytes: "ustar ") and version (offset 263, 2 bytes: " \0"),
        // distinct from the POSIX ustar magic/version, as required for GNU sparse extensions
        writeString(header, 257, 6, "ustar ");
        header[263] = ' ';

        // uname (offset 265, 32 bytes)
        writeString(header, 265, 32, userName);

        // gname (offset 297, 32 bytes)
        writeString(header, 297, 32, groupName);

        // devmajor (offset 329, 8 bytes)
        writeOctal(header, 329, 8, 0);

        // devminor (offset 337, 8 bytes)
        writeOctal(header, 337, 8, 0);

        // atime (345, 12), ctime (357, 12), multivolume offset (369, 12), longnames (381, 4),
        // and one unused byte (385) are left zero

        // sparse chunk entries (offset 386, 24 bytes each: 12-byte offset + 12-byte numbytes)
        for (int i = 0; i < SPARSE_CHUNKS_IN_MAIN_BLOCK; i++) {
            if (i < sparseChunks.size()) {
                final var chunk = sparseChunks.get(i);
                final var base = 386 + i * 24;
                writeOctal(header, base, 12, chunk.offset());
                writeOctal(header, base + 12, 12, chunk.length());
            }
        }

        // isextended (offset 482, 1 byte)
        header[482] = (byte) (sparseChunks.size() > SPARSE_CHUNKS_IN_MAIN_BLOCK ? 1 : 0);

        // realsize (offset 483, 12 bytes) - the full logical (expanded) size
        writeOctal(header, 483, 12, realSize);

        // compute checksum
        long checksum = 0;
        for (final byte b : header) {
            checksum += (b & 0xFF);
        }
        final var checksumStr = String.format("%06o\0 ", checksum);
        final var checksumBytes = checksumStr.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(checksumBytes, 0, header, 148, Math.min(checksumBytes.length, 8));

        return header;
    }

    /**
     * Reads the typeflag byte from a raw 512-byte header block, without validating
     * its checksum. Used to detect GNU long-name/long-link and PAX extended header
     * blocks before they are fully parsed.
     *
     * @param block the 512-byte header block
     * @return the typeflag character
     */
    static char typeFlag(final byte[] block) {
        return (char) block[156];
    }

    /**
     * Parses a 512-byte ustar header block into a {@link TarHeader}.
     *
     * @param block the 512-byte header block
     * @return the parsed {@link TarHeader}
     * @throws IllegalArgumentException if the header checksum does not match its contents
     */
    static TarHeader parseHeader(final byte[] block) {
        verifyChecksum(block);

        final var name = readString(block, 0, 100);
        final var permissions = (int) parseOctal(block, 100, 8);
        final var uid = (int) parseOctal(block, 108, 8);
        final var gid = (int) parseOctal(block, 116, 8);
        final var size = parseOctal(block, 124, 12);
        final var modTime = parseOctal(block, 136, 12);
        final var typeFlag = (char) block[156];
        final var linkName = readString(block, 157, 100);
        final var userName = readString(block, 265, 32);
        final var groupName = readString(block, 297, 32);
        final var prefix = readString(block, 345, 155);

        final var fullName = prefix.isEmpty() ? name : prefix + "/" + name;
        final var directory = typeFlag == '5' || fullName.endsWith("/");
        final var hardlink = typeFlag == '1';

        return new TarHeader(fullName, directory ? 0 : size, modTime, directory, permissions, linkName,
            uid, gid, userName, groupName, hardlink, List.of());
    }

    /**
     * Validates a raw 512-byte header block's checksum field against its actual contents.
     *
     * @param block the 512-byte header block
     * @throws IllegalArgumentException if the header checksum does not match its contents
     */
    private static void verifyChecksum(final byte[] block) {
        final var storedChecksum = parseOctal(block, 148, 8);

        long computedChecksum = 0;
        for (int i = 0; i < BLOCK_SIZE; i++) {
            computedChecksum += (i >= 148 && i < 156) ? ' ' : (block[i] & 0xFF);
        }
        if (storedChecksum != computedChecksum) {
            throw new IllegalArgumentException(
                "Invalid tar header checksum: expected " + storedChecksum + " but computed " + computedChecksum);
        }
    }

    /**
     * Parses the fields of a GNU old-style sparse ({@code 'S'}) main header block (and, if its
     * {@code isextended} flag is set, any following extension blocks are the caller's
     * responsibility to read and pass via {@code additionalSparseBlocks}).
     *
     * @param block the 512-byte sparse main header block
     * @return the parsed {@link TarHeader}, with {@link #size()} set to the entry's logical
     * (expanded) size and {@link #sparseChunks()} containing only the chunks embedded
     * directly in this block; the caller must append any chunks from extension blocks
     * @throws IllegalArgumentException if the header checksum does not match its contents
     */
    static TarHeader parseSparseMainBlock(final byte[] block) {
        verifyChecksum(block);

        final var name = readString(block, 0, 100);
        final var permissions = (int) parseOctal(block, 100, 8);
        final var uid = (int) parseOctal(block, 108, 8);
        final var gid = (int) parseOctal(block, 116, 8);
        final var modTime = parseOctal(block, 136, 12);
        final var userName = readString(block, 265, 32);
        final var groupName = readString(block, 297, 32);
        final var realSize = parseOctal(block, 483, 12);

        final var chunks = new ArrayList<SparseChunk>();
        for (int i = 0; i < SPARSE_CHUNKS_IN_MAIN_BLOCK; i++) {
            final var base = 386 + i * 24;
            final var offset = parseOctal(block, base, 12);
            final var length = parseOctal(block, base + 12, 12);
            if (length > 0) {
                chunks.add(new SparseChunk(offset, length));
            }
        }

        return new TarHeader(name, realSize, modTime, false, permissions, "", uid, gid, userName, groupName, false, chunks);
    }

    /**
     * Whether a GNU old-style sparse main header block's {@code isextended} flag is set,
     * indicating that a sparse extension block immediately follows.
     *
     * @param block the 512-byte sparse main header block just read
     * @return {@code true} if a sparse extension block follows
     */
    static boolean sparseMainBlockIsExtended(final byte[] block) {
        return block[482] != 0;
    }

    /**
     * Whether a GNU old-style sparse extension block's {@code isextended} flag is set,
     * indicating that another sparse extension block immediately follows.
     *
     * @param block the 512-byte sparse extension block just read
     * @return {@code true} if another sparse extension block follows
     */
    static boolean sparseExtensionBlockIsExtended(final byte[] block) {
        return block[SPARSE_CHUNKS_PER_EXTENSION_BLOCK * 24] != 0;
    }

    /**
     * Parses the sparse chunk entries from a GNU old-style sparse extension block.
     *
     * @param block the 512-byte sparse extension block
     * @return the chunks found in this block
     */
    static List<SparseChunk> parseSparseExtensionBlock(final byte[] block) {
        final var chunks = new ArrayList<SparseChunk>();
        for (int i = 0; i < SPARSE_CHUNKS_PER_EXTENSION_BLOCK; i++) {
            final var base = i * 24;
            final var offset = parseOctal(block, base, 12);
            final var length = parseOctal(block, base + 12, 12);
            if (length > 0) {
                chunks.add(new SparseChunk(offset, length));
            }
        }
        return chunks;
    }

    /**
     * Returns a copy of this header with additional {@link #sparseChunks()} appended, and
     * {@link #size()} unchanged (it already holds the logical/expanded size).
     *
     * @param additionalChunks the chunks to append, from sparse extension blocks
     * @return the derived {@link TarHeader}
     */
    TarHeader withAdditionalSparseChunks(final List<SparseChunk> additionalChunks) {
        final var merged = new ArrayList<>(sparseChunks);
        merged.addAll(additionalChunks);
        return new TarHeader(name, size, modTime, directory, permissions, linkName, uid, gid, userName, groupName, hardlink, merged);
    }

    /**
     * Returns a copy of this header with a different {@link #name()}, recomputing
     * {@link #directory()} in case the new name ends with {@code '/'}.
     *
     * @param newName the replacement name
     * @return the derived {@link TarHeader}
     */
    TarHeader withName(final String newName) {
        return new TarHeader(newName, size, modTime, directory || newName.endsWith("/"), permissions, linkName,
            uid, gid, userName, groupName, hardlink, sparseChunks);
    }

    /**
     * Returns a copy of this header with a different {@link #linkName()}.
     *
     * @param newLinkName the replacement link target
     * @return the derived {@link TarHeader}
     */
    TarHeader withLinkName(final String newLinkName) {
        return new TarHeader(name, size, modTime, directory, permissions, newLinkName, uid, gid, userName, groupName, hardlink, sparseChunks);
    }

    /**
     * Returns a copy of this header with a different {@link #size()}.
     *
     * @param newSize the replacement size
     * @return the derived {@link TarHeader}
     */
    TarHeader withSize(final long newSize) {
        return new TarHeader(name, newSize, modTime, directory, permissions, linkName, uid, gid, userName, groupName, hardlink, sparseChunks);
    }

    /**
     * Returns a copy of this header with a different {@link #modTime()}.
     *
     * @param newModTime the replacement modification time
     * @return the derived {@link TarHeader}
     */
    TarHeader withModTime(final long newModTime) {
        return new TarHeader(name, size, newModTime, directory, permissions, linkName, uid, gid, userName, groupName, hardlink, sparseChunks);
    }

    /**
     * Returns a copy of this header with different ownership.
     *
     * @param newUid       the replacement owner id
     * @param newGid       the replacement group id
     * @param newUserName  the replacement owner name
     * @param newGroupName the replacement group name
     * @return the derived {@link TarHeader}
     */
    TarHeader withOwner(final int newUid, final int newGid, final String newUserName, final String newGroupName) {
        return new TarHeader(name, size, modTime, directory, permissions, linkName, newUid, newGid, newUserName, newGroupName, hardlink, sparseChunks);
    }

    /**
     * Finds the rightmost {@code '/'} in {@code name} that splits it into a prefix
     * (at most 155 bytes) and a name (at most 100 bytes), as required by the POSIX
     * ustar prefix field. The slash itself is not included in either half.
     *
     * @param name the entry name, already known to exceed 100 UTF-8 bytes
     * @return the index of the separating {@code '/'}
     * @throws IllegalArgumentException if no {@code '/'} yields a valid split
     */
    private static int prefixSplitIndex(final String name) {
        final var splitIndex = findPrefixSplitIndex(name);
        if (splitIndex < 0) {
            throw new IllegalArgumentException("Entry name too long: " + name);
        }
        return splitIndex;
    }

    /**
     * Finds the rightmost {@code '/'} in {@code name} that splits it into a prefix
     * (at most 155 bytes) and a name (at most 100 bytes), as required by the POSIX
     * ustar prefix field.
     *
     * @param name the entry name
     * @return the index of the separating {@code '/'}, or {@code -1} if no valid split exists
     */
    private static int findPrefixSplitIndex(final String name) {
        int splitIndex = -1;
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) == '/') {
                final var prefixLength = utf8Length(name.substring(0, i));
                final var suffixLength = utf8Length(name.substring(i + 1));
                if (prefixLength <= 155 && suffixLength > 0 && suffixLength <= 100) {
                    splitIndex = i;
                }
            }
        }
        return splitIndex;
    }

    /**
     * Truncates {@code value} to at most {@code maxBytes} bytes when encoded as UTF-8,
     * trimming whole characters so the result never splits a multi-byte sequence.
     *
     * @param value    the string to truncate
     * @param maxBytes the maximum encoded length in bytes
     * @return the truncated string
     */
    private static String truncateToUtf8Bytes(final String value, final int maxBytes) {
        var candidate = value;
        while (utf8Length(candidate) > maxBytes) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        return candidate;
    }

    /**
     * Returns the length of {@code value} in bytes when encoded as UTF-8, which is the
     * unit the fixed-width ustar header fields are measured in.
     *
     * @param value the string to measure
     * @return the UTF-8 encoded length in bytes
     */
    private static int utf8Length(final String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    /**
     * Reads a null-terminated string from the header at the specified offset.
     *
     * @param header the header buffer
     * @param offset the offset
     * @param length the field length
     * @return the string value, with trailing NUL bytes stripped
     */
    private static String readString(final byte[] header, final int offset, final int length) {
        int end = offset;
        while (end < offset + length && header[end] != 0) {
            end++;
        }
        return new String(header, offset, end - offset, StandardCharsets.UTF_8);
    }

    /**
     * Parses an octal field from the header at the specified offset.
     *
     * @param header the header buffer
     * @param offset the offset
     * @param length the field length
     * @return the parsed numeric value, or 0 if the field is blank
     */
    private static long parseOctal(final byte[] header, final int offset, final int length) {
        // GNU base-256 extension: a set high bit on the first byte means the remaining bits
        // of that field form a big-endian binary number instead of an octal ASCII string.
        // This is required for values too large for the field's fixed-width octal encoding
        // (e.g. file sizes at or above 8GB, or a size field, since 8^11-1 is ~8GB).
        if ((header[offset] & 0x80) != 0) {
            long value = header[offset] & 0x7F;
            for (int i = offset + 1; i < offset + length; i++) {
                value = (value << 8) | (header[i] & 0xFFL);
            }
            return value;
        }
        final var text = new String(header, offset, length, StandardCharsets.US_ASCII).trim();
        return text.isEmpty() ? 0L : Long.parseLong(text, 8);
    }

    /**
     * Writes a string into the header at the specified offset.
     *
     * @param header the header buffer
     * @param offset the offset
     * @param length the field length
     * @param value  the string value
     */
    private static void writeString(final byte[] header,
                                    final int offset,
                                    final int length,
                                    final String value) {
        final var bytes = value.getBytes(StandardCharsets.UTF_8);
        final var copyLen = Math.min(bytes.length, length);
        System.arraycopy(bytes, 0, header, offset, copyLen);
    }

    /**
     * Whether {@code value} is too large to fit an octal ustar field of {@code fieldLength}
     * bytes and would therefore fall back to the GNU base-256 extension if written there.
     *
     * @param value       the numeric value
     * @param fieldLength the field length in bytes, e.g. 12 for the size field or 8 for uid/gid
     * @return {@code true} if the field would need base-256 encoding
     */
    static boolean exceedsOctalField(final long value, final int fieldLength) {
        final var maxOctalValue = 1L << (3 * (fieldLength - 1));
        return value < 0 || value >= maxOctalValue;
    }

    /**
     * Writes an octal value into the header at the specified offset.
     *
     * @param header the header buffer
     * @param offset the offset
     * @param length the field length
     * @param value  the numeric value
     */
    private static void writeOctal(final byte[] header,
                                   final int offset,
                                   final int length,
                                   final long value) {
        final var maxOctalValue = 1L << (3 * (length - 1));
        if (value < 0 || value >= maxOctalValue) {
            // too large for the fixed-width octal field: fall back to the GNU base-256 extension
            writeBase256(header, offset, length, value);
            return;
        }
        // format as octal string, null-terminated, right-aligned with leading zeros
        final var octal = String.format("%0" + (length - 1) + "o", value);
        final var bytes = octal.getBytes(StandardCharsets.US_ASCII);
        final var copyLen = Math.min(bytes.length, length - 1);
        System.arraycopy(bytes, 0, header, offset, copyLen);
        // null terminator is already there from Arrays.fill
    }

    /**
     * Writes a numeric value using the GNU base-256 extension: a big-endian binary number
     * filling the field, with the high bit of the first byte set to mark it as binary
     * rather than octal ASCII text.
     *
     * @param header the header buffer
     * @param offset the offset
     * @param length the field length
     * @param value  the numeric value
     */
    private static void writeBase256(final byte[] header, final int offset, final int length, final long value) {
        var remaining = value;
        for (int i = offset + length - 1; i > offset; i--) {
            header[i] = (byte) remaining;
            remaining >>>= 8;
        }
        header[offset] |= (byte) 0x80;
    }
}
