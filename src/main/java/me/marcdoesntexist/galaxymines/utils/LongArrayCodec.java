package me.marcdoesntexist.galaxymines.utils;

import java.io.*;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


public final class LongArrayCodec {
    private static final int MAX_ENTRIES = 4_000_000;

    private LongArrayCodec() {
    }

    public static String encode(final LongHashSet values) {
        try {
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            final long[] encodedValues = values.toLongArray();
            try (DataOutputStream output = new DataOutputStream(new GZIPOutputStream(bytes))) {
                output.writeInt(encodedValues.length);
                for (final long value : encodedValues) {
                    output.writeLong(value);
                }
            }
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException ex) {
            throw new IllegalStateException("Could not encode mine state", ex);
        }
    }

    public static void decode(final String encoded, final LongHashSet target)
            throws IllegalArgumentException {
        final byte[] raw;
        try {
            raw = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("invalid base64", ex);
        }
        try (DataInputStream input = new DataInputStream(
                new GZIPInputStream(new ByteArrayInputStream(raw)))) {
            final int count = input.readInt();
            if (count < 0 || count > MAX_ENTRIES) {
                throw new IllegalArgumentException("invalid entry count: " + count);
            }
            for (int i = 0; i < count; ++i) {
                target.add(input.readLong());
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("corrupted compressed state", ex);
        }
    }
}
