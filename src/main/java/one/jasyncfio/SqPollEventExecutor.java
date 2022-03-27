package one.jasyncfio;

import one.jasyncfio.natives.CompletionQueue;
import one.jasyncfio.natives.SubmissionQueue;

import java.util.concurrent.locks.LockSupport;

public class SqPollEventExecutor extends AbstractEventExecutor {
    SqPollEventExecutor(int entries, int flags, int sqThreadIdle, int sqThreadCpu, int cqSize, int attachWqRingFd) {
        super(entries, flags, sqThreadIdle, sqThreadCpu, cqSize, attachWqRingFd);
    }

    @Override
    void run() {
        CompletionQueue completionQueue = ring.getCompletionQueue();
        SubmissionQueue submissionQueue = ring.getSubmissionQueue();

        for (;;) {
            int submit = 0;
            try {
                submit = submissionQueue.submit();
                boolean submitted = submit != 0;
                state.set(WAIT);
                if (!hasTasks() && !(completionQueue.hasCompletions() || submitted)) {
                    while (state.get() == WAIT) {
                        LockSupport.park();
                    }
                }
            } catch (Throwable t) {
                handleLoopException(t);
            } finally {
                state.set(AWAKE);
            }
            drain(submit);
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
