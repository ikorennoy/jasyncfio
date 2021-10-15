package one.jasyncfio;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class BufferedFileTest {

    @Test
    void open() throws ExecutionException, InterruptedException {
        CompletableFuture<BufferedFile> open = BufferedFile.open("./tmp/file1");
        BufferedFile bufferedFile = open.get();
        System.out.println(bufferedFile.getFd());
    }
}
