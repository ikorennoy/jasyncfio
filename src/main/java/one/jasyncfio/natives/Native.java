package one.jasyncfio.natives;

import java.util.Locale;

public class Native {

    static {
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (!os.contains("linux")) {
                throw new RuntimeException("only supported on linux");
            }
            System.load(Utils.loadLib("libjasyncfio.so").toPath().toAbsolutePath().toString());
        } catch (Throwable ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }

    }

    public static int ioUringEnter(int ringFd, int toSubmit, int minComplete, int flags) {
        int ret = ioUringEnter0(ringFd, toSubmit, minComplete, flags);
        if (ret < 0) {
            throw new RuntimeException("io_uring enter error: " + ret);
        }
        return ret;
    }

    public static long[][] setupIoUring(int entries, int flags, int sqThreadCpu, int cqEntries) {
        return setupIouring0(entries, flags, sqThreadCpu, cqEntries);
    }

    private static native int ioUringEnter0(int ringFd, int toSubmit, int minComplete, int flags);

    private static native long[][] setupIouring0(int entries, int flags, int sqThreadCpu, int cqEntries);

    public static byte IORING_OP_READ = Constants.ioRingOpRead();
    public static byte IORING_OP_WRITE = Constants.ioRingOpWrite();
    public static byte IORING_OP_CLOSE = Constants.ioRingOpClose();
    public static byte IORING_OP_OPENAT = Constants.ioRingOpenAt();
    public static byte IORING_OP_NOP = Constants.ioRingOpNop();

    public static int IORING_ENTER_GETEVENTS = Constants.ioRingEnterGetEvents();
    public static int IORING_ENTER_SQ_WAKEUP = Constants.ioRingEnterSqWakeup();
    public static int IORING_SQ_NEED_WAKEUP = Constants.ioRingSqNeedWakeup();

}
