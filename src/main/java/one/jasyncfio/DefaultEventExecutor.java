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
            drain(0);
        }
    }


    @Override
    protected void wakeup(boolean inEventLoop) {
        if (!inEventLoop && state.get() != AWAKE) {
            // write to the eventfd which will then wake-up submitAndWait
            Native.eventFdWrite(eventFd, 1L);
        }
    }
}
