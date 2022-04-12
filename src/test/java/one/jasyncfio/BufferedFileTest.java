package one.jasyncfio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
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
    private final EventExecutorGroup eventExecutorGroup = EventExecutorGroup.initDefault();

    @Test
    void open() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        assertTrue(bufferedFile.getRawFd() > 0);
    }

    @Test
    void open_fileNotExist() {
        CompletableFuture<BufferedFile> open = eventExecutorGroup.openBufferedFile("some/path");
        assertThrows(ExecutionException.class, () -> open.get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void open_dir() {
        CompletableFuture<BufferedFile> bufferedFile = eventExecutorGroup.openBufferedFile("/tmp", OpenOption.CREATE);
        assertThrows(ExecutionException.class, () -> bufferedFile.get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void open_truncate() throws Exception {
        Path file = Files.createTempFile(tmpDir, "temp-", "-file");
        writeStringToFile("file is not empty", file);
        assertTrue(Files.size(file) > 0);
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(file.toString(), OpenOption.WRITE_ONLY, OpenOption.TRUNCATE)
                .get(1000, TimeUnit.MILLISECONDS);
        // file was actually open
        assertTrue(bufferedFile.getRawFd() > 0);
        // file was truncated
        assertEquals(0, Files.size(file));
    }

    @Test
    void open_newFile() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.open_newFile(bufferedFile);
    }

    @Test
    void read() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.CREATE, OpenOption.READ_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.read(bufferedFile);
    }

    @Test
    void read_wrongBuffer() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.CREATE, OpenOption.READ_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        assertThrows(IllegalArgumentException.class, () -> bufferedFile.read(0, ByteBuffer.allocate(10)));
    }

    @Test
    void read_bufferGreaterThanFile() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.READ_ONLY, OpenOption.CREATE)
                .get();
        CommonTests.read_bufferGreaterThanFile(bufferedFile);
    }

    @Test
    void read_bufferLessThanFile() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.READ_ONLY, OpenOption.CREATE)
                .get();
        CommonTests.read_bufferLessThanFile(bufferedFile);
    }

    @Test
    void read_lengthGreaterThanBufferSize() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.READ_ONLY, OpenOption.CREATE)
                .get();
        CommonTests.read_lengthGreaterThanBufferSize(bufferedFile);
    }

    @Test
    void read_positionGreaterThanFileSize() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.READ_ONLY, OpenOption.CREATE)
                .get();
        CommonTests.read_positionGreaterThanFileSize(bufferedFile);
    }

    @Test
    void read_lengthLessThenBufferSize() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.READ_ONLY, OpenOption.CREATE)
                .get();
        CommonTests.read_lengthLessThenBufferSize(bufferedFile);
    }

    @Test
    void write() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.WRITE_ONLY, OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.write(bufferedFile);
    }

    @Test
    void write_trackPosition() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.WRITE_ONLY, OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.write_trackPosition(bufferedFile);
    }

    @Test
    void write_positionGreaterThanFileSize() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.CREATE, OpenOption.WRITE_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.write_positionGreaterThanFileSize(bufferedFile);
    }

    @Test
    void write_lengthGreaterThanBufferSize() throws Exception {
        BufferedFile dmaFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.CREATE, OpenOption.WRITE_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.write_lengthGreaterThanBufferSize(dmaFile);
    }

    @Test
    void write_lengthLessThenBufferSize() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.WRITE_ONLY, OpenOption.CREATE)
                .get();
        CommonTests.write_lengthLessThenBufferSize(bufferedFile);
    }

    @Test
    void write_lengthZero() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.CREATE, OpenOption.WRITE_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.write_lengthZero(bufferedFile);
    }

    @Test
    void close() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.WRITE_ONLY, OpenOption.CREATE)
                .get();
        CommonTests.close(bufferedFile);
    }

    @Test
    void size() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.size(bufferedFile);
    }

    @Test
    void size_zero() throws Exception {
        BufferedFile f = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.size_zero(f);
    }

    @Test
    void dataSync() throws Exception {
        BufferedFile f = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.dataSync(f);
    }

    @Test
    void dataSync_closedFile() throws Exception {
        BufferedFile f = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.dataSync_closedFile(f);
    }

    @Test
    void preAllocate_emptyFile() throws Exception {
        BufferedFile f = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.CREATE, OpenOption.WRITE_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.preAllocate_emptyFile(f);
    }

    @Test
    void preAllocate_notEmptyFile() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.CREATE, OpenOption.WRITE_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        Path file = Paths.get(bufferedFile.getPath());
        String resultString = prepareString(100);
        long stringLength = resultString.getBytes(StandardCharsets.UTF_8).length;
        writeStringToFile(resultString, file);
        assertEquals(stringLength, bufferedFile.size().get(1000, TimeUnit.MILLISECONDS));
        assertEquals(0, bufferedFile.preAllocate(stringLength * 2).get(1000, TimeUnit.MILLISECONDS));
        assertEquals(stringLength * 2, bufferedFile.size().get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void preAllocate_withOffset() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.CREATE, OpenOption.WRITE_ONLY)
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
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        bufferedFile.close().get(1000, TimeUnit.MILLISECONDS);
        CompletableFuture<Integer> preAllocate = bufferedFile.preAllocate(1024);
        assertThrows(ExecutionException.class, () -> preAllocate.get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void remove() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.remove(bufferedFile);
    }

    @Test
    void remove_removed() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.remove_removed(bufferedFile);
    }

    @Test
    void remove_readOnly() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.READ_ONLY, OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.remove_readOnly(bufferedFile);
    }

    @Test
    void remove_closed() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.READ_ONLY, OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.remove_closed(bufferedFile);
    }

    @Test
    void writev() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.CREATE, OpenOption.WRITE_ONLY)
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
        BufferedFile bufferedFile = eventExecutorGroup.openBufferedFile(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        Integer bytes = bufferedFile.read(0, buffers).get(1000, TimeUnit.MILLISECONDS);

        StringBuilder strings = new StringBuilder();
        for (ByteBuffer bb : buffers) {
            strings.append(StandardCharsets.UTF_8.decode(bb));
        }
        assertEquals((int) Files.size(tempFile), bytes);
        assertEquals(resultString, strings.toString());
    }

    @Test
    void writeFixed() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.CREATE, OpenOption.WRITE_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.writeFixed(bufferedFile, eventExecutorGroup);
    }

    @Test
    void readFixed() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.READ_ONLY, OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.readFixed(bufferedFile, eventExecutorGroup);
    }

    @Test
    void closeRing() throws Exception {
        eventExecutorGroup.stop();
        Path tempFile = Files.createTempFile(tmpDir, "temp-", "-file");
        assertThrows(RejectedExecutionException.class, () -> eventExecutorGroup
                .openBufferedFile(tempFile.toString(), OpenOption.WRITE_ONLY));
    }
}
