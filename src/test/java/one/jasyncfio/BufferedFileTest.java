package one.jasyncfio;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
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
        assertTrue(open.isDone());
        BufferedFile bufferedFile = open.get();
        assertTrue(bufferedFile.getFd() > 0);
    }

    @Test
    void open_fileNotExist() throws InterruptedException {
        CompletableFuture<BufferedFile> open = BufferedFile.open("some/path");
        waitCompletion(open);
        assertTrue(open.isDone());
        assertTrue(open.isCompletedExceptionally());
        assertThrows(ExecutionException.class, open::get);
    }



    private void waitCompletion(CompletableFuture<?> future) throws InterruptedException {
        int cnt = 0;
        while (!future.isDone()) {
            if (cnt > 5) {
                break;
            }
            Thread.sleep(100);
            cnt++;
        }
    }
}
