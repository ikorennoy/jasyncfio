package one.jasyncfio;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("ALL")
public class BenchmarkWorkerIoUring implements Runnable {
    private volatile Thread t;
    public final EventExecutor executor;
    private final ByteBuffer[] buffers;
    private final Path path;
    private final int blockSize;
    private final int bufferSize;
    private final int submissions;
    private final int completions;

    volatile long calls = 0;
    volatile long done = 0;
    volatile long reaps = 0;
    volatile boolean isRunning = true;

    private final CompletionCallback c = new CompletionCallback() {
        @Override
        public void handle(int res, int flags, long userData) {

        }
    };

    public BenchmarkWorkerIoUring(Path path, int bufferSize, int blockSize, int ioDepth, int submissions, int completions) {
        this.path = path;
        this.blockSize = blockSize;
        this.bufferSize = bufferSize;
        this.submissions = submissions;
        this.completions = completions;
        executor = EventExecutor.initDefault();
        buffers = new ByteBuffer[ioDepth];
        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = MemoryUtils.allocateAlignedByteBuffer(bufferSize, MemoryUtils.getPageSize());
        }
        t = new Thread(this);
    }

    public void start() {
        t.start();
    }

    // todo add jmh for enqueue
    //  jmh for processEvents

    @Override
    public void run() {
        try {
            Uring uring = Native.setupIoUring(128, 0, 0, 0, 0, 0);
            AsyncFile file = AsyncFile.open(path, executor, OpenOption.READ_ONLY, OpenOption.NOATIME).get();
            executor.close();
            long maxBlocks = Native.getFileSize(file.getRawFd()) / blockSize;
            CompletableFuture[] submissionsArray = new CompletableFuture[submissions];
            do {
                for (int i = 0; i < submissions; i++) {
                    uring.getSubmissionQueue().enqueueSqe(
                            Native.IORING_OP_READ,
                            0,
                            0,
                            file.getRawFd(),
                            MemoryUtils.getDirectBufferAddress(buffers[i]),
                            bufferSize,
                            getOffset(maxBlocks),
                            0,
                            0,
                            0
                    );
                }
                int ret = uring.getSubmissionQueue().submit(completions);
                calls++;
                reaps += uring.getCompletionQueue().processEvents(c);
                done += ret;
            } while (isRunning);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }


    private long getOffset(long maxBlocks) {
        return (Math.abs(ThreadLocalRandom.current().nextLong()) % (maxBlocks - 1)) * blockSize;
    }
}
