package one.jasyncfio;

import one.jasyncfio.natives.MemoryUtils;
import one.jasyncfio.natives.Native;

import javax.print.attribute.standard.MediaSize;
import java.util.concurrent.CompletableFuture;

public class BufferedFile {
    private final int fd;
    private final String path;

    private BufferedFile(String path, int fd) {
        this.path = path;
        this.fd = fd;
    }

    public int getRawFd() {
        return fd;
    }

    public String getPath() {
        return path;
    }

    /**
     * Attempts to open a file in read-only mode
     *
     * @param path path to file
     * @return {@link CompletableFuture} contains opened file or exception
     */
    public static CompletableFuture<BufferedFile> open(String path) {
        long pathPtr = MemoryUtils.getStringPtr(path);
        CompletableFuture<Integer> futureFd =
                EventExecutorGroup.get().scheduleOpen(-1, pathPtr, Native.O_RDONLY, 0);
        return futureFd
                .whenComplete(((fd, throwable) -> MemoryUtils.releaseString(path, pathPtr)))
                .thenApply((fd) -> new BufferedFile(path, fd));
    }

    /**
     * Opens a file in read write mode.
     * This function will create a file if it does not exist, and will truncate it if it does.
     *
     * @param path path to file
     * @return {@link CompletableFuture} contains opened file or exception
     */
    public static CompletableFuture<BufferedFile> create(String path) {
        long pathPtr = MemoryUtils.getStringPtr(path);
        int flags = Native.O_RDWR | Native.O_CREAT | Native.O_TRUNC;
        CompletableFuture<Integer> futureFd =
                EventExecutorGroup.get().scheduleOpen(-1, pathPtr, flags, 0666);
        return futureFd
                .whenComplete(((fd, ex) -> MemoryUtils.releaseString(path, pathPtr)))
                .thenApply((fd) -> new BufferedFile(path, fd));
    }
}
