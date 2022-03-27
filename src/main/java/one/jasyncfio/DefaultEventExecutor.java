package one.jasyncfio;

import one.jasyncfio.natives.CompletionQueue;
import one.jasyncfio.natives.Native;
import one.jasyncfio.natives.SubmissionQueue;

class DefaultEventExecutor extends AbstractEventExecutor {
    DefaultEventExecutor(int entries, int flags, int sqThreadIdle, int sqThreadCpu, int cqSize, int attachWqRingFd) {
        super(entries, flags, sqThreadIdle, sqThreadCpu, cqSize, attachWqRingFd);
    }

    @Override
    void run() {
        CompletionQueue completionQueue = ring.getCompletionQueue();
        SubmissionQueue submissionQueue = ring.getSubmissionQueue();

        addEventFdRead(submissionQueue);
        for (;;) {
            try {
                state.set(WAIT);
                if (!hasTasks() && !completionQueue.hasCompletions()) {
                    submissionQueue.submitAndWait();
                }
            } catch (Throwable t) {
                handleLoopException(t);
            } finally {
                state.set(AWAKE);
            }
            drain();
        }
    }

    @Override
    protected void wakeup(boolean inEventLoop) {
        boolean localState = state.get();
        if (!inEventLoop && (localState != AWAKE && state.compareAndSet(WAIT, AWAKE))) {
            Native.eventFdWrite(eventFd, 1L);
        }
    }
}
