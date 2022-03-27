package one.jasyncfio.natives;

import java.io.IOException;

import static one.jasyncfio.natives.Native.*;

public class SubmissionQueue {
    private static final long SQE_SIZE = 64;

    private static final int SQE_OP_CODE_FIELD = 0;
    private static final int SQE_FLAGS_FIELD = 1;
    private static final int SQE_IOPRIO_FIELD = 2; // u16
    private static final int SQE_FD_FIELD = 4; // s32
    private static final int SQE_OFFSET_FIELD = 8;
    private static final int SQE_ADDRESS_FIELD = 16;
    private static final int SQE_LEN_FIELD = 24; // u32
    private static final int SQE_RW_FLAGS_FIELD = 28; // u32
    private static final int SQE_USER_DATA_FIELD = 32; // u64
    private static final int SQE_BUF_INDEX = 40; // u16


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
                           long ringFlags) {
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
        System.out.println("Constructor ring flags: " + ringFlags);

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

    public void wakeup() {
        Native.ioUringEnter(ringFd, 0, 0, Native.IORING_ENTER_SQ_WAKEUP);
    }


    public int submit() throws Throwable {
        int submit = tail - head;
        return submit > 0 ? submit(submit, 0, 0) : 0;
    }

    public int submitAndWait() throws Throwable {
        int submit = tail - head;
        return submit(Math.max(submit, 0), 0, Native.IORING_ENTER_GETEVENTS);
    }

    public boolean addRead(int fd, long bufferAddress, long offset, int length, int opId) throws Throwable {
        return enqueueSqe(
                Native.IORING_OP_READ,
                0,
                0,
                fd,
                bufferAddress,
                length,
                offset,
                opId
        );
    }

    public void addNoOp(int opId) throws Throwable {
        enqueueSqe(
                Native.IORING_OP_NOP,
                0,
                0,
                -1,
                0,
                0,
                0,
                opId
        );
    }

    public void addWrite(int fd, long bufferAddress, long offset, int length, int opId) throws Throwable {
        enqueueSqe(Native.IORING_OP_WRITE,
                0,
                0,
                fd,
                bufferAddress,
                length,
                offset,
                opId
        );
    }

    public void addWritev(int fd, long iovecArrAddress, long offset, int iovecArrSize, int opId) throws Throwable {
        enqueueSqe(Native.IORING_OP_WRITEV,
                0,
                0,
                fd,
                iovecArrAddress,
                iovecArrSize,
                offset,
                opId
        );
    }

    public void addReadv(int fd, long iovecArrAddress, long offset, int iovecArrSize, int opId) throws Throwable {
        enqueueSqe(Native.IORING_OP_READV,
                0,
                0,
                fd,
                iovecArrAddress,
                iovecArrSize,
                offset,
                opId
        );
    }

    public void addStatx(int dirfd, long pathAddress, int statxFlags, int mask, long statxBufferAddress, int opId) throws Throwable {
        enqueueSqe(Native.IORING_OP_STATX,
                0,
                statxFlags,
                dirfd,
                pathAddress,
                mask,
                statxBufferAddress,
                opId
        );
    }

    public void addFsync(int fd, int fsyncFlags, int opId) throws Throwable {
        enqueueSqe(Native.IORING_OP_FSYNC,
                0,
                fsyncFlags,
                fd,
                0,
                0,
                0,
                opId
        );
    }

    public void addFallocate(int fd, long length, int mode, long offset, int opId) throws Throwable {
        enqueueSqe(Native.IORING_OP_FALLOCATE,
                0,
                0,
                fd,
                length,
                mode,
                offset,
                opId
        );
    }

    public void addUnlinkAt(int dirFd, long pathAddress, int flags, int opId) throws Throwable {
        enqueueSqe(Native.IORING_OP_UNLINKAT,
                0,
                flags,
                dirFd,
                pathAddress,
                0,
                0,
                opId
        );
    }

    public void addRenameAt(int oldDirFd, long oldPathAddress, int newDirFd, long newPathAddress, int flags, int opId) throws Throwable {
        enqueueSqe(
                Native.IORING_OP_RENAMEAT,
                0,
                flags,
                oldDirFd,
                oldPathAddress,
                newDirFd,
                newPathAddress,
                opId
        );
    }

    public boolean addEventFdRead(int eventFd, long eventfdReadBuf, int position, int limit, int opId) throws Throwable {
        return enqueueSqe(Native.IORING_OP_READ,
                0,
                0,
                eventFd,
                eventfdReadBuf + position,
                limit - position,
                0,
                opId);
    }

    public void addOpenAt(int dirFd, long pathAddress, int openFlags, int mode, int opId) throws Throwable {
        enqueueSqe(Native.IORING_OP_OPENAT,
                0,
                openFlags,
                dirFd,
                pathAddress,
                mode,
                0,
                opId
        );
    }

    public void addClose(int fd, int opId) throws Throwable {
        enqueueSqe(Native.IORING_OP_CLOSE,
                0,
                0,
                fd,
                0,
                0,
                0,
                opId
        );
    }

    private boolean enqueueSqe(byte op, int flags, int rwFlags, int fd,
                               long bufferAddress, int length, long offset, int data) throws Throwable {
        int pending = tail - head;
        boolean submit = pending == ringEntries;
        if (submit) {
            int submitted = submit();
            if (submitted == 0) {
                throw new IOException("submission ring is full");
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
        long userData = UserDataUtils.encode(fd, op, data);
        MemoryUtils.putLong(sqe + SQE_USER_DATA_FIELD, userData);
    }

    private int submit(int toSubmit, int minComplete, int flags) throws Throwable {
        int ret;
        boolean needEnter = false;
        MemoryUtils.putIntOrdered(kTail, tail);
        if (!((ringFlags & IORING_SETUP_SQPOLL) == IORING_SETUP_SQPOLL)) {
            needEnter = true;
            if ((getFlags() & IORING_SQ_NEED_WAKEUP) == IORING_SQ_NEED_WAKEUP) {
                flags |= IORING_ENTER_SQ_WAKEUP;
            }
        }
        if (needEnter) {
            ret = Native.ioUringEnter(ringFd, toSubmit, minComplete, flags);
        } else {
            ret = toSubmit;
        }

        head = MemoryUtils.getIntVolatile(kHead);
        if (ret < 0) {
            throw new IOException(String.format("Error code: %d; message: %s", -ret, Native.decodeErrno(ret)));
        }
        return ret;
    }

    public void submitPooled() {
        if ((getFlags() & IORING_SQ_NEED_WAKEUP) == IORING_SQ_NEED_WAKEUP) {
            wakeup();
        }
        MemoryUtils.putIntOrdered(kTail, tail);
    }
}
