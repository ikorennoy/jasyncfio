package one.jasyncfio;

import cn.danielw.fop.DisruptorObjectPool;
import cn.danielw.fop.ObjectPool;
import cn.danielw.fop.PoolConfig;
import cn.danielw.fop.Poolable;

class Command<T> implements Runnable {
    private static final CommandAllocator<? extends Command<?>> allocator = new CommandAllocator<>();
    private static final PoolConfig poolConfig = new PoolConfig() {
        {
            setPartitionSize(50);
            setMinSize(100);
            setMaxSize(150);
        }
    };
    private static final ObjectPool<? extends Command<?>> pool = new DisruptorObjectPool<>(poolConfig, allocator);

    private byte op;
    private int flags;
    private int rwFlags;
    private int fd;
    private long bufferAddress;
    private int length;
    private long offset;
    private int bufIndex;
    private int fileIndex;
    private T operationResult;
    private EventExecutor executor;
    private PollableStatus pollableStatus;
    private Poolable<?> handle;
    private ResultProvider<T> resultProvider;


    byte getOp() {
        return op;
    }

    int getFlags() {
        return flags;
    }

    int getRwFlags() {
        return rwFlags;
    }

    int getFd() {
        return fd;
    }

    long getBufferAddress() {
        return bufferAddress;
    }

    int getLength() {
        return length;
    }

    long getOffset() {
        return offset;
    }

