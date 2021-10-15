package one.jasyncfio;

import static one.jasyncfio.natives.Native.*;

public class ErrnoDecoder {

    public static Throwable decodeError(int errno) {
        final int err = -errno;
        if (err == EAGAIN) {
            return new RuntimeException("unable to allocate mem");
        } else if (err == EBUSY) {
            return new RuntimeException("event queue overflow");
        } else if (err == EBADF) {
            return new RuntimeException("invalid fd");
        } else if (err == EFAULT) {
            return new RuntimeException("invalid byte buffer");
        } else if (err == EINVAL) {
            return new RuntimeException("submission queue corrupted");
        } else if (err == ENXIO) {
            return new RuntimeException("io uring corrupted");
        } else if (err == EOPNOTSUPP) {
            return new RuntimeException("Operation not supported by kernel or wrong ring fd");
        } else if (err == EINTR) {
            return new RuntimeException("Operation was interrupted");
        } else if (err == ENOENT) {
            return new RuntimeException("No such file or directory");
        }
        return new RuntimeException("Unknown error: " + err);
    }
}
