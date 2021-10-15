package one.jasyncfio.natives;

import java.lang.reflect.Field;
import java.nio.Buffer;

public class MemoryUtils {

    protected static sun.misc.Unsafe unsafe = sun.misc.Unsafe.class.cast(getUnsafe());

    private static Object getUnsafe() {
        try {
            Class sunUnsafe = Class.forName("sun.misc.Unsafe");
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

    public static int getInt(long address) {
        return unsafe.getInt(address);
    }

//    public static long getDirectBufferAddress(Buffer buffer) {
//        if (!buffer.isDirect()) {
//            throw new IllegalArgumentException("buffer is not direct");
//        }
//        return MemoryInternal.getDirectBufferAddress(buffer);
//    }

//    public static long getStringPtr(String str) {
//        return Native.getStringPointer(str);
//    }

//    public static void releaseString(String str, long ptr) {
//        Native.releaseString(str, ptr);
//    }
}
