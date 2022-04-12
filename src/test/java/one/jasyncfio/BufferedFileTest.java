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
        assertTrue(bufferedFile.getRawFd() > 0);
        File file = new File(bufferedFile.getPath());
        assertTrue(file.exists());
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
        Path tempFile = Files.createTempFile(tmpDir, "temp-", "-file");
        String resultString = prepareString(100);
        writeStringToFile(resultString, tempFile);
        int stringLength = resultString.getBytes(StandardCharsets.UTF_8).length;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(stringLength * 2);
        BufferedFile bufferedFile = eventExecutorGroup.openBufferedFile(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        Integer bytes = bufferedFile.read(0, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(stringLength, bytes);
    }

    @Test
    void read_bufferLessThanFile() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "temp-", "-file");
        String resultString = prepareString(100);
        writeStringToFile(resultString, tempFile);
        int stringLength = resultString.getBytes(StandardCharsets.UTF_8).length;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(stringLength / 2);
        BufferedFile bufferedFile = eventExecutorGroup.openBufferedFile(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        Integer bytes = bufferedFile.read(0, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(stringLength / 2, bytes);
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
    void write() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.WRITE_ONLY, OpenOption.CREATE)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.write(bufferedFile);
    }

    @Test
    void write_trackPosition() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "temp-", "-file");
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(tempFile.toString(), OpenOption.WRITE_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        assertEquals(0, Files.size(tempFile));
        String str = prepareString(100);
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
        byteBuffer.put(bytes);
        Integer written = bufferedFile.write(-1, bytes.length, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals((int) Files.size(tempFile), written);
        assertEquals(str, new String(Files.readAllBytes(tempFile)));

        bufferedFile.write(-1, bytes.length, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals((int) Files.size(tempFile), written * 2);
        assertEquals(str + str, new String(Files.readAllBytes(tempFile)));
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
    void write_lengthZero() throws Exception {
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(getTempFile(tmpDir), OpenOption.CREATE, OpenOption.WRITE_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        File file = new File(bufferedFile.getPath());
        assertEquals(0, file.length());
        String str = prepareString(100);
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
        byteBuffer.put(bytes);
        Integer written = bufferedFile.write(0, 0, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(0, written);
        assertEquals(0, file.length());
    }

    @Test
    void close() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "temp-", "-file");
        BufferedFile bufferedFile = eventExecutorGroup.openBufferedFile(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
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
        Path tempFile = Files.createTempFile(tmpDir, "temp-", "-file");
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(tempFile.toString(), OpenOption.WRITE_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        assertEquals(0, Files.size(tempFile));
        ByteBuffer[] buffers = new ByteBuffer[10];
        StringBuilder strings = new StringBuilder();
        String str = prepareString(100);
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < buffers.length; i++) {
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
            byteBuffer.put(bytes);
            byteBuffer.flip();
            buffers[i] = byteBuffer;
            strings.append(str);
        }
        Integer written = bufferedFile.write(0, buffers).get(1000, TimeUnit.MILLISECONDS);
        assertEquals((int) Files.size(tempFile), written);
        assertEquals(strings.toString(), new String(Files.readAllBytes(tempFile)));
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
        ByteBuffer[] buffers = new ByteBuffer[1];
        String str = prepareString(100);
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < buffers.length; i++) {
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
            byteBuffer.put(bytes);
            byteBuffer.flip();
            buffers[i] = byteBuffer;
        }
        IovecArray iovecArray = eventExecutorGroup.registerBuffers(buffers).get(1000, TimeUnit.MILLISECONDS);

        Path tempFile = Files.createTempFile(tmpDir, "temp-", "-file");
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(tempFile.toString(), OpenOption.WRITE_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);

        Integer written = bufferedFile.writeFixed(0, 0, iovecArray).get();

        assertEquals((int) Files.size(tempFile), written);
        assertEquals(str, new String(Files.readAllBytes(tempFile)));
    }

    @Test
    void readFixed() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "temp-", "-file");
        String resultString = prepareString(100);
        writeStringToFile(resultString, tempFile);
        int length = resultString.getBytes(StandardCharsets.UTF_8).length;

        ByteBuffer[] buffers = new ByteBuffer[1];
        for (int i = 0; i < buffers.length; i++) {
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(length);
            buffers[i] = byteBuffer;
        }

        IovecArray iovecArray = eventExecutorGroup.registerBuffers(buffers).get(1000, TimeUnit.MILLISECONDS);

        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(tempFile.toString())
                .get(1000, TimeUnit.MILLISECONDS);

        Integer read = bufferedFile.readFixed(0, 0, iovecArray).get();

        assertEquals(length, read);

        assertEquals(resultString, StandardCharsets.UTF_8.decode(buffers[0]).toString());
    }

    @Test
    void closeRing() throws Exception {
        eventExecutorGroup.stop();
        Path tempFile = Files.createTempFile(tmpDir, "temp-", "-file");
        assertThrows(RejectedExecutionException.class, () -> eventExecutorGroup
                .openBufferedFile(tempFile.toString(), OpenOption.WRITE_ONLY));
    }

    private void deleteOnExit(BufferedFile f) {
        new File(f.getPath()).deleteOnExit();
    }
}
