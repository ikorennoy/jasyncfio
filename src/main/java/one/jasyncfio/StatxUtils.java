package one.jasyncfio;

import one.jasyncfio.natives.MemoryUtils;
import one.jasyncfio.natives.Native;


class StatxUtils {
    public static int BUF_SIZE = 256;
    // offsets
    private static final int STX_MASK = 0; // __u32
    private static final int STX_BLKSIZE = 4; // __u32
    private static final int STX_ATTRIBUTES = 8; // __u64
    private static final int STX_NLINK = 16; // __u32
    private static final int STX_UID = 20; // __u32
    private static final int STX_GID = 24; // __u32
    private static final int STX_MODE = 28; // __u16
    // pad 2 bytes between stx_mode and stx_ino because stx_ino 8-byte aligned
    private static final int STX_INO = 32; // __u64
    private static final int STX_SIZE = 40; // __u64
    private static final int STX_BLOCKS = 48; // __u64
    private static final int STX_ATTRIBUTES_MASK = 56; // __u64

    // also, we have a bunch of timestamp fields but that's too lazy...

    static long getSize(long buf) {
        if (isFlagSet(buf, Native.STATX_SIZE)) {
            return MemoryUtils.getLong(buf + STX_SIZE);
        }
        throw new RuntimeException("failed to get file size");
    }


    private static boolean isFlagSet(long buf, int flag) {
        return (MemoryUtils.getInt(buf) & flag) == flag;
    }
}
