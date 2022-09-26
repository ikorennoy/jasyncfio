package one.jasyncfio;


import one.jasyncfio.collections.IntObjectMap;

class PollRing extends Ring {
    PollRing(int entries, int flags, int sqThreadIdle, int sqThreadCpu, int cqSize, int attachWqRingFd, IntObjectMap<Command<?>> commands) {
        super(entries, flags, sqThreadIdle, sqThreadCpu, cqSize, attachWqRingFd, false, 0, 0, commands);
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
