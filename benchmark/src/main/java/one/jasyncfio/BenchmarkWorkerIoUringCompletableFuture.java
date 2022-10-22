package one.jasyncfio;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class BenchmarkWorkerIoUringCompletableFuture implements Runnable {
    private final Thread t;
    public final EventExecutor executor;
    private final ByteBuffer[] buffers;
    private final Path path;
    private final int blockSize;
    private final int bufferSize;
    private final int batchSubmit;
    private final int batchComplete;

    private final int depth;
    private final int bufMask;
    private int bufTail = 0;
    private long maxBlocks;

    volatile long calls = 0;
    volatile long done = 0;
    volatile long reaps = 0;
    volatile boolean isRunning = true;

    public BenchmarkWorkerIoUringCompletableFuture(Path path, int bufferSize, int blockSize, int depth, int batchSubmit, int batchComplete) {
        this.path = path;
        this.blockSize = blockSize;
        this.bufferSize = bufferSize;
        this.batchSubmit = batchSubmit;
        this.batchComplete = batchComplete;
        this.depth = depth;
        this.bufMask = depth - 1;
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
            AsyncFile file = AsyncFile.open(path, executor, OpenOption.READ_ONLY, OpenOption.NOATIME).get();
            maxBlocks = Native.getFileSize(file.getRawFd()) / blockSize;
            List<CompletableFuture<Integer>> submissions = new ArrayList<>(depth);
            List<CompletableFuture<Integer>> submissionsToRemove = new ArrayList<>(depth);
            int submitted = 0;
            int inFlight = 0;
            do {
                int thisReap, toPrep;
                if (inFlight < depth) {
                    toPrep = Math.min(depth - inFlight, batchSubmit);
                    submitted = submitMoreIos(file, toPrep, submissions);
                }
                inFlight += submitted;
                calls++;

                thisReap = 0;
                do {
                    int r = 0;
                    for (CompletableFuture<Integer> future : submissions) {
                        if (future.isDone()) {
                            Integer res = future.get();
                            if (res != bufferSize) {
                                System.out.println("unexpected res=" + res);
                            }
                            submissionsToRemove.add(future);
                            r++;
                        }
                    }
                    submissions.removeAll(submissionsToRemove);
                    submissionsToRemove.clear();
                    inFlight -= r;
                    thisReap += r;
                } while (false); // todo support io_poll

                reaps += thisReap;
                done += submitted;

                submitted = 0;

            } while (isRunning);

        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }


    private int submitMoreIos(AbstractFile file, int toSubmit, List<CompletableFuture<Integer>> futures) {
        for (int i = 0; i < toSubmit; i++) {
            int idx = bufTail++ & bufMask;
            buffers[idx].clear();
            futures.add(file.read(buffers[idx], getOffset(maxBlocks), bufferSize));
        }
        return toSubmit;
    }


    private long getOffset(long maxBlocks) {
        return (Math.abs(ThreadLocalRandom.current().nextLong()) % (maxBlocks - 1)) * blockSize;
    }
}
