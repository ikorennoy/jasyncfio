package one.jasyncfio;

import one.jasyncfio.*;

import javax.naming.Name;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("ALL")
public class BenchmarkWorkerIoUring implements Runnable {
    private final Random random;
    private final Thread t;
    private final EventExecutor executor;
    private final ByteBuffer[] buffers;
    private final Path path;
    private final int blockSize;
    private final int bufferSize;

    volatile long calls = 0;
    volatile long done = 0;
    volatile long reaps = 0;
    volatile boolean isRunning = true;

    public BenchmarkWorkerIoUring(Path path, int bufferSize, int blockSize, int ioDepth) {
        this.path = path;
        this.blockSize = blockSize;
        this.bufferSize = bufferSize;
        executor = EventExecutor.initDefault();
        buffers = new ByteBuffer[ioDepth];
        Arrays.fill(buffers, MemoryUtils.allocateAlignedByteBuffer(bufferSize, MemoryUtils.getPageSize()));
        t = new Thread(this);
        random = new Random(t.hashCode());
    }

    public void start() {
        t.start();
    }

    @Override
    public void run() {
        try {
            AsyncFile file = AsyncFile.open(path, executor, OpenOption.READ_ONLY, OpenOption.NOATIME).get();
            long maxBlocks = Native.getFileSize(file.getRawFd()) / blockSize;

            do {
                calls++;
                Integer read = file.read(buffers[0], getOffset(maxBlocks), bufferSize).get();
                reaps++;
                if (read != bufferSize) {
                    System.out.println("Unexpected: " + read);
                }
                buffers[0].clear();
                done++;
            } while (isRunning);

        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }


    private long getOffset(long maxBlocks) {
        return (Math.abs(random.nextLong()) % (maxBlocks - 1)) * blockSize;
    }
}
