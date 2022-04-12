package one.jasyncfio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static one.jasyncfio.TestUtils.getTempFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DmaFileTest {

    @TempDir
    Path tmpDir;
    private final EventExecutorGroup eventExecutorGroup = EventExecutorGroup.initDefault();

    @Test
    void readAligned_lengthNotAligned() throws Exception {
        DmaFile dmaFile = eventExecutorGroup
                .openDmaFile(getTempFile(tmpDir), OpenOption.CREATE, OpenOption.READ_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        Path tempFile = Paths.get(dmaFile.getPath());
        String expected = TestUtils.prepareString(100);
        int resultStringLength = expected.getBytes().length;
        TestUtils.writeStringToFile(expected, tempFile);
        ByteBuffer byteBuffer = dmaFile.readAligned(0, resultStringLength).get();
        assertEquals(resultStringLength, byteBuffer.limit());
        String actual = StandardCharsets.UTF_8.decode(byteBuffer).toString();
        assertEquals(expected, actual);
    }

    @Test
    void readAligned_positionNotAligned() throws Exception {
        DmaFile dmaFile = eventExecutorGroup
                .openDmaFile(getTempFile(tmpDir), OpenOption.CREATE, OpenOption.READ_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        Path tempFile = Paths.get(dmaFile.getPath());
        String expected = TestUtils.prepareString(100);
        int resultStringLength = expected.getBytes().length;
        TestUtils.writeStringToFile(expected, tempFile);
        ByteBuffer byteBuffer = dmaFile.readAligned(100, resultStringLength).get();
        assertEquals(resultStringLength, byteBuffer.limit());
        String actual = StandardCharsets.UTF_8.decode(byteBuffer).toString();
        assertEquals(expected.substring(100, resultStringLength), actual);
    }

    @Test
    void readAligned() throws Exception {
        DmaFile dmaFile = eventExecutorGroup
                .openDmaFile(getTempFile(tmpDir), OpenOption.READ_ONLY, OpenOption.CREATE)
                .get();
        Path tempFile = Paths.get(dmaFile.getPath());
        String expected = TestUtils.prepareString(100);
        int resultStringLength = 1024;
        TestUtils.writeStringToFile(expected, tempFile);
        ByteBuffer byteBuffer = dmaFile.readAligned(0, resultStringLength).get();
        assertEquals(resultStringLength, byteBuffer.limit());
        String actual = StandardCharsets.UTF_8.decode(byteBuffer).toString();
        assertEquals(expected.substring(0, resultStringLength), actual);
    }

    @Test
    void read_lengthNotAligned() throws Exception {
        DmaFile dmaFile = eventExecutorGroup
                .openDmaFile(getTempFile(tmpDir), OpenOption.CREATE, OpenOption.READ_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        Path tempFile = Paths.get(dmaFile.getPath());
        String expected = TestUtils.prepareString(10);
        TestUtils.writeStringToFile(expected, tempFile);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(128, DmaFile.DEFAULT_ALIGNMENT);
        assertThrows(ExecutionException.class, () -> dmaFile.read(0, 128, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void read_positionNotAligned() throws Exception {
        DmaFile dmaFile = eventExecutorGroup
                .openDmaFile(getTempFile(tmpDir), OpenOption.CREATE, OpenOption.READ_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        Path tempFile = Paths.get(dmaFile.getPath());
        String expected = TestUtils.prepareString(10);
        TestUtils.writeStringToFile(expected, tempFile);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(1024, DmaFile.DEFAULT_ALIGNMENT);
        assertThrows(ExecutionException.class, () -> dmaFile.read(1, 512, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void read_bufferNotAligned() throws Exception {
        DmaFile dmaFile = eventExecutorGroup
                .openDmaFile(getTempFile(tmpDir), OpenOption.CREATE, OpenOption.READ_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        Path tempFile = Paths.get(dmaFile.getPath());
        String expected = TestUtils.prepareString(10);
        TestUtils.writeStringToFile(expected, tempFile);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(512);
        assertThrows(ExecutionException.class, () -> dmaFile.read(0, 512, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void read_notAligned() throws Exception {
        DmaFile dmaFile = eventExecutorGroup
                .openDmaFile(getTempFile(tmpDir), OpenOption.CREATE, OpenOption.READ_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        Path tempFile = Paths.get(dmaFile.getPath());
        String expected = TestUtils.prepareString(10);
        TestUtils.writeStringToFile(expected, tempFile);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(128);
        assertThrows(ExecutionException.class, () -> dmaFile.read(10, 100, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void read() throws Exception {
        DmaFile dmaFile = eventExecutorGroup
                .openDmaFile(getTempFile(tmpDir), OpenOption.CREATE, OpenOption.READ_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.read(dmaFile);
    }

    @Test
    void read_lengthGreaterThanBufferSize() throws Exception {
        DmaFile dmaFile = eventExecutorGroup
                .openDmaFile(getTempFile(tmpDir), OpenOption.READ_ONLY, OpenOption.CREATE)
                .get();
        Path tempFile = Paths.get(dmaFile.getPath());
        String expected = TestUtils.prepareString(100);
        int readLength = 2048;
        TestUtils.writeStringToFile(expected, tempFile);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(1024, DmaFile.DEFAULT_ALIGNMENT);
        assertThrows(IllegalArgumentException.class, () -> dmaFile.read(0, readLength, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void read_lengthLessThenBufferSize() throws Exception {
        DmaFile dmaFile = eventExecutorGroup
                .openDmaFile(getTempFile(tmpDir), OpenOption.READ_ONLY, OpenOption.CREATE)
                .get();
        Path tempFile = Paths.get(dmaFile.getPath());
        String expected = TestUtils.prepareString(100);
        int readLength = 1024;
        TestUtils.writeStringToFile(expected, tempFile);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(2048, DmaFile.DEFAULT_ALIGNMENT);
        assertEquals(readLength, dmaFile.read(0, readLength, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
        String actual = StandardCharsets.UTF_8.decode(byteBuffer).toString();
        assertEquals(expected.substring(0, readLength), actual);
    }

    @Test
    void read_positionGreaterThanFileSize() throws Exception {
        DmaFile dmaFile = eventExecutorGroup
                .openDmaFile(getTempFile(tmpDir), OpenOption.READ_ONLY, OpenOption.CREATE)
                .get();
        Path tempFile = Paths.get(dmaFile.getPath());
        String expected = TestUtils.prepareString(10);
        int readLength = expected.length();
        TestUtils.writeStringToFile(expected, tempFile);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(2048, DmaFile.DEFAULT_ALIGNMENT);
        assertEquals(0, dmaFile.read(2048, readLength, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void write_lengthNotAligned() throws Exception {
        DmaFile dmaFile = eventExecutorGroup
                .openDmaFile(getTempFile(tmpDir), OpenOption.WRITE_ONLY, OpenOption.CREATE)
                .get();
        String expected = TestUtils.prepareString(10);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(512, DmaFile.DEFAULT_ALIGNMENT);
        byteBuffer.put(expected.getBytes(StandardCharsets.UTF_8));
        assertThrows(ExecutionException.class, () -> dmaFile.write(0, 121, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void write_positionNotAligned() throws Exception {
        DmaFile dmaFile = eventExecutorGroup
                .openDmaFile(getTempFile(tmpDir), OpenOption.WRITE_ONLY, OpenOption.CREATE)
                .get();
        String expected = TestUtils.prepareString(10);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(DmaFile.DEFAULT_ALIGNMENT, DmaFile.DEFAULT_ALIGNMENT);
        byteBuffer.put(expected.getBytes(StandardCharsets.UTF_8));
        assertThrows(ExecutionException.class, () -> dmaFile.write(1, DmaFile.DEFAULT_ALIGNMENT, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void write_bufferNotAligned() throws Exception {
        DmaFile dmaFile = eventExecutorGroup
                .openDmaFile(getTempFile(tmpDir), OpenOption.WRITE_ONLY, OpenOption.CREATE)
                .get();
        String expected = TestUtils.prepareString(10);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4096);
        // because sometimes we accidentally get properly aligned buffer
        while ((MemoryUtils.getDirectBufferAddress(byteBuffer) & (4096 - 1)) == 0) {
            byteBuffer = ByteBuffer.allocateDirect(4096);
        }
        byteBuffer.put(expected.getBytes(StandardCharsets.UTF_8));
        ByteBuffer finalByteBuffer = byteBuffer;
        assertThrows(ExecutionException.class, () -> dmaFile.write(0, DmaFile.DEFAULT_ALIGNMENT, finalByteBuffer).get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void write() throws Exception {
        DmaFile dmaFile = eventExecutorGroup
                .openDmaFile(getTempFile(tmpDir), OpenOption.CREATE, OpenOption.WRITE_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.write(dmaFile);
    }

    @Test
    void write_lengthGreaterThanBufferSize() throws Exception {
        DmaFile dmaFile = eventExecutorGroup
                .openDmaFile(getTempFile(tmpDir), OpenOption.CREATE, OpenOption.WRITE_ONLY)
                .get(1000, TimeUnit.MILLISECONDS);
        CommonTests.write_lengthGreaterThanBufferSize(dmaFile);
    }

    @Test
    void write_lengthLessThenBufferSize() throws Exception {
        DmaFile dmaFile = eventExecutorGroup
                .openDmaFile(getTempFile(tmpDir), OpenOption.WRITE_ONLY, OpenOption.CREATE)
                .get();
        Path tempFile = Paths.get(dmaFile.getPath());
        String expected = TestUtils.prepareString(100);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(1024, DmaFile.DEFAULT_ALIGNMENT);
        byteBuffer.put(expected.substring(0, 1024).getBytes(StandardCharsets.UTF_8));
        assertEquals(1024, dmaFile.write(0, 1024, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
        assertEquals(1024, Files.size(tempFile));
        assertEquals(expected.substring(0, 1024), new String(Files.readAllBytes(tempFile)));
    }

    @Test
    void write_positionGreaterThanFileSize() throws Exception {
        DmaFile dmaFile = eventExecutorGroup
                .openDmaFile(getTempFile(tmpDir), OpenOption.WRITE_ONLY, OpenOption.CREATE)
                .get();
        Path tempFile = Paths.get(dmaFile.getPath());
        String expected = TestUtils.prepareString(100);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(1024, DmaFile.DEFAULT_ALIGNMENT);
        byteBuffer.put(expected.substring(0, 1024).getBytes(StandardCharsets.UTF_8));
        assertEquals(1024, dmaFile.write(512, 1024, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
        assertEquals(1536, Files.size(tempFile));
        assertEquals(expected.substring(0, 1024), new String(Files.readAllBytes(tempFile)).substring(512));
    }
}
