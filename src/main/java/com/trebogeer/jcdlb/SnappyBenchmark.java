package com.trebogeer.jcdlb;

import org.xerial.snappy.Snappy;

import java.io.IOException;

/**
 * @author dimav
 *         Date: 6/14/12
 *         Time: 11:12 AM
 */
public class SnappyBenchmark extends Benchmark {

    public SnappyBenchmark(final byte[][] data) {
        super(data);
    }

    @Override
    protected byte[] compressDataChunk(byte[] dataChunk) {
        try {
            return Snappy.compress(dataChunk);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataChunk;
    }

    @Override
    protected byte[] deCompressDataChunk(byte[] dataChunk) {
        try {
            return Snappy.uncompress(dataChunk);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataChunk;
    }
}
