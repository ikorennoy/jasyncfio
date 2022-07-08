package one.jasyncfio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static one.jasyncfio.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

public class BufferedFileTest {

    @TempDir
    Path tmpDir;
    private final EventExecutor eventExecutor = EventExecutor.initDefault();

    @Test
    void open() throws Exception {
        BufferedFile bufferedFile = BufferedFile.open(getTempFile(tmpDir), eventExecutor, OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        assertTrue(bufferedFile.getRawFd() > 0);
    }

    @Test
    void open_fileNotExist() {
        CompletableFuture<BufferedFile> open = BufferedFile.open("some/path", eventExecutor);
        assertThrows(ExecutionException.class, () -> open.get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void open_dir() {
        CompletableFuture<BufferedFile> bufferedFile = BufferedFile.open("/tmp", eventExecutor, OpenOption.CREATE);
        assertThrows(ExecutionException.class, () -> bufferedFile.get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void open_truncate() throws Exception {
        Path file = Files.createTempFile(tmpDir, "temp-", "-file");
        writeStringToFile("file is not empty", file);
        assertTrue(Files.size(file) > 0);
        BufferedFile bufferedFile = BufferedFile
                .open(file.toString(), eventExecutor, OpenOption.WRITE_ONLY, OpenOption.TRUNCATE)
                .get(1000, TimeUnit.MILLISECONDS);
        // file was actually open
        assertTrue(bufferedFile.getRawFd() > 0);
        // file was truncated
        assertEquals(0, Files.size(file));
    }

    @Test
    void open_newFile() throws Exception {
        BufferedFile bufferedFile = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.open_newFile(bufferedFile);
    }

    @Test
    void read() throws Exception {
        BufferedFile bufferedFile = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.CREATE, OpenOption.READ_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.read(bufferedFile);
    }

    @Test
    void read_wrongBuffer() throws Exception {
        BufferedFile bufferedFile = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.CREATE, OpenOption.READ_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        assertThrows(IllegalArgumentException.class, () -> bufferedFile.read(ByteBuffer.allocate(10), 0));
    }

    @Test
    void read_bufferGreaterThanFile() throws Exception {
        BufferedFile bufferedFile = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.READ_ONLY, OpenOption.CREATE)
                .get();
        CommonTests.read_bufferGreaterThanFile(bufferedFile);
    }

    @Test
    void read_bufferLessThanFile() throws Exception {
        BufferedFile bufferedFile = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.READ_ONLY, OpenOption.CREATE)
                .get();
        CommonTests.read_bufferLessThanFile(bufferedFile);
    }

    @Test
    void read_lengthGreaterThanBufferSize() throws Exception {
        BufferedFile bufferedFile = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.READ_ONLY, OpenOption.CREATE)
                .get();
        CommonTests.read_lengthGreaterThanBufferSize(bufferedFile);
    }

    @Test
    void read_positionGreaterThanFileSize() throws Exception {
        BufferedFile bufferedFile = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.READ_ONLY, OpenOption.CREATE)
                .get();
        CommonTests.read_positionGreaterThanFileSize(bufferedFile);
    }

    @Test
    void read_lengthLessThenBufferSize() throws Exception {
        BufferedFile bufferedFile = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.READ_ONLY, OpenOption.CREATE)
                .get();
        CommonTests.read_lengthLessThenBufferSize(bufferedFile);
    }

    @Test
    void write() throws Exception {
        BufferedFile bufferedFile = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.WRITE_ONLY, OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.write(bufferedFile);
    }

    @Test
    void write_trackPosition() throws Exception {
        BufferedFile bufferedFile = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.WRITE_ONLY, OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.write_trackPosition(bufferedFile);
    }

    @Test
    void write_positionGreaterThanFileSize() throws Exception {
        BufferedFile bufferedFile = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.CREATE, OpenOption.WRITE_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.write_positionGreaterThanFileSize(bufferedFile);
    }

    @Test
    void write_lengthGreaterThanBufferSize() throws Exception {
        BufferedFile dmaFile = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.CREATE, OpenOption.WRITE_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.write_lengthGreaterThanBufferSize(dmaFile);
    }

