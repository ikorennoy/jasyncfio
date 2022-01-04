package one.jasyncfio;

import one.jasyncfio.natives.MemoryUtils;
import one.jasyncfio.natives.Native;

import java.util.concurrent.CompletableFuture;

public class DmaFile {
    private final int fd;
    private final String path;
    private final long pathAddress;


    private DmaFile(int fd, String path, long pathAddress) {
        this.fd = fd;
        this.path = path;
        this.pathAddress = pathAddress;
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
    public static CompletableFuture<DmaFile> open(String path) {
        if (path == null) {
            throw new IllegalArgumentException("path must be not null");
        }
        long pathPtr = MemoryUtils.getStringPtr(path);
        CompletableFuture<Integer> futureFd =
                EventExecutorGroup.get().scheduleOpenAt(-1, pathPtr, Native.O_RDONLY | Native.O_DIRECT, 0);
        return futureFd
                .thenApply((fd) -> new DmaFile(fd, path, pathPtr));
    }


}
