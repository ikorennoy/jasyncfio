package one.jasyncfio;

import one.jasyncfio.natives.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.function.IntSupplier;

public class EventLoop {
    static final boolean AWAKE = true;
    static final boolean WAIT = false;

    final AtomicBoolean state = new AtomicBoolean(WAIT);
    final Queue<ExtRunnable> tasks = new ConcurrentLinkedDeque<>();
    final Map<Integer, CompletableFuture<Integer>> pendingFutures = new HashMap<>();
    final CompletionCallback callback = this::handle;
    final Uring ring;
    final IntSupplier sequencer;
    final Thread t;

    EventLoop(int entries, int flags, int sqThreadIdle, int sqThreadCpu, int cqSize, int attachWqRingFd) {
        sequencer = new IntSupplier() {
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

    void run() {
        CompletionQueue completionQueue = ring.getCompletionQueue();
        SubmissionQueue submissionQueue = ring.getSubmissionQueue();

        for (;;) {
            try {
                submissionQueue.submit();
                state.set(WAIT);
                if (!hasTasks() && !(completionQueue.hasCompletions() || (submissionQueue.getTail() != completionQueue.getHead()))) {
                    while (state.get() == WAIT) {
                        LockSupport.park();
                    }
                }
            } catch (Throwable t) {
                handleLoopException(t);
            } finally {
                state.set(AWAKE);
            }
            drain();
        }
    }


    private void handle(int fd, int res, int flags, byte op, int data) {
        CompletableFuture<Integer> userCallback = pendingFutures.remove(data);
        if (userCallback != null) {
            if (res >= 0) {
                userCallback.complete(res);
            } else {
                userCallback.completeExceptionally(
                        new IOException(String.format("Error code: %d; message: %s", -res, Native.decodeErrno(res))));
            }
        }
    }

    public void execute(ExtRunnable task) {
        boolean inEventLoop = inEventLoop();
        addTask(task);
        wakeup(inEventLoop);
    }

    private void addTask(ExtRunnable task) {
        tasks.add(task);
    }

    protected void wakeup(boolean inEventLoop) {
        boolean localState = state.get();
        if (!inEventLoop && (localState != AWAKE && state.compareAndSet(WAIT, AWAKE))) {
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
        for (;;) {
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

    protected static void handleLoopException(Throwable t) {
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
            pendingFutures.put(opId, f);
            ring.getSubmissionQueue().addNoOp(opId);
        });
        return f;
    }

    public CompletableFuture<Integer> scheduleRead(int fd, long bufferAddress, long offset, int length) {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        execute(() -> {
            int opId = sequencer.getAsInt();
            pendingFutures.put(opId, f);
            ring.getSubmissionQueue().addRead(fd, bufferAddress, offset, length, opId);
        });
        return f;
    }

    public CompletableFuture<Integer> scheduleWrite(int fd, long bufferAddress, long offset, int length) {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        execute(() -> {
            int opId = sequencer.getAsInt();
            pendingFutures.put(opId, f);
            ring.getSubmissionQueue().addWrite(fd, bufferAddress, offset, length, opId);
        });
        return f;
    }

    public CompletableFuture<Integer> scheduleWritev(int fd, long iovecArrAddress, long offset, int iovecArrSize) {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        execute(() -> {
            int opId = sequencer.getAsInt();
            pendingFutures.put(opId, f);
            ring.getSubmissionQueue().addWritev(fd, iovecArrAddress, offset, iovecArrSize, opId);
        });
        return f;
    }

    public CompletableFuture<Integer> scheduleReadv(int fd, long iovecArrAddress, long offset, int iovecArrSize) {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        execute(() -> {
            int opId = sequencer.getAsInt();
            pendingFutures.put(opId, f);
            ring.getSubmissionQueue().addReadv(fd, iovecArrAddress, offset, iovecArrSize, opId);
        });
        return f;
    }

    public CompletableFuture<Integer> scheduleOpenAt(int dirFd, long pathAddress, int openFlags, int mode) {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        execute(() -> {
            int opId = sequencer.getAsInt();
            pendingFutures.put(opId, f);
            ring.getSubmissionQueue().addOpenAt(dirFd, pathAddress, openFlags, mode, opId);
        });
        return f;
    }

    public CompletableFuture<Integer> scheduleStatx(int dirFd, long pathAddress, int statxFlags, int statxMask, long statxBufferAddress) {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        execute(() -> {
            int opId = sequencer.getAsInt();
            pendingFutures.put(opId, f);
            ring.getSubmissionQueue().addStatx(dirFd, pathAddress, statxFlags, statxMask, statxBufferAddress, opId);
        });
        return f;
    }

    public CompletableFuture<Integer> scheduleFsync(int fd, int fsyncFlags) {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        execute(() -> {
            int opId = sequencer.getAsInt();
            pendingFutures.put(opId, f);
            ring.getSubmissionQueue().addFsync(fd, fsyncFlags, opId);
        });
        return f;
    }

    public CompletableFuture<Integer> scheduleFallocate(int fd, long length, int mode, long offset) {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        execute(() -> {
            int opId = sequencer.getAsInt();
            pendingFutures.put(opId, f);
            ring.getSubmissionQueue().addFallocate(fd, length, mode, offset, opId);
        });
        return f;
    }

    public CompletableFuture<Integer> scheduleUnlink(int dirFd, long pathAddress, int flags) {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        execute(() -> {
            int opId = sequencer.getAsInt();
            pendingFutures.put(opId, f);
            ring.getSubmissionQueue().addUnlinkAt(dirFd, pathAddress, flags, opId);
        });
        return f;
    }

    public CompletableFuture<Integer> scheduleRename(int oldDirFd, long oldPathAddress, int newDirFd, int newPathAddress, int flags) {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        execute(() -> {
            int opId = sequencer.getAsInt();
            pendingFutures.put(opId, f);
            ring.getSubmissionQueue().addRenameAt(oldDirFd, oldPathAddress, newDirFd, newPathAddress, flags, opId);
        });
        return f;
    }

    public CompletableFuture<Integer> scheduleClose(int fd) {
        CompletableFuture<Integer> f = new CompletableFuture<>();
        execute(() -> {
            int opId = sequencer.getAsInt();
            pendingFutures.put(opId, f);
            ring.getSubmissionQueue().addClose(fd, opId);
        });
        return f;
    }
}
