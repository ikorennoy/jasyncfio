package one.jasyncfio.natives;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MemoryUtils {

    protected static sun.misc.Unsafe unsafe = (sun.misc.Unsafe) getUnsafe();

    private static Object getUnsafe() {
        try {
            Class<?> sunUnsafe = Class.forName("sun.misc.Unsafe");
            Field f = sunUnsafe.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return f.get(sunUnsafe);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void putIntOrdered(long address, int newValue) {
        unsafe.putOrderedInt(null, address, newValue);
    }

    public static void park() {
        unsafe.park(true, 0);
    }

    public static void unpark(Object thread) {
        unsafe.unpark(thread);
    }

    public static int getIntVolatile(long address) {
        return unsafe.getIntVolatile(null, address);
    }

    public static void putByte(long address, byte value) {
        unsafe.putByte(address, value);
    }

    public static void putInt(long address, int value) {
        unsafe.putInt(address, value);
    }

    public static void putLong(long address, long value) {
        unsafe.putLong(address, value);
    }

    public static void setMemory(long address, long size, byte value) {
        unsafe.setMemory(address, size, value);
    }

    public static long getLong(long address) {
        return unsafe.getLong(address);
    }

    public static long allocateMemory(long size) {
        return unsafe.allocateMemory(size);
    }

    public static void freeMemory(long ptr) {
        unsafe.freeMemory(ptr);
    }

    public static ByteBuffer allocateAlignedByteBuffer(int capacity, long align) {
        // Power of 2 --> single bit, none power of 2 alignments are not allowed.
        if (Long.bitCount(align) != 1) {
            throw new IllegalArgumentException("Alignment must be a power of 2");
        }
        // We over allocate by the alignment, so we know we can have a large enough aligned
        // block of memory to use. Also set order to native while we are here.
        ByteBuffer buffer = ByteBuffer.allocateDirect((int) (capacity + align));
        long address = getDirectBufferAddress(buffer);
        // check if we got lucky and the address is already aligned
        if ((address & (align - 1)) == 0) {
            // set the new limit to intended capacity
            buffer.limit(capacity);
        } else {
            // we need to shift the start position to an aligned address --> address + (align - (address % align))
            // the modulo replacement with the & trick is valid for power of 2 values only
            int newPosition = (int) (align - (address & (align - 1)));
            // change the position
            buffer.position(newPosition);
            int newLimit = newPosition + capacity;
            // set the new limit to accommodate offset + intended capacity
            buffer.limit(newLimit);
        }
        // the slice is now an aligned buffer of the required capacity
        return buffer.slice().order(ByteOrder.nativeOrder());
    }

    public static int getInt(long address) {
        return unsafe.getInt(address);
    }

    public static short getShort(long address) {
        return unsafe.getShort(address);
    }

    public static long getDirectBufferAddress(Buffer buffer) {
        if (!buffer.isDirect()) {
            throw new IllegalArgumentException("buffer is not direct");
        }
//        return ((DirectBuffer) buffer).address();
        return Native.getDirectBufferAddress(buffer);
    }

    public static long getStringPtr(String str) {
        return Native.getStringPointer(str);
    }

    public static void releaseString(String str, long ptr) {
        Native.releaseString(str, ptr);
    }
}
