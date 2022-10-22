package one.jasyncfio;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;

public class BenchmarkWorkerIoUring implements Runnable {
    private final Thread t;
    public final EventExecutor executor;
    private final ByteBuffer[] buffers;
    private final Path path;
    private final int blockSize;
    private final int bufferSize;
    private final int batchSubmit;
    private final int batchComplete;
    private final int depth;

    private long maxBlocks;

    volatile long calls = 0;
    volatile long done = 0;
    volatile long reaps = 0;
    volatile boolean isRunning = true;

    private final CompletionCallback c = new CompletionCallback() {
        @Override
        public void handle(int res, int flags, long userData) {
            if (res != bufferSize) {
                System.out.println("unexpected res: " + res);
            }
        }
    };

    public BenchmarkWorkerIoUring(Path path, int bufferSize, int blockSize, int depth, int batchSubmit, int batchComplete) {
        this.path = path;
        this.blockSize = blockSize;
        this.bufferSize = bufferSize;
        this.batchSubmit = batchSubmit;
        this.batchComplete = batchComplete;
        this.depth = depth;
        executor = EventExecutor.initDefault();
        buffers = new ByteBuffer[depth];
        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = MemoryUtils.allocateAlignedByteBuffer(bufferSize, MemoryUtils.getPageSize());
        }
        t = new Thread(this);
    }

    public void start() {
        t.start();
    }

    @Override
    public void run() {
        try {
            Uring uring = Native.setupIoUring(128, 0, 0, 0, 0, 0);
            AsyncFile file = AsyncFile.open(path, executor, OpenOption.READ_ONLY, OpenOption.NOATIME).get();
            executor.close();
            maxBlocks = Native.getFileSize(file.getRawFd()) / blockSize;
            int ret;
            int prepped = 0;
            int inFlight = 0;
            do {
                int toWait, toSubmit, thisReap, toPrep;
                if (prepped == 0 && inFlight < depth) {
                    toPrep = Math.min(depth - inFlight, batchSubmit);
                    prepped = prepMoreIos(uring, file.getRawFd(), toPrep);
                }
                inFlight += prepped;

                submitMore:
                toSubmit = prepped;

                submit:
                if (toSubmit > 0 && (inFlight + toSubmit <= depth))
                    toWait = 0;
                else
                    toWait = Math.min(inFlight + toSubmit, batchComplete);

                ret = uring.getSubmissionQueue().submit(toWait);
                calls++;

                thisReap = 0;
                do {
                    int r;
                    r = uring.getCompletionQueue().processEvents(c);
                    inFlight -= r;
                    thisReap += r;
                } while (false && thisReap < toWait);
                reaps += thisReap;

                if (ret >= 0) {
                    if (ret < toSubmit) {
                        int diff = toSubmit - ret;
                        done += ret;
                        prepped -= diff;
                        // todo support re-submit
                    }
                    done += ret;
                    prepped = 0;
                    continue;
                }

            } while (isRunning);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }


    private int prepMoreIos(Uring uring, int fd, int toPrep) {
        for (int i = 0; i < toPrep; i++) {
            uring.getSubmissionQueue().enqueueSqe(
                    Native.IORING_OP_READ,
                    0,
                    0,
                    fd,
                    MemoryUtils.getDirectBufferAddress(buffers[i]),
                    bufferSize,
                    getOffset(maxBlocks),
                    0,
                    0,
                    0
            );
        }
        return toPrep;
    }


    private long getOffset(long maxBlocks) {
        return (Math.abs(ThreadLocalRandom.current().nextLong()) % (maxBlocks - 1)) * blockSize;
    }
}
