package one.jasyncfio;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public abstract class BenchmarkWorker implements Runnable {
    private final Thread t;

    final int blockSize;
    volatile long calls = 0;
    volatile long done = 0;
    volatile long reaps = 0;
    volatile boolean isRunning = true;

    protected BenchmarkWorker(int blockSize) {
        this.blockSize = blockSize;
        t = new Thread(this);
    }


    public void start() {
        t.start();
    }

    public void stop() {
        isRunning = false;
    }

    public long getRandomOffset(long maxBlocks) {
        return (Math.abs(ThreadLocalRandom.current().nextLong()) % (maxBlocks - 1)) * blockSize;
    }

    public abstract Map<String, double[]> getLatencies(double[] percentiles);
}
