package one.jasyncfio;

import com.tdunning.math.stats.TDigest;
import one.jasyncfio.collections.IntObjectMap;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

abstract class Ring {
    final Uring ring;
    final CompletionQueue completionQueue;
    final SubmissionQueue submissionQueue;
    private final IntObjectMap<Command<?>> commands;
    private final CompletionCallback callback = this::handle;

    private final ConcurrentMap<Command<?>, Long> commandStarts;

    private final TDigest commandExecutionDelays;
    private final boolean monitoringEnabled;

    private final Map<Short, IoUringBufRing> bufRings;

    Ring(int entries,
         int flags,
         int sqThreadIdle,
         int sqThreadCpu,
         int cqSize,
         int attachWqRingFd,
         List<BufRingDescriptor> bufRingDescriptorList,
         IntObjectMap<Command<?>> commands,
         boolean monitoringEnabled,
         ConcurrentMap<Command<?>, Long> commandStarts,
         TDigest commandExecutionDelays) {

        this.commands = commands;
        this.commandStarts = commandStarts;
        this.commandExecutionDelays = commandExecutionDelays;
        this.monitoringEnabled = monitoringEnabled;
        ring = Native.setupIoUring(entries, flags, sqThreadIdle, sqThreadCpu, cqSize, attachWqRingFd);
        submissionQueue = ring.getSubmissionQueue();
        completionQueue = ring.getCompletionQueue();


        if (!bufRingDescriptorList.isEmpty()) {
            bufRings = new HashMap<>();
            for (BufRingDescriptor descriptor : bufRingDescriptorList) {
                IoUringBufRing ioUringBufRing =
                        new IoUringBufRing(ring.getRingFd(), descriptor.getBufRingBufSize(), descriptor.getBufRingSize(), descriptor.getBufRingId());
                bufRings.put(descriptor.getBufRingId(), ioUringBufRing);
            }
        } else {
            bufRings = null;
        }
    }

    private boolean isIoringCqeFBufferSet(int flags) {
        return (flags & Native.IORING_CQE_F_BUFFER) == Native.IORING_CQE_F_BUFFER;
    }

    private void handle(int res, int flags, long data) {
        Command<?> command = commands.remove((int) data);
        if (command != null) {
            if (res >= 0) {
                if (isIoringCqeFBufferSet(flags)) {
                    int bufferId = flags >> 16;
                    IoUringBufRing bufRing = bufRings.get((short) command.getBufIndex());
                    ByteBuffer buffer = bufRing.getBuffer(bufferId);
                    buffer.position(res);
                    command.complete(new BufRingResult(buffer, res, bufferId, this, (short) command.getBufIndex()));
                } else {
                    command.complete(res);
                }
            } else {
                command.error(new IOException(String.format("Error code: %d; message: %s", -res, Native.decodeErrno(res))));
            }
        }
        if (monitoringEnabled) {
            commandExecutionDelays.add(System.nanoTime() - commandStarts.remove(command));
        }
    }

    void close() {
        if (bufRings != null) {
            bufRings.values().forEach(IoUringBufRing::close);
        }
        ring.close();
    }

    abstract void park();

    abstract void unpark();

    boolean hasCompletions() {
        return completionQueue.hasCompletions();
    }

    int processCompletedTasks() {
        return completionQueue.processEvents(callback);
    }

    void recycleBuffer(int bufferId, short bufRingId) {
        bufRings.get(bufRingId).recycleBuffer(bufferId);
    }

    <T> void addOperation(Command<T> op, long opId) {
        submissionQueue.enqueueSqe(
                op.getOp(),
                op.getFlags(),
                op.getRwFlags(),
                op.getFd(),
                op.getBufferAddress(),
                op.getLength(),
                op.getOffset(),
                opId,
                op.getBufIndex(),
                op.getFileIndex()
        );
    }

    void submitIo() {
        submissionQueue.submit(0);
    }

    boolean hasInKernel() {
        return submissionQueue.getTail() != completionQueue.getHead();
    }


    int getBufferLength(short bufRingId) {
        return bufRings.get(bufRingId).getBufferSize();
    }

    boolean isBufRingInitialized() {
        return bufRings != null;
    }

    boolean hasPending() {
        return submissionQueue.hasPending();
    }
}
