package one.jasyncfio;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class BufferedFile extends AbstractFile {

    BufferedFile(int fd, String path, long pathAddress, EventExecutor eventExecutor) {
        super(fd, path, pathAddress, eventExecutor);
    }

    /**
     * Reads data at the specified position into a buffer.
     * For {@link  DmaFile} position must be properly aligned
     *
     * @param position start position
     * @param buffer   The buffer into which bytes are to be transferred
     * @return {@link CompletableFuture} with the number of bytes read
     */
    public CompletableFuture<Integer> read(long position, ByteBuffer buffer) {
        return read(position, buffer.capacity(), buffer);
    }
}
