package one.jasyncfio;

import java.util.List;

class EventExecutorImpl extends EventExecutor {

    private final Ring sleepableRing;

    protected EventExecutorImpl(
            int entries,
            boolean ioRingSetupSqPoll,
            int sqThreadIdle,
            boolean ioRingSetupSqAff,
            int sqThreadCpu,
            boolean ioRingSetupCqSize,
            int cqSize,
            boolean ioRingSetupClamp,
            boolean ioRingSetupAttachWq,
            int attachWqRingFd,
            List<BufRingDescriptor> bufRingDescriptorList,
            long sleepTimeoutMs,
            boolean monitoring
    ) {
        super(entries, monitoring, sleepTimeoutMs);

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

        sleepableRing = new SleepableRing(
                entries,
                flags,
                sqThreadIdle,
                sqThreadCpu,
                cqSize,
                attachWqRingFd,
                bufRingDescriptorList,
                eventFd,
                commands,
                monitoringEnabled,
                commandsStarts,
                commandExecutionDelays
        );
    }

    int sleepableRingFd() {
        return sleepableRing.ring.getRingFd();
    }

    @Override
    protected void unpark() {
        sleepableRing.unpark();
    }

    @Override
    protected <T> Ring ringFromCommand(Command<T> command) {
        return sleepableRing;
    }

    @Override
    protected int getBufferLength(PollableStatus pollableStatus, short bufRingId) {
        if (!sleepableRing.isBufRingInitialized()) {
            throw new IllegalStateException("Buf ring is not initialized");
        }
        return sleepableRing.getBufferLength(bufRingId);
    }

    @Override
    protected int processAllCompletedTasks() {
        return sleepableRing.processCompletedTasks();
    }

    @Override
    protected void submitIo() {
        if (sleepableRing.hasPending()) {
            sleepableRing.submitIo();
        }
    }

    @Override
    protected boolean canSleep() {
        return !hasTasks() && !hasCompletions();
    }

    @Override
    protected void closeRings() {
        sleepableRing.close();
    }

    private boolean hasCompletions() {
        return sleepableRing.hasCompletions();
    }

    protected void submitTasksAndWait() {
        sleepableRing.park();
    }
}
