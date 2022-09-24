package one.jasyncfio;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class BufferedFile extends AbstractFile {

    private BufferedFile(String path, long pathAddress, int fd, PollableStatus pollableStatus, EventExecutor executor) {
        super(path, pathAddress, fd, pollableStatus, executor);
    }

    public static CompletableFuture<BufferedFile> open(Path path, EventExecutor executor, OpenOption... openOption) {
        return open(path, 438, executor, openOption);
    }

    public static CompletableFuture<BufferedFile> open(Path path, EventExecutor executor) {
        return open(path, 438, executor, OpenOption.READ_ONLY);
    }

    public static CompletableFuture<BufferedFile> open(Path path, int mode, EventExecutor executor, OpenOption... openOption) {
        return open(path.normalize().toAbsolutePath().toString(), mode, executor, openOption);
    }

    public static CompletableFuture<BufferedFile> open(String path, EventExecutor executor) {
        return open(path, 438, executor, OpenOption.READ_ONLY);
    }

    public static CompletableFuture<BufferedFile> open(String path, EventExecutor executor, OpenOption... openOption) {
        return open(path, 438, executor, openOption);
    }

    public static CompletableFuture<BufferedFile> open(String path, int mode, EventExecutor executor, OpenOption... openOption) {
        long patAddress = MemoryUtils.getStringPtr(path);
        return executor.executeCommand(Command.openAt(
                OpenOption.toFlags(openOption),
                patAddress,
                mode,
                executor,
                AsyncResultProvider.newInstance()
        )).thenApply((res) -> new BufferedFile(path, patAddress, res, PollableStatus.NON_POLLABLE, executor));
    }
}
