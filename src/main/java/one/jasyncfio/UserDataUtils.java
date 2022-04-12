package one.jasyncfio;

public class UserDataUtils {

    static long encode(int fd, byte op, int data) {
        return ((long) data << 40) | ((op & 0xFFL) << 32) | fd & 0xFFFFFFFFL;
    }

    static void decode(int res, int flags, long udata, CompletionCallback callback) {
        int fd = (int) (udata & 0xFFFFFFFFL);
        byte op = (byte) ((udata >>>= 32) & 0xFFL);
        int data = (int) (udata >>> 8);
        callback.handle(fd, res, flags, op, data);
    }
}
