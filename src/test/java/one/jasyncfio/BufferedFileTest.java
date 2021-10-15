package one.jasyncfio;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class BufferedFileTest {

    @Test
    void open() throws ExecutionException, InterruptedException, IOException {
        File file = File.createTempFile("temp-", "-file");
        file.deleteOnExit();
        CompletableFuture<BufferedFile> open = BufferedFile.open(file.getPath());
        waitCompletion(open);
        BufferedFile bufferedFile = open.get();
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
    void open_insufficientPrivileges() throws InterruptedException, ExecutionException {
        CompletableFuture<BufferedFile> open = BufferedFile.open("/proc/1/auxv");
        waitCompletion(open);
        assertTrue(open.isCompletedExceptionally());
        assertThrows(ExecutionException.class, open::get);
    }

    @Test
    void create_fileExist() throws IOException, InterruptedException, ExecutionException {
        File file = File.createTempFile("temp-", "-file");
        file.deleteOnExit();
        assertTrue(file.exists());
        FileWriter writer = new FileWriter(file);
        writer.write("file is not empty");
        writer.flush();
        writer.close();
        assertTrue(file.length() > 0);
        CompletableFuture<BufferedFile> create = BufferedFile.create(file.getPath());
        waitCompletion(create);
        BufferedFile bufferedFile = create.get();
        // file was actually open
        assertTrue(bufferedFile.getRawFd() > 0);
        // file was truncated
        assertEquals(0, file.length());
    }

    @Test
    void create_fileDoesNotExist() throws InterruptedException, ExecutionException {
        CompletableFuture<BufferedFile> create = BufferedFile.create("/tmp/temp-jasyncfio-file");
        waitCompletion(create);
        BufferedFile bufferedFile = create.get();
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
    void read() throws InterruptedException, IOException, ExecutionException {
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
        CompletableFuture<BufferedFile> open = BufferedFile.open(tempFile.getPath());
        waitCompletion(open);
        BufferedFile bufferedFile = open.get();
        CompletableFuture<Integer> read = bufferedFile.read(0, bb);
        waitCompletion(read);
        assertEquals((int) tempFile.length(), read.get());
        assertEquals(resultString, StandardCharsets.UTF_8.decode(bb).toString());
    }

    @Test
    void read_wrongBuffer() throws InterruptedException, ExecutionException, IOException {
        File tempFile = File.createTempFile("temp-", "-file");
        tempFile.deleteOnExit();
        CompletableFuture<BufferedFile> open = BufferedFile.open(tempFile.getPath());
        waitCompletion(open);
        BufferedFile bufferedFile = open.get();
        assertThrows(IllegalArgumentException.class, () -> bufferedFile.read(0, ByteBuffer.allocate(10)));
    }


    // read buffer > file size
    // read file > buffer size
    // read offset > file size


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
}
