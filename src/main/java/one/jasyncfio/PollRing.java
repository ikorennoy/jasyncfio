package one.jasyncfio;

import com.tdunning.math.stats.TDigest;
import one.jasyncfio.collections.IntObjectMap;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

class PollRing extends Ring {

    PollRing(int entries,
             int flags,
             int sqThreadIdle,
             int sqThreadCpu,
             int cqSize,
             int attachWqRingFd,
             List<BufRingDescriptor> bufRingDescriptorList,
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
    }

    @Override
    void park() {
        throw new UnsupportedOperationException("Can't park poll ring");
    }

    @Override
    void unpark() {
        throw new UnsupportedOperationException("Can't unpark poll ring");
    }
}
