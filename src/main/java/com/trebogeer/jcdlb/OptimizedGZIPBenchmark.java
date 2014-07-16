package com.trebogeer.jcdlb;

import com.ning.compress.DataHandler;
import com.ning.compress.gzip.GZIPUncompressor;
import com.ning.compress.gzip.OptimizedGZIPOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author dimav
 *         Date: 6/13/12
 *         Time: 4:53 PM
 */
public class OptimizedGZIPBenchmark extends Benchmark {

    private static final int COMPRESSION = 1;

    public OptimizedGZIPBenchmark(final byte[][] data) {
        super(data);
    }

    @Override
    protected byte[] compressDataChunk(byte[] dataChunk) {
        try {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(BUFFER_SIZE);
            final OptimizedGZIPOutputStream gzipOutputStream = new OptimizedGZIPOutputStream(byteArrayOutputStream) {
                {
                    _deflater.setLevel(COMPRESSION);
                }
            };
            gzipOutputStream.write(dataChunk);
            gzipOutputStream.close();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();

        }
        return dataChunk;
    }

//    @Override
//    protected byte[] deCompressDataChunk(byte[] dataChunk) {
//        try {
//            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(dataChunk);
//            final OptimizedGZIPInputStream gzipInputStream = new OptimizedGZIPInputStream(byteArrayInputStream);
//            byte[] buff = new byte[0x2000];
//            int read = gzipInputStream.read(buff);
//            byte[] finalR = new byte[read];
//            System.arraycopy(buff, 0, finalR, 0, read);
//            return finalR;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return dataChunk;
//    }

    @Override
    protected byte[] deCompressDataChunk(byte[] dataChunk) {
        try {
            Collector co = new Collector();
            GZIPUncompressor uncomp = new GZIPUncompressor(co);
            uncomp.feedCompressedData(dataChunk, 0, dataChunk.length);
            uncomp.complete();
            return co.getBytes();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dataChunk;
    }

    private final static class Collector implements DataHandler {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        public void handleData(byte[] buffer, int offset, int len) throws IOException {
            bytes.write(buffer, offset, len);
        }

        public void allDataHandled() throws IOException {
        }

        public byte[] getBytes() {
            return bytes.toByteArray();
        }
    }
}
