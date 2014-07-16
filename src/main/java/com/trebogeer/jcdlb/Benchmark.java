package com.trebogeer.jcdlb;

/**
 * @author dimav
 *         Date: 6/13/12
 *         Time: 12:55 PM
 */
public abstract class Benchmark {

    private static final int DEFAULT_WARM_UPS = 1000;
    protected static final int BUFFER_SIZE = 0x2000;

    private final byte[][] data;

    private final byte[][] compressedData;

    private final byte[][] decompressedData;


    private int warmUps = Integer.getInteger("jcdlb.warm.ups", DEFAULT_WARM_UPS);


    public Benchmark(final byte[][] data) {
        this.data = data;
        this.compressedData = new byte[data.length][];
        this.decompressedData = new byte[data.length][];
    }

    public void runBench() {
        System.err.println("Starting benchmark [" + this.getClass().getSimpleName() + "]");
        System.err.println("Warming up...");
        long originalDataSize = 0;
        for (byte[] aData : data) {
            originalDataSize += aData.length;
        }
        warmUp();
        // compression
        System.err.println("Starting compression benchmark...");
        long startTime = System.nanoTime();


        for (int i = 0; i < data.length; i++) {
            compressedData[i] = compressDataChunk(data[i]);
        }

        long finish = System.nanoTime();

        System.out.println(String.format("Compression of %d data chunks totaling %d bytes completed at %d nanos", data.length, originalDataSize, (finish - startTime)));
        long compressedSize = 0;
        for (byte[] aCompressedData : compressedData) {
            compressedSize += aCompressedData.length;
        }
        System.out.println("Compressed data size is " + compressedSize + " bytes, compression ratio is " + ((double) originalDataSize / (double) compressedSize));
        // decompression
        System.err.println("Starting decompress benchmark...");
        startTime = System.nanoTime();


        for (int i = 0; i < compressedData.length; i++) {
            decompressedData[i] = deCompressDataChunk(compressedData[i]);
        }

        finish = System.nanoTime();

        System.out.println(String.format("Decompression of %d data chunks totaling %d bytes completed at %d nanos", data.length, originalDataSize, (finish - startTime)));
        long decompressedSize = 0;
        for (byte[] aCompressedData : decompressedData) {
            decompressedSize += aCompressedData.length;
        }
        System.out.println("Decompressed data size is " + decompressedSize + " bytes");

    }

    protected abstract byte[] compressDataChunk(byte[] dataChunk);

    protected abstract byte[] deCompressDataChunk(byte[] dataChunk);

    /**
     * classloading, mem allocation, buffers allocation, etc.
     */
    private void warmUp() {

        int max = Math.min(data.length, warmUps);
        for (int i = 0; i < max; i++) {
            compressDataChunk(data[i]);
        }
        System.err.println("Warming up complete, chunks=" + max);
    }
}
