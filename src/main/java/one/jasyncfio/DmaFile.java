package one.jasyncfio;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class DmaFile  extends AbstractFile{
    public static final int DEFAULT_ALIGNMENT = (int) Native.getPageSize();
    DmaFile(String path, long pathAddress, int fd, PollableStatus pollableStatus, EventExecutor executor) {
        super(path, pathAddress, fd, pollableStatus, executor);
    }

    public static CompletableFuture<DmaFile> open(Path path, EventExecutor executor, OpenOption... openOptions) {
        return open(path.normalize().toAbsolutePath().toString(), 438, executor, openOptions);
    }

    public static CompletableFuture<DmaFile> open(Path path, EventExecutor executor) {
        return open(path.normalize().toAbsolutePath().toString(), 438, executor, OpenOption.READ_ONLY);
    }

    public static CompletableFuture<DmaFile> open(Path path, int mode, EventExecutor executor, OpenOption... openOptions) {
        return open(path.normalize().toAbsolutePath().toString(), mode, executor, openOptions);
    }

    public static CompletableFuture<DmaFile> open(String path, EventExecutor executor) {
        return open(path, 438, executor, OpenOption.READ_ONLY);
    }

    public static CompletableFuture<DmaFile> open(String path, EventExecutor executor, OpenOption... openOptions) {
        return open(path, 438, executor, openOptions);
    }

    public static CompletableFuture<DmaFile> open(String path, int mode, EventExecutor executor, OpenOption... openOptions) {
        long pathPtr = MemoryUtils.getStringPtr(path);
        return executor.executeCommand(
                Command.openAt(
                        OpenOption.toFlags(openOptions) | Native.O_DIRECT,
                        pathPtr,
                        mode,
                        executor,
                        CompletableFutureResultProvider.newInstance()
                )
        ).thenApply((fd) -> new DmaFile(path, pathPtr, fd, PollableStatus.NON_POLLABLE, executor));
    }
}
