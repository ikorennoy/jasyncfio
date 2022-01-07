package one.jasyncfio;

import one.jasyncfio.natives.MemoryUtils;
import one.jasyncfio.natives.Native;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import static one.jasyncfio.natives.MemoryUtils.allocateAlignedByteBuffer;

public class DmaFile extends AbstractFile {
    public static final int READ_ALIGNMENT = 512;
    public static final int WRITE_ALIGNMENT = 4096;

    DmaFile(int fd, String path, long pathAddress) {
        super(fd, path, pathAddress);
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
        int flags = Native.O_RDONLY | Native.O_CLOEXEC | Native.O_DIRECT;
        CompletableFuture<Integer> futureFd =
                EventExecutorGroup.get().scheduleOpenAt(-1, pathPtr, flags, 0);
        return futureFd
                .thenApply((fd) -> new DmaFile(fd, path, pathPtr));
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
        int flags = Native.O_RDWR | Native.O_CREAT | Native.O_TRUNC | Native.O_DIRECT;
        CompletableFuture<Integer> futureFd =
                EventExecutorGroup.get().scheduleOpenAt(-1, pathPtr, flags, 0666);
        return futureFd
                .thenApply((fd) -> new BufferedFile(fd, path, pathPtr));
    }

    /**
     * Reads data from a specified position and of a specified length into a byte buffer.
     * <p>
     * It is not necessary to respect the `O_DIRECT` alignment of the file, and
     * this API will internally convert the positions and sizes to match, at a cost.
     * <p>
     * Limit of the result ByteBuffer will be installed at read bytes
     *
     * @param position The file position at which the transfer is to begin; must be non-negative
     * @param length   The content length; must be non-negative
     * @return {@link CompletableFuture} with the result ByteBuffer
     */
    public CompletableFuture<ByteBuffer> readAligned(long position, int length) {
        final long effectivePosition = alignDown(position, READ_ALIGNMENT);
        final long b = (position - effectivePosition);
        final int effectiveSize = alignUp((length + b), READ_ALIGNMENT);
        ByteBuffer byteBuffer = allocateAlignedByteBuffer(effectiveSize, READ_ALIGNMENT);
        CompletableFuture<Integer> read = read(effectivePosition, effectiveSize, byteBuffer);
        return read.thenApply((result) -> {
            byteBuffer.limit(Math.min(result, length));
            return byteBuffer;
        });
    }

    public static int alignUp(long v, long align) {
        return (int) ((v + align - 1) & -align);
    }

    public static int alignDown(long v, long align) {
        return (int) (v & -align);
    }

}
