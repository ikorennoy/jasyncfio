package one.jasyncfio;

class BufRingDescriptor {

    private final int bufRingSize;
    private final int bufRingBufSize;

    private final short bufRingId;

    BufRingDescriptor(int bufRingSize, int bufRingBufSize, short bufRingId) {
        this.bufRingSize = bufRingSize;
        this.bufRingBufSize = bufRingBufSize;
        this.bufRingId = bufRingId;
    }


    public int getBufRingSize() {
        return bufRingSize;
    }

    public int getBufRingBufSize() {
        return bufRingBufSize;
    }

    public short getBufRingId() {
        return bufRingId;
    }
}
