package com.trebogeer.jcdlb;

import org.iq80.snappy.Snappy;

import java.util.Arrays;

/**
 * @author dimav
 *         Date: 6/14/12
 *         Time: 11:06 AM
 */
public class SnappyIQ80Benchmark extends Benchmark {

    public SnappyIQ80Benchmark(final byte[][] data) {
        super(data);
    }

    @Override
    protected byte[] compressDataChunk(byte[] dataChunk) {
        byte[] compressed = new byte[Snappy.maxCompressedLength(dataChunk.length)];
        int compressedSize = Snappy.compress(dataChunk, 0, dataChunk.length, compressed, 0);
        return Arrays.copyOf(compressed, compressedSize);
    }

    @Override
    protected byte[] deCompressDataChunk(byte[] dataChunk) {
        return Snappy.uncompress(dataChunk, 0, dataChunk.length);
    }
}
