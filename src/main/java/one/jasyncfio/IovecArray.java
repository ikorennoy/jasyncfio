package one.jasyncfio;

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
    private final int count;
    private final long size;
    private final Iovec[] iovecs;

    public IovecArray(ByteBuffer[] buffers) {
        iovecArray = ByteBuffer.allocateDirect(buffers.length * IOV_SIZE);
        iovecArrayAddress = MemoryUtils.getDirectBufferAddress(iovecArray);
        int count = 0;
        int size = 0;
        iovecs = new Iovec[buffers.length];

        for (ByteBuffer buffer : buffers) {
            long buffAddress = MemoryUtils.getDirectBufferAddress(buffer);
            int len = buffer.limit();

            final int baseOffset = count * IOV_SIZE;
            final int lenOffset = baseOffset + ADDRESS_SIZE;
            iovecs[count] = new Iovec(buffAddress, len);

            size += len;
            ++count;

            MemoryUtils.putLong(baseOffset + iovecArrayAddress, buffAddress);
            MemoryUtils.putLong(lenOffset + iovecArrayAddress, len);
        }

        this.count = count;
        this.size = size;
    }

    public long getIovecArrayAddress() {
        return iovecArrayAddress;
    }

    public Iovec getIovec(int position) {
        if (position >= count) {
            throw new IllegalArgumentException("position can't be greater than iovec array size");
        }

        return iovecs[position];
    }

    public int getCount() {
        return count;
    }

    public long getSize() {
        return size;
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
