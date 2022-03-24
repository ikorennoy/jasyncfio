package one.jasyncfio;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import static one.jasyncfio.natives.MemoryUtils.allocateAlignedByteBuffer;

public class DmaFile extends AbstractFile {
    public static final int DEFAULT_ALIGNMENT = 512;

    DmaFile(int fd, String path, long pathAddress, DefaultEventExecutor defaultEventExecutor) {
        super(fd, path, pathAddress, defaultEventExecutor);
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
        final long effectivePosition = alignDown(position, DEFAULT_ALIGNMENT);
        final long b = (position - effectivePosition);
        final int effectiveSize = alignUp((length + b), DEFAULT_ALIGNMENT);
        ByteBuffer byteBuffer = allocateAlignedByteBuffer(effectiveSize, DEFAULT_ALIGNMENT);
        CompletableFuture<Integer> read = read(effectivePosition, effectiveSize, byteBuffer);
        return read.thenApply((result) -> {
            byteBuffer.position((int) position).limit(Math.min(result, length));
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
