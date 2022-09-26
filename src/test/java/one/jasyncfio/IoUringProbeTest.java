package one.jasyncfio;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IoUringProbeTest {
    private final EventExecutor eventExecutor = EventExecutor.initDefault();
    private final IoUringProbe uringProbeUtils = new IoUringProbe(eventExecutor.sleepableRingFd());


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
