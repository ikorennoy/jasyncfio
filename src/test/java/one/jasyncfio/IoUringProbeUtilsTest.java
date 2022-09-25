package one.jasyncfio;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IoUringProbeUtilsTest {
    private final EventExecutor eventExecutor = EventExecutor.initDefault();
    private final IoUringProbeUtils uringProbeUtils = new IoUringProbeUtils(eventExecutor.sleepableRingFd());


    @Test
    void probeTest() {
        Assertions.assertTrue(uringProbeUtils.isOpSupported(Native.IORING_OP_NOP));
    }


    @Test
    void iterateTest() {
        // make sure we don't segfault
        uringProbeUtils.iterateOpArray();
    }
}
