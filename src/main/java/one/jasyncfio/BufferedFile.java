package one.jasyncfio;

import one.jasyncfio.natives.MemoryUtils;
import one.jasyncfio.natives.Native;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class BufferedFile {
    private final int fd;
    private final String path;
    private final long pathAddress;

    // todo sort args
    private BufferedFile(String path, int fd, long pathAddress) {
        this.path = path;
        this.fd = fd;
        this.pathAddress = pathAddress;
    }

    public int getRawFd() {
        return fd;
    }

    public String getPath() {
        return path;
    }

    /**
     * Attempts to open a file in read-only mode
     *
     * @param path path to file
     * @return {@link CompletableFuture} contains opened file or exception
     */
    public static CompletableFuture<BufferedFile> open(String path) {
        if (path == null) {
            throw new IllegalArgumentException("path must be not null");
        }
        long pathPtr = MemoryUtils.getStringPtr(path);
        CompletableFuture<Integer> futureFd =
                EventExecutorGroup.get().scheduleOpenAt(-1, pathPtr, Native.O_RDONLY, 0);
        return futureFd
                .thenApply((fd) -> new BufferedFile(path, fd, pathPtr));
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
        int flags = Native.O_RDWR | Native.O_CREAT | Native.O_TRUNC;
        CompletableFuture<Integer> futureFd =
                EventExecutorGroup.get().scheduleOpenAt(-1, pathPtr, flags, 0666);
        return futureFd
                .thenApply((fd) -> new BufferedFile(path, fd, pathPtr));
    }

    /**
     * Reads data from a specified position and of a specified length into a byte buffer.
     *
     * @param position The file position at which the transfer is to begin; must be non-negative
     * @param length   The content length; must be non-negative
     * @param buffer   The buffer from which bytes are to be retrieved
     * @return {@link CompletableFuture} with the number of bytes read
     */
    public CompletableFuture<Integer> read(long position, int length, ByteBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must be not null");
        } else if (position < 0L) {
            throw new IllegalArgumentException("position must be positive");
        } else if (length < 0L) {
            throw new IllegalArgumentException("length must be positive");
        }
        return EventExecutorGroup.get()
                .scheduleRead(fd, MemoryUtils.getDirectBufferAddress(buffer), position, length);
    }

    /**
     * Reads data at the specified position into a buffer.
     *
     * @param position start position
     * @param buffer   The buffer into which bytes are to be transferred
     * @return {@link CompletableFuture} with the number of bytes read
     */
    public CompletableFuture<Integer> read(long position, ByteBuffer buffer) {
        return read(position, buffer.limit(), buffer);
    }

    /**
     * Write the data with in the byte buffer the specified length starting at the given file position.
     * If the given position is greater than the file's current size then the file will be grown to accommodate the new bytes;
     * the values of any bytes between the previous end-of-file and the newly-written bytes are unspecified.
     *
     * @param position The file position at which the transfer is to begin; must be non-negative
     * @param length   The content length; must be non-negative
     * @param buffer   The buffer from which bytes are to be retrieved
     * @return {@link CompletableFuture} with the number of bytes written
     */

    public CompletableFuture<Integer> write(long position, int length, ByteBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must be not null");
        } else if (position < 0L) {
            throw new IllegalArgumentException("position must be positive");
        } else if (length < 0L) {
            throw new IllegalArgumentException("length must be positive");
        }
        return EventExecutorGroup.get()
                .scheduleWrite(fd, MemoryUtils.getDirectBufferAddress(buffer), position, length);
    }

    /**
     * Returns the size of a file, in bytes.
     *
     * @return {@link CompletableFuture} with file size
     */
    public CompletableFuture<Long> size() {
        long bufAddress = MemoryUtils.allocateMemory(StatxUtils.BUF_SIZE);
        return EventExecutorGroup.get()
                .scheduleStatx(-1, pathAddress, 0, Native.STATX_SIZE, bufAddress)
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
        return EventExecutorGroup.get().scheduleFsync(fd, Native.IORING_FSYNC_DATASYNC);
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
        return EventExecutorGroup.get().scheduleFallocate(fd, size, 0, offset);
    }


    /**
     * Remove this file.
     * <p>
     * The file does not have to be closed to be removed.
     * Removing removes the name from the filesystem but the file will still be accessible for as long as it is open.
     */
    public CompletableFuture<Integer> remove() {
        return EventExecutorGroup.get()
                .scheduleUnlink(-1, pathAddress, 0);
    }

    @Override
    protected void finalize() {
        MemoryUtils.releaseString(path, pathAddress);
    }

    /**
     * Closes this file.
     */
    public CompletableFuture<Integer> close() {
        return EventExecutorGroup.get().scheduleClose(fd);
    }
}
