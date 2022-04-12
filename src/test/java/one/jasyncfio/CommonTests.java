package one.jasyncfio;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static one.jasyncfio.TestUtils.prepareString;
import static one.jasyncfio.TestUtils.writeStringToFile;
import static org.junit.jupiter.api.Assertions.*;

public class CommonTests {

    static void size(AbstractFile testFile) throws Exception {
        Path file = Paths.get(testFile.getPath());
        String resultString = prepareString(100);
        writeStringToFile(resultString, file);
        int expectedLength = resultString.getBytes(StandardCharsets.UTF_8).length;
        long actualLength = testFile.size().get(1000, TimeUnit.MILLISECONDS);
        assertEquals(expectedLength, actualLength);
    }

    static void size_zero(AbstractFile testFile) throws Exception {
        assertEquals(0, testFile.size().get(1000, TimeUnit.MILLISECONDS));
    }

    static void close(AbstractFile testFile) throws Exception {
        assertTrue(testFile.getRawFd() > 0);
        testFile.close().get(1000, TimeUnit.MILLISECONDS);
        CompletableFuture<Integer> read = testFile.read(0, 10, ByteBuffer.allocateDirect(10));
        assertThrows(ExecutionException.class, () -> read.get(1000, TimeUnit.MILLISECONDS));
    }

    static void dataSync(AbstractFile testFile) throws Exception {
        assertEquals(0, testFile.dataSync().get(1000, TimeUnit.MILLISECONDS));
    }

    static void dataSync_closedFile(AbstractFile testFile) throws Exception {
        testFile.close().get(1000, TimeUnit.MILLISECONDS);
        CompletableFuture<Integer> dataSync = testFile.dataSync();
        assertThrows(ExecutionException.class, () -> dataSync.get(1000, TimeUnit.MILLISECONDS));
    }

    static void preAllocate_emptyFile(AbstractFile testFile) throws Exception {
        assertEquals(0, testFile.size().get(1000, TimeUnit.MILLISECONDS));
        assertEquals(0, testFile.preAllocate(1024).get(1000, TimeUnit.MILLISECONDS));
        assertEquals(1024, testFile.size().get(1000, TimeUnit.MILLISECONDS));
    }

    static void remove(AbstractFile testFile) throws Exception {
        File file = new File(testFile.getPath());
        assertTrue(file.exists());
        assertEquals(0, testFile.remove().get(1000, TimeUnit.MILLISECONDS));
        assertFalse(file.exists());
    }

    static void remove_removed(AbstractFile testFile) throws Exception {
        File file = new File(testFile.getPath());
        assertTrue(file.exists());
        assertEquals(0, testFile.remove().get(1000, TimeUnit.MILLISECONDS));
        assertFalse(file.exists());
        CompletableFuture<Integer> remove = testFile.remove();
        assertThrows(ExecutionException.class, () -> remove.get(1000, TimeUnit.MILLISECONDS));
    }

    static void remove_readOnly(AbstractFile testFile) throws Exception {
        Path file = Paths.get(testFile.getPath());
        assertTrue(Files.exists(file));
        assertEquals(0, testFile.remove().get(1000, TimeUnit.MILLISECONDS));
        assertFalse(Files.exists(file));
    }

    static void remove_closed(AbstractFile testFile) throws Exception {
        Path path = Paths.get(testFile.getPath());
        assertTrue(Files.exists(path));
        assertEquals(0, testFile.close().get(1000, TimeUnit.MILLISECONDS));
        assertEquals(0, testFile.remove().get(1000, TimeUnit.MILLISECONDS));
        assertFalse(Files.exists(path));
    }

