package one.jasyncfio.natives;

import one.jasyncfio.CompletionCallback;
import one.jasyncfio.natives.MemoryUtils;

public class CompletionQueue {
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
    private final long kRingPointer;

    // values
    private final int ringSize;
    private int ringHead;
    private final int ringMask;
    private final int ringFd;

    public CompletionQueue(long kHead,
                                  long kTail,
                                  long kRingMask,
                                  long kRingEntries,
                                  long kOverflow,
                                  long kCompletionArray,
                                  long kRingPointer,
                                  int ringSize,
                                  int ringFd) {
        this.kHead = kHead;
        this.kTail = kTail;
        this.kRingMask = kRingMask;
        this.kRingEntries = kRingEntries;
        this.kOverflow = kOverflow;
        this.kCompletionArray = kCompletionArray;
        this.kRingPointer = kRingPointer;
        this.ringSize = ringSize;
        this.ringFd = ringFd;

        this.ringHead = MemoryUtils.getIntVolatile(kHead);
        this.ringMask = MemoryUtils.getIntVolatile(kRingMask);
    }

    public boolean hasCompletions() {
        return ringHead != MemoryUtils.getIntVolatile(kTail);
    }

    static void decode(int res, int flags, long udata, CompletionCallback callback) {
        int fd = (int) (udata & 0xFFFFFFFFL);
        byte op = (byte) ((udata >>>= 32) & 0xFFL);
        int data = (int) (udata >>> 8);
        callback.handle(fd, res, flags, op, data);
    }

    public int processEvents(CompletionCallback callback) {
        int tail = MemoryUtils.getIntVolatile(kTail);
        int i = 0;
        while (ringHead != tail) {
            long cqeAddress = kCompletionArray + (ringHead & ringMask) * CQE_SIZE;
            long userData = MemoryUtils.getLong(cqeAddress + CQE_USER_DATA_FIELD);
            int res = MemoryUtils.getInt(cqeAddress + CQE_RES_FIELD);
            int flags = MemoryUtils.getInt(cqeAddress + CQE_FLAGS_FIELD);
            ringHead += 1;
            MemoryUtils.putIntOrdered(kHead, ringHead);
            i++;
            decode(res, flags, userData, callback);
        }
        return i;
    }
}
