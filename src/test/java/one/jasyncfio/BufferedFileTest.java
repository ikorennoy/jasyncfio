package one.jasyncfio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static one.jasyncfio.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

public class BufferedFileTest {
    // todo replace
    private final String TEMP_FILE_NAME = "/tmp/" + UUID.randomUUID();

    @TempDir
    Path tmpDir;
    private final EventExecutorGroup eventExecutorGroup = EventExecutorGroup.initDefault();

    @Test
    void open() throws Exception {
        Path file = Files.createTempFile(tmpDir, "temp-", "-file");
        BufferedFile bufferedFile = eventExecutorGroup.openBufferedFile(file.toString()).get(1000, TimeUnit.MILLISECONDS);
        assertTrue(bufferedFile.getRawFd() > 0);
    }

    @Test
    void open_fileNotExist() {
        CompletableFuture<BufferedFile> open = eventExecutorGroup.openBufferedFile("some/path");
        assertThrows(ExecutionException.class, () -> open.get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void create_dir() {
        CompletableFuture<BufferedFile> bufferedFile = eventExecutorGroup.createBufferedFile("/tmp");
        assertThrows(ExecutionException.class, () -> bufferedFile.get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void create_fileExist() throws Exception {
        Path file = Files.createTempFile(tmpDir, "temp-", "-file");
        writeStringToFile("file is not empty", file.toFile());
        assertTrue(Files.size(file) > 0);
        BufferedFile bufferedFile = eventExecutorGroup.createBufferedFile(file.toString()).get(1000, TimeUnit.MILLISECONDS);
        // file was actually open
        assertTrue(bufferedFile.getRawFd() > 0);
        // file was truncated
        assertEquals(0, Files.size(file));
    }

    @Test
    void create_fileDoesNotExist() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup.createBufferedFile(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
        assertTrue(bufferedFile.getRawFd() > 0);
        File file = new File(bufferedFile.getPath());
        assertTrue(file.exists());
        file.deleteOnExit();
    }

    @Test
    void read() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "temp-", "-file");
        String resultString = prepareString(100);
        writeStringToFile(resultString, tempFile.toFile());
        assertTrue(Files.size(tempFile) > 0);
        ByteBuffer bb = ByteBuffer.allocateDirect((int) Files.size(tempFile));
        BufferedFile bufferedFile = eventExecutorGroup.openBufferedFile(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        Integer bytes = bufferedFile.read(0, bb).get(1000, TimeUnit.MILLISECONDS);
        assertEquals((int) Files.size(tempFile), bytes);
        assertEquals(resultString, StandardCharsets.UTF_8.decode(bb).toString());
    }

    @Test
    void read_wrongBuffer() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "temp-", "-file");
        BufferedFile bufferedFile = eventExecutorGroup.openBufferedFile(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        assertThrows(IllegalArgumentException.class, () -> bufferedFile.read(0, ByteBuffer.allocate(10)));
    }

    @Test
    void read_bufferGreaterThanFile() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "temp-", "-file");
        String resultString = prepareString(100);
        writeStringToFile(resultString, tempFile.toFile());
        int stringLength = resultString.getBytes(StandardCharsets.UTF_8).length;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(stringLength * 2);
        BufferedFile bufferedFile = eventExecutorGroup.openBufferedFile(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        Integer bytes = bufferedFile.read(0, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(stringLength, bytes);
    }

    @Test
    void read_bufferLessThanFile() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "temp-", "-file");
        String resultString = prepareString(100);
        writeStringToFile(resultString, tempFile.toFile());
        int stringLength = resultString.getBytes(StandardCharsets.UTF_8).length;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(stringLength / 2);
        BufferedFile bufferedFile = eventExecutorGroup.openBufferedFile(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        Integer bytes = bufferedFile.read(0, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(stringLength / 2, bytes);
    }

    @Test
    void read_lengthGreaterThanBufferSize() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(100);
        int readLength = 2048;
        TestUtils.writeStringToFile(expected, tempFile.toFile());
        BufferedFile bufferedFile = eventExecutorGroup.openBufferedFile(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        assertThrows(IllegalArgumentException.class, () -> bufferedFile.read(0, readLength, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void read_positionGreaterThanFileSize() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "temp-", "-file");
        String resultString = prepareString(100);
        writeStringToFile(resultString, tempFile.toFile());
        int stringLength = resultString.getBytes(StandardCharsets.UTF_8).length;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(stringLength);
        BufferedFile bufferedFile = eventExecutorGroup.openBufferedFile(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        Integer bytes = bufferedFile.read(stringLength * 2L, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(0, bytes);
    }

    @Test
    void write_emptyFile() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "temp-", "-file");
        BufferedFile bufferedFile = eventExecutorGroup.createBufferedFile(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(0, Files.size(tempFile));
        String str = prepareString(100);
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
        byteBuffer.put(bytes);
        Integer written = bufferedFile.write(0, bytes.length, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals((int) Files.size(tempFile), written);
        assertEquals(str, new String(Files.readAllBytes(tempFile)));
    }

    @Test
    void write_trackPosition() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "temp-", "-file");
        BufferedFile bufferedFile = eventExecutorGroup.createBufferedFile(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(0, Files.size(tempFile));
        String str = prepareString(100);
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
        byteBuffer.put(bytes);
        Integer written = bufferedFile.write(-1, bytes.length, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals((int) Files.size(tempFile), written);
        assertEquals(str, new String(Files.readAllBytes(tempFile)));

        bufferedFile.write(-1, bytes.length, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals((int) Files.size(tempFile), written * 2);
        assertEquals(str + str, new String(Files.readAllBytes(tempFile)));
    }

    @Test
    void write_positionGreaterThanFileSize() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup.createBufferedFile(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
        File file = new File(bufferedFile.getPath());
        file.deleteOnExit();
        assertEquals(0, file.length());
        String str = prepareString(100);
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
        byteBuffer.put(bytes);
        Integer written = bufferedFile.write(100, bytes.length, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(file.length(), written + 100);
        assertEquals(bytes.length, written);
    }

    @Test
    void write_lengthZero() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup.createBufferedFile(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
        File file = new File(bufferedFile.getPath());
        file.deleteOnExit();
        assertEquals(0, file.length());
        String str = prepareString(100);
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
        byteBuffer.put(bytes);
        Integer written = bufferedFile.write(0, 0, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(0, written);
        assertEquals(0, file.length());
    }

    @Test
    void close() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "temp-", "-file");
        BufferedFile bufferedFile = eventExecutorGroup.openBufferedFile(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        assertTrue(bufferedFile.getRawFd() > 0);
        bufferedFile.close().get(1000, TimeUnit.MILLISECONDS);
        CompletableFuture<Integer> read = bufferedFile.read(0, ByteBuffer.allocateDirect(10));
        assertThrows(ExecutionException.class, () -> read.get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void size() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup.createBufferedFile(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
        File file = new File(bufferedFile.getPath());
        file.deleteOnExit();
        String resultString = prepareString(100);
        writeStringToFile(resultString, file);
        int expectedLength = resultString.getBytes(StandardCharsets.UTF_8).length;
        long actualLength = bufferedFile.size().get(1000, TimeUnit.MILLISECONDS);
        assertEquals(expectedLength, actualLength);
    }

    @Test
    void size_zero() throws Exception {
        BufferedFile f = eventExecutorGroup.createBufferedFile(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
        deleteOnExit(f);
        assertEquals(0, f.size().get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void dataSync() throws Exception {
        BufferedFile f = eventExecutorGroup.createBufferedFile(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
        deleteOnExit(f);
        assertEquals(0, f.dataSync().get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void dataSync_closedFile() throws Exception {
        BufferedFile f = eventExecutorGroup.createBufferedFile(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
        deleteOnExit(f);
        f.close().get(1000, TimeUnit.MILLISECONDS);
        CompletableFuture<Integer> dataSync = f.dataSync();
        assertThrows(ExecutionException.class, () -> dataSync.get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void preAllocate_emptyFile() throws Exception {
        BufferedFile f = eventExecutorGroup.createBufferedFile(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
        deleteOnExit(f);
        assertEquals(0, f.size().get(1000, TimeUnit.MILLISECONDS));
        assertEquals(0, f.preAllocate(1024).get(1000, TimeUnit.MILLISECONDS));
        assertEquals(1024, f.size().get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void preAllocate_notEmptyFile() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup.createBufferedFile(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
        File file = new File(bufferedFile.getPath());
        file.deleteOnExit();
        String resultString = prepareString(100);
        long stringLength = resultString.getBytes(StandardCharsets.UTF_8).length;
        writeStringToFile(resultString, file);
        assertEquals(stringLength, bufferedFile.size().get(1000, TimeUnit.MILLISECONDS));
        assertEquals(0, bufferedFile.preAllocate(stringLength * 2).get(1000, TimeUnit.MILLISECONDS));
        assertEquals(stringLength * 2, bufferedFile.size().get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void preAllocate_withOffset() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup.createBufferedFile(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
        File file = new File(bufferedFile.getPath());
        file.deleteOnExit();
        String resultString = prepareString(100);
        long stringLength = resultString.getBytes(StandardCharsets.UTF_8).length;
        writeStringToFile(resultString, file);
        assertEquals(stringLength, bufferedFile.size().get(1000, TimeUnit.MILLISECONDS));
        assertEquals(0, bufferedFile.preAllocate(stringLength, stringLength).get(1000, TimeUnit.MILLISECONDS));
        assertEquals(stringLength * 2, bufferedFile.size().get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void preAllocate_closedFile() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup.createBufferedFile(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
        deleteOnExit(bufferedFile);
        bufferedFile.close().get(1000, TimeUnit.MILLISECONDS);
        CompletableFuture<Integer> preAllocate = bufferedFile.preAllocate(1024);
        assertThrows(ExecutionException.class, () -> preAllocate.get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void remove() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup.createBufferedFile(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
        File file = new File(bufferedFile.getPath());
        assertTrue(file.exists());
        assertEquals(0, bufferedFile.remove().get(1000, TimeUnit.MILLISECONDS));
        assertFalse(file.exists());
    }

    @Test
    void remove_removed() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup.createBufferedFile(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
        File file = new File(bufferedFile.getPath());
        assertTrue(file.exists());
        assertEquals(0, bufferedFile.remove().get(1000, TimeUnit.MILLISECONDS));
        assertFalse(file.exists());
        CompletableFuture<Integer> remove = bufferedFile.remove();
        assertThrows(ExecutionException.class, () -> remove.get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void remove_readOnly() throws Exception {
        File tempFile = File.createTempFile("temp-", "-file");
        assertTrue(tempFile.exists());
        BufferedFile bufferedFile = eventExecutorGroup.openBufferedFile(tempFile.getPath()).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(0, bufferedFile.remove().get(1000, TimeUnit.MILLISECONDS));
        assertFalse(tempFile.exists());
    }

    @Test
    void remove_closed() throws Exception {
        File tempFile = File.createTempFile("temp-", "-file");
        assertTrue(tempFile.exists());
        BufferedFile bufferedFile = eventExecutorGroup.openBufferedFile(tempFile.getPath()).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(0, bufferedFile.close().get(1000, TimeUnit.MILLISECONDS));
        assertEquals(0, bufferedFile.remove().get(1000, TimeUnit.MILLISECONDS));
        assertFalse(tempFile.exists());
    }

//    @Test
//    void read_ioPoll() throws Exception {
//        EventExecutorGroup eventExecutorGroup = EventExecutorGroup.builder()
//                .ioRingSetupSqPoll(500_000)
//                .build();
//        Path tempFile = Files.createTempFile(tmpDir, "temp-", "-file");
//        BufferedFile bufferedFile = eventExecutorGroup.createBufferedFile(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
//        assertEquals(0, Files.size(tempFile));
//        String str = prepareString(100);
//        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
//        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
//        byteBuffer.put(bytes);
//        Integer written = bufferedFile.write(0, bytes.length, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
//        assertEquals((int) Files.size(tempFile), written);
//        assertEquals(str, new String(Files.readAllBytes(tempFile)));
//    }

    private void deleteOnExit(BufferedFile f) {
        new File(f.getPath()).deleteOnExit();
    }
}
