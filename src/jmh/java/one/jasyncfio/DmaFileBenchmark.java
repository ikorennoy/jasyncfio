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
 * sudo java -Xmx3g -Djmh.ignoreLock=true -DBLOCK_DEVICE=/dev/nvme0n1 -jar build/libs/jasyncfio-0.0.1-jmh.jar
 *
 */
@Warmup(iterations = 1)
@Measurement(iterations = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class DmaFileBenchmark {

    @State(Scope.Thread)
    public static class Data {
        // can't provide this via System.getProperty because annotations require constants
        public static final int batchSubmit = 32;
        public static final int batchComplete = 32;

        public static final int blockSize = Integer.parseInt(System.getProperty("BLOCK_SIZE", "512"));
        public static final int ioDepth = Integer.parseInt(System.getProperty("IO_DEPTH", "128"));
        public static final long pageSize = Native.getPageSize();
        public static final EventExecutorGroup eventExecutors = EventExecutorGroup.builder()
                .entries(ioDepth)
                .ioRingSetupSqPoll(2000)
                .ioRingSetupIoPoll()
                .build();

        public final ByteBuffer[] buffers = new ByteBuffer[ioDepth];


        public String device;
        public DmaFile file;
        public long maxSize;
        public long maxBlocks;
        public CompletableFuture<Integer>[] futures = new CompletableFuture[batchComplete];
        public long[] positions = new long[batchSubmit];

        @Setup
        public void setup() throws Exception {
            device = System.getProperty("BLOCK_DEVICE");
            if (device == null) {
                throw new IllegalArgumentException("BLOCK_DEVICE system property must be specified");
            }
            file = eventExecutors.openDmaFile(device).get();
            Random random = new Random();
            maxSize = Native.getFileSize(file.fd);
            maxBlocks = maxSize / blockSize;
            for (int i = 0; i < ioDepth; i++) {
                buffers[i] = MemoryUtils.allocateAlignedByteBuffer(blockSize, pageSize);
            }
            for (int i = 0; i < batchSubmit; i++) {
                positions[i] = (Math.abs(random.nextLong()) % (maxBlocks - 1)) * Data.blockSize;
            }
        }

        @TearDown
        public void tearDown() throws Exception {
//            file.close().get();
        }
    }

    /**
     * close analogue to t/io_uring -d128 -s32 -c32 -b512 -p1 -B0 -D0 -F0 -n1 -O1 -R1 <block-device>
     */
    @Benchmark
    @OperationsPerInvocation(Data.batchSubmit)
    @Fork(value = 1)
    @Threads(1)
    public void jasyncfioRandomRead(Data data) throws Exception {
        for (int i = 0; i < Data.batchSubmit; i++) {
            CompletableFuture<Integer> read = data.file.read(data.positions[i], Data.blockSize, data.buffers[i]);
            if (i < Data.batchComplete) {
                data.futures[i] = read;
            }
        }
        CompletableFuture.allOf(data.futures).get();
    }

    /**
     * close analogue to t/io_uring -d128 -s32 -c32 -b512 -p1 -B0 -D0 -F0 -n1 -O1 -R0 <block-device>
     */
    @Benchmark
    @OperationsPerInvocation(Data.batchSubmit)
    @Fork(value = 1)
    @Threads(1)
    public void jasyncfioSequentialRead(Data data) throws Exception {
        long currentOffset = 0;
        for (int i = 0; i < Data.batchSubmit; i++) {
            long position = currentOffset;
            if (currentOffset + Data.blockSize > data.maxSize) {
                currentOffset = 0;
            }
            currentOffset += Data.blockSize;

            CompletableFuture<Integer> read = data.file.read(position, Data.blockSize, data.buffers[i]);
            if (i < Data.batchComplete) {
                data.futures[i] = read;
            }
        }
        CompletableFuture.allOf(data.futures).get();
    }
}
