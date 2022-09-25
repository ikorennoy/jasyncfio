package one.jasyncfio;

import java.nio.ByteBuffer;

public class IoUringBufRing {

    private static class IoUringBufReg {
        private static final int RING_ADDR = 0; // __u64
        private static final int RING_ENTRIES = 8; // __u32
        private static final int BG_ID = 12; // __u16
        private static final long SIZE = Native.ioUringBufRegSize();

        static void putRingAddr(long address, long value) {
            MemoryUtils.putLong(address, value);
        }

        static void putRingEntries(long address, int value) {
            MemoryUtils.putInt(address + RING_ENTRIES, value);
        }

        static void putBgId(long address, short value) {
            MemoryUtils.putShort(address + BG_ID, value);
        }
    }

    private static class IoUringBufRingStruct {
        private static final int TAIL = 14;
        private static final int IO_URING_BUF = 14;

        static void putTail(long address, short value) {
            MemoryUtils.putShort(address + TAIL, value);
        }

        static void publishTail(long address, short value) {
            MemoryUtils.putShortVolatile(address, value);
        }

        static short getTail(long address) {
            return MemoryUtils.getShort(address);
        }

        static long getIoUringBuf(long address, int tail, int offset, int mask) {
            long ioUringBufArrayBase = address + IO_URING_BUF;
            return ioUringBufArrayBase + ((tail + offset) & mask) * Native.ioUringBufSize();
        }
    }

    private static class IoUringBuf {
        private static final int ADDR = 0; // __u64
        private static final int LEN = 8; // __u32
        private static final int BID = 12; // __u16

        static void setAddr(long baseAddress, long addr) {
            MemoryUtils.putLong(baseAddress, addr);
        }

        static void setLen(long baseAddress, int len) {
            MemoryUtils.putInt(baseAddress + LEN, len);
        }

        static void setBid(long baseAddress, short bid) {
            MemoryUtils.putShort(baseAddress + BID, bid);
        }
    }

    private final int bufSize;
    private final int numOfBuffers;

    private final int bufRingSize;
    private final ByteBuffer bufRingBuffer;
    private final long bufRingBaseAddress;
    private final long bufferBaseAddress;


    public IoUringBufRing(EventExecutor executor, int bufSize, int numOfBuffers) {
        this.bufSize = bufSize;
        this.numOfBuffers = numOfBuffers;
        this.bufRingSize = (int) ((Native.ioUringBufSize() + bufSize) * numOfBuffers);
        this.bufRingBuffer = MemoryUtils.allocateAlignedByteBuffer(bufRingSize, Native.getPageSize());
        this.bufRingBaseAddress = MemoryUtils.getDirectBufferAddress(bufRingBuffer);
        IoUringBufRingStruct.putTail(bufRingBaseAddress, (short) 0);
        ByteBuffer registerBufRingBuffer = ByteBuffer.allocateDirect((int) IoUringBufReg.SIZE);
        long registerBufRingBufferAddress = MemoryUtils.getDirectBufferAddress(registerBufRingBuffer);
        IoUringBufReg.putRingAddr(registerBufRingBufferAddress, bufRingBaseAddress);
        IoUringBufReg.putRingEntries(registerBufRingBufferAddress, numOfBuffers);
        IoUringBufReg.putBgId(registerBufRingBufferAddress, (short) 0);
        this.bufferBaseAddress = bufRingBaseAddress + Native.ioUringBufSize() * numOfBuffers;
        Native.ioUringRegister(executor.sleepableRingFd(), Native.IORING_REGISTER_PBUF_RING, registerBufRingBufferAddress, 1);
        for (int i = 0; i < numOfBuffers; i++) {
            addBuffer(i);
        }
        IoUringBufRingStruct.publishTail(bufferBaseAddress, (short) numOfBuffers);
    }

    private void addBuffer(int offset) {
        long ioUringBuf = IoUringBufRingStruct.getIoUringBuf(
                bufRingBaseAddress,
                IoUringBufRingStruct.getTail(bufferBaseAddress),
                offset, getBufRingMask());

        IoUringBuf.setAddr(ioUringBuf, getBufferAddress(offset));
        IoUringBuf.setLen(ioUringBuf, bufSize);
        IoUringBuf.setBid(ioUringBuf, (short) offset);

    }

    private long getBufferAddress(int bufferId) {
        return bufferBaseAddress + (long) bufferId * bufSize;
    }

    private int getBufRingMask() {
        return numOfBuffers - 1;
    }
}
