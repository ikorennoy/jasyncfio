package one.jasyncfio;

import java.util.Locale;

public class Native {

    static {
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (!os.contains("linux")) {
                throw new RuntimeException("only supported on linux");
            }
            System.load(Utils.loadLib("libjasyncfio.so").toPath().toAbsolutePath().toString());
            String kernelVersion = Native.kernelVersion();
            if (!Native.checkKernelVersion(kernelVersion)) {
                throw new UnsupportedOperationException("you need at least kernel version 5.11, current version is: " + kernelVersion);
            }
        } catch (Throwable ex) {
            throw (Error) new UnsatisfiedLinkError("can't load native library").initCause(ex);
        }
    }

    static int ioUringEnter(int ringFd, int toSubmit, int minComplete, int flags) {
        int ret = ioUringEnter0(ringFd, toSubmit, minComplete, flags);
        if (ret < 0) {
            throw new RuntimeException("io_uring enter error: " + ret);
        }
        return ret;
    }

    static Uring setupIoUring(int entries, int flags, int sqThreadIdle, int sqThreadCpu, int cqSize, int attachWqRingFd) {
        long[][] pointers = setupIoUring0(entries, flags, sqThreadIdle, sqThreadCpu, cqSize, attachWqRingFd);
        final SubmissionQueue submissionQueue = new SubmissionQueue(
                pointers[0][0],
                pointers[0][1],
                pointers[0][2],
                pointers[0][3],
                pointers[0][4],
                pointers[0][5],
                pointers[0][6],
                pointers[0][7],
                (int) pointers[0][8],
                pointers[0][9],
                (int) pointers[0][10],
                pointers[0][11]
        );
        final CompletionQueue completionQueue = new CompletionQueue(
                pointers[1][0],
                pointers[1][1],
                pointers[1][2],
                pointers[1][3],
                pointers[1][4],
                pointers[1][5],
                pointers[1][6],
                (int) pointers[1][7],
                (int) pointers[1][8],
                pointers[1][9]
        );
        return new Uring(completionQueue, submissionQueue, (int) pointers[0][10]);
    }

    private static native int ioUringEnter0(int ringFd, int toSubmit, int minComplete, int flags);

    private static native long[][] setupIoUring0(int entries, int flags, int sqThreadIdle, int sqThreadCpu, int cqSize, int attachWqRingFd);

    public static native int getEventFd();

    public static native int eventFdWrite(int fd, long value);

    static native long getDirectBufferAddress(java.nio.Buffer buffer);

    static native long getStringPointer(String str);

    static native void releaseString(String str, long ptr);

    static native String kernelVersion();

    public static native String decodeErrno(int errorCode);

    public static native void ioUringRegister(int fd, int opcode, long argPtr, int nrArgs);

    public static native long getFileSize(int fd);

    public static native long getPageSize();

    public static native void closeRing(int ringFd, long sqRingPtr, int sqRingSize, long cqRingPtr, int cqRingSize);

    /**
     * took from netty io uring project
     */
    static boolean checkKernelVersion(String kernelVersion) {
        String[] versionComponents = kernelVersion.split("\\.");
        if (versionComponents.length < 3) {
            return false;
        }

        int major;
        try {
            major = Integer.parseInt(versionComponents[0]);
        } catch (NumberFormatException e) {
            return false;
        }

        if (major <= 4) {
            return false;
        }
        if (major > 5) {
            return true;
        }

        int minor;
        try {
            minor = Integer.parseInt(versionComponents[1]);
        } catch (NumberFormatException e) {
            return false;
        }

        return minor >= 11;
    }


    static final byte IORING_OP_READ = UringConstants.ioRingOpRead();
    static final byte IORING_OP_WRITE = UringConstants.ioRingOpWrite();
    static final byte IORING_OP_CLOSE = UringConstants.ioRingOpClose();
    static final byte IORING_OP_OPENAT = UringConstants.ioRingOpenAt();
    static final byte IORING_OP_NOP = UringConstants.ioRingOpNop();
    static final byte IORING_OP_STATX = UringConstants.ioRingOpStatx();
    static final byte IORING_OP_FSYNC = UringConstants.ioRingOpFsync();
    static final byte IORING_OP_FALLOCATE = UringConstants.ioRingOpFallocate();
    static final byte IORING_OP_UNLINKAT = UringConstants.ioRingOpUnlinkAt();
    static final byte IORING_OP_RENAMEAT = UringConstants.ioRingOpRenameAt();
    static final byte IORING_OP_READV = UringConstants.ioRingOpReadv();
    static final byte IORING_OP_WRITEV = UringConstants.ioRingOpWritev();
    static final byte IORING_OP_READ_FIXED = UringConstants.ioRingOpReadFixed();
    static final byte IORING_OP_WRITE_FIXED = UringConstants.ioRingOpWriteFixed();

    static final int IORING_REGISTER_BUFFERS = UringConstants.ioRingRegisterBuffers();
    static final int IORING_UNREGISTER_BUFFERS = UringConstants.ioRingUnregisterBuffers();
    static final int IORING_REGISTER_FILES = UringConstants.ioRingRegisterFiles();
    static final int IORING_UNREGISTER_FILES = UringConstants.ioRingUnregisterFiles();
    static final int IORING_ENTER_GETEVENTS = UringConstants.ioRingEnterGetEvents();
    static final int IORING_ENTER_SQ_WAKEUP = UringConstants.ioRingEnterSqWakeup();
    static final int IORING_SQ_NEED_WAKEUP = UringConstants.ioRingSqNeedWakeup();
    static final int IORING_SQ_CQ_OVERFLOW = UringConstants.ioRingSqCqOverflow();
    static final int IORING_FSYNC_DATASYNC = UringConstants.ioRingFsyncDatasync();
    static final int IORING_SETUP_SQPOLL = UringConstants.ioRingSetupSqPoll();
    static final int IORING_SETUP_IOPOLL = UringConstants.ioRingSetupIoPoll();
    static final int IORING_SETUP_SQ_AFF = UringConstants.ioRingSetupSqAff();
    static final int IORING_SETUP_CQ_SIZE = UringConstants.ioRingSetupCqSize();
    static final int IORING_SETUP_CLAMP = UringConstants.ioRingSetupClamp();
    static final int IORING_SETUP_ATTACH_WQ = UringConstants.ioRingSetupAttachWq();

    static final int O_RDONLY = FileIoConstants.oRdOnly();
    static final int O_WRONLY = FileIoConstants.oWrOnly();
    static final int O_RDWR = FileIoConstants.oRdWr();
    static final int O_TRUNC = FileIoConstants.oTrunc();
    static final int O_CREAT = FileIoConstants.oCreat();
    static final int STATX_SIZE = FileIoConstants.statxSize();
    static final int O_DIRECT = FileIoConstants.oDirect();
    static final int O_CLOEXEC = FileIoConstants.oCloexec();
    static final int O_APPEND = FileIoConstants.oAppend();
    static final int O_DSYNC = FileIoConstants.oDsync();
    static final int O_EXCL = FileIoConstants.oExcl();
    static final int O_NOATIME = FileIoConstants.oNoAtime();
    static final int O_SYNC = FileIoConstants.oSync();
}
