package one.jasyncfio;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class BufferedFileTest {

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
        BufferedFile bufferedFile = waitCompletionAndGet(BufferedFile.create("/tmp/temp-jasyncfio-file"));;
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
    void read_bufferLargerThanFile() throws Exception {
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
    void read_offsetLargerThanFileSize() throws Exception {
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

    private void waitCompletion(CompletableFuture<?> future) throws InterruptedException {
        int cnt = 0;
        while (!future.isDone()) {
            if (cnt > 5) {
                break;
            }
            Thread.sleep(10);
            cnt++;
        }
        assertTrue(future.isDone());
    }

    private <T> T waitCompletionAndGet(CompletableFuture<T> future) throws Exception {
        waitCompletion(future);
        return future.get();
    }
}
