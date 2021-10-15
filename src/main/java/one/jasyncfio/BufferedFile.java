package one.jasyncfio;

import one.jasyncfio.natives.MemoryUtils;
import one.jasyncfio.natives.Native;

import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public class BufferedFile {
    private final int fd;
    private final String path;

    private BufferedFile(String path, int fd) {
        this.path = path;
        this.fd = fd;
    }

    public int getFd() {
        return fd;
    }

    public static CompletableFuture<BufferedFile> open(String path) {
        long stringPtr = MemoryUtils.getStringPtr(path);
        CompletableFuture<Integer> futureFd =
                EventExecutorGroup.get().scheduleOpenBuffered(-1, stringPtr, Native.O_RDONLY);
        return futureFd
                .whenComplete(((integer, throwable) -> MemoryUtils.releaseString(path, stringPtr)))
                .thenApply((fd) -> new BufferedFile(path, fd));
    }
}
