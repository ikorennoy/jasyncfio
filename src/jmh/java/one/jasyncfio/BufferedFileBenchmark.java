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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 1)
@Measurement(iterations = 2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class BufferedFileBenchmark {

    @State(Scope.Benchmark)
    public static class Data {
        Path src;
        Path dst;
        Path tmpDir;

        public int sizeBytes = 512;
        public int jasyncfioIterations = 128;

        public byte[] content = generateContent(sizeBytes);
        public ByteBuffer[] byteBuffer = new ByteBuffer[jasyncfioIterations];
        public EventExecutorGroup eventExecutorGroup = EventExecutorGroup.initDefault();
        public CompletableFuture<Integer>[] futures = new CompletableFuture[jasyncfioIterations];

        @Setup
        public void prepare() throws IOException {
            tmpDir = Files.createTempDirectory("tmp-dir-");
            src = tmpDir.resolve("temp-src");
            dst = tmpDir.resolve("temp-dst");
            Files.createFile(src);
            Files.createFile(dst);
            Files.write(src, content, StandardOpenOption.WRITE);
            for (int i = 0; i < jasyncfioIterations; i++) {
                byteBuffer[i] = ByteBuffer.allocateDirect(512);
            }
        }

        @TearDown
        public void tearDown() throws IOException {
            Files.delete(tmpDir.resolve("temp-src"));
            Files.delete(tmpDir.resolve("temp-dst"));
            Files.delete(tmpDir);
        }

        private static byte[] generateContent(int sizeBytes) {
            byte[] content = new byte[sizeBytes];
            new Random().nextBytes(content);
            return content;
        }
    }

    @Benchmark
    @OperationsPerInvocation(128)
    @Fork(1)
    public Integer jasyncfioRead(Data data) throws Exception {
        BufferedFile sourceFile = data.eventExecutorGroup.openBufferedFile(data.src.toString()).join();

        for (int i = 0; i < data.jasyncfioIterations; i++) {
            data.futures[i] = sourceFile.read(0, data.byteBuffer[i]);
        }
        CompletableFuture.allOf(data.futures).get();

        return sourceFile.close().get();
    }

    @Benchmark
    @Fork(1)
    public int nioRead(Data data) throws Exception {
        try (FileChannel sourceChannel = FileChannel.open(data.src, StandardOpenOption.READ)) {
            int read = sourceChannel.read(data.byteBuffer[0], 0);
            data.byteBuffer[0].flip();
            return read;
        }
    }
}
