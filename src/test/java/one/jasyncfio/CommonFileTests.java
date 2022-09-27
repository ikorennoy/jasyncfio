package one.jasyncfio;

import org.apache.commons.math3.stat.inference.TestUtils;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Time;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class CommonFileTests {
    private static final int DEFAULT_ALIGNMENT = (int) Native.getPageSize();

    static class Pair<A, B> {
        final A e1;
        final B e2;

        Pair(A e1, B e2) {
            this.e1 = e1;
            this.e2 = e2;
        }
    }

    static <T> void preAllocate_notEmptyFile(Pair<Path, AbstractFile> testFilePair) throws Exception {
        Path tempFile = testFilePair.e1;
        AbstractFile abstractFile = testFilePair.e2;
        assertEquals(0, abstractFile.size().get(1000, TimeUnit.MILLISECONDS));
        initTestFile(tempFile, 512);
        assertEquals(0, abstractFile.preAllocate(512, 512).get(1000, TimeUnit.MILLISECONDS));
        assertEquals(1024, abstractFile.size().get(1000, TimeUnit.MILLISECONDS));
        Files.deleteIfExists(tempFile);
    }

    static <T> void size_smallFile(Pair<Path, AbstractFile> testFilePair) throws Exception {
        Path tempFile = testFilePair.e1;
        AbstractFile abstractFile = testFilePair.e2;
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            int testSize = random.nextInt(1000);
            initTestFile(tempFile, testSize);
            assertEquals(testSize, abstractFile.size().get(1000, TimeUnit.MILLISECONDS));
        }
        Files.deleteIfExists(tempFile);
    }

    static <T> void size_largeFile(Pair<Path, AbstractFile> testFilePair) throws Exception {
        Path tempFile = testFilePair.e1;
        AbstractFile abstractFile = testFilePair.e2;
        long testSize = Integer.MAX_VALUE * 2L;
        initTestFile(tempFile, 10);
        try (RandomAccessFile f = new RandomAccessFile(tempFile.toFile(), "rw")) {
            FileChannel channel = f.getChannel();
            channel.map(FileChannel.MapMode.READ_WRITE, testSize, 10);
            assertEquals(testSize + 10, abstractFile.size().get(1000, TimeUnit.MILLISECONDS));
        }
        Files.deleteIfExists(tempFile);
    }

    static <T> void close(Pair<Path, AbstractFile> testFilePair) throws Exception {
        AbstractFile abstractFile = testFilePair.e2;
        abstractFile.close().get(1000, TimeUnit.MILLISECONDS);
        assertThrows(ExecutionException.class,
                () -> abstractFile.read(ByteBuffer.allocateDirect(10)).get(1000, TimeUnit.MILLISECONDS));
    }

    static <T> void size_zero(Pair<Path, AbstractFile> testFile) throws Exception {
        assertEquals(0, testFile.e2.size().get(1000, TimeUnit.MILLISECONDS));
    }

    static <T> void dataSync(Pair<Path, AbstractFile> testFile) throws Exception {
        assertEquals(0, testFile.e2.dataSync().get(1000, TimeUnit.MILLISECONDS));
    }

    static <T> void preAllocate_emptyFile(Pair<Path, AbstractFile> testFile) throws Exception {
        assertEquals(0, testFile.e2.size().get(1000, TimeUnit.MILLISECONDS));
        assertEquals(0, testFile.e2.preAllocate(1024, 0).get(1000, TimeUnit.MILLISECONDS));
        assertEquals(1024, testFile.e2.size().get(1000, TimeUnit.MILLISECONDS));
    }

    static void remove(Pair<Path, AbstractFile> testFile) throws Exception {
        assertTrue(Files.exists(testFile.e1));
        assertEquals(0, testFile.e2.remove().get(1000, TimeUnit.MILLISECONDS));
        assertFalse(Files.exists(testFile.e1));
    }

    static void dataSync_closedFile(Pair<Path, AbstractFile> testFile) throws Exception {
        testFile.e2.close().get(1000, TimeUnit.MILLISECONDS);
        CompletableFuture<Integer> dataSync = testFile.e2.dataSync();
        assertThrows(ExecutionException.class, () -> dataSync.get(1000, TimeUnit.MILLISECONDS));
    }

    static void remove_removed(Pair<Path, AbstractFile> testFile) throws Exception {
        assertTrue(Files.exists(testFile.e1));
        assertEquals(0, testFile.e2.remove().get(1000, TimeUnit.MILLISECONDS));
        assertFalse(Files.exists(testFile.e1));
        CompletableFuture<Integer> remove = testFile.e2.remove();
        assertThrows(ExecutionException.class, () -> remove.get(1000, TimeUnit.MILLISECONDS));
    }

    static void remove_readOnly(Pair<Path, AbstractFile> testFile) throws Exception {
        assertTrue(Files.exists(testFile.e1));
        assertEquals(0, testFile.e2.remove().get(1000, TimeUnit.MILLISECONDS));
        assertFalse(Files.exists(testFile.e1));
    }

    static void remove_closed(Pair<Path, AbstractFile> testFile) throws Exception {
        assertTrue(Files.exists(testFile.e1));
        assertEquals(0, testFile.e2.close().get(1000, TimeUnit.MILLISECONDS));
        assertThrows(ExecutionException.class, () -> testFile.e2.remove().get(1000, TimeUnit.MILLISECONDS));
    }

    static void write(Pair<Path, AbstractFile> testFile) throws Exception {
        String expected = prepareString(270);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(DEFAULT_ALIGNMENT, DEFAULT_ALIGNMENT);
        byteBuffer.put(expected.substring(0, DEFAULT_ALIGNMENT).getBytes(StandardCharsets.UTF_8));
        byteBuffer.flip();
        assertEquals(DEFAULT_ALIGNMENT, testFile.e2.write(byteBuffer, 0, DEFAULT_ALIGNMENT).get(1000, TimeUnit.MILLISECONDS));
        assertEquals(DEFAULT_ALIGNMENT, Files.size(testFile.e1));
        assertEquals(expected.substring(0, DEFAULT_ALIGNMENT), new String(Files.readAllBytes(testFile.e1)));
    }

    static void write_lengthGreaterThanBufferSize(Pair<Path, AbstractFile> testFile) {
        String expected = prepareString(50);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(512, DEFAULT_ALIGNMENT);
        byteBuffer.put(expected.substring(0, 512).getBytes(StandardCharsets.UTF_8)).flip();
        assertThrows(ExecutionException.class, () ->
                testFile.e2.write(byteBuffer, 0, 1024).get(1000, TimeUnit.MILLISECONDS));
    }

    static void read_1(Pair<Path, AbstractFile> testFile) throws Exception {
        Path tempFile = testFile.e1;
        String expected = prepareString(100);
        int readLength = 1024;
        writeStringToFile(expected, tempFile);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(readLength, DEFAULT_ALIGNMENT);
        Integer read = testFile.e2.read(byteBuffer, 0, readLength).get(1000, TimeUnit.MILLISECONDS);
        byteBuffer.flip();
        assertEquals(readLength, read);
        assertEquals(read, byteBuffer.limit());
        assertEquals(0, byteBuffer.position());
        String actual = StandardCharsets.UTF_8.decode(byteBuffer).toString();
        assertEquals(expected.substring(0, readLength), actual);
    }

    static void read_lengthGreaterThanBufferSize(Pair<Path, AbstractFile> testFile) throws Exception {
        Path tempFile = testFile.e1;
        String expected = prepareString(100);
        int readLength = 2048;
        writeStringToFile(expected, tempFile);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(1024, DEFAULT_ALIGNMENT);
        assertThrows(ExecutionException.class, () -> testFile.e2.read(byteBuffer, 0, readLength).get(1000, TimeUnit.MILLISECONDS));
    }

    static void read_positionGreaterThanFileSize(Pair<Path, AbstractFile> testFile) throws Exception {
        Path tempFile = testFile.e1;
        String expected = prepareString(10);
        int readLength = expected.length();
        writeStringToFile(expected, tempFile);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(2048, DEFAULT_ALIGNMENT);
        assertEquals(0, testFile.e2.read(byteBuffer, 2048, readLength).get(1000, TimeUnit.MILLISECONDS));
    }


    static void write_positionGreaterThanFileSize(Pair<Path, AbstractFile> testFile) throws Exception {
        Path tempFile = testFile.e1;
        String expected = prepareString(100);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(1024, DEFAULT_ALIGNMENT);
        byteBuffer.put(expected.substring(0, 1024).getBytes(StandardCharsets.UTF_8));
        byteBuffer.flip();
        assertEquals(1024, testFile.e2.write(byteBuffer, 512, 1024).get(1000, TimeUnit.MILLISECONDS));
        assertEquals(1536, Files.size(tempFile));
        assertEquals(expected.substring(0, 1024), new String(Files.readAllBytes(tempFile)).substring(512));
    }

    static void write_lengthLessThenBufferSize(Pair<Path, AbstractFile> testFile) throws Exception {
        Path tempFile = testFile.e1;
        String expected = prepareString(100);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(1024, DEFAULT_ALIGNMENT);
        byteBuffer.put(expected.substring(0, 1024).getBytes(StandardCharsets.UTF_8));
        byteBuffer.flip();
        assertEquals(1024, testFile.e2.write(byteBuffer, 0, 1024).get(1000, TimeUnit.MILLISECONDS));
        assertEquals(1024, Files.size(tempFile));
        assertEquals(expected.substring(0, 1024), new String(Files.readAllBytes(tempFile)));
    }

    static void write_trackPosition(Pair<Path, AbstractFile> testFile) throws Exception {
        Path tempFile = testFile.e1;
        assertEquals(0, Files.size(tempFile));
        String str = prepareString(100).substring(0, 512);
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(bytes.length, DEFAULT_ALIGNMENT);
        byteBuffer.put(bytes);
        byteBuffer.flip();

        Integer written = testFile.e2.write(byteBuffer, -1, bytes.length).get(1000, TimeUnit.MILLISECONDS);
        assertEquals((int) Files.size(tempFile), written);
        assertEquals(str, new String(Files.readAllBytes(tempFile)));
        byteBuffer.flip();
        testFile.e2.write(byteBuffer, -1, bytes.length).get(1000, TimeUnit.MILLISECONDS);
        assertEquals((int) Files.size(tempFile), written * 2);
        assertEquals(str + str, new String(Files.readAllBytes(tempFile)));
    }

    static void write_lengthZero(Pair<Path, AbstractFile> testFile) throws Exception {
        assertEquals(0, Files.size(testFile.e1));
        String str = prepareString(100);
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(bytes.length, DEFAULT_ALIGNMENT);
        byteBuffer.put(bytes);
        byteBuffer.flip();
        Integer written = testFile.e2.write(byteBuffer, 0, 0).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(0, written);
        assertEquals(0, Files.size(testFile.e1));
    }

    static void writev(Pair<Path, AbstractFile> testFile) throws Exception {
        Path tempFile = testFile.e1;
        assertEquals(0, Files.size(tempFile));
        ByteBuffer[] buffers = new ByteBuffer[10];
        StringBuilder strings = new StringBuilder();
        String str = prepareString(100).substring(0, 512);
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < buffers.length; i++) {
            ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(bytes.length, DEFAULT_ALIGNMENT);
            byteBuffer.put(bytes);
            byteBuffer.flip();
            buffers[i] = byteBuffer;
            strings.append(str);
        }
        Integer written = testFile.e2.write(buffers, 0).get(1000, TimeUnit.MILLISECONDS);
        assertEquals((int) Files.size(tempFile), written);
        assertEquals(strings.toString(), new String(Files.readAllBytes(tempFile)));
    }

    static void read_lengthLessThenBufferSize(Pair<Path, AbstractFile> testFile) throws Exception {
        Path tempFile = testFile.e1;
        String expected = prepareString(100);
        int readLength = 1024;
        writeStringToFile(expected, tempFile);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(2048, DEFAULT_ALIGNMENT);
        assertEquals(readLength, testFile.e2.read(byteBuffer, 0, readLength).get(1000, TimeUnit.MILLISECONDS));
        byteBuffer.flip();
        String actual = StandardCharsets.UTF_8.decode(byteBuffer).toString();
        assertEquals(expected.substring(0, readLength), actual);
    }

    static void read_bufferGreaterThanFile(Pair<Path, AbstractFile> testFile) throws Exception {
        Path tempFile = testFile.e1;
        String resultString = prepareString(100).substring(0, 512);
        writeStringToFile(resultString, tempFile);
        int stringLength = resultString.getBytes(StandardCharsets.UTF_8).length;
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(stringLength * 2, DEFAULT_ALIGNMENT);
        Integer bytes = testFile.e2.read(byteBuffer, 0, byteBuffer.capacity()).get(1000, TimeUnit.MILLISECONDS);
        byteBuffer.flip();
        assertEquals(stringLength, bytes);
        assertEquals(resultString, StandardCharsets.UTF_8.decode(byteBuffer).toString());
    }

    static void read_bufferLessThanFile(Pair<Path, AbstractFile> testFile) throws Exception {
        Path tempFile = testFile.e1;
        String resultString = prepareString(300).substring(0, 1024);
        writeStringToFile(resultString, tempFile);
        int stringLength = resultString.getBytes(StandardCharsets.UTF_8).length;
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(stringLength / 2, DEFAULT_ALIGNMENT);
        Integer bytes = testFile.e2.read(byteBuffer, 0, byteBuffer.capacity()).get(1000, TimeUnit.MILLISECONDS);
        byteBuffer.flip();
        assertEquals(stringLength / 2, bytes);
        assertEquals(resultString.substring(0, 512), StandardCharsets.UTF_8.decode(byteBuffer).toString());
    }

    static void open_newFile(Pair<Path, AbstractFile> testFile) {
        assertTrue(testFile.e2.getRawFd() > 0);
        assertTrue(Files.exists(testFile.e1));
    }


    static String prepareString(int iters) {
        StringBuilder sb = new StringBuilder();
        String s = "String number ";
        for (int i = 0; i < iters; i++) {
            sb.append(s).append(i).append("\n");
        }
        return sb.toString();
    }

    static void writeStringToFile(String stringToWrite, Path f) throws IOException {
        Files.write(f, stringToWrite.getBytes(StandardCharsets.UTF_8));
    }

    static String getTempFile(Path dir) throws Exception {
        return dir.resolve("temp").toAbsolutePath().toString();
    }


    private static void initTestFile(Path tempFile, int testSize) throws IOException {
        byte[] bytes = new byte[testSize];
        for (int i = 0; i < testSize; i++) {
            bytes[i] = 'e';
        }
        Files.write(
                tempFile,
                bytes,
                StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
        );
    }
}
