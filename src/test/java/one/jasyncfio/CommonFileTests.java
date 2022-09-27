package one.jasyncfio;

import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Time;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class CommonFileTests {

    static class Pair<A, B> {
        final A e1;
        final B e2;

        Pair(A e1, B e2) {
            this.e1 = e1;
            this.e2 = e2;
        }
    }

    static <T> void preAllocate_notEmptyFile(Pair<Path, AbstractFile> testFilePair) throws Exception {
        Path tempFile = testFilePair.e1;
        AbstractFile abstractFile = testFilePair.e2;
        assertEquals(0, abstractFile.size().get(1000, TimeUnit.MILLISECONDS));
        initTestFile(tempFile, 512);
        assertEquals(0, abstractFile.preAllocate(512, 512).get(1000, TimeUnit.MILLISECONDS));
        assertEquals(1024, abstractFile.size().get(1000, TimeUnit.MILLISECONDS));
        Files.deleteIfExists(tempFile);
    }

    static <T> void size_smallFile(Pair<Path, AbstractFile> testFilePair) throws Exception {
        Path tempFile = testFilePair.e1;
        AbstractFile abstractFile = testFilePair.e2;
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            int testSize = random.nextInt(1000);
            initTestFile(tempFile, testSize);
            assertEquals(testSize, abstractFile.size().get(1000, TimeUnit.MILLISECONDS));
        }
        Files.deleteIfExists(tempFile);
    }

    static <T> void size_largeFile(Pair<Path, AbstractFile> testFilePair) throws Exception {
        Path tempFile = testFilePair.e1;
        AbstractFile abstractFile = testFilePair.e2;
        long testSize = Integer.MAX_VALUE * 2L;
        initTestFile(tempFile, 10);
        try (RandomAccessFile f = new RandomAccessFile(tempFile.toFile(), "rw")) {
            FileChannel channel = f.getChannel();
            channel.map(FileChannel.MapMode.READ_WRITE, testSize, 10);
            assertEquals(testSize + 10, abstractFile.size().get(1000, TimeUnit.MILLISECONDS));
        }
        Files.deleteIfExists(tempFile);
    }

    static <T> void close(Pair<Path, AbstractFile> testFilePair) throws Exception {
        AbstractFile abstractFile = testFilePair.e2;
        abstractFile.close().get(1000, TimeUnit.MILLISECONDS);
        assertThrows(ExecutionException.class,
                () -> abstractFile.read(ByteBuffer.allocateDirect(10)).get(1000, TimeUnit.MILLISECONDS));
    }

    static <T> void size_zero(Pair<Path, AbstractFile> testFile) throws Exception {
        assertEquals(0, testFile.e2.size().get(1000, TimeUnit.MILLISECONDS));
    }

    static <T> void dataSync(Pair<Path, AbstractFile> testFile) throws Exception {
        assertEquals(0, testFile.e2.dataSync().get(1000, TimeUnit.MILLISECONDS));
    }

    static <T> void preAllocate_emptyFile(Pair<Path, AbstractFile> testFile) throws Exception {
        assertEquals(0, testFile.e2.size().get(1000, TimeUnit.MILLISECONDS));
        assertEquals(0, testFile.e2.preAllocate(1024, 0).get(1000, TimeUnit.MILLISECONDS));
        assertEquals(1024, testFile.e2.size().get(1000, TimeUnit.MILLISECONDS));
    }

    static void remove(Pair<Path, AbstractFile> testFile) throws Exception {
        assertTrue(Files.exists(testFile.e1));
        assertEquals(0, testFile.e2.remove().get(1000, TimeUnit.MILLISECONDS));
        assertFalse(Files.exists(testFile.e1));
    }

    static void dataSync_closedFile(AbstractFile testFile) throws Exception {
        testFile.close().get(1000, TimeUnit.MILLISECONDS);
        CompletableFuture<Integer> dataSync = testFile.dataSync();
        assertThrows(ExecutionException.class, () -> dataSync.get(1000, TimeUnit.MILLISECONDS));
    }


    private static void initTestFile(Path tempFile, int testSize) throws IOException {
        byte[] bytes = new byte[testSize];
        for (int i = 0; i < testSize; i++) {
            bytes[i] = 'e';
        }
        Files.write(
                tempFile,
                bytes,
                StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
        );
    }
}
