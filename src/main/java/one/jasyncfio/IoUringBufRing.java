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

        static void putTail(long address, short value) {
            MemoryUtils.putShort(address + TAIL, value);
        }

        static void publishTail(long address, short value) {
            MemoryUtils.putShortVolatile(address + TAIL, (short) (getTail(address) + value));
        }

        static short getTail(long address) {
            return MemoryUtils.getShort(address + TAIL);
        }

        static long getIoUringBuf(long address, int tail, int offset, int mask) {
            return address + ((tail + offset) & mask) * Native.ioUringBufSize();
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

    private final int bufferSize;
    private final int numOfBuffers;

    private final ByteBuffer bufRingBuffer;
    private final long bufRingBaseAddress;

    private final ByteBuffer bufferBaseBb;
    private final ByteBuffer[] buffers;
    private final long bufferBaseAddress;


    public IoUringBufRing(int ringFd, int bufferSize, int numOfBuffers) {
        this.bufferSize = bufferSize;
        this.numOfBuffers = numOfBuffers;
        int bufRingSize = (int) ((Native.ioUringBufSize() + bufferSize) * numOfBuffers);
        this.bufRingBuffer = MemoryUtils.allocateAlignedByteBuffer(bufRingSize, Native.getPageSize());
        this.bufRingBaseAddress = MemoryUtils.getDirectBufferAddress(bufRingBuffer);
        this.buffers = new ByteBuffer[numOfBuffers];
        // init buf ring struct
        IoUringBufRingStruct.putTail(bufRingBaseAddress, (short) 0);

        ByteBuffer registerBufRingBuffer = ByteBuffer.allocateDirect((int) IoUringBufReg.SIZE);
        long registerBufRingBufferAddress = MemoryUtils.getDirectBufferAddress(registerBufRingBuffer);

        IoUringBufReg.putRingAddr(registerBufRingBufferAddress, bufRingBaseAddress);
        IoUringBufReg.putRingEntries(registerBufRingBufferAddress, numOfBuffers);
        IoUringBufReg.putBgId(registerBufRingBufferAddress, (short) 0);

        this.bufferBaseAddress = bufRingBaseAddress + Native.ioUringBufSize() * numOfBuffers;
        this.bufferBaseBb = ((ByteBuffer) bufRingBuffer.position((int) (Native.ioUringBufSize() * numOfBuffers))).slice();

        Native.ioUringRegister(ringFd, Native.IORING_REGISTER_PBUF_RING, registerBufRingBufferAddress, 1);
        for (int i = 0; i < numOfBuffers; i++) {
            addBuffer(i);
            initBbArrayElement(i);
        }
        IoUringBufRingStruct.publishTail(bufRingBaseAddress, (short) numOfBuffers);
    }

    void recycleBuffer(int id) {
        addBuffer(id);
        buffers[id].clear();
        IoUringBufRingStruct.publishTail(bufRingBaseAddress, (short) 1);
    }

    ByteBuffer getBuffer(int id) {
        return buffers[id];
    }


    private void initBbArrayElement(int id) {
        buffers[id] = ((ByteBuffer) bufferBaseBb.position(id * bufferSize)).slice();
    }

    private void addBuffer(int id) {
        long ioUringBuf = IoUringBufRingStruct.getIoUringBuf(
                bufRingBaseAddress,
                IoUringBufRingStruct.getTail(bufRingBaseAddress),
                id,
                getBufRingMask());

        IoUringBuf.setAddr(ioUringBuf, getRingBufferAddress(id));
        IoUringBuf.setLen(ioUringBuf, bufferSize);
        IoUringBuf.setBid(ioUringBuf, (short) id);

    }

    private long getRingBufferAddress(int bufferId) {
        return bufferBaseAddress + (long) bufferId * bufferSize;
    }

    private int getBufRingMask() {
        return numOfBuffers - 1;
    }
}
