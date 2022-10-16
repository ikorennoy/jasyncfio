package one.jasyncfio;

import one.jasyncfio.collections.IntObjectHashMap;
import one.jasyncfio.collections.IntObjectMap;
import org.jctools.queues.MpscChunkedArrayQueue;

import java.util.Queue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

class EventExecutorImpl extends EventExecutor {
    private static final int STOP = 2;
    private static final int AWAKE = 1;
    private static final int WAIT = 0;

    private final ResultProvider<Integer> eventFdReadResultProvider = new ResultProvider<Integer>() {
        @Override
        public void onSuccess(int result) {
            EventExecutorImpl.this.addEventFdRead();
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
    private final Queue<Runnable> tasks = new MpscChunkedArrayQueue<>(65536);
    private final Ring sleepableRing;
    private final Ring pollRing;
    private final AtomicInteger state = new AtomicInteger(AWAKE);
    private final long eventFdBuffer = MemoryUtils.allocateMemory(8);
    private final int eventFd = Native.getEventFd();
    final IntObjectMap<Command<?>> commands;
    private final Thread t;

    private final long sleepTimeout;
    private long startWork = -1;

    private final IntSupplier sequencer = new IntSupplier() {
        private int i = 0;

        @Override
        public int getAsInt() {
            return i++;
        }
    };

    EventExecutorImpl(int entries,
                      boolean ioRingSetupSqPoll,
                      int sqThreadIdle,
                      boolean ioRingSetupSqAff,
                      int sqThreadCpu,
                      boolean ioRingSetupCqSize,
                      int cqSize,
                      boolean ioRingSetupClamp,
                      boolean ioRingSetupAttachWq,
                      int attachWqRingFd,
                      boolean withBufRing,
                      int numOfBuffers,
                      int bufRingBufSize,
                      long sleepTimeoutMs
    ) {
        this.commands = new IntObjectHashMap<>(entries);

        int flags = 0;
        if (ioRingSetupSqPoll) {
            flags |= Native.IORING_SETUP_SQPOLL;
        }
        if (ioRingSetupSqAff) {
            flags |= Native.IORING_SETUP_SQ_AFF;
        }
        if (ioRingSetupCqSize) {
            flags |= Native.IORING_SETUP_CQ_SIZE;
        }
        if (ioRingSetupClamp) {
            flags |= Native.IORING_SETUP_CLAMP;
        }
        if (ioRingSetupAttachWq) {
            flags |= Native.IORING_SETUP_ATTACH_WQ;
        }


        this.sleepTimeout = TimeUnit.NANOSECONDS.convert(sleepTimeoutMs, TimeUnit.MILLISECONDS);
        sleepableRing = new SleepableRing(entries, flags, sqThreadIdle, sqThreadCpu, cqSize, attachWqRingFd, withBufRing, bufRingBufSize, numOfBuffers, eventFd, eventFdBuffer, this, commands);
        pollRing = new PollRing(entries, flags | Native.IORING_SETUP_IOPOLL, sqThreadIdle, sqThreadCpu, cqSize, attachWqRingFd, withBufRing, bufRingBufSize, numOfBuffers, commands);

        this.t = new Thread(this::run, "EventExecutor");
    }

    void execute(Runnable task) {
        boolean inEventLoop = inEventLoop();
        if (inEventLoop) {
            safeExec(task);
        } else {
            addTask(task);
            wakeup(inEventLoop);
        }
    }

    private void wakeup(boolean inEventLoop) {
        if (!inEventLoop && state.getAndSet(AWAKE) != AWAKE) {
            unpark();
        }
    }

    private boolean inEventLoop() {
        return t == Thread.currentThread();
    }

    private void addTask(Runnable task) {
        if (state.get() != STOP) {
            tasks.add(task);
        } else {
            throw new RejectedExecutionException("Event loop is stopped");
        }
    }

    @Override
    public <T> T executeCommand(Command<T> command) {
        T resultHolder = command.getOperationResult();
        execute(command);
        return resultHolder;
    }

    @Override
    <T> long scheduleCommand(Command<T> command) {
        int id = sequencer.getAsInt();
        commands.put(id, command);
        return id;
    }


    @Override
    int bufRingId(PollableStatus pollableStatus) {
        if (!pollRing.isBufRingInitialized() && !sleepableRing.isBufRingInitialized()) {
            throw new IllegalStateException("Buf ring is not initialized");
        }
        final int id;
        if (PollableStatus.POLLABLE == pollableStatus) {
            id = pollRing.getBufRingId();
        } else {
            id = sleepableRing.getBufRingId();
        }
        return id;
    }

    @Override
    int getBufferLength(PollableStatus pollableStatus) {
        if (!pollRing.isBufRingInitialized() && !sleepableRing.isBufRingInitialized()) {
            throw new IllegalStateException("Buf ring is not initialized");
        }
        final int bufferSize;
        if (PollableStatus.POLLABLE == pollableStatus) {
            bufferSize = pollRing.getBufferLength();
        } else {
            bufferSize = sleepableRing.getBufferLength();
        }
        return bufferSize;
    }

    @Override
    <T> Ring ringFromCommand(Command<T> command) {
        final Ring result;
        if (command.getOp() == Native.IORING_OP_READ || command.getOp() == Native.IORING_OP_WRITE) {
            if (PollableStatus.POLLABLE == command.getPollableStatus()) {
                result = pollRing;
            } else {
                result = sleepableRing;
            }
        } else {
            result = sleepableRing;
        }
        return result;
    }

    private void run() {
        addEventFdRead();
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

    private void resetSleepTimeout() {
        startWork = System.nanoTime();
    }

    private boolean sleepTimeout() {
        return System.nanoTime() - startWork >= sleepTimeout;
    }

    @Override
    void addEventFdRead() {
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

    private boolean canSleep() {
        return !hasTasks() && !hasCompletions() && !pollRing.hasInKernel();
    }

    private void submitIo() {
        if (sleepableRing.hasPending()) {
            sleepableRing.submitIo();
        }
        if (pollRing.hasInKernel() || pollRing.hasPending()) {
            pollRing.submissionQueue.submit();
        }
    }

    private void submitTasksAndWait() {
        sleepableRing.park();
    }

    private void unpark() {
        sleepableRing.unpark();
    }

    @Override
    public void close() {
        if (state.getAndSet(STOP) == WAIT) {
            wakeup(inEventLoop());
        }
    }

    @Override
    public void start() {
        t.start();
    }

    private void closeRings() {
        sleepableRing.close();
        pollRing.close();
    }

    private void drain() {
        boolean moreWork = true;
        do {
            try {
                int processed = processAllCompletedTasks();
                boolean run = runAllTasks();
                moreWork = processed != 0 || run;
            } catch (Throwable r) {
                handleLoopException(r);
            }
        } while (moreWork);
        submitIo();
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

    private static void safeExec(Runnable task) {
        try {
            task.run();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    private int processAllCompletedTasks() {
        int result = 0;
        result += sleepableRing.processCompletedTasks();
        result += pollRing.processCompletedTasks();
        return result;
    }

    private void handleLoopException(Throwable t) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            t.printStackTrace();
            e.printStackTrace();
        }
    }

    private boolean hasCompletions() {
        return sleepableRing.hasCompletions() || pollRing.hasCompletions();
    }

    private boolean hasTasks() {
        return !tasks.isEmpty();
    }

    int sleepableRingFd() {
        return sleepableRing.ring.getRingFd();
    }

}
