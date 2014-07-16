package com.trebogeer.jcdlb;

/**
 * @author dimav
 *         Date: 6/13/12
 *         Time: 3:06 PM
 */
public class QuickLZBenchmark extends Benchmark {

    private static final int LEVEL = 1;

    public QuickLZBenchmark(final byte[][] data) {
        super(data);
    }

    @Override
    protected byte[] compressDataChunk(byte[] dataChunk) {
        return QuickLZ.compress(dataChunk, LEVEL);
    }

    @Override
    protected byte[] deCompressDataChunk(byte[] dataChunk) {
        return QuickLZ.decompress(dataChunk);
    }
}
