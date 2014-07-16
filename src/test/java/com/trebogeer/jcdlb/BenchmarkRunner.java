package com.trebogeer.jcdlb;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * @author dimav
 *         Date: 6/13/12
 *         Time: 1:25 PM
 */
public class BenchmarkRunner {

    private List<Benchmark> benchmarks = new LinkedList<Benchmark>();


    @BeforeClass
    public void setup() throws URISyntaxException, IOException {

        URL url = this.getClass().getResource("");

        File resources = new File(url.toURI());
        String resStr = resources.getAbsolutePath();
        List<String> files = new LinkedList<String>();
        if (resources.isDirectory()) {
            Collections.addAll(files, resources.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name != null && !name.contains("class");
                }
            }));
        }
        int mult = 1;
        byte[][] data = new byte[files.size() * mult][];   /* getData(3048, 1000);*/
        int i = 0;
        for(int g = 0; g < mult; g++)
        for (String f : files) {
            FileInputStream fileInputStream = new FileInputStream(resStr + File.separator + f);
            byte[] buff = new byte[0x2000];
            int bytesRead = fileInputStream.read(buff);
            byte[] finalBytes = new byte[bytesRead];
            System.arraycopy(buff, 0, finalBytes, 0, bytesRead);
            data[i] = finalBytes;
            i++;
            fileInputStream.close();
        }

        benchmarks.add(new QuickLZBenchmark(data));
        benchmarks.add(new GZIPCDBenchmark(data));
        benchmarks.add(new JFastLZBenchmark(data));
        //benchmarks.add(new JSmazBenchmark(data));
        benchmarks.add(new LZFBenchmark(data));
        benchmarks.add(new OptimizedGZIPBenchmark(data));
        benchmarks.add(new SnappyIQ80Benchmark(data));
        benchmarks.add(new SnappyBenchmark(data));

    }


    private static byte[][] getData(int chunkSize, int chunks) throws IOException {
        byte[][] data = new byte[chunks][];
        for (int i = 0; i < chunks; i++) {
            data[i] = constructFluff(chunkSize);
        }
        return data;
    }


    @Test
    public void runBenchmarks() {
        for (Benchmark b : benchmarks) {
            b.runBench();
            System.gc();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {

            }
        }
    }

    private final static byte[] ABCD = new byte[]{'a', 'b', 'c', 'd'};

    protected static byte[] constructFluff(int length) throws IOException {
        Random rnd = new Random(length);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(length + 100);
        while (bytes.size() < length) {
            int num = rnd.nextInt();
            switch (num & 3) {
                case 0:
                    bytes.write(ABCD);
                    break;
                case 1:
                    bytes.write(num);
                    break;
                default:
                    bytes.write((num >> 3) & 0x7);
                    break;
            }
        }
        return bytes.toByteArray();
    }
}
