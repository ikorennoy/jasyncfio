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
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(128, DmaFile.READ_ALIGNMENT);
        assertThrows(ExecutionException.class, () -> dmaFile.read(0, 128, byteBuffer).get(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void read_positionNotAligned() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(10);
        TestUtils.writeStringToFile(expected, tempFile.toFile());
        DmaFile dmaFile = DmaFile.open(tempFile.toString()).get(1000, TimeUnit.MILLISECONDS);
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(128, DmaFile.READ_ALIGNMENT);
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
        ByteBuffer byteBuffer = MemoryUtils.allocateAlignedByteBuffer(readLength, DmaFile.READ_ALIGNMENT);
        Integer read = dmaFile.read(0, readLength, byteBuffer).get(1000, TimeUnit.MILLISECONDS);
        assertEquals(readLength, read);
        assertEquals(read, byteBuffer.limit());
        assertEquals(0, byteBuffer.position());
        String actual = StandardCharsets.UTF_8.decode(byteBuffer).toString();
        assertEquals(expected.substring(0, readLength), actual);
    }



    // read position aligned len not
    // read len aligned position not
    // read not aligned
    // read all aligned
    // read all aligned buffer not and buffer bigger
    // read all aligned buffer not and buffer less


}
