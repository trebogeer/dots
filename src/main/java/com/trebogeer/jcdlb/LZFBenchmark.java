package com.trebogeer.jcdlb;

import com.ning.compress.lzf.ChunkDecoder;
import com.ning.compress.lzf.LZFDecoder;
import com.ning.compress.lzf.LZFEncoder;
import com.ning.compress.lzf.util.ChunkDecoderFactory;

import java.io.IOException;

/**
 * @author dimav
 *         Date: 6/13/12
 *         Time: 4:44 PM
 */
public class LZFBenchmark extends Benchmark {

    final ChunkDecoder decoder = ChunkDecoderFactory.optimalInstance();

    public LZFBenchmark(final byte[][] data) {
        super(data);
    }

    @Override
    protected byte[] compressDataChunk(byte[] dataChunk) {
        try {
            return LZFEncoder.encode(dataChunk);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataChunk;
    }

    @Override
    protected byte[] deCompressDataChunk(byte[] dataChunk) {
        try {
            return decoder.decode(dataChunk);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataChunk;
    }
}
