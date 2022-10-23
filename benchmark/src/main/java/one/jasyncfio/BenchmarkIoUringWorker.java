package one.jasyncfio;

import java.nio.ByteBuffer;
import java.nio.file.Path;

public abstract class BenchmarkIoUringWorker extends BenchmarkWorker {

    final EventExecutor executor;
    final ByteBuffer[] buffers;
    final Path path;
    final AsyncFile file;
    final int batchSubmit;
    final int batchComplete;
    final int depth;
    final int bufMask;
    final long maxBlocks;
    final boolean fixedBuffers;

    private int bufTail = 0;


    public BenchmarkIoUringWorker(
            Path path,
            int blockSize,
            int depth,
            int batchSubmit,
            int batchComplete,
            boolean pooledIo,
            boolean fixedBuffers,
            boolean directIo,
            boolean noOp,
            boolean trackLatencies,
            boolean randomIo
    ) {
        super(blockSize);
        this.path = path;
        this.depth = depth;
        this.batchSubmit = batchSubmit;
        this.batchComplete = batchComplete;
        this.bufMask = depth - 1;
        this.fixedBuffers = fixedBuffers;
        EventExecutor.Builder ioUringBuilder = EventExecutor.builder();

        if (pooledIo) {
            ioUringBuilder.ioRingSetupIoPoll();
        }
        if (fixedBuffers) {
            ioUringBuilder.addBufRing(depth, blockSize, (short) 0);
            buffers = null;
        } else {
            buffers = new ByteBuffer[depth];
            for (int i = 0; i < buffers.length; i++) {
                buffers[i] = MemoryUtils.allocateAlignedByteBuffer(blockSize, Native.getPageSize());
            }
        }
        if (trackLatencies) {
            ioUringBuilder.monitoring();
        }
        this.executor = ioUringBuilder.build();

        if (directIo) {
            this.file = AsyncFile.open(path, executor, OpenOption.READ_ONLY, OpenOption.NOATIME, OpenOption.DIRECT).join();
        } else {
            this.file = AsyncFile.open(path, executor, OpenOption.READ_ONLY, OpenOption.NOATIME).join();
        }
        maxBlocks = Native.getFileSize(file.getRawFd()) / blockSize;
    }

    public int getNextBuffer() {
        return bufTail++ & bufMask;
    }
}
