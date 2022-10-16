package one.jasyncfio;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AsyncFileTest {
    private final EventExecutor executor = EventExecutor.initDefault();

    @TempDir
    private Path tmpDir;

    @Test
    void atomicAppend() throws Exception {
        Random random = new Random();
        Path tempFile = Files.createTempFile(tmpDir, "temp-", " file");
        int nThreads = 16;
        int writes = 1000;

        ExecutorService pool = Executors.newFixedThreadPool(nThreads);
        for (int i = 0; i < nThreads; i++) {
            pool.execute(() -> {
                for (int j = 0; j < writes; j++) {
                    try {
                        AsyncFile asyncFile = AsyncFile.open(tempFile, executor, OpenOption.WRITE_ONLY, OpenOption.APPEND).get(1000, TimeUnit.MILLISECONDS);
                        ByteBuffer buffer = ByteBuffer.allocateDirect(1);
                        buffer.put((byte) 'c');
                        buffer.flip();
                        if (random.nextBoolean()) {
                            ByteBuffer[] buffers = new ByteBuffer[]{buffer};
                            asyncFile.write(buffers);
                        } else {
                            asyncFile.write(buffer);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);
        AsyncFile asyncFile = AsyncFile.open(tempFile, executor, OpenOption.WRITE_ONLY, OpenOption.APPEND).get(1000, TimeUnit.MILLISECONDS);
        assertEquals((nThreads * writes), asyncFile.size().get(1000, TimeUnit.MILLISECONDS));
        Files.deleteIfExists(tempFile);
    }

    @Test
    void read() throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.setLength(4);

        CommonFileTests.Pair<Path, AbstractFile> testFile = prepareFile();

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
    void scatteringRead_1() throws Exception {
        int numBuffers = 3;
        int bufferCap = 3;

        ByteBuffer[] buffers = new ByteBuffer[numBuffers];
        for (int i = 0; i < numBuffers; i++) {
            buffers[i] = ByteBuffer.allocateDirect(bufferCap);
        }
        CommonFileTests.Pair<Path, AbstractFile> pair = prepareFile();
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
    void scatteringRead_2() throws Exception {
        ByteBuffer[] byteBuffers = Arrays.asList(
                        ByteBuffer.allocateDirect(10),
                        ByteBuffer.allocateDirect(10))
                .toArray(new ByteBuffer[0]);

        CommonFileTests.Pair<Path, AbstractFile> pathBufferedFilePair = prepareFile();

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
    void buffersUpdate() throws Exception {
        int bufsNum = 4;
        ByteBuffer[] buffers = new ByteBuffer[bufsNum];
        for (int i = 0; i < bufsNum; i++) {
            buffers[i] = ByteBuffer.allocateDirect(10);
        }

        buffers[0].put((byte)1); buffers[0].flip();
        buffers[1].put((byte)2); buffers[1].flip();
        buffers[2].put((byte)3); buffers[2].flip();
        buffers[3].put((byte)4); buffers[3].flip();

        CommonFileTests.Pair<Path, AbstractFile> pair = prepareFile(OpenOption.READ_WRITE);

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
    void preAllocate_notEmptyFile() throws Exception {
        CommonFileTests.preAllocate_notEmptyFile(prepareFile(OpenOption.WRITE_ONLY));
    }

    @Test
    void size_smallFile() throws Exception {
        CommonFileTests.size_smallFile(prepareFile(OpenOption.READ_WRITE));
    }

    @Test
    void size_largeFile() throws Exception {
        CommonFileTests.size_largeFile(prepareFile(OpenOption.READ_WRITE));
    }

    @Test
    void close() throws Exception {
        CommonFileTests.close(prepareFile(OpenOption.READ_WRITE));
    }

    @Test
    void size_zero() throws Exception {
        CommonFileTests.size_zero(prepareFile());
    }

    @Test
    void dataSync() throws Exception {
        CommonFileTests.dataSync(prepareFile());
    }

    @Test
    void preAllocate_emptyFile() throws Exception {
        CommonFileTests.preAllocate_emptyFile(prepareFile(OpenOption.WRITE_ONLY));
    }

    @Test
    void remove() throws Exception {
        CommonFileTests.remove(prepareFile());
    }

    @Test
    void dataSync_closedFile() throws Exception {
        CommonFileTests.dataSync_closedFile(prepareFile());
    }

    @Test
    void remove_removed() throws Exception {
        CommonFileTests.remove_removed(prepareFile());
    }

    @Test
    void remove_readOnly() throws Exception {
        CommonFileTests.remove_readOnly(prepareFile());
    }

    @Test
    void remove_closed() throws Exception {
        CommonFileTests.remove_closed(prepareFile());
    }

    @Test
    void write() throws Exception {
        CommonFileTests.write(prepareFile(OpenOption.READ_WRITE));
    }

    @Test
    void write_lengthGreaterThanBufferSize() throws Exception {
        CommonFileTests.write_lengthGreaterThanBufferSize(prepareFile(OpenOption.READ_WRITE));
    }

    @Test
    void read_1() throws Exception {
        CommonFileTests.read_1(prepareFile(OpenOption.READ_WRITE));
    }

    @Test
    void read_lengthGreaterThanBufferSize() throws Exception {
        CommonFileTests.read_lengthGreaterThanBufferSize(prepareFile(OpenOption.READ_WRITE));
    }

    @Test
    void read_positionGreaterThanFileSize() throws Exception {
        CommonFileTests.read_positionGreaterThanFileSize(prepareFile(OpenOption.READ_WRITE));
    }

    @Test
    void write_positionGreaterThanFileSize() throws Exception {
        CommonFileTests.write_positionGreaterThanFileSize(prepareFile(OpenOption.READ_WRITE));
    }

    @Test
    void write_lengthLessThenBufferSize() throws Exception {
        CommonFileTests.write_lengthLessThenBufferSize(prepareFile(OpenOption.READ_WRITE));
    }

    @Test
    void write_trackPosition() throws Exception {
        CommonFileTests.write_trackPosition(prepareFile(OpenOption.READ_WRITE));
    }

    @Test
    void write_lengthZero() throws Exception {
        CommonFileTests.write_lengthZero(prepareFile(OpenOption.READ_WRITE));
    }

    @Test
    void writev() throws Exception {
        CommonFileTests.write_lengthZero(prepareFile(OpenOption.READ_WRITE));
    }

    @Test
    void read_lengthLessThenBufferSize() throws Exception {
        CommonFileTests.read_lengthLessThenBufferSize(prepareFile(OpenOption.READ_WRITE));
    }

    @Test
    void read_bufferGreaterThanFile() throws Exception {
        CommonFileTests.read_bufferGreaterThanFile(prepareFile(OpenOption.READ_WRITE));
    }

    @Test
    void read_bufferLessThanFile() throws Exception {
        CommonFileTests.read_bufferLessThanFile(prepareFile(OpenOption.READ_WRITE));
    }

    @Test
    void open_newFile() throws Exception {
        CommonFileTests.open_newFile(prepareFile());
    }

    @Test
    void read_aligned() throws Exception {
        CommonFileTests.read_aligned(prepareFile(OpenOption.READ_WRITE, OpenOption.DIRECT));
    }

    @Test
    @Disabled("required 5.19+ CI kernel version")
    void bufRing() throws Exception {
        EventExecutor ee = EventExecutor.builder()
                .withBufRing(4, 4096).build();
        Path tempFile = Files.createTempFile(tmpDir, "test-", " file");
        AsyncFile file = AsyncFile.open(tempFile, ee, OpenOption.READ_WRITE).get(1000, TimeUnit.MILLISECONDS);
        CommonFileTests.bufRing(new CommonFileTests.Pair<>(tempFile, file));
    }

    private CommonFileTests.Pair<Path, AbstractFile> prepareFile(OpenOption... openOptions) throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", " file");
        AsyncFile file = AsyncFile.open(tempFile, executor, openOptions).get(1000, TimeUnit.MILLISECONDS);
        return new CommonFileTests.Pair<>(tempFile, file);
    }

    private CommonFileTests.Pair<Path, AbstractFile> prepareFile() throws Exception {
        return prepareFile(OpenOption.READ_ONLY);
    }

}
