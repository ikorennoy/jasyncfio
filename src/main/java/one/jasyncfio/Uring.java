package one.jasyncfio;

public class Uring {
    private final CompletionQueue completionQueue;
    private final SubmissionQueue submissionQueue;
    private final int ringFd;

    Uring(CompletionQueue completionQueue, SubmissionQueue submissionQueue, int ringFd) {
        this.ringFd = ringFd;
        this.completionQueue = completionQueue;
        this.submissionQueue = submissionQueue;
    }

    CompletionQueue getCompletionQueue() {
        return completionQueue;
    }

    SubmissionQueue getSubmissionQueue() {
        return submissionQueue;
    }

    int getRingFd() {
        return ringFd;
    }

    void close() {
        Native.closeRing(ringFd, submissionQueue.kRingPointer, submissionQueue.ringSize, completionQueue.kRingPointer, completionQueue.ringSize);
    }
}
