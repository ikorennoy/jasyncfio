package one.jasyncfio;

import java.util.List;

class PollEventExecutorImpl extends EventExecutor {

    private final Ring sleepableRing;
    private final Ring pollRing;

    PollEventExecutorImpl(int entries,
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
        pollRing = new PollRing(
                entries,
                flags | Native.IORING_SETUP_IOPOLL,
                sqThreadIdle,
                sqThreadCpu,
                cqSize,
                attachWqRingFd,
                bufRingDescriptorList,
                commands,
                monitoringEnabled,
                commandsStarts,
                commandExecutionDelays
        );
    }

    @Override
    int getBufferLength(PollableStatus pollableStatus, short bufRingId) {
        if (!pollRing.isBufRingInitialized() && !sleepableRing.isBufRingInitialized()) {
            throw new IllegalStateException("Buf ring is not initialized");
        }
        final int bufferSize;
        if (PollableStatus.POLLABLE == pollableStatus) {
            bufferSize = pollRing.getBufferLength(bufRingId);
        } else {
            bufferSize = sleepableRing.getBufferLength(bufRingId);
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

    void run() {
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

    void submitIo() {
        if (sleepableRing.hasPending()) {
            sleepableRing.submitIo();
        }
        if (pollRing.hasInKernel() || pollRing.hasPending()) {
            pollRing.submissionQueue.submit();
        }
    }

    void unpark() {
        sleepableRing.unpark();
    }

    @Override
    int processAllCompletedTasks() {
        int result = 0;
        result += sleepableRing.processCompletedTasks();
        result += pollRing.processCompletedTasks();
        return result;
    }

    int sleepableRingFd() {
        return sleepableRing.ring.getRingFd();
    }

    private boolean hasCompletions() {
        return sleepableRing.hasCompletions() || pollRing.hasCompletions();
    }

    private void closeRings() {
        sleepableRing.close();
        pollRing.close();
    }

    private void submitTasksAndWait() {
        sleepableRing.park();
    }

    private boolean canSleep() {
        return !hasTasks() && !hasCompletions() && !pollRing.hasInKernel();
    }
}
