package one.jasyncfio;

import com.tdunning.math.stats.TDigest;
import one.jasyncfio.collections.IntObjectMap;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

class SleepableRing extends Ring {
    private final int eventFd;

    SleepableRing(int entries,
                  int flags,
                  int sqThreadIdle,
                  int sqThreadCpu,
                  int cqSize,
                  int attachWqRingFd,
                  List<BufRingDescriptor> bufRingDescriptorList,
                  int eventFd,
                  IntObjectMap<Command<?>> commands,
                  boolean monitoringEnabled,
                  ConcurrentMap<Command<?>, Long> commandStarts,
                  TDigest commandExecutionDelays) {
        super(entries,
                flags,
                sqThreadIdle,
                sqThreadCpu,
                cqSize,
                attachWqRingFd,
                bufRingDescriptorList,
                commands,
                monitoringEnabled,
                commandStarts,
                commandExecutionDelays
        );
        this.eventFd = eventFd;

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
