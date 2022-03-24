package one.jasyncfio;

import one.jasyncfio.natives.CompletionQueue;
import one.jasyncfio.natives.MemoryUtils;
import one.jasyncfio.natives.SubmissionQueue;

public class SqPollEventExecutor extends AbstractEventExecutor {
    SqPollEventExecutor(int entries, int flags, int sqThreadIdle, int sqThreadCpu, int cqSize, int attachWqRingFd) {
        super(entries, flags, sqThreadIdle, sqThreadCpu, cqSize, attachWqRingFd);
    }

    @Override
    void run() {
        CompletionQueue completionQueue = ring.getCompletionQueue();
        SubmissionQueue submissionQueue = ring.getSubmissionQueue();

        for (;;) {
            try {
                submissionQueue.submitPooled();
                state.set(WAIT);
                if (!hasTasks() && !(completionQueue.hasCompletions() || submissionQueue.hasSubmitted())) {
                    while (state.get() == WAIT) {
                        MemoryUtils.park(false, 0);
                    }
                }
            } catch (Throwable t) {
                handleLoopException(t);
            }
            drain();
        }
    }

    @Override
    protected void wakeup(boolean inEventLoop) {
        if (!inEventLoop && state.get() != AWAKE) {
           state.set(AWAKE);
           MemoryUtils.unpark(t);
        }
    }
}
