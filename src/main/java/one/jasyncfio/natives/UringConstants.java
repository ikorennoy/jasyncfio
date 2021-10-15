package one.jasyncfio.natives;

public class UringConstants {

    static native int ioRingEnterGetEvents();

    static native int ioRingEnterSqWakeup();

    static native int ioRingSqNeedWakeup();

    static native int ioRingSetupSqPoll();

    static native int ioRingSetupIoPoll();

    static native int ioRingSetupSqAff();

    static native int ioRingSetupCqSize();

    static native byte ioRingOpRead();

    static native byte ioRingOpWrite();

    static native byte ioRingOpenAt();

    static native byte ioRingOpClose();

    static native byte ioRingOpNop();

    static native byte ioRingOpReadv();

    static native byte ioRingOpWritev();

    static native byte ioRingOpFsync();

    static native byte ioRingOpReadFixed();

    static native byte ioRingOpWriteFixed();

    static native byte ioRingOpPollAdd();

    static native byte ioRingOpPollRemove();
}
