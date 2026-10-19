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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An {@link OutputStream} that writes entries in POSIX ustar tar format, falling back to a
 * PAX extended header ({@code x} typeflag) for any entry whose name or link target does not
 * fit the fixed-width ustar fields (and cannot be expressed via the ustar prefix field either).
 * <p>
 * Usage: create a {@link TarOutputStream} wrapping an existing {@link OutputStream},
 * then repeatedly call {@link #putNextEntry(TarEntry)} followed by {@link #write(byte[], int, int)}
 * for file data, and finally {@link #close()} to finalize the archive.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public final class TarOutputStream extends OutputStream {

    /**
     * The block size for tar archives.
     */
    private static final int BLOCK_SIZE = TarHeader.BLOCK_SIZE;

    /**
     * The underlying output stream.
     */
    private final OutputStream out;

    /**
     * The number of data bytes written for the current entry.
     */
    private long bytesWritten;

    /**
     * Constructs a {@link TarOutputStream} wrapping the specified {@link OutputStream}.
     *
     * @param out the underlying {@link OutputStream}
     */
    public TarOutputStream(final OutputStream out) {
        this.out = out;
        this.bytesWritten = 0;
    }

    /**
     * Writes the header for the next tar entry. If a previous entry had file data,
     * any required padding to a 512-byte boundary is written first.
     *
     * @param entry the {@link TarEntry} to write
     * @throws IOException if an I/O error occurs
     */
    public void putNextEntry(final TarEntry entry) throws IOException {
        // pad the previous entry to a block boundary if needed
        padCurrentEntry();

        var header = entry.header();
        if (header.nameNeedsPaxExtension() || header.linkNameNeedsPaxExtension()) {
            writePaxExtendedHeader(header);
            header = header.toUstarSafeHeader();
        }

        // write the header block, plus any GNU sparse extension blocks it requires
        out.write(header.toBytes());
        for (final var block : header.sparseExtensionBlocks()) {
            out.write(block);
        }

        // reset byte counter
        bytesWritten = 0;
    }

    /**
     * Writes a PAX extended header ({@code x} typeflag) block carrying the {@code path}
     * and/or {@code linkpath} records needed to convey a name or link target that does not
     * fit the ustar fixed-width fields, followed by its data and block padding.
     * <p>
     * Since a PAX header is already being paid for, this also includes {@code size}/{@code uid}/
     * {@code gid} records for any of those fields that would otherwise only be recoverable via
     * the GNU base-256 extension, for maximum compatibility with readers that understand PAX
     * but not base-256.
     *
     * @param header the entry header whose name and/or link name require a PAX extension
     * @throws IOException if an I/O error occurs
     */
    private void writePaxExtendedHeader(final TarHeader header) throws IOException {
        final var records = new LinkedHashMap<String, String>();
        if (header.nameNeedsPaxExtension()) {
            records.put("path", header.name());
        }
        if (header.linkNameNeedsPaxExtension()) {
            records.put("linkpath", header.linkName());
        }
        if (TarHeader.exceedsOctalField(header.size(), 12)) {
            records.put("size", String.valueOf(header.size()));
        }
        if (TarHeader.exceedsOctalField(header.uid(), 8)) {
            records.put("uid", String.valueOf(header.uid()));
        }
        if (TarHeader.exceedsOctalField(header.gid(), 8)) {
            records.put("gid", String.valueOf(header.gid()));
        }

        final var data = encodePaxRecords(records);
        out.write(TarHeader.createExtensionHeaderBlock("PaxHeader", data.length, 'x'));
        out.write(data);
        final var padding = computePadding(data.length);
        if (padding > 0) {
            out.write(new byte[(int) padding]);
        }
    }

    /**
     * Encodes PAX extended header records in the standard {@code "<length> <key>=<value>\n"}
     * form, where {@code length} is the decimal length of the entire record including itself.
     *
     * @param records the records to encode, in iteration order
     * @return the encoded PAX data block
     */
    private static byte[] encodePaxRecords(final Map<String, String> records) {
        final var buffer = new ByteArrayOutputStream();
        records.forEach((key, value) -> {
            final var keyValueBytes = (key + "=" + value + "\n").getBytes(StandardCharsets.UTF_8);
            // the length prefix includes its own digit count, which can grow the total,
            // so grow the digit count until it stops changing the total length
            var digits = 1;
            var recordLength = keyValueBytes.length + 1 + digits;
            while (String.valueOf(recordLength).length() != digits) {
                digits = String.valueOf(recordLength).length();
                recordLength = keyValueBytes.length + 1 + digits;
            }
            buffer.writeBytes((recordLength + " ").getBytes(StandardCharsets.US_ASCII));
            buffer.writeBytes(keyValueBytes);
        });
        return buffer.toByteArray();
    }

    @Override
    public void write(final int b) throws IOException {
        out.write(b);
        bytesWritten++;
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        out.write(b, off, len);
        bytesWritten += len;
    }

    @Override
    public void close() throws IOException {
        // pad the last entry
        padCurrentEntry();

        // write two 512-byte zero blocks to mark end of archive
        final var zeroBlock = new byte[BLOCK_SIZE];
        out.write(zeroBlock);
        out.write(zeroBlock);

        out.flush();
        out.close();
    }

    /**
     * Pads the current entry's data to a 512-byte block boundary.
     *
     * @throws IOException if an I/O error occurs
     */
    private void padCurrentEntry() throws IOException {
        if (bytesWritten > 0) {
            final var remainder = (int) (bytesWritten % BLOCK_SIZE);
            if (remainder != 0) {
                final var padding = BLOCK_SIZE - remainder;
                out.write(new byte[padding]);
            }
        }
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