    static void write(AbstractFile testFile) throws Exception {
        Path tempFile = Paths.get(testFile.getPath());
        String expected = TestUtils.prepareString(270);

        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(DmaFile.DEFAULT_ALIGNMENT, DmaFile.DEFAULT_ALIGNMENT);
        byteBuffer.put(expected.substring(0, DmaFile.DEFAULT_ALIGNMENT).getBytes(StandardCharsets.UTF_8));
        assertEquals(DmaFile.DEFAULT_ALIGNMENT, testFile.write(0, DmaFile.DEFAULT_ALIGNMENT, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
        assertEquals(DmaFile.DEFAULT_ALIGNMENT, Files.size(tempFile));
        assertEquals(expected.substring(0, DmaFile.DEFAULT_ALIGNMENT), new String(Files.readAllBytes(tempFile)));
    }

    static void write_lengthGreaterThanBufferSize(AbstractFile testFile) {
        String expected = TestUtils.prepareString(50);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(512, DmaFile.DEFAULT_ALIGNMENT);
        byteBuffer.put(expected.substring(0, 512).getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> testFile.write(0, 1024, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
    }

    static void read(AbstractFile testFile) throws Exception {
        Path tempFile = Paths.get(testFile.getPath());
        String expected = TestUtils.prepareString(100);
        int readLength = 1024;
        TestUtils.writeStringToFile(expected, tempFile);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(readLength, DmaFile.DEFAULT_ALIGNMENT);
        Integer read = testFile.read(0, readLength, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(readLength, read);
        assertEquals(read, byteBuffer.limit());
        assertEquals(0, byteBuffer.position());
        String actual = StandardCharsets.UTF_8.decode(byteBuffer).toString();
        assertEquals(expected.substring(0, readLength), actual);
    }

    static void read_lengthGreaterThanBufferSize(AbstractFile testFile) throws Exception {
        Path tempFile = Paths.get(testFile.getPath());
        String expected = TestUtils.prepareString(100);
        int readLength = 2048;
        TestUtils.writeStringToFile(expected, tempFile);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(1024, DmaFile.DEFAULT_ALIGNMENT);
        assertThrows(IllegalArgumentException.class, () -> testFile.read(0, readLength, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
    }

    static void read_positionGreaterThanFileSize(AbstractFile testFile) throws Exception {
        Path tempFile = Paths.get(testFile.getPath());
        String expected = TestUtils.prepareString(10);
        int readLength = expected.length();
        TestUtils.writeStringToFile(expected, tempFile);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(2048, DmaFile.DEFAULT_ALIGNMENT);
        assertEquals(0, testFile.read(2048, readLength, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
    }

    static void readv(AbstractFile testFile) throws Exception {

    }

    static void write_positionGreaterThanFileSize(AbstractFile testFile) throws Exception {
        Path tempFile = Paths.get(testFile.getPath());
        String expected = TestUtils.prepareString(100);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(1024, DmaFile.DEFAULT_ALIGNMENT);
        byteBuffer.put(expected.substring(0, 1024).getBytes(StandardCharsets.UTF_8));
        assertEquals(1024, testFile.write(512, 1024, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
        assertEquals(1536, Files.size(tempFile));
        assertEquals(expected.substring(0, 1024), new String(Files.readAllBytes(tempFile)).substring(512));
    }

    static void write_lengthLessThenBufferSize(AbstractFile testFile) throws Exception {
        Path tempFile = Paths.get(testFile.getPath());
        String expected = TestUtils.prepareString(100);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(1024, DmaFile.DEFAULT_ALIGNMENT);
        byteBuffer.put(expected.substring(0, 1024).getBytes(StandardCharsets.UTF_8));
        assertEquals(1024, testFile.write(0, 1024, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
        assertEquals(1024, Files.size(tempFile));
        assertEquals(expected.substring(0, 1024), new String(Files.readAllBytes(tempFile)));
    }

    static void write_trackPosition(AbstractFile testFile) throws Exception {
        Path tempFile = Paths.get(testFile.getPath());
        assertEquals(0, Files.size(tempFile));
        String str = prepareString(100).substring(0, 512);
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(bytes.length, DmaFile.DEFAULT_ALIGNMENT);
        byteBuffer.put(bytes);
        Integer written = testFile.write(-1, bytes.length, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals((int) Files.size(tempFile), written);
        assertEquals(str, new String(Files.readAllBytes(tempFile)));

        testFile.write(-1, bytes.length, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals((int) Files.size(tempFile), written * 2);
        assertEquals(str + str, new String(Files.readAllBytes(tempFile)));
    }

    static void write_lengthZero(AbstractFile testFile) throws Exception {
        File file = new File(testFile.getPath());
        assertEquals(0, file.length());
        String str = prepareString(100);
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(bytes.length, DmaFile.DEFAULT_ALIGNMENT);
        byteBuffer.put(bytes);
        Integer written = testFile.write(0, 0, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(0, written);
        assertEquals(0, file.length());
    }

    static void writev(AbstractFile testFile) throws Exception {
        Path tempFile = Paths.get(testFile.getPath());
        assertEquals(0, Files.size(tempFile));
        ByteBuffer[] buffers = new ByteBuffer[10];
        StringBuilder strings = new StringBuilder();
        String str = prepareString(100).substring(0, 512);
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < buffers.length; i++) {
            ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(bytes.length, DmaFile.DEFAULT_ALIGNMENT);
            byteBuffer.put(bytes);
            byteBuffer.flip();
            buffers[i] = byteBuffer;
            strings.append(str);
        }
        Integer written = testFile.write(0, buffers).get(1000, TimeUnit.MILLISECONDS);
        assertEquals((int) Files.size(tempFile), written);
        assertEquals(strings.toString(), new String(Files.readAllBytes(tempFile)));
    }

    static void writeFixed(AbstractFile testFile, EventExecutorGroup eventExecutorGroup) throws Exception {
        Path tempFile = Paths.get(testFile.getPath());
        ByteBuffer[] buffers = new ByteBuffer[1];
        String str = prepareString(100).substring(0, 512);
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < buffers.length; i++) {
            ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(bytes.length, DmaFile.DEFAULT_ALIGNMENT);
            byteBuffer.put(bytes);
            byteBuffer.flip();
            buffers[i] = byteBuffer;
        }
        IovecArray iovecArray = eventExecutorGroup.registerBuffers(buffers).get(1000, TimeUnit.MILLISECONDS);
        Integer written = testFile.writeFixed(0, 0, iovecArray).get();
        assertEquals((int) Files.size(tempFile), written);
        assertEquals(str, new String(Files.readAllBytes(tempFile)));
    }

    static void readFixed(AbstractFile testFile, EventExecutorGroup eventExecutorGroup) throws Exception {
        Path tempFile = Paths.get(testFile.getPath());
        String resultString = prepareString(100).substring(0, 512);
        writeStringToFile(resultString, tempFile);
        int length = resultString.getBytes(StandardCharsets.UTF_8).length;
        ByteBuffer[] buffers = new ByteBuffer[1];
        for (int i = 0; i < buffers.length; i++) {
            ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(length, DmaFile.DEFAULT_ALIGNMENT);
            buffers[i] = byteBuffer;
        }
        IovecArray iovecArray = eventExecutorGroup.registerBuffers(buffers).get(1000, TimeUnit.MILLISECONDS);
        Integer read = testFile.readFixed(0, 0, iovecArray).get();
        assertEquals(length, read);
        assertEquals(resultString, StandardCharsets.UTF_8.decode(buffers[0]).toString());
    }

    static void read_lengthLessThenBufferSize(AbstractFile testFile) throws Exception {
        Path tempFile = Paths.get(testFile.getPath());
        String expected = TestUtils.prepareString(100);
        int readLength = 1024;
        TestUtils.writeStringToFile(expected, tempFile);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(2048, DmaFile.DEFAULT_ALIGNMENT);
        assertEquals(readLength, testFile.read(0, readLength, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
        String actual = StandardCharsets.UTF_8.decode(byteBuffer).toString();
        assertEquals(expected.substring(0, readLength), actual);
    }

    static void read_bufferGreaterThanFile(AbstractFile testFile) throws Exception {
        Path tempFile = Paths.get(testFile.getPath());
        String resultString = prepareString(100).substring(0, 512);
        writeStringToFile(resultString, tempFile);
        int stringLength = resultString.getBytes(StandardCharsets.UTF_8).length;
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(stringLength * 2, DmaFile.DEFAULT_ALIGNMENT);
        Integer bytes = testFile.read(0, byteBuffer.capacity(), byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(stringLength, bytes);
        assertEquals(resultString, StandardCharsets.UTF_8.decode(byteBuffer).toString());
    }

    static void read_bufferLessThanFile(AbstractFile testFile) throws Exception {
        Path tempFile = Paths.get(testFile.getPath());
        String resultString = prepareString(300).substring(0, 1024);
        writeStringToFile(resultString, tempFile);
        int stringLength = resultString.getBytes(StandardCharsets.UTF_8).length;
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(stringLength / 2, DmaFile.DEFAULT_ALIGNMENT);
        Integer bytes = testFile.read(0, byteBuffer.capacity(), byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(stringLength / 2, bytes);
        assertEquals(resultString.substring(0, 512), StandardCharsets.UTF_8.decode(byteBuffer).toString());
    }

    static void open_newFile(AbstractFile testFile) {
        assertTrue(testFile.getRawFd() > 0);
        Path file = Paths.get(testFile.getPath());
        assertTrue(Files.exists(file));
    }
}
