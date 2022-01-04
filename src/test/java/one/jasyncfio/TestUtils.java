package one.jasyncfio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestUtils {

    public static String prepareString(int iters) {
        StringBuilder sb = new StringBuilder();
        String s = "String number ";
        for (int i = 0; i < iters; i++) {
            sb.append(s).append(i).append("\n");
        }
        return sb.toString();
    }

    public static String readFileToString(File f) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(f));
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();
        while (line != null) {
            sb.append(line).append("\n");
            line = br.readLine();
        }
        return sb.toString();
    }

    public static void waitCompletion(CompletableFuture<?> future) throws InterruptedException {
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

    public static <T> T waitCompletionAndGet(CompletableFuture<T> future) throws Exception {
        waitCompletion(future);
        return future.get();
    }
}
