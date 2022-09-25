package one.jasyncfio;

import java.nio.ByteBuffer;

public class IoUringProbeUtils {

    // io_uring_probe
    private static final int LAST_OP = 0; // __u8
    private static final int OPS_LEN = 1; // __u8
    private static final int RESV = 2; // __u16
    private static final int RESV2 = 4;// __u32[3]
    private static final int OPS = 16;


    // io_uring_probe_op
    private static final int OP = 0; // __u8
    private static final int FLAGS = 2; // __u16

    private final ByteBuffer probeBuffer = ByteBuffer.allocateDirect((int) Native.probeBufferSize());
    private final byte lastOpSupported;
    private final byte probeOpsArrayLen;
    private final long probeOpsArrayAddressBase;


    IoUringProbeUtils(int ringFd) {
        long probeBufferAddress = MemoryUtils.getDirectBufferAddress(probeBuffer);
        Native.ioUringRegister(ringFd, Native.IORING_REGISTER_PROBE, probeBufferAddress, 256);
        probeOpsArrayAddressBase = probeBufferAddress + OPS;
        probeOpsArrayLen = MemoryUtils.getByte(probeBufferAddress + OPS_LEN);
        lastOpSupported = MemoryUtils.getByte(probeBufferAddress);
    }

    boolean isOpSupported(int ioUringOp) {
        return lastOpSupported >= ioUringOp;
    }

    void iterateOpArray() {
        long probeOpSize = Native.probeOpSize();
        for (int i = 0; i < probeOpsArrayLen; i++) {
            long probeOpStructElementAddress = probeOpsArrayAddressBase + (probeOpSize * i);
            byte probeOp = getProbeOp(probeOpStructElementAddress);
            short probeOpFlags = getProbeOpFlags(probeOpStructElementAddress);
        }

    }

    private byte getProbeOp(long probeOpStructAddress) {
        return MemoryUtils.getByte(probeOpStructAddress);
    }

    private short getProbeOpFlags(long probeOpStructAddress) {
        return MemoryUtils.getShort(probeOpStructAddress + FLAGS);
    }
}
