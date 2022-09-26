package one.jasyncfio;

import one.jasyncfio.collections.IntObjectMap;

class SleepableRing extends Ring {
    private final int eventFd;
    private final long eventFdBuffer;
    private final EventExecutor executor;

    SleepableRing(int entries,
                  int flags,
                  int sqThreadIdle,
                  int sqThreadCpu,
                  int cqSize,
                  int attachWqRingFd,
                  boolean withBufRing,
                  int bufRingBufSize,
                  int numOfBuffers,
                  int eventFd,
                  long eventFdBuffer,
                  EventExecutor executor,
                  IntObjectMap<Command<?>> commands
    ) {
        super(entries, flags, sqThreadIdle, sqThreadCpu, cqSize, attachWqRingFd, withBufRing, bufRingBufSize, numOfBuffers, commands);
        this.eventFd = eventFd;
        this.eventFdBuffer = eventFdBuffer;
        this.executor = executor;

    }

    @Override
    void park() {
        submissionQueue.submitAndWait();
    }

    @Override
    void unpark() {
        Native.eventFdWrite(eventFd, 1L);
    }
}