    @Test
    void write_lengthLessThenBufferSize() throws Exception {
        BufferedFile bufferedFile = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.WRITE_ONLY, OpenOption.CREATE)
                .get();
        CommonTests.write_lengthLessThenBufferSize(bufferedFile);
    }

    @Test
    void write_lengthZero() throws Exception {
        BufferedFile bufferedFile = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.CREATE, OpenOption.WRITE_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.write_lengthZero(bufferedFile);
    }

    @Test
    void close() throws Exception {
        BufferedFile bufferedFile = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.WRITE_ONLY, OpenOption.CREATE)
                .get();
        CommonTests.close(bufferedFile);
    }

    @Test
    void size() throws Exception {
        BufferedFile bufferedFile = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.size(bufferedFile);
    }

    @Test
    void size_zero() throws Exception {
        BufferedFile f = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.size_zero(f);
    }

    @Test
    void dataSync() throws Exception {
        BufferedFile f = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.dataSync(f);
    }

    @Test
    void dataSync_closedFile() throws Exception {
        BufferedFile f = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.dataSync_closedFile(f);
    }

    @Test
    void preAllocate_emptyFile() throws Exception {
        BufferedFile f = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.CREATE, OpenOption.WRITE_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.preAllocate_emptyFile(f);
    }

    @Test
    void preAllocate_notEmptyFile() throws Exception {
        BufferedFile bufferedFile = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.CREATE, OpenOption.WRITE_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        Path file = Paths.get(bufferedFile.getPath());
        String resultString = prepareString(100);
        long stringLength = resultString.getBytes(StandardCharsets.UTF_8).length;
        writeStringToFile(resultString, file);
        assertEquals(stringLength, bufferedFile.size().get(1000, TimeUnit.MILLISECONDS));
        assertEquals(0, bufferedFile.preAllocate(stringLength * 2, 0).get(1000, TimeUnit.MILLISECONDS));
        assertEquals(stringLength * 2, bufferedFile.size().get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void preAllocate_withOffset() throws Exception {
        BufferedFile bufferedFile = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.CREATE, OpenOption.WRITE_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        Path file = Paths.get(bufferedFile.getPath());
        String resultString = prepareString(100);
        long stringLength = resultString.getBytes(StandardCharsets.UTF_8).length;
        writeStringToFile(resultString, file);
        assertEquals(stringLength, bufferedFile.size().get(1000, TimeUnit.MILLISECONDS));
        assertEquals(0, bufferedFile.preAllocate(stringLength, stringLength).get(1000, TimeUnit.MILLISECONDS));
        assertEquals(stringLength * 2, bufferedFile.size().get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void preAllocate_closedFile() throws Exception {
        BufferedFile bufferedFile = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        bufferedFile.close().get(1000, TimeUnit.MILLISECONDS);
        CompletableFuture<Integer> preAllocate = bufferedFile.preAllocate(1024, 0);
        assertThrows(ExecutionException.class, () -> preAllocate.get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void remove() throws Exception {
        BufferedFile bufferedFile = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.remove(bufferedFile);
    }

    @Test
    void remove_removed() throws Exception {
        BufferedFile bufferedFile = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.remove_removed(bufferedFile);
    }

    @Test
    void remove_readOnly() throws Exception {
        BufferedFile bufferedFile = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.READ_ONLY, OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.remove_readOnly(bufferedFile);
    }

    @Test
    void remove_closed() throws Exception {
        BufferedFile bufferedFile = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.READ_ONLY, OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.remove_closed(bufferedFile);
    }

    @Test
    void writev() throws Exception {
        BufferedFile bufferedFile = BufferedFile
                .open(getTempFile(tmpDir), eventExecutor, OpenOption.CREATE, OpenOption.WRITE_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.writev(bufferedFile);
    }

    @Test
    void readv() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "temp-", "-file");
        String resultString = prepareString(100);
        writeStringToFile(resultString, tempFile);
        int length = resultString.getBytes(StandardCharsets.UTF_8).length;

        ByteBuffer[] buffers = new ByteBuffer[10];

        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = ByteBuffer.allocateDirect(length / 10);
        }

        assertTrue(Files.size(tempFile) > 0);
        BufferedFile bufferedFile = BufferedFile.open(tempFile.toString(), eventExecutor).get(1000, TimeUnit.MILLISECONDS);
        Integer bytes = bufferedFile.read(buffers, 0, length).get(1000, TimeUnit.MILLISECONDS);

        StringBuilder strings = new StringBuilder();
        for (ByteBuffer bb : buffers) {
            strings.append(StandardCharsets.UTF_8.decode(bb));
        }
        assertEquals((int) Files.size(tempFile), bytes);
        assertEquals(resultString, strings.toString());
    }

//    @Test
//    void writeFixed() throws Exception {
//        BufferedFile bufferedFile = BufferedFile
//                .open(getTempFile(tmpDir), eventExecutor, OpenOption.CREATE, OpenOption.WRITE_ONLY)
//                .get(1000, TimeUnit.MILLISECONDS);
//        CommonTests.writeFixed(bufferedFile);
//    }

//    @Test
//    void readFixed() throws Exception {
//        BufferedFile bufferedFile = BufferedFile
//                .openBufferedFile(getTempFile(tmpDir), OpenOption.READ_ONLY, OpenOption.CREATE)
//                .get(1000, TimeUnit.MILLISECONDS);
//        CommonTests.readFixed(bufferedFile, eventExecutor);
//    }
//
//    @Test
//    void closeRing() throws Exception {
//        eventExecutor.stop();
//        Path tempFile = Files.createTempFile(tmpDir, "temp-", "-file");
//        assertThrows(RejectedExecutionException.class, () -> eventExecutor
//                .openBufferedFile(tempFile.toString(), OpenOption.WRITE_ONLY));
//    }
}
