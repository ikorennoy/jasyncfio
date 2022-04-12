package one.jasyncfio;

import one.jasyncfio.collections.IntObjectHashMap;
import one.jasyncfio.natives.*;
import org.jctools.queues.MpscChunkedArrayQueue;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class EventExecutor {
    private static final int STOP = 2;
    private static final int AWAKE = 1;
    private static final int WAIT = 0;

    final AtomicInteger state = new AtomicInteger(AWAKE);
    final Queue<ExtRunnable> tasks = new MpscChunkedArrayQueue<>(131073);
    final IntObjectHashMap<CompletableFuture<Integer>> futures = new IntObjectHashMap<>(1024);
    final CompletionCallback callback = this::handle;
    final Uring ring;
    final PrimitiveIntSupplier sequencer;
    final Thread t;

    EventExecutor(int entries, int flags, int sqThreadIdle, int sqThreadCpu, int cqSize, int attachWqRingFd) {
        sequencer = new PrimitiveIntSupplier() {
            private int i = 0;

            @Override
            public int getAsInt() {
                return Math.abs(i++ % 16_777_215);
            }
        };
        ring = Native.setupIoUring(entries, flags, sqThreadIdle, sqThreadCpu, cqSize, attachWqRingFd);
        t = new Thread(this::run);
        t.start();
    }

    private interface PrimitiveIntSupplier {
        int getAsInt();
    }

    void run() {
        CompletionQueue completionQueue = ring.getCompletionQueue();
        SubmissionQueue submissionQueue = ring.getSubmissionQueue();

        while (true) {
            try {
                submissionQueue.submit();
                state.compareAndSet(AWAKE, WAIT);
                if (!hasTasks() && !(completionQueue.hasCompletions() || (submissionQueue.getTail() != completionQueue.getHead()))) {
                    while (state.get() == WAIT) {
                        LockSupport.park();
                    }
                }
            } catch (Throwable t) {
                handleLoopException(t);
            } finally {
                state.compareAndSet(WAIT, AWAKE);
            }
            drain();
            if (state.get() == STOP) {
                drain();
                closeRing();
                break;
            }
        }
    }

    private void closeRing() {
        ring.close();
    }

    void execute(ExtRunnable task) {
        boolean inEventLoop = inEventLoop();
        addTask(task);
        wakeup(inEventLoop);
    }

    void wakeup(boolean inEventLoop) {
        int localState = state.get();
        if (!inEventLoop && (localState != AWAKE && state.compareAndSet(WAIT, AWAKE))) {
            LockSupport.unpark(t);
        }
    }

    void stop() {
        if (state.getAndSet(STOP) == WAIT) {
            LockSupport.unpark(t);
        }
    }

    boolean hasTasks() {
        return !tasks.isEmpty();
    }

    boolean runAllTasks() {
        ExtRunnable t = tasks.poll();
        if (t == null) {
            return false;
        }
        while (true) {
            safeExec(t);
            t = tasks.poll();
            if (t == null) {
                return true;
            }
        }
    }

    void drain() {
        boolean moreWork = true;
        do {
            try {
                int processed = ring.getCompletionQueue().processEvents(callback);
                boolean run = runAllTasks();
                moreWork = processed != 0 || run;
            } catch (Throwable t) {
                handleLoopException(t);
            }
        } while (moreWork);
    }


    static void handleLoopException(Throwable t) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            // ignore
        }
    }

    CompletableFuture<Integer> scheduleNoop() {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        execute(() -> {
            int opId = sequencer.getAsInt();
            futures.put(opId, f);
            ring.getSubmissionQueue().addNoOp(opId);
        });
        return f;
    }

    CompletableFuture<Integer> scheduleRead(int fd, long bufferAddress, long offset, int length) {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        execute(() -> {
            int opId = sequencer.getAsInt();
            futures.put(opId, f);
            ring.getSubmissionQueue().addRead(fd, bufferAddress, offset, length, opId);
        });
        return f;
    }

    CompletableFuture<Integer> scheduleWrite(int fd, long bufferAddress, long offset, int length) {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        execute(() -> {
            int opId = sequencer.getAsInt();
            futures.put(opId, f);
            ring.getSubmissionQueue().addWrite(fd, bufferAddress, offset, length, opId);
        });
        return f;
    }

    CompletableFuture<Integer> scheduleWritev(int fd, long iovecArrAddress, long offset, int iovecArrSize) {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        execute(() -> {
            int opId = sequencer.getAsInt();
            futures.put(opId, f);
            ring.getSubmissionQueue().addWritev(fd, iovecArrAddress, offset, iovecArrSize, opId);
        });
        return f;
    }

    CompletableFuture<Integer> scheduleReadv(int fd, long iovecArrAddress, long offset, int iovecArrSize) {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        execute(() -> {
            int opId = sequencer.getAsInt();
            futures.put(opId, f);
            ring.getSubmissionQueue().addReadv(fd, iovecArrAddress, offset, iovecArrSize, opId);
        });
        return f;
    }

    CompletableFuture<Integer> scheduleWriteFixed(int fd, long buffAddress, long offset, int length, int bufIndex) {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        execute(() -> {
            int opId = sequencer.getAsInt();
            futures.put(opId, f);
            ring.getSubmissionQueue().addWriteFixed(fd, buffAddress, offset, length, bufIndex, opId);
        });
        return f;
    }

    public CompletableFuture<Integer> scheduleReadFixed(int fd, long buffAddress, long offset, int length, int bufIndex) {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        execute(() -> {
            int opdId = sequencer.getAsInt();
            futures.put(opdId, f);
            ring.getSubmissionQueue().addReadFixed(fd, buffAddress, offset, length, bufIndex, opdId);
        });
        return f;
    }


    CompletableFuture<Integer> scheduleOpenAt(int dirFd, long pathAddress, int openFlags, int mode) {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        execute(() -> {
            int opId = sequencer.getAsInt();
            futures.put(opId, f);
            ring.getSubmissionQueue().addOpenAt(dirFd, pathAddress, openFlags, mode, opId);
        });
        return f;
    }

    CompletableFuture<Integer> scheduleStatx(int dirFd, long pathAddress, int statxFlags, int statxMask, long statxBufferAddress) {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        execute(() -> {
            int opId = sequencer.getAsInt();
            futures.put(opId, f);
            ring.getSubmissionQueue().addStatx(dirFd, pathAddress, statxFlags, statxMask, statxBufferAddress, opId);
        });
        return f;
    }

    CompletableFuture<Integer> scheduleFsync(int fd, int fsyncFlags) {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        execute(() -> {
            int opId = sequencer.getAsInt();
            futures.put(opId, f);
            ring.getSubmissionQueue().addFsync(fd, fsyncFlags, opId);
        });
        return f;
    }

    CompletableFuture<Integer> scheduleFallocate(int fd, long length, int mode, long offset) {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        execute(() -> {
            int opId = sequencer.getAsInt();
            futures.put(opId, f);
            ring.getSubmissionQueue().addFallocate(fd, length, mode, offset, opId);
        });
        return f;
    }

    CompletableFuture<Integer> scheduleUnlink(int dirFd, long pathAddress, int flags) {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        execute(() -> {
            int opId = sequencer.getAsInt();
            futures.put(opId, f);
            ring.getSubmissionQueue().addUnlinkAt(dirFd, pathAddress, flags, opId);
        });
        return f;
    }

    CompletableFuture<Integer> scheduleRename(int oldDirFd, long oldPathAddress, int newDirFd, int newPathAddress, int flags) {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        execute(() -> {
            int opId = sequencer.getAsInt();
            futures.put(opId, f);
            ring.getSubmissionQueue().addRenameAt(oldDirFd, oldPathAddress, newDirFd, newPathAddress, flags, opId);
        });
        return f;
    }

    CompletableFuture<Integer> scheduleClose(int fd) {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        execute(() -> {
            int opId = sequencer.getAsInt();
            futures.put(opId, f);
            ring.getSubmissionQueue().addClose(fd, opId);
        });
        return f;
    }

    CompletableFuture<Void> registerBuffers(IovecArray buffers) {
        return CompletableFuture.runAsync(() ->
                Native.ioUringRegister(ring.getRingFd(), Native.IORING_REGISTER_BUFFERS, buffers.getIovecArrayAddress(), buffers.getCount()));
    }

    CompletableFuture<Void> unregisterBuffers() {
        return CompletableFuture.runAsync(() ->
                Native.ioUringRegister(ring.getRingFd(), Native.IORING_UNREGISTER_BUFFERS, -1, 0));
    }

    CompletableFuture<Void> registerFiles(IntBuffer fds, int size) {
        return CompletableFuture.runAsync(() ->
                Native.ioUringRegister(ring.getRingFd(), Native.IORING_REGISTER_FILES, MemoryUtils.getDirectBufferAddress(fds), size));
    }

    CompletableFuture<Void> unregisterFiles() {
        return CompletableFuture.runAsync(() ->
                Native.ioUringRegister(ring.getRingFd(), Native.IORING_UNREGISTER_FILES, -1, 0));
    }

    private void handle(int fd, int res, int flags, byte op, int data) {
        CompletableFuture<Integer> userCallback = futures.get(data);
        if (userCallback != null) {
            if (res >= 0) {
                userCallback.complete(res);
            } else {
                userCallback.completeExceptionally(
                        new IOException(String.format("Error code: %d; message: %s", -res, Native.decodeErrno(res))));
            }
        }
    }

    private void addTask(ExtRunnable task) {
        if (state.get() != STOP) {
            tasks.add(task);
        } else {
            throw new RejectedExecutionException("Event loop is stopped");
        }
    }

    private static void safeExec(ExtRunnable task) {
        try {
            task.run();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    private boolean inEventLoop() {
        return t == Thread.currentThread();
    }
}
