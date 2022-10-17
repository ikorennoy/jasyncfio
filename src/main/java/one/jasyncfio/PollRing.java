package one.jasyncfio;


import one.jasyncfio.collections.IntObjectMap;

import java.util.List;

class PollRing extends Ring {
    PollRing(int entries,
             int flags,
             int sqThreadIdle,
             int sqThreadCpu,
             int cqSize,
             int attachWqRingFd,
             List<BufRingDescriptor> bufRingDescriptorList,
             IntObjectMap<Command<?>> commands
    ) {
        super(entries, flags, sqThreadIdle, sqThreadCpu, cqSize, attachWqRingFd, bufRingDescriptorList, commands);
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
