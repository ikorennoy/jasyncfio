package one.jasyncfio;

//import sun.misc.Cleaner;

import java.nio.ByteBuffer;

/**
 * <pre>
 * struct iovec {
 *   void  *iov_base;
 *   size_t iov_len;
 * };
 * </pre>
 * <p>
 * 24.03.2022
 *
 * @author ikorennoy
 */
public class IovecArray {
    private static final int ADDRESS_SIZE = MemoryUtils.addressSize();

    private static final int IOV_SIZE = 2 * ADDRESS_SIZE;

    // todo support max capacity

    private final long iovecArrayAddress;
    private final ByteBuffer iovecArray;
    private final int size;
    private final long sizeBytes;
    private final Iovec[] iovecs;

    private final ByteBuffer[] buffers;

    public IovecArray(ByteBuffer[] buffers) {
        this.buffers = buffers;
        iovecArray = ByteBuffer.allocateDirect(buffers.length * IOV_SIZE);
        iovecArrayAddress = MemoryUtils.getDirectBufferAddress(iovecArray);
        int size = 0;
        iovecs = new Iovec[buffers.length];

        for (int i = 0; i < buffers.length; i++) {
            ByteBuffer buffer = buffers[i];
            long buffAddress = MemoryUtils.getDirectBufferAddress(buffer);
            int len = buffer.remaining();

            final int baseOffset = i * IOV_SIZE;
            final int lenOffset = baseOffset + ADDRESS_SIZE;
            iovecs[i] = new Iovec(buffAddress, len);

            size += len;

            MemoryUtils.putLong(baseOffset + iovecArrayAddress, buffAddress);
            MemoryUtils.putLong(lenOffset + iovecArrayAddress, len);
        }

        this.size = buffers.length;
        this.sizeBytes = size;

//        Cleaner.create(this, () -> {
//            if (iovecArrayAddress != 0) {
//                MemoryUtils.freeMemory(iovecArrayAddress);
//            }
//        });
    }

    public void updatePositions(int bytesRead) {
        int left = bytesRead;
        for (int i = 0; i < buffers.length; i++) {
            ByteBuffer buffer = buffers[i];
            int rem = buffer.remaining();
            int n = Math.min(left, rem);
            buffer.position(buffer.position() + n);
            left -= n;
        }
    }

    public long getIovecArrayAddress() {
        return iovecArrayAddress;
    }

    public Iovec getIovec(int position) {
        if (position >= size) {
            throw new IllegalArgumentException("position can't be greater than iovec array size");
        }

        return iovecs[position];
    }

    public int getSize() {
        return size;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public static class Iovec {
        private final long iovBase;
        private final long iovLen;

        public Iovec(long iovBase, long iovLen) {
            this.iovBase = iovBase;
            this.iovLen = iovLen;
        }

        public long getIovBase() {
            return iovBase;
        }

        public long getIovLen() {
            return iovLen;
        }
    }
}
