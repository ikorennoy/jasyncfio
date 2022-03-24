package one.jasyncfio;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class BufferedFile extends AbstractFile {

    BufferedFile(int fd, String path, long pathAddress, AbstractEventExecutor abstractEventExecutor) {
        super(fd, path, pathAddress, abstractEventExecutor);
    }

    /**
     * Reads data at the specified position into the buffer.
     * For {@link  DmaFile} position and buffer must be properly aligned
     *
     * @param position start position
     * @param buffer   The buffer into which bytes are to be transferred
     * @return {@link CompletableFuture} with the number of bytes read
     */
    public CompletableFuture<Integer> read(long position, ByteBuffer buffer) {
        return read(position, buffer.capacity(), buffer);
    }

    /**
     * Writes data to the specified position into the file.
     * For {@link DmaFile} position and buffer must be properly aligned
     *
     * @param position start position
     * @param buffer   The buffer from which bytes are to be transferred
     * @return {@link  CompletableFuture} with the number of bytes written
     */
    public CompletableFuture<Integer> write(long position, ByteBuffer buffer) {
        return write(position, buffer.capacity(), buffer);
    }
}
