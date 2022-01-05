package one.jasyncfio;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static one.jasyncfio.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

public class BufferedFileTest {
    private final String TEMP_FILE_NAME = "/tmp/" + UUID.randomUUID();

    @Test
    void open() throws Exception {
        File file = File.createTempFile("temp-", "-file");
        file.deleteOnExit();
        BufferedFile bufferedFile = BufferedFile.open(file.getPath()).get(1000, TimeUnit.MILLISECONDS);
        assertTrue(bufferedFile.getRawFd() > 0);
    }

    @Test
    void open_fileNotExist() {
        CompletableFuture<BufferedFile> open = BufferedFile.open("some/path");
        assertThrows(ExecutionException.class, () -> open.get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void create_dir() {
        CompletableFuture<BufferedFile> bufferedFile = BufferedFile.create("/tmp");
        assertThrows(ExecutionException.class, () -> bufferedFile.get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void create_fileExist() throws Exception {
        File file = File.createTempFile("temp-", "-file");
        file.deleteOnExit();
        assertTrue(file.exists());
        FileWriter writer = new FileWriter(file);
        writer.write("file is not empty");
        writer.flush();
        writer.close();
        assertTrue(file.length() > 0);
        BufferedFile bufferedFile = BufferedFile.create(file.getPath()).get(1000, TimeUnit.MILLISECONDS);
        // file was actually open
        assertTrue(bufferedFile.getRawFd() > 0);
        // file was truncated
        assertEquals(0, file.length());
    }

    @Test
    void create_fileDoesNotExist() throws Exception {
        BufferedFile bufferedFile = BufferedFile.create(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
        assertTrue(bufferedFile.getRawFd() > 0);
        File file = new File(bufferedFile.getPath());
        assertTrue(file.exists());
        file.deleteOnExit();
    }

    @Test
    void read() throws Exception {
        File tempFile = File.createTempFile("temp-", "-file");
        tempFile.deleteOnExit();
        FileWriter fw = new FileWriter(tempFile);
        String resultString = prepareString(100);
        fw.write(resultString);
        fw.flush();
        fw.close();
        assertTrue(tempFile.length() > 0);
        ByteBuffer bb = ByteBuffer.allocateDirect((int) tempFile.length());
        BufferedFile bufferedFile = BufferedFile.open(tempFile.getPath()).get(1000, TimeUnit.MILLISECONDS);
        Integer bytes = bufferedFile.read(0, bb).get(1000, TimeUnit.MILLISECONDS);
        assertEquals((int) tempFile.length(), bytes);
        assertEquals(resultString, StandardCharsets.UTF_8.decode(bb).toString());
    }

    @Test
    void read_wrongBuffer() throws Exception {
        File tempFile = File.createTempFile("temp-", "-file");
        tempFile.deleteOnExit();
        BufferedFile bufferedFile = BufferedFile.open(tempFile.getPath()).get(1000, TimeUnit.MILLISECONDS);
        assertThrows(IllegalArgumentException.class, () -> bufferedFile.read(0, ByteBuffer.allocate(10)));
    }

    @Test
    void read_bufferGreaterThanFile() throws Exception {
        File tempFile = File.createTempFile("temp-", "-file");
        tempFile.deleteOnExit();
        FileWriter fw = new FileWriter(tempFile);
        String resultString = prepareString(100);
        fw.write(resultString);
        fw.flush();
        fw.close();
        int stringLength = resultString.getBytes(StandardCharsets.UTF_8).length;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(stringLength * 2);
        BufferedFile bufferedFile = BufferedFile.open(tempFile.getPath()).get(1000, TimeUnit.MILLISECONDS);
        Integer bytes = bufferedFile.read(0, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(stringLength, bytes);
    }

    @Test
    void read_bufferLessThanFile() throws Exception {
        File tempFile = File.createTempFile("temp-", "-file");
        tempFile.deleteOnExit();
        FileWriter fw = new FileWriter(tempFile);
        String resultString = prepareString(100);
        fw.write(resultString);
        fw.flush();
        fw.close();
        int stringLength = resultString.getBytes(StandardCharsets.UTF_8).length;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(stringLength / 2);
        BufferedFile bufferedFile = BufferedFile.open(tempFile.getPath()).get(1000, TimeUnit.MILLISECONDS);
        Integer bytes = bufferedFile.read(0, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(stringLength / 2, bytes);
    }

    @Test
    void read_offsetGreaterThanFileSize() throws Exception {
        File tempFile = File.createTempFile("temp-", "-file");
        tempFile.deleteOnExit();
        FileWriter fw = new FileWriter(tempFile);
        String resultString = prepareString(100);
        fw.write(resultString);
        fw.flush();
        fw.close();
        int stringLength = resultString.getBytes(StandardCharsets.UTF_8).length;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(stringLength);
        BufferedFile bufferedFile = BufferedFile.open(tempFile.getPath()).get(1000, TimeUnit.MILLISECONDS);
        Integer bytes = bufferedFile.read(stringLength * 2L, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(0, bytes);
    }

    // read len 0

    @Test
    void write_emptyFile() throws Exception {
        File tempFile = File.createTempFile("temp-", "-file");
        tempFile.deleteOnExit();
        BufferedFile bufferedFile = BufferedFile.create(tempFile.getPath()).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(0, tempFile.length());
        String str = prepareString(100);
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
        byteBuffer.put(bytes);
        Integer wrote = bufferedFile.write(0, bytes.length, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals((int) tempFile.length(), wrote);
        assertEquals(str, readFileToString(tempFile));
    }

    @Test
    void write_positionGreaterThanFileSize() throws Exception {
        BufferedFile bufferedFile = BufferedFile.create(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
        File file = new File(bufferedFile.getPath());
        file.deleteOnExit();
        assertEquals(0, file.length());
        String str = prepareString(100);
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
        byteBuffer.put(bytes);
        Integer wrote = bufferedFile.write(100, bytes.length, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(file.length(), wrote + 100);
        assertEquals(bytes.length, wrote);
    }

    @Test
    void write_lengthZero() throws Exception {
        BufferedFile bufferedFile = BufferedFile.create(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
        File file = new File(bufferedFile.getPath());
        file.deleteOnExit();
        assertEquals(0, file.length());
        String str = prepareString(100);
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
        byteBuffer.put(bytes);
        Integer wrote = bufferedFile.write(0, 0, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(0, wrote);
        assertEquals(0, file.length());
    }

    @Test
    void close() throws Exception {
        File tempFile = File.createTempFile("temp-", "-file");
        tempFile.deleteOnExit();
        BufferedFile bufferedFile = BufferedFile.open(tempFile.getPath()).get(1000, TimeUnit.MILLISECONDS);
        assertTrue(bufferedFile.getRawFd() > 0);
        bufferedFile.close().get(1000, TimeUnit.MILLISECONDS);
        CompletableFuture<Integer> read = bufferedFile.read(0, ByteBuffer.allocateDirect(10));
        assertThrows(ExecutionException.class, () -> read.get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void size() throws Exception {
        BufferedFile bufferedFile = BufferedFile.create(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
        File file = new File(bufferedFile.getPath());
        file.deleteOnExit();
        String resultString = prepareString(100);
        FileWriter fw = new FileWriter(file);
        fw.write(resultString);
        fw.flush();
        fw.close();
        int expectedLength = resultString.getBytes(StandardCharsets.UTF_8).length;
        long actualLength = bufferedFile.size().get(1000, TimeUnit.MILLISECONDS);
        assertEquals(expectedLength, actualLength);
    }

    @Test
    void size_zero() throws Exception {
        BufferedFile f = BufferedFile.create(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
        deleteOnExit(f);
        assertEquals(0, f.size().get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void dataSync() throws Exception {
        BufferedFile f = BufferedFile.create(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
        deleteOnExit(f);
        assertEquals(0, f.dataSync().get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void dataSync_closedFile() throws Exception {
        BufferedFile f = BufferedFile.create(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
        deleteOnExit(f);
        f.close().get(1000, TimeUnit.MILLISECONDS);
        CompletableFuture<Integer> dataSync = f.dataSync();
        assertThrows(ExecutionException.class, () -> dataSync.get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void preAllocate_emptyFile() throws Exception {
        BufferedFile f = BufferedFile.create(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
        deleteOnExit(f);
        assertEquals(0, f.size().get(1000, TimeUnit.MILLISECONDS));
        assertEquals(0, f.preAllocate(1024).get(1000, TimeUnit.MILLISECONDS));
        assertEquals(1024, f.size().get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void preAllocate_notEmptyFile() throws Exception {
        BufferedFile bufferedFile = BufferedFile.create(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
        File file = new File(bufferedFile.getPath());
        file.deleteOnExit();
        String resultString = prepareString(100);
        long stringLength = resultString.getBytes(StandardCharsets.UTF_8).length;
        FileWriter fw = new FileWriter(file);
        fw.write(resultString);
        fw.flush();
        fw.close();
        assertEquals(stringLength, bufferedFile.size().get(1000, TimeUnit.MILLISECONDS));
        assertEquals(0, bufferedFile.preAllocate(stringLength * 2).get(1000, TimeUnit.MILLISECONDS));
        assertEquals(stringLength * 2, bufferedFile.size().get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void preAllocate_withOffset() throws Exception {
        BufferedFile bufferedFile = BufferedFile.create(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
        File file = new File(bufferedFile.getPath());
        file.deleteOnExit();
        String resultString = prepareString(100);
        long stringLength = resultString.getBytes(StandardCharsets.UTF_8).length;
        FileWriter fw = new FileWriter(file);
        fw.write(resultString);
        fw.flush();
        fw.close();
        assertEquals(stringLength, bufferedFile.size().get(1000, TimeUnit.MILLISECONDS));
        assertEquals(0, bufferedFile.preAllocate(stringLength, stringLength).get(1000, TimeUnit.MILLISECONDS));
        assertEquals(stringLength * 2, bufferedFile.size().get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void preAllocate_closedFile() throws Exception {
        BufferedFile bufferedFile = BufferedFile.create(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
        deleteOnExit(bufferedFile);
        bufferedFile.close().get(1000, TimeUnit.MILLISECONDS);
        CompletableFuture<Integer> preAllocate = bufferedFile.preAllocate(1024);
        assertThrows(ExecutionException.class, () -> preAllocate.get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void remove() throws Exception {
        BufferedFile bufferedFile = BufferedFile.create(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
        File file = new File(bufferedFile.getPath());
        assertTrue(file.exists());
        assertEquals(0, bufferedFile.remove().get(1000, TimeUnit.MILLISECONDS));
        assertFalse(file.exists());
    }

    @Test
    void remove_removed() throws Exception {
        BufferedFile bufferedFile = BufferedFile.create(TEMP_FILE_NAME).get(1000, TimeUnit.MILLISECONDS);
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
        BufferedFile bufferedFile = BufferedFile.open(tempFile.getPath()).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(0, bufferedFile.remove().get(1000, TimeUnit.MILLISECONDS));
        assertFalse(tempFile.exists());
    }

    @Test
    void remove_closed() throws Exception {
        File tempFile = File.createTempFile("temp-", "-file");
        assertTrue(tempFile.exists());
        BufferedFile bufferedFile = BufferedFile.open(tempFile.getPath()).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(0, bufferedFile.close().get(1000, TimeUnit.MILLISECONDS));
        assertEquals(0, bufferedFile.remove().get(1000, TimeUnit.MILLISECONDS));
        assertFalse(tempFile.exists());
    }

    private void deleteOnExit(BufferedFile f) {
        new File(f.getPath()).deleteOnExit();
    }
}
