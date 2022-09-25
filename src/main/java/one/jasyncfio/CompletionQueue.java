package one.jasyncfio;

class CompletionQueue {
    // offsets
    private static final int CQE_USER_DATA_FIELD = 0;
    private static final int CQE_RES_FIELD = 8;
    private static final int CQE_FLAGS_FIELD = 12;
    private static final int CQE_SIZE = 16;

    // pointers
    private final long kHead;
    private final long kTail;
    private final long kRingMask;
    private final long kRingEntries;
    private final long kOverflow;
    private final long kCompletionArray;
    final long kRingPointer;

    // values
    final int ringSize;
    private int ringHead;
    private final int ringMask;
    private final int ringFd;
    private final long ringFlags;

    CompletionQueue(long kHead,
                           long kTail,
                           long kRingMask,
                           long kRingEntries,
                           long kOverflow,
                           long kCompletionArray,
                           long kRingPointer,
                           int ringSize,
                           int ringFd,
                           long ringFlags) {
        this.kHead = kHead;
        this.kTail = kTail;
        this.kRingMask = kRingMask;
        this.kRingEntries = kRingEntries;
        this.kOverflow = kOverflow;
        this.kCompletionArray = kCompletionArray;
        this.kRingPointer = kRingPointer;
        this.ringSize = ringSize;
        this.ringFd = ringFd;
        this.ringFlags = ringFlags;

        this.ringHead = MemoryUtils.getIntVolatile(kHead);
        this.ringMask = MemoryUtils.getIntVolatile(kRingMask);
    }

    boolean hasCompletions() {
        return ringHead != MemoryUtils.getIntVolatile(kTail);
    }

    int processEvents(CompletionCallback callback) {
        int tail = MemoryUtils.getIntVolatile(kTail);
        int i = 0;
        while (ringHead != tail) {
            long cqeAddress = kCompletionArray + (long) (ringHead & ringMask) * CQE_SIZE;

            long userData = MemoryUtils.getLong(cqeAddress + CQE_USER_DATA_FIELD);
            int res = MemoryUtils.getInt(cqeAddress + CQE_RES_FIELD);
            int flags = MemoryUtils.getInt(cqeAddress + CQE_FLAGS_FIELD);

            ringHead += 1;
            MemoryUtils.putIntOrdered(kHead, ringHead);
            i++;
            callback.handle(res, flags, userData);
        }
        return i;
    }

    int getHead() {
        return ringHead;
    }
}
