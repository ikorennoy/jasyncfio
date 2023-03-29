package one.jasyncfio;

import com.tdunning.math.stats.TDigest;
import one.jasyncfio.collections.IntObjectHashMap;
import one.jasyncfio.collections.IntObjectMap;
import org.jctools.queues.MpscChunkedArrayQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

public abstract class EventExecutor implements AutoCloseable {

    static final int STOP = 2;
    static final int AWAKE = 1;
    static final int WAIT = 0;

    final TDigest commandExecutionDelays = TDigest.createDigest(100.0);
    final AtomicInteger state = new AtomicInteger(AWAKE);
    final int eventFd = Native.getEventFd();
    final ConcurrentMap<Command<?>, Long> commandsStarts = new ConcurrentHashMap<>();

    private final ResultProvider<Integer> eventFdReadResultProvider = new ResultProvider<Integer>() {
        @Override
        public void onSuccess(int result) {
            addEventFdRead();
        }

        @Override
        public void onSuccess(Object object) {

        }

        @Override
        public void onError(Throwable ex) {

        }

        @Override
        public Integer getInner() {
            return null;
        }

        @Override
        public void release() {

        }
    };
    private final IntSupplier sequencer = new IntSupplier() {
        private int i = 0;

        @Override
        public int getAsInt() {
            return i++;
        }
    };
    private final Queue<Runnable> tasks = new MpscChunkedArrayQueue<>(65536);
    private final long eventFdBuffer = MemoryUtils.allocateMemory(8);

    final long sleepTimeout;
    final boolean monitoringEnabled;
    private final Thread t;
    final IntObjectMap<Command<?>> commands;

    long startWork = -1;

    protected EventExecutor(int entries, boolean monitoringEnabled, long sleepTimeout) {
        this.monitoringEnabled = monitoringEnabled;
        this.sleepTimeout = sleepTimeout;

        this.commands = new IntObjectHashMap<>(entries);
        this.t = new Thread(this::run, "EventExecutor");
    }

    public CompletableFuture<double[]> getCommandExecutionLatencies(double[] percentiles) {
        return getLatencies(percentiles, commandExecutionDelays);
    }

    @Override
    public void close() {
        if (state.getAndSet(STOP) == WAIT) {
            wakeup(inEventLoop());
        }
    }

    <T> T executeCommand(Command<T> command) {
        if (monitoringEnabled) {
            commandsStarts.put(command, Native.getCpuTimer());
        }
        T resultHolder = command.getOperationResult();
        execute(command);
        return resultHolder;
    }

    <T> long scheduleCommand(Command<T> command) {
        int id = sequencer.getAsInt();
        commands.put(id, command);
        return id;
    }

    boolean hasTasks() {
        return !tasks.isEmpty();
    }

    private void execute(Runnable task) {
        boolean inEventLoop = inEventLoop();
        if (inEventLoop) {
            safeExec(task);
        } else {
            addTask(task);
            wakeup(inEventLoop);
        }
    }

    private void wakeup(boolean inEventLoop) {
        int localState = state.get();
        if (!inEventLoop && (localState != AWAKE && state.compareAndSet(WAIT, AWAKE))) {
            unpark();
        }
    }

    private boolean runAllTasks() {
        Runnable task = tasks.poll();
        if (task == null) {
            return false;
        }
        while (true) {
            safeExec(task);
            task = tasks.poll();
            if (task == null) {
                return true;
            }
        }
    }

    private void drain() {
        boolean moreWork = true;
        do {
            try {
                boolean run = runAllTasks();
                if (run) {
                    submitIo();
                }
                int processed = processAllCompletedTasks();
                moreWork = processed != 0 || run;
            } catch (Throwable r) {
                handleLoopException(r);
            }
        } while (moreWork);
    }

    private void resetSleepTimeout() {
        startWork = System.nanoTime();
    }

    private boolean sleepTimeout() {
        return System.nanoTime() - startWork >= sleepTimeout;
    }

