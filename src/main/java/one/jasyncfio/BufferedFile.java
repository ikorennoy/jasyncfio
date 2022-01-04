package one.jasyncfio;

import one.jasyncfio.natives.MemoryUtils;
import one.jasyncfio.natives.Native;

import java.util.concurrent.CompletableFuture;

public class BufferedFile extends AbstractFile {

    BufferedFile(int fd, String path, long pathAddress) {
        super(fd, path, pathAddress);
    }

    /**
     * Attempts to open a file in read-only mode
     *
     * @param path path to file
     * @return {@link CompletableFuture} contains opened file or exception
     */
    public static CompletableFuture<BufferedFile> open(String path) {
        if (path == null) {
            throw new IllegalArgumentException("path must be not null");
        }
        long pathPtr = MemoryUtils.getStringPtr(path);
        CompletableFuture<Integer> futureFd =
                EventExecutorGroup.get().scheduleOpenAt(-1, pathPtr, Native.O_RDONLY, 0);
        return futureFd
                .thenApply((fd) -> new BufferedFile(fd, path, pathPtr));
    }

    /**
     * Opens a file in read write mode.
     * This function will create a file if it does not exist, and will truncate it if it does.
     *
     * @param path path to file
     * @return {@link CompletableFuture} contains opened file or exception
     */
    public static CompletableFuture<BufferedFile> create(String path) {
        if (path == null) {
            throw new IllegalArgumentException("path must be not null");
        }
        long pathPtr = MemoryUtils.getStringPtr(path);
        int flags = Native.O_RDWR | Native.O_CREAT | Native.O_TRUNC;
        CompletableFuture<Integer> futureFd =
                EventExecutorGroup.get().scheduleOpenAt(-1, pathPtr, flags, 0666);
        return futureFd
                .thenApply((fd) -> new BufferedFile(fd, path, pathPtr));
    }
}
