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
        File file = new File(testFile.getPath());
        file.deleteOnExit();
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
}