    int getBufIndex() {
        return bufIndex;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    PollableStatus getPollableStatus() {
        return pollableStatus;
    }

    T getOperationResult() {
        return operationResult;
    }

    EventExecutor getExecutor() {
        return executor;
    }

    Command() {
    }


    @Override
    public void run() {
        long opId = executor.scheduleCommand(this);
        executor.ringFromCommand(this).addOperation(this, opId);
    }

    void complete(Object obj) {
        try {
            resultProvider.onSuccess(obj);
        } finally {
            release();
        }
    }

    void complete(int result) {
        try {
            resultProvider.onSuccess(result);
        } finally {
            release();
        }
    }

    void error(Throwable ex) {
        try {
            resultProvider.onError(ex);
        } finally {
            release();
        }
    }

    static <T> Command<T> writeVectored(
            int fd,
            long offset,
            long iovecArrayAddress,
            int iovecArraySize,
            EventExecutor executor,
            ResultProvider<T> resultProvider
    ) {
        return init(
                Native.IORING_OP_WRITEV,
                0,
                0,
                fd,
                iovecArrayAddress,
                iovecArraySize,
                offset,
                0,
                0,
                null,
                executor,
                resultProvider
        );
    }

    static <T> Command<T> write(
            int fd,
            long offset,
            int length,
            long bufferAddress,
            PollableStatus pollableStatus,
            EventExecutor executor,
            ResultProvider<T> resultProvider
    ) {
        return init(
                Native.IORING_OP_WRITE,
                0,
                0,
                fd,
                bufferAddress,
                length,
                offset,
                0,
                0,
                pollableStatus,
                executor,
                resultProvider
        );
    }

    static <T> Command<T> readVectored(
            int fd,
            long offset,
            long length,
            int iovecArraySize,
            EventExecutor executor,
            ResultProvider<T> resultProvider
    ) {
        return init(
                Native.IORING_OP_READV,
                0,
                0,
                fd,
                length,
                iovecArraySize,
                offset,
                0,
                0,
                null,
                executor,
                resultProvider
        );
    }

    static <T> Command<T> read(
            int fd,
            long offset,
            int length,
            long bufferAddress,
            PollableStatus pollableStatus,
            EventExecutor executor,
            ResultProvider<T> resultProvider
    ) {
        return init(
                Native.IORING_OP_READ,
                0,
                0,
                fd,
                bufferAddress,
                length,
                offset,
                0,
                0,
                pollableStatus,
                executor,
                resultProvider
        );
    }

    static <T> Command<T> readBufRing(
            int fd,
            int offset,
            int length,
            PollableStatus pollableStatus,
            EventExecutor executor,
            ResultProvider<T> resultProvider
    ) {
        return init(
                Native.IORING_OP_READ,
                Native.IOSQE_BUFFER_SELECT,
                0,
                fd,
                0,
                length,
                offset,
                0,
                0,
                pollableStatus,
                executor,
                resultProvider
        );
    }

    static <T> Command<T> size(
            long pathAddress,
            long statxBuffer,
            EventExecutor executor,
            ResultProvider<T> resultProvider
    ) {
        return init(
                Native.IORING_OP_STATX,
                0,
                0,
                -1,
                pathAddress,
                Native.STATX_SIZE,
                statxBuffer,
                0,
                0,
                null,
                executor,
                resultProvider
        );
    }

    static <T> Command<T> openAt(
            int openFlags,
            long pathPtr,
            int mode,
            EventExecutor executor,
            ResultProvider<T> resultProvider
    ) {
        return init(
                Native.IORING_OP_OPENAT,
                0,
                openFlags,
                -1,
                pathPtr,
                mode,
                0,
                0,
                0,
                null,
                executor,
                resultProvider
        );
    }

    static <T> Command<T> close(
            int fd,
            EventExecutor executor,
            ResultProvider<T> resultProvider
    ) {
        return init(
                Native.IORING_OP_CLOSE,
                0,
                0,
                fd,
                0,
                0,
                0,
                0,
                0,
                null,
                executor,
                resultProvider
        );
    }

    static <T> Command<T> splice(
            int srcFd,
            long srcOffst,
            int dstFd,
            long dstOffset,
            int len,
            int flags,
            EventExecutor executor,
            ResultProvider<T> resultProvider
    ) {
        return init(
                Native.IORING_OP_SPLICE,
                0,
                flags,
                dstFd,
                srcOffst,
                len,
                dstOffset,
                0,
                srcFd,
                null,
                executor,
                resultProvider
        );
    }

    static <T> Command<T> dataSync(
            int fd,
            EventExecutor executor,
            ResultProvider<T> resultProvider
    ) {
        return init(
                Native.IORING_OP_FSYNC,
                0,
                Native.IORING_FSYNC_DATASYNC,
                fd,
                0,
                0,
                0,
                0,
                0,
                null,
                executor,
                resultProvider
        );
    }

    static <T> Command<T> preAllocate(
            int fd,
            long length,
            int mode,
            long offset,
            EventExecutor executor,
            ResultProvider<T> resultProvider
    ) {
        return init(
                Native.IORING_OP_FALLOCATE,
                0,
                0,
                fd,
                length,
                mode,
                offset,
                0,
                0,
                null,
                executor,
                resultProvider
        );
    }

    static <T> Command<T> unlink(
            int dirFd,
            long pathAddress,
            int flags,
            EventExecutor executor,
            ResultProvider<T> resultProvider
    ) {
        return init(
                Native.IORING_OP_UNLINKAT,
                0,
                flags,
                dirFd,
                pathAddress,
                0,
                0,
                0,
                0,
                null,
                executor,
                resultProvider
        );
    }

    private static <T> Command<T> init(
            byte op,
            int flags,
            int rwFlags,
            int fd,
            long bufferAddress,
            int length,
            long offset,
            int bufIndex,
            int fileIndex,
            PollableStatus pollableStatus,
            EventExecutor executor,
            ResultProvider<T> resultProvider
    ) {
        Poolable<Command<T>> handle = (Poolable<Command<T>>) pool.borrowObject();
        Command<T> command = handle.getObject();
        command.op = op;
        command.flags = flags;
        command.rwFlags = rwFlags;
        command.fd = fd;
        command.bufferAddress = bufferAddress;
        command.length = length;
        command.offset = offset;
        command.bufIndex = bufIndex;
        command.fileIndex = fileIndex;
        command.executor = executor;
        command.pollableStatus = pollableStatus;
        command.resultProvider = resultProvider;
        command.operationResult = resultProvider.getInner();
        command.handle = handle;
        return command;
    }

    void release() {
        op = 0;
        flags = 0;
        rwFlags = 0;
        fd = 0;
        bufferAddress = 0;
        length = 0;
        offset = 0;
        bufIndex = 0;
        fileIndex = 0;
        operationResult = null;
        resultProvider = null;
        executor = null;
        pollableStatus = null;
        handle.close();
    }

    @Override
    public String toString() {
        return "Command{" +
                "op=" + op +
                ", fd=" + fd +
                '}';
    }
}
