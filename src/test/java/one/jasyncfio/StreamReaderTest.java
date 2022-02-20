package one.jasyncfio;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class StreamReaderTest {

    @TempDir
    Path tmpDir;
    private final EventExecutorGroup eventExecutorGroup = EventExecutorGroup.initDefault();


    @Test
    void read() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(100);
        int resultStringLength = expected.getBytes().length;
        TestUtils.writeStringToFile(expected, tempFile.toFile());
        StreamReader streamReader = new StreamReader(eventExecutorGroup.openBufferedFile(tempFile.toString()).get());

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(resultStringLength / 10);

        StringBuilder resultString = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            streamReader.read(byteBuffer).get(1000, TimeUnit.MILLISECONDS);
            resultString.append(StandardCharsets.UTF_8.decode(byteBuffer));
            byteBuffer.clear();
        }
        Assertions.assertEquals(expected, resultString.toString());
    }

    @Test
    void read_withPosition() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(100);
        int resultStringLength = expected.getBytes().length;
        TestUtils.writeStringToFile(expected, tempFile.toFile());
        StreamReader streamReader = new StreamReader(eventExecutorGroup.openBufferedFile(tempFile.toString()).get());

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(resultStringLength / 10);

        StringBuilder resultString = new StringBuilder();
        long currentPosition = 0;

        for (int i = 0; i < 10; i++) {
            Integer readBytes = streamReader.read(byteBuffer, currentPosition).get(1000, TimeUnit.MILLISECONDS);
            currentPosition += readBytes;
            resultString.append(StandardCharsets.UTF_8.decode(byteBuffer));
            byteBuffer.clear();
        }
        Assertions.assertEquals(expected, resultString.toString());
    }
}