    private void handleLoopException(Throwable t) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            t.printStackTrace();
            e.printStackTrace();
        }
    }

    private boolean inEventLoop() {
        return t == Thread.currentThread();
    }

    private void addEventFdRead() {
        executeCommand(Command.read(
                eventFd,
                0,
                8,
                eventFdBuffer,
                PollableStatus.NON_POLLABLE,
                this,
                eventFdReadResultProvider
        ));
    }

    private void run() {
        addEventFdRead();
        resetSleepTimeout();
        while (true) {
            try {
                state.set(WAIT);
                if (canSleep()) {
                    if (sleepTimeout()) {
                        submitTasksAndWait();
                        resetSleepTimeout();
                    }
                }
            } catch (Throwable t) {
                handleLoopException(t);
            } finally {
                state.set(AWAKE);
            }
            drain();
            if (state.get() == STOP) {
                while (!canSleep()) {
                    // make sure we proceed all tasks, submit all submissions and wait all completions
                    drain();
                }
                closeRings();
                break;
            }
        }
    }

    private void start() {
        t.start();
    }

    private void addTask(Runnable task) {
        if (state.get() != STOP) {
            tasks.add(task);
        } else {
            throw new RejectedExecutionException("Event loop is stopped");
        }
    }

    private static void safeExec(Runnable task) {
        try {
            task.run();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    private CompletableFuture<double[]> getLatencies(double[] percentiles, TDigest digest) {
        if (!monitoringEnabled) {
            throw new IllegalStateException("monitoring is not enabled");
        }
        CompletableFuture<double[]> f = new CompletableFuture<>();
        execute(() -> {
            double[] res = new double[percentiles.length];
            for (int i = 0; i < percentiles.length; i++) {
                res[i] = digest.quantile(percentiles[i]);
            }
            f.complete(res);
        });
        return f;
    }

    protected abstract void unpark();

    protected abstract <T> Ring ringFromCommand(Command<T> command);

    protected abstract int getBufferLength(PollableStatus pollableStatus, short bufRingId);

    protected abstract boolean canSleep();

    protected abstract void submitTasksAndWait();

    protected abstract void closeRings();

    protected abstract int processAllCompletedTasks();

    protected abstract void submitIo();

    public static class Builder {
        private int entries = 4096;
        private boolean ioRingSetupSqPoll = false;
        private int sqThreadIdle = 0;
        private boolean ioRingSetupSqAff = false;
        private int sqThreadCpu = 0;
        private boolean ioRingSetupCqSize = false;
        private int cqSize = 0;
        private boolean ioRingSetupClamp = false;
        private boolean ioRingSetupAttachWq = false;
        private int attachWqRingFd = 0;
        private long sleepTimeoutMs = 1000;

        private final List<BufRingDescriptor> bufRingDescriptors = new ArrayList<>();
        private boolean ioPoll;
        private boolean monitoring = false;

        private Builder() {
        }

        /**
         * Denotes the number of Submission Queue Entries that will be associated with this io_uring instance.
         */
        public Builder entries(int entries) {
            this.entries = entries;
            return this;
        }

        /**
         * When this flag is specified, a kernel thread is created to perform submission queue polling.
         * An io_uring instance configured in this way enables an application to issue I/O without ever context switching into the kernel.
         * By using the submission queue to fill in new submission queue entries and watching for completions on the completion queue,
         * the application can submit and reap I/Os without doing a single system call.
         *
         * @param sqThreadIdleMillis max kernel thread idle time in milliseconds
         */

        public Builder ioRingSetupSqPoll(int sqThreadIdleMillis) {
            if (sqThreadIdleMillis < 0) {
                throw new IllegalArgumentException("sqThreadIdleMillis < 0");
            }
            this.ioRingSetupSqPoll = true;
            this.sqThreadIdle = sqThreadIdleMillis;
            return this;
        }

        /**
         * If this flag is specified, then the poll thread will be bound to the cpu set in the sq_thread_cpu field of the struct io_uring_params.
         * This flag is only meaningful when IORING_SETUP_SQPOLL is specified.
         * When cgroup setting cpuset.cpus changes (typically in container environment), the bounded cpu set may be changed as well.
         *
         * @param sqThreadCpu cpu to bound to
         */
        public Builder ioRingSetupSqAff(int sqThreadCpu) {
            this.ioRingSetupSqAff = true;
            this.sqThreadCpu = sqThreadCpu;
            return this;
        }

        /**
         * Create the completion queue with struct io_uring_params.cq_entries entries.
         * The value must be greater than entries, and may be rounded up to the next power-of-two.
         */
        public Builder ioRingSetupCqSize(int cqSize) {
            if (cqSize < 0) {
                throw new IllegalArgumentException("cqSize < 0");
            }
            this.ioRingSetupCqSize = true;
            this.cqSize = cqSize;
            return this;
        }

        /**
         * If this flag is specified, and if entries exceeds IORING_MAX_ENTRIES, then entries will be clamped at IORING_MAX_ENTRIES.
         * If the flag IORING_SETUP_SQPOLL is set, and if the value of struct io_uring_params.cq_entries exceeds IORING_MAX_CQ_ENTRIES,
         * then it will be clamped at IORING_MAX_CQ_ENTRIES.
         */
        public Builder ioRingSetupClamp() {
            this.ioRingSetupClamp = true;
            return this;
        }

        /**
         * This flag should be set in conjunction with struct io_uring_params.wq_fd being set to an existing io_uring ring file descriptor.
         * When set, the io_uring instance being created will share the asynchronous worker thread backend of the specified io_uring ring,
         * rather than create a new separate thread pool.
         *
         * @param attachWqRingFd existing io_uring ring fd.
         */
        public Builder ioRingSetupAttachWq(int attachWqRingFd) {
            this.ioRingSetupAttachWq = true;
            this.attachWqRingFd = attachWqRingFd;
            return this;
        }

        /**
         * Setup buf ring with provided parameters. Later if you want to read to thus buf ring you must specify
         * the bufRingId provided for this call
         *
         * @param bufRingSize    number of buffers in the ring, must be power of 2
         * @param bufRingBufSize buffer size
         * @param bufRingId      id, which is used during the read request
         */
        public Builder addBufRing(int bufRingSize, int bufRingBufSize, short bufRingId) {
            if (bufRingBufSize <= 0) {
                throw new IllegalArgumentException("bufRingBufSize must be positive");
            }
            if (bufRingSize <= 0 || !isPowerOfTwo(bufRingSize)) {
                throw new IllegalArgumentException("bufRingSize must be positive and power of 2");
            }
            bufRingDescriptors.add(new BufRingDescriptor(bufRingSize, bufRingBufSize, bufRingId));
            return this;
        }

        /**
         * The time after which the EventLoop thread will be put to sleep. The higher the value, the higher
         * the CPU consumption, but the lower the latency, the lower the CPU consumption and the higher the latency.
         * Default value is 1000 milliseconds
         *
         * @param sleepTimeoutMs sleep timeout in milliseconds
         */
        public Builder sleepTimeout(long sleepTimeoutMs) {
            this.sleepTimeoutMs = sleepTimeoutMs;
            return this;
        }

        /**
         * Perform busy-waiting for an I/O completion, as opposed to getting notifications via an asynchronous IRQ (Interrupt Request).
         * The file system (if any) and block device must support polling in order for this to work.
         * Busy-waiting provides lower latency, but may consume more CPU resources than interrupt driven I/O.
         */
        public Builder ioRingSetupIoPoll() {
            this.ioPoll = true;
            return this;
        }

        /**
         * Enable latencies monitoring.
         */
        public Builder monitoring() {
            this.monitoring = true;
            return this;
        }

        public EventExecutor build() {
            if (entries > 4096 || !isPowerOfTwo(entries)) {
                throw new IllegalArgumentException("entries must be power of 2 and less than 4096");
            }
            if (ioRingSetupCqSize && cqSize < entries) {
                throw new IllegalArgumentException("cqSize must be greater than entries");
            }
            if (ioRingSetupSqAff && !ioRingSetupSqPoll) {
                throw new IllegalArgumentException("IORING_SETUP_SQ_AFF is only meaningful when IORING_SETUP_SQPOLL is specified");
            }

            final EventExecutor executor;

            if (ioPoll) {
                executor = new PollEventExecutorImpl(entries,
                        ioRingSetupSqPoll,
                        sqThreadIdle,
                        ioRingSetupSqAff,
                        sqThreadCpu,
                        ioRingSetupCqSize,
                        cqSize,
                        ioRingSetupClamp,
                        ioRingSetupAttachWq,
                        attachWqRingFd,
                        bufRingDescriptors,
                        sleepTimeoutMs,
                        monitoring
                );
            } else {
                executor = new EventExecutorImpl(entries,
                        ioRingSetupSqPoll,
                        sqThreadIdle,
                        ioRingSetupSqAff,
                        sqThreadCpu,
                        ioRingSetupCqSize,
                        cqSize,
                        ioRingSetupClamp,
                        ioRingSetupAttachWq,
                        attachWqRingFd,
                        bufRingDescriptors,
                        sleepTimeoutMs,
                        monitoring
                );
            }
            executor.start();
            return executor;
        }

    }

    public static EventExecutor initDefault() {
        return new Builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    private static boolean isPowerOfTwo(int x) {
        return (x != 0) && ((x & (x - 1)) == 0);
    }
}
