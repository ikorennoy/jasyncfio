package one.jasyncfio;

import com.tdunning.math.stats.TDigest;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class SyncRead extends BenchmarkWorker {
    private final Path path;
    private final FileChannel channel;
    final ByteBuffer[] buffers;
    private int bufTail = 0;
    final int bufMask;
    final long maxBlocks;
    private final boolean trackLatencies;
    private final TDigest latencies = TDigest.createDigest(100.0);

    protected SyncRead(Path path, int blockSize, int depth, boolean trackLatencies, int id) {
        super(blockSize, id);
        this.path = path;
        this.trackLatencies = trackLatencies;
        try {
            this.channel = FileChannel.open(path, StandardOpenOption.READ);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.bufMask = depth - 1;
        this.maxBlocks = getMaxBlocks();
        buffers = new ByteBuffer[depth];
        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = MemoryUtils.allocateAlignedByteBuffer(blockSize, Native.getPageSize());
        }
    }

    private long getMaxBlocks() {
        try (RandomAccessFile file = new java.io.RandomAccessFile(path.toFile(), "r")) {
            FileDescriptor fd = file.getFD();
            Field f = FileDescriptor.class.getDeclaredField("fd");
            f.setAccessible(true);
            int trueFd = (int) f.get(fd);
            return Native.getFileSize(trueFd) / blockSize;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public double[] getLatencies(double[] percentiles) {
        double[] res = new double[percentiles.length];
        for (int i = 0; i < percentiles.length; i++) {
            res[i] = latencies.quantile(percentiles[i]);
        }
        return res;
    }

    @Override
    public void run() {
        do {
            long start = Native.getCpuTimer();

            calls++;
            int id = getNextBuffer();
            try {
                buffers[id].clear();
                long randomOffset = getRandomOffset(maxBlocks);
                int res = channel.read(buffers[id], randomOffset);
                if (res != blockSize) {
                    System.out.println("Unexpected res=" + res);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            done++;

            if (trackLatencies) {
                latencies.add(Native.getCpuTimer() - start);
            }

        } while (isRunning);
    }

    public int getNextBuffer() {
        return bufTail++ & bufMask;
    }
}
