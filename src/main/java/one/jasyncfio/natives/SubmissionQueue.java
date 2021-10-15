package one.jasyncfio.natives;

import one.jasyncfio.natives.MemoryUtils;
import one.jasyncfio.natives.Native;

public class SubmissionQueue {
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
    private static final int SQE_PAD_FIELD = 40;

    private final long kHead;
    private final long kTail;
    private final long kRingEntries;
    private final long kFlags;
    private final long kDropped;
    private final long kArray;
    private final long submissionArrayQueueAddress;

    private final int ringSize;
    private final long kRingPointer;


    private final int ringEntries;
    private final int ringMask;
    private int head;
    private int tail;
    private final int ringFd;

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
                                  int ringFd) {
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

    private boolean enqueueSqe(byte op, int flags, int rwFlags, int fd,
                               long bufferAddress, int length, long offset, int data) {
        int pending = tail - head;
        boolean submit = pending == ringEntries;
        if (submit) {
            int submitted = submit();
            if (submitted == 0) {
                throw new RuntimeException("Sq ring full");
            }
        }
        long sqe = submissionArrayQueueAddress + (tail++ & ringMask) * SQE_SIZE;
        setData(sqe, op, flags, rwFlags, fd, bufferAddress, length, offset, data);
        return submit;
    }

    private void setData(long sqe, byte op, int flags, int rwFlags, int fd, long bufferAddress, int length,
                         long offset, int data) {

        MemoryUtils.putByte(sqe + SQE_OP_CODE_FIELD, op);
        MemoryUtils.putByte(sqe + SQE_FLAGS_FIELD, (byte) flags);
        MemoryUtils.putInt(sqe + SQE_FD_FIELD, fd);
        MemoryUtils.putLong(sqe + SQE_OFFSET_FIELD, offset);
        MemoryUtils.putLong(sqe + SQE_ADDRESS_FIELD, bufferAddress);
        MemoryUtils.putInt(sqe + SQE_LEN_FIELD, length);
        MemoryUtils.putInt(sqe + SQE_RW_FLAGS_FIELD, rwFlags);
        long userData = encode(fd, op, data);
        MemoryUtils.putLong(sqe + SQE_USER_DATA_FIELD, userData);
    }


    public static long encode(int fd, byte op, int data) {
        return ((long) data << 40) | ((op & 0xFFL) << 32) | fd & 0xFFFFFFFFL;
    }

    public int submitWakeUp() {
        System.out.println("submit wake");
        int submit = tail - head;
        return submit > 0 ? submit(submit, 0, Native.IORING_ENTER_SQ_WAKEUP) : 0;
    }

    public int submit() {
        int submit = tail - head;
        return submit > 0 ? submit(submit, 0, 0) : 0;
    }

    public int submitAndWait() {
        int submit = tail - head;
        if (submit > 0) {
            return submit(submit, 1, Native.IORING_ENTER_GETEVENTS);
        }
        return Native.ioUringEnter(ringFd, 0, 1, Native.IORING_ENTER_GETEVENTS);
    }

    private int submit(int toSubmit, int minComplete, int flags) {
        MemoryUtils.putIntOrdered(kTail, tail);
        int ret = Native.ioUringEnter(ringFd, toSubmit, minComplete, flags);
        head = MemoryUtils.getIntVolatile(kHead);
        return ret;
    }
}
