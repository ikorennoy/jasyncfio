package one.jasyncfio.natives;

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

    public IovecArray(ByteBuffer[] buffers) {
        iovecArray = ByteBuffer.allocateDirect(buffers.length * IOV_SIZE);
        iovecArrayAddress = MemoryUtils.getDirectBufferAddress(iovecArray);
        int count = 0;
        int size = 0;

        for (ByteBuffer buffer : buffers) {
            long buffAddress = MemoryUtils.getDirectBufferAddress(buffer) + buffer.position();
            int len = buffer.limit();

            final int baseOffset = count * IOV_SIZE;
            final int lenOffset = baseOffset + ADDRESS_SIZE;

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

    public int getCount() {
        return count;
    }

    public long getSize() {
        return size;
    }
}
