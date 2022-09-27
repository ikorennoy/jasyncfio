package one.jasyncfio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BufferedFileTest {
    private final EventExecutor executor = EventExecutor.initDefault();

    @TempDir
    private Path tmpDir;

    @Test
    public void atomicAppend() throws Exception {
        Random random = new Random();
        Path tempFile = Files.createTempFile(tmpDir, "temp-", " file");
        int nThreads = 16;
        int writes = 1000;

        ExecutorService pool = Executors.newFixedThreadPool(nThreads);
        for (int i = 0; i < nThreads; i++) {
            pool.execute(() -> {
                for (int j = 0; j < writes; j++) {
                    try {
                        BufferedFile bufferedFile = BufferedFile.open(tempFile, executor, OpenOption.WRITE_ONLY, OpenOption.APPEND).get(1000, TimeUnit.MILLISECONDS);
                        ByteBuffer buffer = ByteBuffer.allocateDirect(1);
                        buffer.put((byte) 'c');
                        buffer.flip();
                        if (random.nextBoolean()) {
                            ByteBuffer[] buffers = new ByteBuffer[]{buffer};
                            bufferedFile.write(buffers);
                        } else {
                            bufferedFile.write(buffer);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);
        BufferedFile bufferedFile = BufferedFile.open(tempFile, executor, OpenOption.WRITE_ONLY, OpenOption.APPEND).get(1000, TimeUnit.MILLISECONDS);
        assertEquals((nThreads * writes), bufferedFile.size().get(1000, TimeUnit.MILLISECONDS));
        Files.deleteIfExists(tempFile);
    }

    @Test
    public void read() throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.setLength(4);

        CommonFileTests.Pair<Path, BufferedFile> testFile = prepareFile();

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(testFile.e1.toFile()))) {
            for (int i = 0; i < 4000; i++) {
                String num = String.valueOf(i);
                for (int j = 0; j < 4 - num.length(); j++) {
                    bufferedWriter.write("0");
                }
                bufferedWriter.write(num);
                bufferedWriter.newLine();
            }
        }
        int charPerLine = 5;
        for (int i = 0; i < 1000; i++) {
            int offset = i * charPerLine;
            int expectedResult = offset / charPerLine;
            ByteBuffer buffer = ByteBuffer.allocateDirect(charPerLine);

            testFile.e2.read(buffer).get(1000, TimeUnit.MILLISECONDS);

            for (int j = 0; j < 4; j++) {
                byte b = buffer.get(j);
                builder.setCharAt(j, (char) b);
            }
            int result = Integer.parseInt(builder.toString());
            assertEquals(expectedResult, result);
        }
    }

    @Test
    public void scatteringRead_1() throws Exception {
        int numBuffers = 3;
        int bufferCap = 3;

        ByteBuffer[] buffers = new ByteBuffer[numBuffers];
        for (int i = 0; i < numBuffers; i++) {
            buffers[i] = ByteBuffer.allocateDirect(bufferCap);
        }
        CommonFileTests.Pair<Path, BufferedFile> pair = prepareFile();
        try (FileOutputStream fileOutputStream = new FileOutputStream(pair.e1.toFile())) {
            for (int i = -128; i < 128; i++) {
                fileOutputStream.write(i);
            }
        }

        byte expectedResult = (byte) (-128);

        for (int k = 0; k < 20; k++) {
            pair.e2.read(buffers).get(1000, TimeUnit.MILLISECONDS);
            for (int i = 0; i < numBuffers; i++) {
                for (int j = 0; j < bufferCap; j++) {
                    byte b = buffers[i].get(j);
                    assertEquals(b, expectedResult++);
                }
                buffers[i].flip();
            }
        }
        pair.e2.close().get(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void scatteringRead_2() throws Exception {
        ByteBuffer[] byteBuffers = Arrays.asList(
                        ByteBuffer.allocateDirect(10),
                        ByteBuffer.allocateDirect(10))
                .toArray(new ByteBuffer[0]);

        CommonFileTests.Pair<Path, BufferedFile> pathBufferedFilePair = prepareFile();

        try (FileOutputStream fileOutputStream = new FileOutputStream(pathBufferedFilePair.e1.toFile())) {
            for (int i = 0; i < 15; i++) {
                fileOutputStream.write(92);
            }
        }
        pathBufferedFilePair.e2.read(byteBuffers).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(10, byteBuffers[1].limit());
        pathBufferedFilePair.e2.close().get(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void buffersUpdate() throws Exception {
        int bufsNum = 4;
        ByteBuffer[] buffers = new ByteBuffer[bufsNum];
        for (int i = 0; i < bufsNum; i++) {
            buffers[i] = ByteBuffer.allocateDirect(10);
        }

        buffers[0].put((byte)1); buffers[0].flip();
        buffers[1].put((byte)2); buffers[1].flip();
        buffers[2].put((byte)3); buffers[2].flip();
        buffers[3].put((byte)4); buffers[3].flip();

        CommonFileTests.Pair<Path, BufferedFile> pair = prepareFile(OpenOption.READ_WRITE);

        pair.e2.write(buffers, 0, 2).get(1000, TimeUnit.MILLISECONDS);
        ByteBuffer bb = ByteBuffer.allocateDirect(10);
        pair.e2.read(bb).get(1000, TimeUnit.MILLISECONDS);
        bb.flip();
        assertEquals((byte) 1, bb.get());
        assertEquals((byte) 2, bb.get());
        assertThrows(BufferUnderflowException.class, bb::get);
        pair.e2.close().get(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void preAllocate_notEmptyFile() throws Exception {
        CommonFileTests.preAllocate_notEmptyFile(executor, BufferedFile.class);
    }

    @Test
    public void size_smallFile() throws Exception {
        CommonFileTests.size_smallFile(executor, BufferedFile.class);
    }

    @Test
    public void size_largeFile() throws Exception {
        CommonFileTests.size_largeFile(executor, BufferedFile.class);
    }

    @Test
    public void close() throws Exception {
        CommonFileTests.close(executor, BufferedFile.class);
    }

    @Test
    public void size_zero() throws Exception {
        CommonFileTests.size_zero(executor, BufferedFile.class);
    }

    @Test
    public void dataSync() throws Exception {
        CommonFileTests.dataSync(executor, BufferedFile.class);
    }

    @Test
    public void preAllocate_emptyFile() throws Exception {
        CommonFileTests.preAllocate_emptyFile(executor, BufferedFile.class);
    }

    @Test
    public void remove() throws Exception {
        CommonFileTests.remove(executor, BufferedFile.class);
    }

    private CommonFileTests.Pair<Path, BufferedFile> prepareFile(OpenOption... openOptions) throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", " file");
        BufferedFile file = BufferedFile.open(tempFile, executor, openOptions).get(1000, TimeUnit.MILLISECONDS);
        return new CommonFileTests.Pair<>(tempFile, file);
    }

    private CommonFileTests.Pair<Path, BufferedFile> prepareFile() throws Exception {
        return prepareFile(OpenOption.READ_ONLY);
    }

}
