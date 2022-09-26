package one.jasyncfio;

class UringConstants {

    static native int ioRingEnterGetEvents();

    static native int ioRingEnterSqWakeup();

    static native int ioRingSqNeedWakeup();

    static native int ioRingSqCqOverflow();

    static native int ioRingSetupSqPoll();

    static native int ioRingSetupIoPoll();

    static native int ioRingSetupSqAff();

    static native int ioRingSetupCqSize();

    static native int ioRingFsyncDatasync();

    static native int ioRingSetupClamp();

    static native int ioRingSetupAttachWq();

    static native byte ioRingOpRead();

    static native byte ioRingOpWrite();

    static native byte ioRingOpenAt();

    static native byte ioRingOpClose();

    static native byte ioRingOpNop();

    static native byte ioRingOpStatx();

    static native byte ioRingOpReadv();

    static native byte ioRingOpWritev();

    static native byte ioRingOpFsync();

    static native byte ioRingOpFallocate();

    static native byte ioRingOpUnlinkAt();

    static native byte ioRingOpRenameAt();

    static native byte ioRingOpReadFixed();

    static native byte ioRingOpWriteFixed();

    static native byte ioRingOpPollAdd();

    static native byte ioRingOpPollRemove();

    static native byte ioRingOpConnect();

    static native byte ioRingOpAccept();

    static native byte ioRingOpTimeout();

    static native byte ioRingOpTimeoutRemove();

    static native byte ioRingOpSendMsg();

    static native byte ioRingOpRecvMsg();

    static native byte ioRingOpSend();

    static native byte ioRingOpRecv();

    static native byte ioRingOpShutdown();

    static native byte ioRingOpSendZc();

    static native int ioRingRegisterBuffers();

    static native int ioRingUnregisterBuffers();

    static native int ioRingRegisterFiles();

    static native int ioRingUnregisterFiles();

    static native int ioRingRegisterProbe();
    public static native int ioRingRegisterPbufRing();

    static native byte ioRingOpSplice();

    static native int iosqeBufferSelect();
}
