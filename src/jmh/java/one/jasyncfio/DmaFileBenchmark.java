package one.jasyncfio;

import one.jasyncfio.natives.MemoryUtils;
import org.openjdk.jmh.annotations.*;
import java.io.IOException;
import java.nio.ByteBuffer;
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
public class DmaFileBenchmark {

    @State(Scope.Benchmark)
    public static class Data {
        private static final int sizeBytes = 512;
        private static final int jasyncfioIterations = 128;

        public ByteBuffer[] readBuffers = new ByteBuffer[jasyncfioIterations];
        public ByteBuffer[] writeBuffers = new ByteBuffer[jasyncfioIterations];
        public EventExecutorGroup eventExecutorGroup = EventExecutorGroup.initDefault();
        public CompletableFuture<Integer>[] futures = new CompletableFuture[jasyncfioIterations];

        Path tmpDir;
        Path readTestFile;
        Path writeTestFile;

        {
            try {
                tmpDir = Files.createTempDirectory("tmp-dir-");
                readTestFile = Files.createFile(tmpDir.resolve("read-test-file"));
                writeTestFile = Files.createFile(tmpDir.resolve("write-test-file"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        @Setup
        public void setup() throws Exception {
            Files.write(readTestFile, generateContent(sizeBytes), StandardOpenOption.WRITE);
            Random random = new Random();
            byte[] bytes = new byte[sizeBytes];
            for (int i = 0; i < jasyncfioIterations; i++) {
                readBuffers[i] = MemoryUtils.allocateAlignedByteBuffer(sizeBytes, DmaFile.DEFAULT_ALIGNMENT);
                ByteBuffer writeBuffer = MemoryUtils.allocateAlignedByteBuffer(sizeBytes, DmaFile.DEFAULT_ALIGNMENT);
                random.nextBytes(bytes);
                writeBuffer.put(bytes);
                writeBuffers[i] = writeBuffer;
            }
        }

        @TearDown
        public void tearDown() throws IOException {
            Files.delete(readTestFile);
            Files.delete(writeTestFile);
            Files.delete(tmpDir);
        }

        private static byte[] generateContent(int sizeBytes) {
            byte[] content = new byte[sizeBytes];
            new Random().nextBytes(content);
            return content;
        }
    }

    @Benchmark
    @OperationsPerInvocation(Data.jasyncfioIterations)
    @Fork(value = 1)
    public Integer jasyncfioRead(Data data) throws Exception {
        DmaFile readTestFile = data.eventExecutorGroup.openDmaFile(data.readTestFile.toString()).join();

        for (int i = 0; i < Data.jasyncfioIterations; i++) {
            data.futures[i] = readTestFile.read(-1, 512, data.readBuffers[i]);
        }
        CompletableFuture.allOf(data.futures).get();
        return readTestFile.close().get();
    }

    @Benchmark
    @OperationsPerInvocation(Data.jasyncfioIterations)
    @Fork(value = 1)
    public Integer jasyncfioWrite(Data data) throws Exception {
        DmaFile readTestFile = data.eventExecutorGroup.openDmaFile(data.readTestFile.toString()).join();

        for (int i = 0; i < Data.jasyncfioIterations; i++) {
            data.futures[i] = readTestFile.write(-1, 512, data.readBuffers[i]);
        }
        CompletableFuture.allOf(data.futures).get();
        return readTestFile.close().get();
    }
}
