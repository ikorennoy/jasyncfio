package one.jasyncfio;

import one.jasyncfio.natives.IovecArray;
import one.jasyncfio.natives.MemoryUtils;
import one.jasyncfio.natives.Native;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

class AbstractFile {
    public final int fd;
    final String path;
    private final long pathAddress;
    private final EventExecutorGroup eventExecutorGroup;


    AbstractFile(int fd, String path, long pathAddress, EventExecutorGroup eventExecutorGroup) {
        this.fd = fd;
        this.path = path;
        this.pathAddress = pathAddress;
        this.eventExecutorGroup = eventExecutorGroup;
    }

    public int getRawFd() {
        return fd;
    }

    public String getPath() {
        return path;
    }

    /**
     * Reads data from a specified position and of a specified length into a byte buffer.
     * <p>
     * When using {@link DmaFile}  position, length and buffer must be properly aligned for DIRECT_IO.
     * In most platforms that means 512 bytes.
     *
     * @param position The file position at which the transfer is to begin; must be non-negative
     * @param length   The content length; must be non-negative
     * @param buffer   The buffer from which bytes are to be retrieved
     * @return {@link CompletableFuture} with the number of bytes read
     */
    public CompletableFuture<Integer> read(long position, int length, ByteBuffer buffer) {
        checkConstraints(position, length, buffer);
        return eventExecutorGroup.get().scheduleRead(fd, MemoryUtils.getDirectBufferAddress(buffer), position, length)
                .thenApply((result) -> {
                    buffer.limit(Math.min(result, length));
                    return result;
                });
    }

    public CompletableFuture<Integer> writeFixed(long position, int bufferIndex, IovecArray registeredBuffers) {
        IovecArray.Iovec buffer = registeredBuffers.getIovec(bufferIndex);
        return eventExecutorGroup.get()
                .scheduleWriteFixed(fd, buffer.getIovBase(), position, (int) buffer.getIovLen(), bufferIndex);
    }

    public CompletableFuture<Integer> readFixed(long position, int bufferIndex, IovecArray registeredBuffers) {
        IovecArray.Iovec buffer = registeredBuffers.getIovec(bufferIndex);
        return eventExecutorGroup.get()
                .scheduleReadFixed(fd, buffer.getIovBase(), position, (int) buffer.getIovLen(), bufferIndex);
    }

    /**
     * Write the data with in the byte buffer the specified length starting at the given file position.
     * If the given position is greater than the file's current size then the file will be grown to accommodate the new bytes;
     * the values of any bytes between the previous end-of-file and the newly-written bytes are unspecified.
     * <p>
     * When using {@link DmaFile}  position, length and buffer must be properly aligned for DIRECT_IO.
     * In most platforms that means 4096 bytes
     *
     * @param position The file position at which the transfer is to begin; must be non-negative
     * @param length   The content length; must be non-negative
     * @param buffer   The buffer from which bytes are to be retrieved
     * @return {@link CompletableFuture} with the number of bytes written
     */

    public CompletableFuture<Integer> write(long position, int length, ByteBuffer buffer) {
        checkConstraints(position, length, buffer);
        return eventExecutorGroup.get().scheduleWrite(fd, MemoryUtils.getDirectBufferAddress(buffer), position, length);
    }

    public CompletableFuture<Integer> write(long position, ByteBuffer[] buffers) {
        IovecArray iovecArray = new IovecArray(buffers);
        return eventExecutorGroup.get().scheduleWritev(fd, iovecArray.getIovecArrayAddress(), position, iovecArray.getCount());
    }

    public CompletableFuture<Integer> read(long position, ByteBuffer[] buffers) {
        IovecArray iovecArray = new IovecArray(buffers);
        return eventExecutorGroup.get().scheduleReadv(fd, iovecArray.getIovecArrayAddress(), position, iovecArray.getCount());
    }


    private void checkConstraints(long position, int length, ByteBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must be not null");
        } else if (length < 0L) {
            throw new IllegalArgumentException("length must be positive");
        }
        if (buffer.capacity() < length) {
            throw new IllegalArgumentException("buffer capacity less than length");
        }
    }

    /**
     * Returns the size of a file, in bytes.
     *
     * @return {@link CompletableFuture} with file size
     */
    public CompletableFuture<Long> size() {
        long bufAddress = MemoryUtils.allocateMemory(StatxUtils.BUF_SIZE);
        return eventExecutorGroup.getServiceRing().scheduleStatx(-1, pathAddress, 0, Native.STATX_SIZE, bufAddress)
                .thenApply((r) -> {
                    long size = StatxUtils.getSize(bufAddress);
                    MemoryUtils.freeMemory(bufAddress);
                    return size;
                });
    }

    /**
     * Issues fdatasync for the underlying file, instructing the OS to flush all writes to the device,
     * providing durability even if the system crashes or is rebooted.
     */
    public CompletableFuture<Integer> dataSync() {
        return eventExecutorGroup.getServiceRing().scheduleFsync(fd, Native.IORING_FSYNC_DATASYNC);
    }

    /**
     * Pre-allocates space in the filesystem to hold a file at least as big as the size argument from 0 offset.
     *
     * @param size bytes to allocate; must be non-negative
     */
    public CompletableFuture<Integer> preAllocate(long size) {
        return preAllocate(size, 0);
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
        if (size < 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be positive");
        }
        return eventExecutorGroup.getServiceRing().scheduleFallocate(fd, size, 0, offset);
    }


    /**
     * Remove this file.
     * <p>
     * The file does not have to be closed to be removed.
     * Removing removes the name from the filesystem but the file will still be accessible for as long as it is open.
     */
    public CompletableFuture<Integer> remove() {
        return eventExecutorGroup.getServiceRing().scheduleUnlink(-1, pathAddress, 0);
    }

    @Override
    protected void finalize() {
        MemoryUtils.releaseString(path, pathAddress);
    }

    /**
     * Asynchronously closes this file.
     */
    public CompletableFuture<Integer> close() {
        return eventExecutorGroup.getServiceRing().scheduleClose(fd);
    }

}
