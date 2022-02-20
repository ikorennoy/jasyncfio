package one.jasyncfio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BufferedFileChannelTest {

    @TempDir
    Path tmpDir;
    private final EventExecutorGroup eventExecutorGroup = EventExecutorGroup.initDefault();


    @Test
    void read() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(100);
        int resultStringLength = expected.getBytes().length;
        TestUtils.writeStringToFile(expected, tempFile.toFile());
        BufferedFileChannel streamReader = new BufferedFileChannel(eventExecutorGroup.openBufferedFile(tempFile.toString()).get());

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(resultStringLength / 10);

        StringBuilder resultString = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            streamReader.read(byteBuffer).get(1000, TimeUnit.MILLISECONDS);
            resultString.append(StandardCharsets.UTF_8.decode(byteBuffer));
            byteBuffer.clear();
        }
        assertEquals(expected, resultString.toString());
    }

    @Test
    void read_withPosition() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(100);
        int resultStringLength = expected.getBytes().length;
        TestUtils.writeStringToFile(expected, tempFile.toFile());
        BufferedFileChannel streamReader = new BufferedFileChannel(eventExecutorGroup.openBufferedFile(tempFile.toString()).get());

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(resultStringLength / 10);

        StringBuilder resultString = new StringBuilder();
        long currentPosition = 0;

        for (int i = 0; i < 10; i++) {
            Integer readBytes = streamReader.read(byteBuffer, currentPosition).get(1000, TimeUnit.MILLISECONDS);
            currentPosition += readBytes;
            resultString.append(StandardCharsets.UTF_8.decode(byteBuffer));
            byteBuffer.clear();
        }
        assertEquals(expected, resultString.toString());
    }

    @Test
    void write() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(100);
        int resultStringLength = expected.getBytes().length;
        BufferedFileChannel bufferedFileChannel = new BufferedFileChannel(eventExecutorGroup.createBufferedFile(tempFile.toString()).get());
        int bufferLen = resultStringLength / 10;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bufferLen);

        int currentStringPosition = 0;

        for (int i = 0; i < 10; i++) {
            byteBuffer.put(expected.substring(currentStringPosition, bufferLen + currentStringPosition).getBytes(StandardCharsets.UTF_8));
            bufferedFileChannel.write(byteBuffer).get(1000, TimeUnit.MILLISECONDS);
            currentStringPosition += bufferLen;
            byteBuffer.clear();
        }

        assertEquals(expected, new String(Files.readAllBytes(tempFile)));
    }

    @Test
    void write_withPosition() throws Exception {
        Path tempFile = Files.createTempFile(tmpDir, "test-", "file");
        String expected = TestUtils.prepareString(100);
        int resultStringLength = expected.getBytes().length;
        BufferedFileChannel bufferedFileChannel = new BufferedFileChannel(eventExecutorGroup.createBufferedFile(tempFile.toString()).get());
        int bufferLen = resultStringLength / 10;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bufferLen);

        int currentPosition = 0;

        for (int i = 0; i < 10; i++) {
            byteBuffer.put(expected.substring(currentPosition, bufferLen + currentPosition).getBytes(StandardCharsets.UTF_8));
            bufferedFileChannel.write(byteBuffer, (long) currentPosition).get(1000, TimeUnit.MILLISECONDS);
            currentPosition += bufferLen;
            byteBuffer.clear();
        }

        assertEquals(expected, new String(Files.readAllBytes(tempFile)));
    }
}
