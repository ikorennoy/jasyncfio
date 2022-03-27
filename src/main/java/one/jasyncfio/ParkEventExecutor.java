package one.jasyncfio;

import one.jasyncfio.natives.CompletionQueue;
import one.jasyncfio.natives.SubmissionQueue;

import java.util.concurrent.locks.LockSupport;

public class ParkEventExecutor extends AbstractEventExecutor {
    ParkEventExecutor(int entries, int flags, int sqThreadIdle, int sqThreadCpu, int cqSize, int attachWqRingFd) {
        super(entries, flags, sqThreadIdle, sqThreadCpu, cqSize, attachWqRingFd);
    }

    @Override
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

    @Override
    protected void wakeup(boolean inEventLoop) {
        boolean localState = state.get();
        if (!inEventLoop && (localState != AWAKE && state.compareAndSet(WAIT, AWAKE))) {
            LockSupport.unpark(t);
        }
    }
}
