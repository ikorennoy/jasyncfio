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

    @Override
    public void run() {
        try {
            AsyncFile file = AsyncFile.open(path, executor, OpenOption.READ_ONLY, OpenOption.NOATIME).get();
            long maxBlocks = Native.getFileSize(file.getRawFd()) / blockSize;
            CompletableFuture[] submissionsArray = new CompletableFuture[submissions];
            do {
                for (int i = 0; i < submissions; i++) {
                    buffers[i].clear();
                    calls++;
                    submissionsArray[i] = file.read(buffers[i], getOffset(maxBlocks), bufferSize);
                }
                for (int i = 0; i < completions; i++) {
                    submissionsArray[i].get();
                    reaps++;
                    done++;
                }
            } while (isRunning);

        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }


    private long getOffset(long maxBlocks) {
        return (Math.abs(ThreadLocalRandom.current().nextLong()) % (maxBlocks - 1)) * blockSize;
    }
}
