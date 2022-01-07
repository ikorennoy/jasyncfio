package one.jasyncfio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DmaFileTest {

    @Test
    void alignUp() {
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            assertEquals(0, DmaFile.alignUp(i, 512) % 512);
            assertEquals(0, DmaFile.alignUp(i, 4096) % 4096);
        }
    }

    @Test
    void alignDown() {
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            assertEquals(0, DmaFile.alignDown(i, 512) % 512);
            assertEquals(0, DmaFile.alignDown(i, 4096) % 4096);
        }
    }

    @Test
    void readAligned(@TempDir Path tempDir) throws Exception {
        Path file = Files.createTempFile(tempDir, "test-", "file");
        String expected = TestUtils.prepareString(100);
        int resultStringLength = expected.getBytes().length;
        TestUtils.writeStringToFile(expected, file.toFile());
        DmaFile dmaFile = DmaFile.open(file.toAbsolutePath().toString()).get(1000, TimeUnit.MILLISECONDS);
        ByteBuffer byteBuffer = dmaFile.readAligned(0, resultStringLength).get();
        assertEquals(resultStringLength, byteBuffer.limit());
        String actual = StandardCharsets.UTF_8.decode(byteBuffer).toString();
        assertEquals(expected, actual);
    }

    @Test
    void read_positionAligned() {

    }



    // read position aligned len not
    // read len aligned position not
    // read not aligned
    // read all aligned
    // read all aligned buffer not and buffer bigger
    // read all aligned buffer not and buffer less




}
