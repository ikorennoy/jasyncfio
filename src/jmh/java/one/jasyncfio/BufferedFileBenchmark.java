package one.jasyncfio;

import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 1)
@Measurement(iterations = 2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class BufferedFileBenchmark {

    @State(Scope.Benchmark)
    public static class Data {
        public final int sizeBytes = 512;
        public final int jasyncfioIterations = 128;
        public final String tmpDirName = "tmp-dir-";
        private final String readTestFileName = "read-test-file";
        private final String writeTestFileName = "write-test-file";
        public ByteBuffer[] readBuffers = new ByteBuffer[jasyncfioIterations];
        public ByteBuffer[] writeBuffers = new ByteBuffer[jasyncfioIterations];
        public EventExecutorGroup eventExecutorGroup = EventExecutorGroup.initDefault();
        public CompletableFuture<Integer>[] futures = new CompletableFuture[jasyncfioIterations];

        Path readTestFile;
        Path writeTestFile;
        Path tmpDir;


        @Setup
        public void prepare() throws IOException {
            tmpDir = Files.createTempDirectory(tmpDirName);
            readTestFile = tmpDir.resolve(readTestFileName);
            writeTestFile = tmpDir.resolve(writeTestFileName);
            Files.createFile(readTestFile);
            Files.createFile(writeTestFile);
            Files.write(readTestFile, generateContent(sizeBytes), StandardOpenOption.WRITE);
            Random random = new Random();
            byte[] bytes = new byte[sizeBytes];
            for (int i = 0; i < jasyncfioIterations; i++) {
                readBuffers[i] = ByteBuffer.allocateDirect(sizeBytes);
                ByteBuffer writeBuffer = ByteBuffer.allocateDirect(sizeBytes);
                random.nextBytes(bytes);
                writeBuffer.put(bytes);
                writeBuffers[i] = writeBuffer;
            }
        }

        @TearDown
        public void tearDown() throws IOException {
            Files.delete(tmpDir.resolve(readTestFileName));
            Files.delete(tmpDir.resolve(writeTestFileName));
            Files.delete(tmpDir);
        }

        private static byte[] generateContent(int sizeBytes) {
            byte[] content = new byte[sizeBytes];
            new Random().nextBytes(content);
            return content;
        }
    }

    @Benchmark
    // must be the same value with Data.jasyncfioIterations
    @OperationsPerInvocation(128)
    @Fork(1)
    public Integer jasyncfioRead(Data data) throws Exception {
        BufferedFile readTestFile = data.eventExecutorGroup.openBufferedFile(data.readTestFile.toString()).join();

        for (int i = 0; i < data.jasyncfioIterations; i++) {
            data.futures[i] = readTestFile.read(0, data.readBuffers[i]);
        }
        CompletableFuture.allOf(data.futures).get();

        return readTestFile.close().get();
    }

    @Benchmark
    @Fork(1)
    public int jasyncfioWrite(Data data) throws Exception {
        BufferedFile bufferedFile = data.eventExecutorGroup.createBufferedFile(data.writeTestFile.toString()).get();
        bufferedFile.write(-1, data.writeBuffers[0]);
        return bufferedFile.close().get();
    }

    @Benchmark
    @Fork(1)
    public int nioRead(Data data) throws Exception {
        try (FileChannel readTestFileChannel = FileChannel.open(data.readTestFile, StandardOpenOption.READ)) {
            int read = readTestFileChannel.read(data.readBuffers[0]);
            data.readBuffers[0].flip();
            return read;
        }
    }

    @Benchmark
    @Fork(1)
    public int nioWrite(Data data) throws Exception {
        try (FileChannel writeTestFileChannel = FileChannel.open(data.writeTestFile, StandardOpenOption.WRITE)) {
            int written = writeTestFileChannel.write(data.writeBuffers[0]);
            data.writeBuffers[0].flip();
            return written;
        }
    }
}
