package one.jasyncfio;

import one.jasyncfio.collections.IntObjectMap;

import java.io.IOException;
import java.nio.ByteBuffer;

abstract class Ring {
    final Uring ring;
    final CompletionQueue completionQueue;
    final SubmissionQueue submissionQueue;
    private final IntObjectMap<Command<?>> commands;
    private final CompletionCallback callback = this::handle;

    private final IoUringBufRing bufRing;

    Ring(int entries, int flags, int sqThreadIdle, int sqThreadCpu, int cqSize, int attachWqRingFd, boolean withBufRing, int bufRingBufSize, int numOfBuffers, IntObjectMap<Command<?>> commands) {
        this.commands = commands;
        ring = Native.setupIoUring(entries, flags, sqThreadIdle, sqThreadCpu, cqSize, attachWqRingFd);
        submissionQueue = ring.getSubmissionQueue();
        completionQueue = ring.getCompletionQueue();

        if (withBufRing) {
            bufRing = new IoUringBufRing(ring.getRingFd(), bufRingBufSize, numOfBuffers);
        } else {
            bufRing = null;
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
                    ByteBuffer buffer = bufRing.getBuffer(bufferId);
                    buffer.position(res);
                    command.complete(new BufRingResult(buffer, res, bufferId, this));
                } else {
                    command.complete(res);
                }
            } else {
                command.error(new IOException(String.format("Error code: %d; message: %s", -res, Native.decodeErrno(res))));
            }
        }
    }

    void close() {
        ring.close();
        bufRing.close();
    }

    abstract void park();

    abstract void unpark();

    boolean hasCompletions() {
        return completionQueue.hasCompletions();
    }

    int processCompletedTasks() {
        return completionQueue.processEvents(callback);
    }

    void recycleBuffer(int bufferId) {
        bufRing.recycleBuffer(bufferId);
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

    int getBufRingId() {
        return bufRing.getId();
    }

    int getBufferLength() {
        return bufRing.getBufferSize();
    }

    boolean isBufRingInitialized() {
        return bufRing != null;
    }

    boolean hasPending() {
        return submissionQueue.hasPending();
    }
}
