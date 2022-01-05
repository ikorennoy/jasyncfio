package one.jasyncfio;

import java.io.IOException;
import java.io.InterruptedIOException;

import static one.jasyncfio.natives.Native.*;

// todo improve with glommio example
// add errno to error
public class ErrnoDecoder {

    public static Throwable decodeOpenAtError(int errno) {
        final Throwable result;
        final int err = -errno;
        if (err == EBADF) {
            result = new IllegalArgumentException("dirfd is not a valid file descriptor");
        } else if (err == ENOTDIR) {
            result = new IllegalArgumentException("pathname is relative and dirfd is a file descriptor referring to a file other than a directory");
        } else if (err == EACCES) {
            result = new SecurityException("the requested access to the file is not allowed");
        } else if (err == EDQUOT) {
            result = new IOException("can't create file, not enough resources");
        } else if (err == EEXIST) {
            result = new IOException("file already exist");
        } else if (err == EFAULT) {
            result = new SecurityException("wrong path address");
        } else if (err == EFBIG || err == EOVERFLOW) {
            result = new IOException("file is too big to open");
        } else if (err == EINTR) {
            result = new IOException("call was interrupted by a signal handler");
        } else if (err == EINVAL) {
            result = new IllegalArgumentException("direct io not supported, or wrong flag combination");
        } else if (err == EISDIR) {
            result = new SecurityException("path is dir");
        } else if (err == ELOOP) {
            result = new IOException("too many symbolic link");
        } else if (err == ENAMETOOLONG) {
            result = new IllegalArgumentException("pathname too long");
        } else if (err == ENFILE) {
            result = new IOException("the system-wide limit on the total number of open files has been reached");
        } else if (err == ENODEV) {
            result = new IllegalArgumentException("pathname device does not exist");
        } else if (err == ENOENT) {
            result = new IllegalArgumentException("pathname refers to non existing dir");
        } else if (err == ENOMEM) {
            result = new IOException("not enough memory");
        } else if (err == ENOSPC) {
            result = new IOException("not enough memory on device");
        } else if (err == ENXIO) {
            result = new IllegalArgumentException("the file is not suitable for operation");
        } else if (err == EOPNOTSUPP) {
            result = new IOException("the filesystem containing pathname does not support tmpfile");
        } else if (err == EPERM) {
            result = new SecurityException("not enough permissions");
        } else if (err == EROFS) {
            result = new SecurityException("read only filesystem");
        } else if (err == ETXTBSY) {
            result = new IllegalArgumentException("can't modify executable file");
        } else if (err == EWOULDBLOCK) {
            result = new IllegalArgumentException("incompatible lease was held on the file");
        } else {
            result = null;
        }
        return result;
    }

    public static Throwable decodeIoUringCqeError(int errno) {
        final Throwable result;
        final int err = -errno;
        if (err == EACCES) {
            result = new IllegalStateException("the flags field or opcode in a submission queue entry is not allowed due to registered restrictions");
        } else if (err == EBADF) {
            result = new IllegalArgumentException("the fd field in the submission queue entry is invalid or fixed files not registered");
        } else if (err == EFAULT) {
            result = new IllegalArgumentException("buffer address is wrong or buffers were not registered");
        } else if (err == EINVAL) {
            result = new IllegalArgumentException("invalid io arguments");
        } else if (err == EOPNOTSUPP) {
            result = new IllegalStateException("opcode is not supported by this kernel");
        } else {
            result = new RuntimeException("unknown error: " + err);
        }
        return result;
    }

    public static Throwable decodeIoUringEnterError(int errno) {
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
}
