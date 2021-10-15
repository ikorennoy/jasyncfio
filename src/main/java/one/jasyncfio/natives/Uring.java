package one.jasyncfio.natives;

public class Uring {
    private final CompletionQueue completionQueue;
    private final SubmissionQueue submissionQueue;

    Uring(CompletionQueue completionQueue, SubmissionQueue submissionQueue) {
        this.completionQueue = completionQueue;
        this.submissionQueue = submissionQueue;
    }
}
