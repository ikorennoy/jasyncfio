package one.jasyncfio;

import java.util.concurrent.ThreadLocalRandom;

public abstract class BenchmarkWorker implements Runnable {
    private final Thread t;

    private final int id;
    final int blockSize;
    volatile long calls = 0;
    volatile long done = 0;
    volatile long reaps = 0;
    volatile boolean isRunning = true;

    protected BenchmarkWorker(int blockSize, int id) {
        this.blockSize = blockSize;
        this.id = id;
        t = new Thread(this);
    }


    public void start() {
        t.start();
    }

    public void stop() {
        isRunning = false;
    }

    public String getId() {
        return "Worker-" + id;
    }

    public long getRandomOffset(long maxBlocks) {
        return (Math.abs(ThreadLocalRandom.current().nextLong()) % (maxBlocks - 1)) * blockSize;
    }

    public abstract double[] getLatencies(double[] percentiles);
}
