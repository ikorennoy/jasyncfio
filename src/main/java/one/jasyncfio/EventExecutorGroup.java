package one.jasyncfio;

import one.jasyncfio.natives.IovecArray;
import one.jasyncfio.natives.MemoryUtils;
import one.jasyncfio.natives.Native;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ikorennoy
 * date 12.10.2021
 */
public class EventExecutorGroup {

    private final AtomicInteger sequencer = new AtomicInteger();
    private final EventExecutor[] executors;
    private final EventExecutor serviceRing;

    EventExecutorGroup(int numberOfRings,
                       int entries,
                       boolean ioRingSetupIoPoll,
                       boolean ioRingSetupSqPoll,
                       int sqThreadIdle,
                       boolean ioRingSetupSqAff,
                       int sqThreadCpu,
                       boolean ioRingSetupCqSize,
                       int cqSize,
                       boolean ioRingSetupClamp,
                       boolean ioRingSetupAttachWq,
                       int attachWqRingFd) {

        int flags = 0;
        if (ioRingSetupIoPoll) {
            flags |= Native.IORING_SETUP_IOPOLL;
        }
        if (ioRingSetupSqPoll) {
            flags |= Native.IORING_SETUP_SQPOLL;
        }
        if (ioRingSetupSqAff) {
            flags |= Native.IORING_SETUP_SQ_AFF;
        }
        if (ioRingSetupCqSize) {
            flags |= Native.IORING_SETUP_CQ_SIZE;
        }
        if (ioRingSetupClamp) {
            flags |= Native.IORING_SETUP_CLAMP;
        }
        if (ioRingSetupAttachWq) {
            flags |= Native.IORING_SETUP_ATTACH_WQ;
        }
        executors = new EventExecutor[numberOfRings];
        for (int i = 0; i < numberOfRings; i++) {
            executors[i] = new EventExecutor(entries, flags, sqThreadIdle, sqThreadCpu, cqSize, attachWqRingFd);
        }
        serviceRing = new EventExecutor(entries, 0, 0, 0, 0, 0);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static EventExecutorGroup initDefault() {
        return new Builder().build();
    }

    EventExecutor get() {
        if (executors.length == 1) {
            return executors[0];
        } else {
            return executors[sequencer.getAndIncrement() % executors.length];
        }
    }

    EventExecutor getServiceRing() {
        return serviceRing;
    }

    public CompletableFuture<IovecArray> registerBuffers(ByteBuffer[] buffers) {
        if (executors.length > 1) {
            throw new RuntimeException("The operation is not supported if more than one io_uring instance is configured");
        }
        IovecArray iovecBuffers = new IovecArray(buffers);
        return executors[0].registerBuffers(iovecBuffers).thenApply((r) -> iovecBuffers);
    }

    public CompletableFuture<Void> unregisterBuffers() {
        if (executors.length > 1) {
            throw new RuntimeException("The operation is not supported if more than one io_uring instance is configured");
        }
        return executors[0].unregisterBuffers();
    }

    public CompletableFuture<Void> registerFiles(AbstractFile[] files) {
        if (executors.length > 1) {
            throw new RuntimeException("The operation is not supported if more than one io_uring instance is configured");
        }
        ByteBuffer buffer = ByteBuffer.allocateDirect(files.length * 4);
        buffer.order(ByteOrder.nativeOrder());
        IntBuffer fds = buffer.asIntBuffer();
        for (AbstractFile file : files) {
            fds.put(file.fd);
        }
        return executors[0].registerFiles(fds, files.length);
    }

    public CompletableFuture<Void> unregisterFiles() {
        if (executors.length > 1) {
            throw new RuntimeException("The operation is not supported if more than one io_uring instance is configured");
        }
        return executors[0].unregisterFiles();
    }


    /**
     * Opens DmaFile, possible errors are described in the openAt man page.
     * Open file mode currently hardcoded to 666 (octal)
     *
     * @param path    path to file
     * @param options file open flags
     * @return {@link CompletableFuture} contains opened file or exception
     */
    public CompletableFuture<DmaFile> openDmaFile(String path, OpenOption... options) {
        if (path == null) {
            throw new IllegalArgumentException("path must be not null");
        }
        long pathPtr = MemoryUtils.getStringPtr(path);
        int flags = OpenOption.toFlags(options);
        flags |= Native.O_DIRECT;
        CompletableFuture<Integer> futureFd =
                serviceRing.scheduleOpenAt(-1, pathPtr, flags, 0666);
        return futureFd
                .thenApply((fd) -> new DmaFile(fd, path, pathPtr, this));
    }

    /**
     * Opens BufferedFile, possible errors are described in the openAt man page.
     * Open file mode currently hardcoded to 666 (octal)
     * Default access (no OpenOptions specified) is read only
     *
     * @param path    path to file
     * @param options file open flags
     * @return {@link CompletableFuture} contains opened file or exception
     */
    public CompletableFuture<BufferedFile> openBufferedFile(String path, OpenOption... options) {
        if (path == null) {
            throw new IllegalArgumentException("path must be not null");
        }
        long pathPtr = MemoryUtils.getStringPtr(path);
        int flags = OpenOption.toFlags(options);
        CompletableFuture<Integer> futureFd =
                serviceRing.scheduleOpenAt(-1, pathPtr, flags, 0666);
        return futureFd
                .thenApply((fd) -> new BufferedFile(fd, path, pathPtr, this));
    }

    public void stop() {
        serviceRing.stop();
        for (EventExecutor executor : executors) {
            executor.stop();
        }
    }


    public static class Builder {
        private int numberOfRings = 1;
        private int entries = 4096;
        private boolean ioRingSetupIoPoll = false;
        private boolean ioRingSetupSqPoll = false;
        private int sqThreadIdle = 0;
        private boolean ioRingSetupSqAff = false;
        private int sqThreadCpu = 0;
        private boolean ioRingSetupCqSize = false;
        private int cqSize = 0;
        private boolean ioRingSetupClamp = false;
        private boolean ioRingSetupAttachWq = false;
        private int attachWqRingFd = 0;

        private Builder() {
        }


        /**
         * The number of io_uring instances to be created.
         */
        public Builder numberOfRings(int numberOfRings) {
            this.numberOfRings = numberOfRings;
            return this;
        }

        /**
         * Denotes the number of Submission Queue Entries that will be associated with this io_uring instance.
         */
        public Builder entries(int entries) {
            this.entries = entries;
            return this;
        }

        /**
         * Perform busy-waiting for an I/O completion, as opposed to getting notifications via an
         * asynchronous IRQ (Interrupt Request).
         * The file system (if any) and block device must support polling in order for this to work.
         * Busy-waiting provides lower latency, but may consume more CPU resources than interrupt driven I/O.
         * Currently, this feature is usable only on a file descriptor opened using the O_DIRECT flag.
         * When a read or write is submitted to a polled context, the application must poll
         * for completions on the CQ ring by calling io_uring_enter(2).
         * It is illegal to mix and match polled and non-polled I/O on an io_uring instance.
         */
        public Builder ioRingSetupIoPoll() {
            this.ioRingSetupIoPoll = true;
            return this;
        }


        /**
         * When this flag is specified, a kernel thread is created to perform submission queue polling.
         * An io_uring instance configured in this way enables an application to issue I/O without ever context switching into the kernel.
         * By using the submission queue to fill in new submission queue entries and watching for completions on the completion queue,
         * the application can submit and reap I/Os without doing a single system call.
         *
         * @param sqThreadIdleMillis max kernel thread idle time in milliseconds
         */

        public Builder ioRingSetupSqPoll(int sqThreadIdleMillis) {
            if (sqThreadIdleMillis < 0) {
                throw new IllegalArgumentException("sqThreadIdleMillis < 0");
            }
            this.ioRingSetupSqPoll = true;
            this.sqThreadIdle = sqThreadIdleMillis;
            return this;
        }

        /**
         * If this flag is specified, then the poll thread will be bound to the cpu set in the sq_thread_cpu field of the struct io_uring_params.
         * This flag is only meaningful when IORING_SETUP_SQPOLL is specified.
         * When cgroup setting cpuset.cpus changes (typically in container environment), the bounded cpu set may be changed as well.
         *
         * @param sqThreadCpu cpu to bound to
         */
        public Builder ioRingSetupSqAff(int sqThreadCpu) {
            this.ioRingSetupSqAff = true;
            this.sqThreadCpu = sqThreadCpu;
            return this;
        }

        /**
         * Create the completion queue with struct io_uring_params.cq_entries entries.
         * The value must be greater than entries, and may be rounded up to the next power-of-two.
         */
        public Builder ioRingSetupCqSize(int cqSize) {
            if (cqSize < 0) {
                throw new IllegalArgumentException("cqSize < 0");
            }
            this.ioRingSetupCqSize = true;
            this.cqSize = cqSize;
            return this;
        }

        /**
         * If this flag is specified, and if entries exceeds IORING_MAX_ENTRIES, then entries will be clamped at IORING_MAX_ENTRIES.
         * If the flag IORING_SETUP_SQPOLL is set, and if the value of struct io_uring_params.cq_entries exceeds IORING_MAX_CQ_ENTRIES,
         * then it will be clamped at IORING_MAX_CQ_ENTRIES.
         */
        public Builder ioRingSetupClamp() {
            this.ioRingSetupClamp = true;
            return this;
        }

        /**
         * This flag should be set in conjunction with struct io_uring_params.wq_fd being set to an existing io_uring ring file descriptor.
         * When set, the io_uring instance being created will share the asynchronous worker thread backend of the specified io_uring ring,
         * rather than create a new separate thread pool.
         *
         * @param attachWqRingFd existing io_uring ring fd.
         */
        public Builder ioRingSetupAttachWq(int attachWqRingFd) {
            this.ioRingSetupAttachWq = true;
            this.attachWqRingFd = attachWqRingFd;
            return this;
        }


        public EventExecutorGroup build() {
            if (numberOfRings < 1) {
                throw new IllegalArgumentException("num of rings must be at least 1");
            }
            if (entries > 4096 || !isPowerOfTwo(entries)) {
                throw new IllegalArgumentException("entries must be power of 2 and less than 4096");
            }
            if (ioRingSetupCqSize && cqSize < entries) {
                throw new IllegalStateException("cqSize must be greater than entries");
            }
            if (ioRingSetupSqAff && !ioRingSetupSqPoll) {
                throw new IllegalArgumentException("IORING_SETUP_SQ_AFF is only meaningful when IORING_SETUP_SQPOLL is specified");
            }
            return new EventExecutorGroup(numberOfRings,
                    entries,
                    ioRingSetupIoPoll,
                    ioRingSetupSqPoll,
                    sqThreadIdle,
                    ioRingSetupSqAff,
                    sqThreadCpu,
                    ioRingSetupCqSize,
                    cqSize,
                    ioRingSetupClamp,
                    ioRingSetupAttachWq,
                    attachWqRingFd
            );
        }

        private static boolean isPowerOfTwo(int x) {
            return (x != 0) && ((x & (x - 1)) == 0);
        }

    }
}
