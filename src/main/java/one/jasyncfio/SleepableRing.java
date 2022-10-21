package one.jasyncfio;

import com.tdunning.math.stats.TDigest;
import one.jasyncfio.collections.IntObjectMap;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

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
                  List<BufRingDescriptor> bufRingDescriptorList,
                  int eventFd,
                  long eventFdBuffer,
                  EventExecutor executor,
                  IntObjectMap<Command<?>> commands,
                  ConcurrentMap<Command<?>, Long> commandStarts, TDigest commandExecutionDelays) {
        super(entries, flags, sqThreadIdle, sqThreadCpu, cqSize, attachWqRingFd, bufRingDescriptorList, commands, commandStarts, commandExecutionDelays);
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
