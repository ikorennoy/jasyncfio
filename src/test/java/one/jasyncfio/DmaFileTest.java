package one.jasyncfio;

import one.jasyncfio.natives.MemoryUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DmaFileTest {

    @TempDir
    Path tmpDir;

    @Test
    void readAligned_lengthNotAligned() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(100);
        int resultStringLength = expected.getBytes().length;
        TestUtils.writeStringToFile(expected, tempFile.toFile());
        DmaFile dmaFile = DmaFile.open(tempFile.toAbsolutePath().toString()).get(1000, TimeUnit.MILLISECONDS);
        ByteBuffer byteBuffer = dmaFile.readAligned(0, resultStringLength).get();
        assertEquals(resultStringLength, byteBuffer.limit());
        String actual = StandardCharsets.UTF_8.decode(byteBuffer).toString();
        assertEquals(expected, actual);
    }

    @Test
    void readAligned_positionNotAligned() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(100);
        int resultStringLength = expected.getBytes().length;
        TestUtils.writeStringToFile(expected, tempFile.toFile());
        DmaFile dmaFile = DmaFile.open(tempFile.toAbsolutePath().toString()).get(1000, TimeUnit.MILLISECONDS);
        ByteBuffer byteBuffer = dmaFile.readAligned(100, resultStringLength).get();
        assertEquals(resultStringLength, byteBuffer.limit());
        String actual = StandardCharsets.UTF_8.decode(byteBuffer).toString();
        assertEquals(expected.substring(100, resultStringLength), actual);
    }

    @Test
    void readAligned() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(100);
        int resultStringLength = 1024;
        TestUtils.writeStringToFile(expected, tempFile.toFile());
        DmaFile dmaFile = DmaFile.open(tempFile.toAbsolutePath().toString()).get(1000, TimeUnit.MILLISECONDS);
        ByteBuffer byteBuffer = dmaFile.readAligned(0, resultStringLength).get();
        assertEquals(resultStringLength, byteBuffer.limit());
        String actual = StandardCharsets.UTF_8.decode(byteBuffer).toString();
        assertEquals(expected.substring(0, resultStringLength), actual);
    }

    @Test
    void read_lengthNotAligned() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(10);
        TestUtils.writeStringToFile(expected, tempFile.toFile());
        DmaFile dmaFile = DmaFile.open(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(128, DmaFile.DEFAULT_ALIGNMENT);
        assertThrows(ExecutionException.class, () -> dmaFile.read(0, 128, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void read_positionNotAligned() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(10);
        TestUtils.writeStringToFile(expected, tempFile.toFile());
        DmaFile dmaFile = DmaFile.open(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(128, DmaFile.DEFAULT_ALIGNMENT);
        assertThrows(ExecutionException.class, () -> dmaFile.read(1, 512, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void read_bufferNotAligned() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(10);
        TestUtils.writeStringToFile(expected, tempFile.toFile());
        DmaFile dmaFile = DmaFile.open(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(512);
        assertThrows(ExecutionException.class, () -> dmaFile.read(0, 512, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void read_notAligned() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(10);
        TestUtils.writeStringToFile(expected, tempFile.toFile());
        DmaFile dmaFile = DmaFile.open(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(128);
        assertThrows(ExecutionException.class, () -> dmaFile.read(10, 100, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void read_aligned() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(100);
        int readLength = 1024;
        TestUtils.writeStringToFile(expected, tempFile.toFile());
        DmaFile dmaFile = DmaFile.open(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(readLength, DmaFile.DEFAULT_ALIGNMENT);
        Integer read = dmaFile.read(0, readLength, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(readLength, read);
        assertEquals(read, byteBuffer.limit());
        assertEquals(0, byteBuffer.position());
        String actual = StandardCharsets.UTF_8.decode(byteBuffer).toString();
        assertEquals(expected.substring(0, readLength), actual);
    }

    @Test
    void read_lengthGreaterThanBufferSize() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(100);
        int readLength = 2048;
        TestUtils.writeStringToFile(expected, tempFile.toFile());
        DmaFile dmaFile = DmaFile.open(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(1024, DmaFile.DEFAULT_ALIGNMENT);
        assertThrows(IllegalArgumentException.class, () -> dmaFile.read(0, readLength, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void read_lengthLessThenBufferSize() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(100);
        int readLength = 1024;
        TestUtils.writeStringToFile(expected, tempFile.toFile());
        DmaFile dmaFile = DmaFile.open(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(2048, DmaFile.DEFAULT_ALIGNMENT);
        assertEquals(readLength, dmaFile.read(0, readLength, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
        String actual = StandardCharsets.UTF_8.decode(byteBuffer).toString();
        assertEquals(expected.substring(0, readLength), actual);
    }

    @Test
    void read_positionGreaterThanFileSize() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(10);
        int readLength = expected.length();
        TestUtils.writeStringToFile(expected, tempFile.toFile());
        DmaFile dmaFile = DmaFile.open(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(2048, DmaFile.DEFAULT_ALIGNMENT);
        assertEquals(0, dmaFile.read(2048, readLength, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void write_lengthNotAligned() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(10);
        DmaFile dmaFile = DmaFile.create(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(512, DmaFile.DEFAULT_ALIGNMENT);
        byteBuffer.put(expected.getBytes(StandardCharsets.UTF_8));
        assertThrows(ExecutionException.class, () -> dmaFile.write(0, 121, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void write_positionNotAligned() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(10);
        DmaFile dmaFile = DmaFile.create(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(512, DmaFile.DEFAULT_ALIGNMENT);
        byteBuffer.put(expected.getBytes(StandardCharsets.UTF_8));
        assertThrows(ExecutionException.class, () -> dmaFile.write(1, DmaFile.DEFAULT_ALIGNMENT, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
    }
    @Test
    void write_bufferNotAligned() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(10);
        DmaFile dmaFile = DmaFile.create(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(512);
        byteBuffer.put(expected.getBytes(StandardCharsets.UTF_8));
        assertThrows(ExecutionException.class, () -> dmaFile.write(0, DmaFile.DEFAULT_ALIGNMENT, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void write_allAligned() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(50);
        DmaFile dmaFile = DmaFile.create(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(512, DmaFile.DEFAULT_ALIGNMENT);
        byteBuffer.put(expected.substring(0, 512).getBytes(StandardCharsets.UTF_8));
        assertEquals(DmaFile.DEFAULT_ALIGNMENT, dmaFile.write(0, 512, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
        assertEquals(DmaFile.DEFAULT_ALIGNMENT, Files.size(tempFile));
        assertEquals(expected.substring(0, 512), new String(Files.readAllBytes(tempFile)));
    }

    @Test
    void write_lengthGreaterThanBufferSize() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(50);
        DmaFile dmaFile = DmaFile.create(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(512, DmaFile.DEFAULT_ALIGNMENT);
        byteBuffer.put(expected.substring(0, 512).getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> dmaFile.write(0, 1024, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void write_lengthLessThenBufferSize() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(100);
        DmaFile dmaFile = DmaFile.create(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(1024, DmaFile.DEFAULT_ALIGNMENT);
        byteBuffer.put(expected.substring(0, 1024).getBytes(StandardCharsets.UTF_8));
        assertEquals(1024, dmaFile.write(0, 1024, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
        assertEquals(1024, Files.size(tempFile));
        assertEquals(expected.substring(0, 1024), new String(Files.readAllBytes(tempFile)));
    }

    @Test
    void write_positionGreaterThanFileSize() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(100);
        DmaFile dmaFile = DmaFile.create(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(1024, DmaFile.DEFAULT_ALIGNMENT);
        byteBuffer.put(expected.substring(0, 1024).getBytes(StandardCharsets.UTF_8));
        assertEquals(1024, dmaFile.write(512, 1024, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
        assertEquals(1536, Files.size(tempFile));
        assertEquals(expected.substring(0, 1024), new String(Files.readAllBytes(tempFile)).substring(512));
    }
}
