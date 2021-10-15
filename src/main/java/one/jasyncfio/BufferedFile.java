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
        String str = Paths.get(path).toAbsolutePath().toString();
        long stringPtr = MemoryUtils.getStringPtr(str);
        CompletableFuture<Integer> integerCompletableFuture =
                EventExecutorGroup.get().scheduleOpenBuffered(-1, stringPtr, Native.O_RDONLY);
        return integerCompletableFuture.handle((i, e) -> {
            MemoryUtils.releaseString(str, stringPtr);
            if (e == null) {
                return new BufferedFile(path, i);
            } else {
                e.printStackTrace();
                return null;
            }
        });
    }

}
