package com.trebogeer.jcdlb;

/**
 * @author dimav
 *         Date: 6/13/12
 *         Time: 4:15 PM
 */
public class JSmazBenchmark extends Benchmark {

    public JSmazBenchmark(final byte[][] data) {
        super(data);
    }

    @Override
    protected byte[] compressDataChunk(byte[] dataChunk) {
        return JSmaz.compress(new String(dataChunk));
    }

    @Override
    protected byte[] deCompressDataChunk(byte[] dataChunk) {
        return JSmaz.decompress(dataChunk).getBytes();
    }
}
