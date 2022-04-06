package one.jasyncfio.natives;

public class Uring {
    private final CompletionQueue completionQueue;
    private final SubmissionQueue submissionQueue;
    private final int ringFd;

    Uring(CompletionQueue completionQueue, SubmissionQueue submissionQueue, int ringFd) {
        this.ringFd = ringFd;
        this.completionQueue = completionQueue;
        this.submissionQueue = submissionQueue;
    }

    public CompletionQueue getCompletionQueue() {
        return completionQueue;
    }

    public SubmissionQueue getSubmissionQueue() {
        return submissionQueue;
    }

    public int getRingFd() {
        return ringFd;
    }
}
