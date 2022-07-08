package one.jasyncfio;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class AbstractFile {
    private final String path;
    private final long pathAddress;
    private final int fd;
    private final PollableStatus pollableStatus;
    private final EventExecutor executor;

    AbstractFile(String path, long pathAddress, int fd, PollableStatus pollableStatus, EventExecutor executor) {
        this.path = path;
        this.pathAddress = pathAddress;
        this.fd = fd;
        this.pollableStatus = pollableStatus;
        this.executor = executor;
    }

    /**
     * Reads a sequence of bytes from this file into the given buffer.
     * <p>
     * Bytes are read starting at this file current position, and
     * then the file position is updated with the number of bytes actually
     * read.
     *
     * @param buffer The buffer into which bytes are to be transferred. Must be allocated with {@link ByteBuffer#allocateDirect(int)}
     * @return {@link CompletableFuture} with the number of bytes read, possibly zero
     */
    public CompletableFuture<Integer> read(ByteBuffer buffer) {
        return read(buffer, buffer.limit());
    }

    /**
     * Reads a sequence of bytes from this file into a subsequence of the
     * given buffers.
     * <p>
     * Bytes are read starting at offset, and
     * then the file position is updated with the number of bytes actually
     * read.
     *
     * @param buffers  The buffers into which bytes are to be transferred. Must be allocated with [ByteBuffer.allocateDirect]
     * @param position The file position at which the transfer is to begin; must be non-negative
     * @param length   The maximum number of buffers to be accessed; must be non-negative and no larger than buffers
     * @return {@link CompletableFuture} with the number of bytes read, possibly zero
     */
    public CompletableFuture<Integer> read(ByteBuffer[] buffers, long position, int length) {
        IovecArray iovecArray = new IovecArray(buffers);
        CompletableFuture<Integer> result = executor.executeCommand(
                Command.readVectored(
                        fd,
                        position,
                        iovecArray.getIovecArrayAddress(),
                        length,
                        executor,
                        CompletableFutureResultProvider.newInstance()
                )
        );
        return result.whenComplete((res, ex) -> iovecArray.updatePositions(res));
    }

    /**
     * Reads a sequence of bytes from this file into the given buffers.
     * <p>
     * Bytes are read starting at this file current position, and
     * then the file position is updated with the number of bytes actually
     * read.
     *
     * @param buffers The buffers into which bytes are to be transferred. Must be allocated with [ByteBuffer.allocateDirect]
     * @return {@link CompletableFuture} with the number of bytes read, possibly zero
     */
    public CompletableFuture<Integer> read(ByteBuffer[] buffers) {
        return read(buffers, -1, buffers.length);
    }

    /**
     * Writes a sequence of bytes to this file from the given buffer.
     * <p>
     * Bytes are written starting at this file current position
     * unless the file is in append mode, in which case the position is
     * first advanced to the end of the file. The file is grown, if necessary,
     * to accommodate the written bytes, and then the file position is updated
     * with the number of bytes actually written.
     *
     * @param buffer The buffer from which bytes are to be retrieved. Must be allocated with [ByteBuffer.allocateDirect]
     * @return {@link CompletableFuture} with the number of bytes written, possibly zero
     */
    public CompletableFuture<Integer> write(ByteBuffer buffer) {
        return write(buffer, buffer.limit());
    }

    /**
     * Writes a sequence of bytes from a subsequence of the
     * given buffers to this file.
     * <p>
     * Bytes are written starting at given position and write up to given length.
     *
     * @param buffers  The buffer from which bytes are to be retrieved. Must be allocated with [ByteBuffer.allocateDirect]
     * @param position The file position at which the transfer is to begin; must be non-negative
     * @param length   The maximum number of buffers to be accessed; must be non-negative and no larger than buffers
     * @return {@link CompletableFuture} with the number of bytes read, possibly zero
     */

    public CompletableFuture<Integer> write(ByteBuffer[] buffers, long position, int length) {
        IovecArray iovecArray = new IovecArray(buffers);
        CompletableFuture<Integer> result = executor.executeCommand(
                Command.writeVectored(
                        fd,
                        position,
                        iovecArray.getIovecArrayAddress(),
                        length,
                        executor,
                        CompletableFutureResultProvider.newInstance()
                )
        );
        return result.whenComplete((written, ex) -> iovecArray.updatePositions(written));
    }

    /**
     * Returns the size of a file, in bytes.
     *
     * @return file size in bytes
     */
    public CompletableFuture<Long> size() {
        long statxBuffer = MemoryUtils.allocateMemory(StatxUtils.BUF_SIZE);
        CompletableFuture<Integer> res = executor.executeCommand(
                Command.size(
                        pathAddress,
                        statxBuffer,
                        executor,
                        CompletableFutureResultProvider.newInstance()
                )
        );
        return res.thenApply(r -> {
            long size = StatxUtils.getSize(statxBuffer);
            MemoryUtils.freeMemory(statxBuffer);
            return size;
        });
    }

    /**
     * Issues fdatasync for the underlying file, instructing the OS to flush all writes to the device,
     * providing durability even if the system crashes or is rebooted.
     */
    public CompletableFuture<Integer> dataSync() {
        return executor.executeCommand(
                Command.dataSync(
                        fd,
                        executor,
                        CompletableFutureResultProvider.newInstance()
                )
        );
    }

    /**
     * Reads a sequence of bytes from this file to the given buffer,
     * starting at the given file position.
     * This method works in the same manner as the [AbstractFile.read(ByteBuffer)]
     * method, except that bytes are read starting at
     * the given file position rather than at the file current position.
     *
     * @param buffer   The buffer into which bytes are to be transferred. Must be allocated with [ByteBuffer.allocateDirect]
     * @param position The file position at which the transfer is to begin; must be non-negative
     * @return {@link CompletableFuture} with the number of bytes written, possibly zero
     */
    public CompletableFuture<Integer> read(ByteBuffer buffer, long position) {
        return read(buffer, position, buffer.limit());
    }

    /**
     * Writes a sequence of bytes to this file from the given buffer,
     * starting at the given file position.
     * This method works in the same manner as the [AbstractFile.write(ByteBuffer)]
     * method, except that bytes are written starting at
     * the given file position rather than at the file's current position.
     *
     * @param buffers  The buffer from which bytes are to be retrieved. Must be allocated with [ByteBuffer.allocateDirect]
     * @param position The file position at which the transfer is to begin; must be non-negative
     * @return {@link CompletableFuture} with the number of bytes written, possibly zero
     */
    public CompletableFuture<Integer> write(ByteBuffer buffer, long position) {
        return write(buffer, position, buffer.limit());
    }

    /**
     * Reads a sequence of bytes from this file to the given buffer,
     * starting at the given file position and read up to length bytes.
     * This method works in the same manner as the [AbstractFile.read(ByteBuffer)]
     * method, except that bytes are read starting at
     * the given file position rather than at the file current position.
     *
     * @param position The file position at which the transfer is to begin; must be non-negative
     * @param length   The content length; must be non-negative
     * @param buffer   The buffer into which bytes are to be transferred. Must be allocated with [ByteBuffer.allocateDirect]
     * @return {@link CompletableFuture} with the number of bytes read
     */
    public CompletableFuture<Integer> read(ByteBuffer buffer, long position, int length) {
        if (buffer.remaining() == 0) {
            return CompletableFuture.completedFuture(0);
        }
        int bufPosition = buffer.position();
        CompletableFuture<Integer> res = executor.executeCommand(
                Command.read(
                        fd,
                        position,
                        length,
                        MemoryUtils.getDirectBufferAddress(buffer) + bufPosition,
                        pollableStatus,
                        executor,
                        CompletableFutureResultProvider.newInstance()
                )
        );
        return res.whenComplete((read, ex) -> {
            if (read > 0) {
                buffer.position(bufPosition + read);
            }
        });
    }


    /**
     * Reads a sequence of bytes from this file to the given buffer,
     * starting at the given file position and read up to length bytes.
     * This method works in the same manner as the [AbstractFile.read(ByteBuffer)]
     * method, except that bytes are read starting at
     * the given file position rather than at the file current position.
     *
     * @param length The content length; must be non-negative
     * @param buffer The buffer in which the bytes are to be read
     * @return {@link CompletableFuture} with the number of bytes read
     */
    public CompletableFuture<Integer> read(ByteBuffer buffer, int length) {
        return read(buffer, -1, length);
    }

    /**
     * Writes the data with in the byte buffer the specified length starting at the given file position.
     * If the given position is greater than the file's current size then the file will be grown to accommodate the new bytes;
     * the values of any bytes between the previous end-of-file and the newly-written bytes are unspecified.
     *
     * @param position The file position at which the transfer is to begin; must be non-negative
     * @param length   The content length; must be non-negative
     * @param buffer   The buffer from which bytes are to be retrieved
     * @return {@link CompletableFuture} with the number of bytes written
     */
    public CompletableFuture<Integer> write(ByteBuffer buffer, long position, int length) {
        if (buffer.remaining() == 0) {
            return CompletableFuture.completedFuture(0);
        }
        int bufPosition = buffer.position();
        CompletableFuture<Integer> executionRes = executor.executeCommand(
                Command.write(
                        fd,
                        position,
                        length,
                        MemoryUtils.getDirectBufferAddress(buffer) + bufPosition,
                        pollableStatus,
                        executor,
                        CompletableFutureResultProvider.newInstance()
                )
        );
        return executionRes.whenComplete((written, ex) -> {
            if (written > 0) {
                buffer.position(bufPosition + written);
            }
        });
    }

    /**
     * Writes a sequence of bytes to this file from the given buffer,
     * starting at this file current position
     * unless the file is in append mode, in which case the position is
     * first advanced to the end of the file. The file is grown, if necessary,
     * to accommodate the written bytes, and then the file position is updated
     * with the number of bytes actually written.
     *
     * @param length number of bytes to write
     * @param buffer The buffer from which bytes are to be retrieved
     * @return {@link CompletableFuture} with the number of bytes written
     */
    public CompletableFuture<Integer> write(ByteBuffer buffer, int length) {
        return write(buffer, -1, length);
    }

    /**
     * Writes a sequence of bytes from a subsequence of the
     * given buffers to this file.
     * <p>
     * Bytes are written starting at given position.
     *
     * @param buffers  The buffer from which bytes are to be retrieved. Must be allocated with [ByteBuffer.allocateDirect]
     * @param position The file position at which the transfer is to begin; must be non-negative
     * @return {@link CompletableFuture} with the number of bytes written, possibly zero
     */
    public CompletableFuture<Integer> write(ByteBuffer[] buffers, long position) {
        return write(buffers, position, buffers.length);
    }

    /**
     * Writes a sequence of bytes from this file into the given buffer.
     * <p>
     * Bytes are read starting at this file current position, and
     * then the file position is updated with the number of bytes actually
     * read.
     *
     * @param buffers The buffer into which bytes are to be transferred. Must be allocated with [ByteBuffer.allocateDirect]
     * @return {@link CompletableFuture} with the number of bytes written, possibly zero
     */
    public CompletableFuture<Integer> write(ByteBuffer[] buffers) {
        return write(buffers, -1);
    }

    /**
     * Pre-allocates space in the filesystem to hold a file at least as big as the size argument from specified offset.
     * After a successful call, subsequent writes into the range
     * specified by offset and len are guaranteed not to fail because of
     * lack of disk space.
     *
     * @param size   bytes to allocate; must be non-negative
     * @param offset start offset; must be non-negative
     */
    public CompletableFuture<Integer> preAllocate(long size, long offset) {
        return executor.executeCommand(
                Command.preAllocate(
                        fd,
                        size,
                        0,
                        offset,
                        executor,
                        CompletableFutureResultProvider.newInstance()
                )
        );
    }

    /**
     * Remove this file.
     * The file does not have to be closed to be removed.
     * Removing removes the name from the filesystem but the file will still be accessible for as long as it is open.
     */
    public CompletableFuture<Integer> remove() {
        return executor.executeCommand(
                Command.unlink(
                        -1,
                        pathAddress,
                        0,
                        executor,
                        CompletableFutureResultProvider.newInstance()
                )
        );
    }

    /**
     * Asynchronously closes this file.
     */
    public CompletableFuture<Integer> close() {
        MemoryUtils.freeMemory(pathAddress);
        return executor.executeCommand(
                Command.close(
                        fd,
                        executor,
                        CompletableFutureResultProvider.newInstance()
                )
        );
    }

    public int getRawFd() {
        return fd;
    }

    public String getPath() {
        return path;
    }
}
