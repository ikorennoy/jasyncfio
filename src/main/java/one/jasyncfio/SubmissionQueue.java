package one.jasyncfio;

import static one.jasyncfio.Native.*;

class SubmissionQueue {
    private static final long SQE_SIZE = 64;

    private static final int SQE_OP_CODE_FIELD = 0;
    private static final int SQE_FLAGS_FIELD = 1;
    private static final int SQE_IOPRIO_FIELD = 2; // u16
    private static final int SQE_FD_FIELD = 4; // s32
    private static final int SQE_OFFSET_FIELD = 8;
    private static final int SQE_ADDRESS_FIELD = 16;
    private static final int SQE_LEN_FIELD = 24;
    private static final int SQE_RW_FLAGS_FIELD = 28;
    private static final int SQE_USER_DATA_FIELD = 32;

    private static final int SQE_BUF_INDEX = 40; // u16

    private static final int SQE_PERSONALITY = 42;

    private static final int SQE_FILE_INDEX = 44;

    private final long kHead;
    private final long kTail;
    private final long kRingEntries;
    private final long kFlags;
    private final long kDropped;
    private final long kArray;
    private final long submissionArrayQueueAddress;

    public final int ringSize;
    public final long kRingPointer;


    private final int ringEntries;
    private final int ringMask;
    private int head;
    private int tail;
    private final int ringFd;

    private final long ringFlags;

    public SubmissionQueue(long kHead,
                           long kTail,
                           long kRingMask,
                           long kRingEntries,
                           long kFlags,
                           long kDropped,
                           long kArray,
                           long submissionArrayQueueAddress,
                           int ringSize,
                           long kRingPointer,
                           int ringFd,
                           long ringFlags
    ) {
        this.kHead = kHead;
        this.kTail = kTail;
        this.kRingEntries = kRingEntries;
        this.kFlags = kFlags;
        this.kDropped = kDropped;
        this.kArray = kArray;
        this.submissionArrayQueueAddress = submissionArrayQueueAddress;
        this.ringSize = ringSize;
        this.kRingPointer = kRingPointer;
        this.ringFd = ringFd;
        this.ringFlags = ringFlags;

        this.ringEntries = MemoryUtils.getIntVolatile(kRingEntries);
        this.ringMask = MemoryUtils.getIntVolatile(kRingMask);
        this.head = MemoryUtils.getIntVolatile(kHead);
        this.tail = MemoryUtils.getIntVolatile(kTail);

        MemoryUtils.setMemory(submissionArrayQueueAddress, ringEntries * SQE_SIZE, (byte) 0);

        long address = kArray;
        for (int i = 0; i < ringEntries; i++, address += Integer.BYTES) {
            MemoryUtils.putInt(address, i);
        }
    }

    public int getFlags() {
        return MemoryUtils.getIntVolatile(kFlags);
    }


    public int submit(int minComplete) {
        int flags = minComplete > 0 ? IORING_ENTER_GETEVENTS : 0;
        return submit(tail - head, minComplete, flags);
    }

    public int submit() {
        int submit = tail - head;
        if (!isIoPoll()) {
            return submit > 0 ? submit(submit, submit, Native.IORING_ENTER_GETEVENTS) : 0;
        } else {
            return submit(submit, 1, Native.IORING_ENTER_GETEVENTS);
        }
    }

    private boolean isIoPoll() {
        return (ringFlags & Native.IORING_SETUP_IOPOLL) == Native.IORING_SETUP_IOPOLL;
    }

    public int submitAndWait() {
        int submit = tail - head;
        return submit(Math.max(submit, 0), 1, Native.IORING_ENTER_GETEVENTS);
    }

    public boolean enqueueSqe(byte op, int flags, int rwFlags, int fd,
                              long bufferAddress, int length, long offset, long data, int bufIndex, int fileIndex) {
        int pending = tail - head;
        boolean submit = pending == ringEntries;
        if (submit) {
            int submitted = submit();
            if (submitted == 0) {
                throw new RuntimeException("submission ring is full");
            }
        }
        long sqe = submissionArrayQueueAddress + (tail++ & ringMask) * SQE_SIZE;
        setData(sqe, op, flags, rwFlags, fd, bufferAddress, length, offset, data, bufIndex, fileIndex);
        return submit;
    }

    private void setData(long sqe, byte op, int flags, int rwFlags, int fd, long bufferAddress, int length,
                         long offset, long userData, int bufIndex, int fileIndex
    ) {
        MemoryUtils.putByte(sqe + SQE_OP_CODE_FIELD, op);
        MemoryUtils.putByte(sqe + SQE_FLAGS_FIELD, (byte) flags);
        MemoryUtils.putInt(sqe + SQE_FD_FIELD, fd);
        MemoryUtils.putLong(sqe + SQE_OFFSET_FIELD, offset);
        MemoryUtils.putLong(sqe + SQE_ADDRESS_FIELD, bufferAddress);
        MemoryUtils.putInt(sqe + SQE_LEN_FIELD, length);
        MemoryUtils.putInt(sqe + SQE_RW_FLAGS_FIELD, rwFlags);
        MemoryUtils.putLong(sqe + SQE_USER_DATA_FIELD, userData);
        MemoryUtils.putInt(sqe + SQE_BUF_INDEX, bufIndex);
        MemoryUtils.putInt(sqe + SQE_FILE_INDEX, fileIndex);
    }

    private int submit(int toSubmit, int minComplete, int flags) {
        int ret;
        boolean needEnter = true;
        MemoryUtils.putIntOrdered(kTail, tail);
        if ((ringFlags & IORING_SETUP_SQPOLL) == IORING_SETUP_SQPOLL) {
            needEnter = false;
            if ((getFlags() & IORING_SQ_NEED_WAKEUP) == IORING_SQ_NEED_WAKEUP) {
                flags |= IORING_ENTER_SQ_WAKEUP;
                needEnter = true;
            }
        }
        if (needEnter) {
            ret = Native.ioUringEnter(ringFd, toSubmit, minComplete, flags);
        } else {
            ret = toSubmit;
        }
        head = MemoryUtils.getIntVolatile(kHead);
        if (ret < 0) {
            throw new RuntimeException(String.format("Error code: %d; message: %s", -ret, Native.decodeErrno(ret)));
        }
        return ret;
    }

    public int getTail() {
        return tail;
    }

    public boolean hasPending() {
        return tail - head > 0;
    }
}
