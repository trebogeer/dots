package com.trebogeer.jcdlb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author dimav
 *         Date: 6/13/12
 *         Time: 1:18 PM
 */
public class GZIPCDBenchmark extends Benchmark {

    private static final int COMPRESSION = 1;

    public GZIPCDBenchmark(final byte[][] data) {
        super(data);
    }

    @Override
    protected byte[] compressDataChunk(byte[] dataChunk) {
        try {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(BUFFER_SIZE);
            final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream, BUFFER_SIZE) {
                {
                    def.setLevel(COMPRESSION);
                }
            };
            gzipOutputStream.write(dataChunk);
            gzipOutputStream.finish();
            gzipOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();

        }
        return dataChunk;
    }

    @Override
    protected byte[] deCompressDataChunk(byte[] dataChunk) {
        try {
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(dataChunk);
            final GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream, dataChunk.length);
            byte[] buff = new byte[0x2000];
            int read = gzipInputStream.read(buff);
            byte[] finalR = new byte[read];
            System.arraycopy(buff, 0, finalR, 0, read);
            return finalR;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dataChunk;
    }
}
