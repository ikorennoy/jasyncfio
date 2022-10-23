package one.jasyncfio;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class EventExecutor implements AutoCloseable {

    abstract <T> T executeCommand(Command<T> command);

    abstract <T> long scheduleCommand(Command<T> command);

    abstract <T> Ring ringFromCommand(Command<T> command);

    abstract void addEventFdRead();

    abstract void start();

    abstract int sleepableRingFd();

    abstract int getBufferLength(PollableStatus pollableStatus, short bufRingId);

    public abstract CompletableFuture<double[]> getWakeupLatencies(double[] percentiles);

    public abstract CompletableFuture<double[]> getCommandExecutionLatencies(double[] percentiles);

    public static class Builder {
        private int entries = 4096;
        private boolean ioRingSetupSqPoll = false;
        private int sqThreadIdle = 0;
        private boolean ioRingSetupSqAff = false;
        private int sqThreadCpu = 0;
        private boolean ioRingSetupCqSize = false;
        private int cqSize = 0;
        private boolean ioRingSetupClamp = false;
        private boolean ioRingSetupAttachWq = false;
        private int attachWqRingFd = 0;
        private long sleepTimeoutMs = 1000;

        private final List<BufRingDescriptor> bufRingDescriptors = new ArrayList<>();
        private boolean ioPoll;

        private Builder() {
        }

        /**
         * Denotes the number of Submission Queue Entries that will be associated with this io_uring instance.
         */
        public Builder entries(int entries) {
            this.entries = entries;
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

        /**
         * Setup buf ring with provided parameters. Later if you want to read to thus buf ring you must specify
         * the bufRingId provided for this call
         *
         * @param bufRingSize    number of buffers in the ring, must be power of 2
         * @param bufRingBufSize buffer size
         * @param bufRingId      id, which is used during the read request
         */
        public Builder addBufRing(int bufRingSize, int bufRingBufSize, short bufRingId) {
            if (bufRingBufSize <= 0) {
                throw new IllegalArgumentException("bufRingBufSize must be positive");
            }
            if (bufRingSize <= 0 || !isPowerOfTwo(bufRingSize)) {
                throw new IllegalArgumentException("bufRingSize must be positive and power of 2");
            }
            bufRingDescriptors.add(new BufRingDescriptor(bufRingSize, bufRingBufSize, bufRingId));
            return this;
        }


        /**
         * The time after which the EventLoop thread will be put to sleep. The higher the value, the higher
         * the CPU consumption, but the lower the latency, the lower the CPU consumption and the higher the latency.
         * Default value is 1000 milliseconds
         *
         * @param sleepTimeoutMs sleep timeout in milliseconds
         */
        public Builder sleepTimeout(long sleepTimeoutMs) {
            this.sleepTimeoutMs = sleepTimeoutMs;
            return this;
        }

        /**
         * Perform busy-waiting for an I/O completion, as opposed to getting notifications via an asynchronous IRQ (Interrupt Request).
         * The file system (if any) and block device must support polling in order for this to work.
         * Busy-waiting provides lower latency, but may consume more CPU resources than interrupt driven I/O.
         */
        public Builder setupIoPoll() {
            this.ioPoll = true;
            return this;
        }


        public EventExecutor build() {
            if (entries > 4096 || !isPowerOfTwo(entries)) {
                throw new IllegalArgumentException("entries must be power of 2 and less than 4096");
            }
            if (ioRingSetupCqSize && cqSize < entries) {
                throw new IllegalArgumentException("cqSize must be greater than entries");
            }
            if (ioRingSetupSqAff && !ioRingSetupSqPoll) {
                throw new IllegalArgumentException("IORING_SETUP_SQ_AFF is only meaningful when IORING_SETUP_SQPOLL is specified");
            }
            EventExecutor pollEventExecutor = new EventExecutorImpl(entries,
                    ioRingSetupSqPoll,
                    sqThreadIdle,
                    ioRingSetupSqAff,
                    sqThreadCpu,
                    ioRingSetupCqSize,
                    cqSize,
                    ioRingSetupClamp,
                    ioRingSetupAttachWq,
                    attachWqRingFd,
                    bufRingDescriptors,
                    sleepTimeoutMs,
                    ioPoll
            );
            pollEventExecutor.start();
            return pollEventExecutor;
        }

    }

    public static EventExecutor initDefault() {
        return new Builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    private static boolean isPowerOfTwo(int x) {
        return (x != 0) && ((x & (x - 1)) == 0);
    }
}
