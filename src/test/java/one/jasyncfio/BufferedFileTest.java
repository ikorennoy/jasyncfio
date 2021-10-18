package one.jasyncfio;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class BufferedFileTest {
    private final String TEMP_FILE_NAME = "/tmp/" + UUID.randomUUID();

    @Test
    void open() throws Exception {
        File file = File.createTempFile("temp-", "-file");
        file.deleteOnExit();
        BufferedFile bufferedFile = waitCompletionAndGet(BufferedFile.open(file.getPath()));
        assertTrue(bufferedFile.getRawFd() > 0);
    }

    @Test
    void open_fileNotExist() throws InterruptedException {
        CompletableFuture<BufferedFile> open = BufferedFile.open("some/path");
        waitCompletion(open);
        assertTrue(open.isCompletedExceptionally());
        assertThrows(ExecutionException.class, open::get);
    }

    @Test
    void open_insufficientPrivileges() throws InterruptedException {
        CompletableFuture<BufferedFile> open = BufferedFile.open("/proc/1/auxv");
        waitCompletion(open);
        assertTrue(open.isCompletedExceptionally());
        assertThrows(ExecutionException.class, open::get);
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
        BufferedFile bufferedFile = waitCompletionAndGet(BufferedFile.create(file.getPath()));
        // file was actually open
        assertTrue(bufferedFile.getRawFd() > 0);
        // file was truncated
        assertEquals(0, file.length());
    }

    @Test
    void create_fileDoesNotExist() throws Exception {
        BufferedFile bufferedFile = waitCompletionAndGet(BufferedFile.create(TEMP_FILE_NAME));
        assertTrue(bufferedFile.getRawFd() > 0);
        File file = new File(bufferedFile.getPath());
        assertTrue(file.exists());
        file.deleteOnExit();
    }

    @Test
    void create_insufficientPrivileges() throws InterruptedException {
        CompletableFuture<BufferedFile> create = BufferedFile.create("/root/temp-jasyncfio-file");
        waitCompletion(create);
        assertTrue(create.isCompletedExceptionally());
    }

    @Test
    void read() throws Exception {
        File tempFile = File.createTempFile("temp-", "-file");
        tempFile.deleteOnExit();
        FileWriter fw = new FileWriter(tempFile);
        StringBuilder sb = new StringBuilder();
        String s = "String number ";
        for (int i = 0; i < 100; i++) {
            sb.append(s).append(i);
        }
        String resultString = sb.toString();
        fw.write(resultString);
        fw.flush();
        fw.close();
        assertTrue(tempFile.length() > 0);
        ByteBuffer bb = ByteBuffer.allocateDirect((int) tempFile.length());
        BufferedFile bufferedFile = waitCompletionAndGet(BufferedFile.open(tempFile.getPath()));
        Integer bytes = waitCompletionAndGet(bufferedFile.read(0, bb));
        assertEquals((int) tempFile.length(), bytes);
        assertEquals(resultString, StandardCharsets.UTF_8.decode(bb).toString());
    }

    @Test
    void read_wrongBuffer() throws Exception {
        File tempFile = File.createTempFile("temp-", "-file");
        tempFile.deleteOnExit();
        BufferedFile bufferedFile = waitCompletionAndGet(BufferedFile.open(tempFile.getPath()));
        assertThrows(IllegalArgumentException.class, () -> bufferedFile.read(0, ByteBuffer.allocate(10)));
    }

    @Test
    void read_bufferGreaterThanFile() throws Exception {
        File tempFile = File.createTempFile("temp-", "-file");
        tempFile.deleteOnExit();
        FileWriter fw = new FileWriter(tempFile);
        StringBuilder sb = new StringBuilder();
        String s = "String number ";
        for (int i = 0; i < 100; i++) {
            sb.append(s).append(i);
        }
        String resultString = sb.toString();
        fw.write(resultString);
        fw.flush();
        fw.close();
        int stringLength = resultString.getBytes(StandardCharsets.UTF_8).length;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(stringLength * 2);
        BufferedFile bufferedFile = waitCompletionAndGet(BufferedFile.open(tempFile.getPath()));
        Integer bytes = waitCompletionAndGet(bufferedFile.read(0, byteBuffer));
        assertEquals(stringLength, bytes);
    }

    @Test
    void read_bufferLessThanFile() throws Exception {
        File tempFile = File.createTempFile("temp-", "-file");
        tempFile.deleteOnExit();
        FileWriter fw = new FileWriter(tempFile);
        StringBuilder sb = new StringBuilder();
        String s = "String number ";
        for (int i = 0; i < 100; i++) {
            sb.append(s).append(i);
        }
        String resultString = sb.toString();
        fw.write(resultString);
        fw.flush();
        fw.close();
        int stringLength = resultString.getBytes(StandardCharsets.UTF_8).length;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(stringLength / 2);
        BufferedFile bufferedFile = waitCompletionAndGet(BufferedFile.open(tempFile.getPath()));
        Integer bytes = waitCompletionAndGet(bufferedFile.read(0, byteBuffer));
        assertEquals(stringLength / 2, bytes);
    }

    @Test
    void read_offsetGreaterThanFileSize() throws Exception {
        File tempFile = File.createTempFile("temp-", "-file");
        tempFile.deleteOnExit();
        FileWriter fw = new FileWriter(tempFile);
        StringBuilder sb = new StringBuilder();
        String s = "String number ";
        for (int i = 0; i < 100; i++) {
            sb.append(s).append(i);
        }
        String resultString = sb.toString();
        fw.write(resultString);
        fw.flush();
        fw.close();
        int stringLength = resultString.getBytes(StandardCharsets.UTF_8).length;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(stringLength);
        BufferedFile bufferedFile = waitCompletionAndGet(BufferedFile.open(tempFile.getPath()));
        Integer bytes = waitCompletionAndGet(bufferedFile.read(stringLength * 2, byteBuffer));
        assertEquals(0, bytes);
    }

    // read len 0

    @Test
    void write_emptyFile() throws Exception {
        System.out.println(TEMP_FILE_NAME);
        File tempFile = File.createTempFile("temp-", "-file");
        tempFile.deleteOnExit();
        BufferedFile bufferedFile = waitCompletionAndGet(BufferedFile.create(tempFile.getPath()));
        assertEquals(0, tempFile.length());
        String str = prepareString(100);
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
        byteBuffer.put(bytes);
        Integer wrote = waitCompletionAndGet(bufferedFile.write(0, bytes.length, byteBuffer));
        assertEquals((int) tempFile.length(), wrote);
        assertEquals(str, readFileToString(tempFile));
    }

    @Test
    void write_positionGreaterThanFileSize() throws Exception {
        System.out.println(TEMP_FILE_NAME);
        BufferedFile bufferedFile = waitCompletionAndGet(BufferedFile.create(TEMP_FILE_NAME));
        File file = new File(bufferedFile.getPath());
        file.deleteOnExit();
        assertEquals(0, file.length());
        String str = prepareString(100);
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
        byteBuffer.put(bytes);
        Integer wrote = waitCompletionAndGet(bufferedFile.write(100, bytes.length, byteBuffer));
        assertEquals(file.length(), wrote + 100);
        assertEquals(bytes.length, wrote);
    }

    @Test
    void write_lengthZero() throws Exception {
        System.out.println(TEMP_FILE_NAME);
        BufferedFile bufferedFile = waitCompletionAndGet(BufferedFile.create(TEMP_FILE_NAME));
        File file = new File(bufferedFile.getPath());
        file.deleteOnExit();
        assertEquals(0, file.length());
        String str = prepareString(100);
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
        byteBuffer.put(bytes);
        Integer wrote = waitCompletionAndGet(bufferedFile.write(0, 0, byteBuffer));
        assertEquals(0, wrote);
        assertEquals(0, file.length());
    }

    @Test
    void close() throws Exception {
        File tempFile = File.createTempFile("temp-", "-file");
        tempFile.deleteOnExit();
        BufferedFile bufferedFile = waitCompletionAndGet(BufferedFile.open(tempFile.getPath()));
        assertTrue(bufferedFile.getRawFd() > 0);
        waitCompletion(bufferedFile.close());
        CompletableFuture<Integer> read = bufferedFile.read(0, ByteBuffer.allocateDirect(10));
        waitCompletion(read);
        assertTrue(read.isCompletedExceptionally());
    }

    private void waitCompletion(CompletableFuture<?> future) throws InterruptedException {
        int cnt = 0;
        while (!future.isDone()) {
            if (cnt > 1000) {
                break;
            }
            Thread.sleep(1);
            cnt++;
        }
        assertTrue(future.isDone());
    }

    private <T> T waitCompletionAndGet(CompletableFuture<T> future) throws Exception {
        waitCompletion(future);
        return future.get();
    }

    private String readFileToString(File f) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(f));
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();
        while (line != null) {
            sb.append(line).append("\n");
            line = br.readLine();
        }
        return sb.toString();
    }

    private String prepareString(int iters) {
        StringBuilder sb = new StringBuilder();
        String s = "String number ";
        for (int i = 0; i < iters; i++) {
            sb.append(s).append(i).append("\n");
        }
        return sb.toString();
    }
}
