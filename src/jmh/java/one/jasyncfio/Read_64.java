package one.jasyncfio;

import one.jasyncfio.natives.MemoryUtils;
import one.jasyncfio.natives.Native;
import org.openjdk.jmh.annotations.*;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * jvmArgsAppend = "-agentpath:/libasyncProfiler.so=start,event=cpu,file=sqpoll.jfr,jfr" - profiler
 */
@Warmup(iterations = 1)
@Measurement(iterations = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class Read_64 {

    @State(Scope.Thread)
    public static class Data {

        public static final int ioDepth = 256;
        public static final int batchSubmit = 64;

        @Param({"true", "false"})
        boolean sqPoll;

        @Param({"true", "false"})
        boolean ioPoll;

        public final int blockSize = Integer.parseInt(System.getProperty("BLOCK_SIZE", "512"));
        public final long pageSize = Native.getPageSize();

        public EventExecutorGroup eventExecutors;
        public final ByteBuffer[] buffers = new ByteBuffer[ioDepth];

        public String device;
        public DmaFile file;
        public long maxSize;
        public long maxBlocks;
        public long[] positions = new long[batchSubmit];
        public CompletableFuture<Integer>[] futures = new CompletableFuture[batchSubmit];

        @Setup
        public void setup() throws Exception {
            device = System.getProperty("BLOCK_DEVICE");
            if (device == null) {
                throw new IllegalArgumentException("BLOCK_DEVICE system property must be specified");
            }
            EventExecutorGroup.Builder builder = EventExecutorGroup
                    .builder()
                    .entries(ioDepth);

            if (sqPoll) builder.ioRingSetupSqPoll(2000);
            if (ioPoll) builder.ioRingSetupIoPoll();

            eventExecutors = builder.build();

            file = eventExecutors.openDmaFile(device).get();
            Random random = new Random();
            maxSize = Native.getFileSize(file.fd);
            maxBlocks = maxSize / blockSize;
            for (int i = 0; i < ioDepth; i++) {
                buffers[i] = MemoryUtils.allocateAlignedByteBuffer(blockSize, pageSize);
            }
            for (int i = 0; i < batchSubmit; i++) {
                positions[i] = (Math.abs(random.nextLong()) % (maxBlocks - 1)) * blockSize;
            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(Data.batchSubmit)
    @Fork(value = 1)
    @Threads(1)
    public void randomRead(Data data) throws Exception {
        Benchmarks.randomRead(data.file, data.positions, data.blockSize, Data.batchSubmit, data.buffers, data.futures);
    }

    @Benchmark
    @OperationsPerInvocation(Data.batchSubmit)
    @Fork(value = 1)
    @Threads(1)
    public void sequentialRead(Data data) throws Exception {
        Benchmarks.sequentialRead(data.file, data.maxSize, data.blockSize, Data.batchSubmit, data.buffers, data.futures);
    }
}
