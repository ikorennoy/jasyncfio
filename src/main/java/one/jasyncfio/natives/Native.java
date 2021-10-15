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

    private static boolean isPowerOfTwo(int x) {
        return (x != 0) && ((x & (x - 1)) == 0);
    }

    public static Uring setupIoUring(int entries, int flags) {
        if (entries > 4096 || !isPowerOfTwo(entries)) {
            throw new IllegalArgumentException("entries must be power of 2 and less than 4096");
        }
        long[][] pointers = setupIouring0(entries, flags, 0, 0);
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
                (int) pointers[0][10]
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
                (int) pointers[1][8]
        );
        return new Uring(completionQueue, submissionQueue);
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
