package one.jasyncfio;

import java.io.IOException;
import java.io.InterruptedIOException;

import static one.jasyncfio.natives.Native.*;

public class ErrnoDecoder {



    public static Throwable decodeIoUringError(int errno) {
        final Throwable result;
        final int err = -errno;
        if (err == EBADF) {
            result = new IllegalArgumentException("invalid file descriptor");
        } else if (err == EBADFD) {
            result = new IllegalStateException("io uring ring is not in the right state");
        } else if (err == EBUSY) {
            result = new IOException("the number of requests is overcommitted");
        } else if (err == EINVAL) {
            result = new IllegalArgumentException("incorrect flags");
        } else if (err == EFAULT) {
            result = new IllegalStateException("the io_uring instance is in the process of being torn down");
        } else if (err == EOPNOTSUPP) {
            result = new IllegalArgumentException("fd does not refer to an io_uring instance");
        } else if (err == EINTR) {
            result = new InterruptedIOException("the operation was interrupted by a delivery of a signal before it could complete");
        } else {
            result = new RuntimeException("unknown error: " + err);
        }
        return result;
    }

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
