package com.trebogeer.jcdlb;

import org.jfastlz.JFastLZ;
import org.jfastlz.JFastLZLevel;

import java.io.IOException;

/**
 * @author dimav
 *         Date: 6/13/12
 *         Time: 3:29 PM
 */
public class JFastLZBenchmark extends Benchmark {

    JFastLZ jFastLZ = new JFastLZ();
    JFastLZLevel level = JFastLZLevel.One;

    public JFastLZBenchmark(final byte[][] data) {
        super(data);
    }

    @Override
    protected byte[] compressDataChunk(byte[] dataChunk) {

        byte[] res = new byte[dataChunk.length];
        try {
            int packedSize = jFastLZ.fastlzCompress(level, dataChunk, 0, dataChunk.length, res, 0, res.length);
            byte[] b = new byte[packedSize];
            System.arraycopy(res, 0, b, 0, packedSize);
            return b;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataChunk;
    }

    @Override
    protected byte[] deCompressDataChunk(byte[] dataChunk) {

        byte[] res = new byte[6 * dataChunk.length];
        int unpackedSize = jFastLZ.fastlzDecompress(dataChunk, 0, dataChunk.length, res, 0, res.length, level);
        byte[] r = new byte[unpackedSize];
        System.arraycopy(res, 0, r, 0, unpackedSize);
        return r;
    }
}
