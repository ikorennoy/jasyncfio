package one.jasyncfio;

import java.nio.ByteBuffer;

public class BufRingResult implements AutoCloseable {
    private ByteBuffer buffer;
    private int readBytes;
    private int bufferId;
    private Ring ownerRing;
    private short bufRingId;

    BufRingResult(ByteBuffer buffer, int readBytes, int bufferId, Ring ownerRing, short bufRingId) {
        this.buffer = buffer;
        this.readBytes = readBytes;
        this.bufferId = bufferId;
        this.ownerRing = ownerRing;
        this.bufRingId = bufRingId;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public int getReadBytes() {
        return readBytes;
    }

    int getBufferId() {
        return bufferId;
    }

    Ring getOwnerRing() {
        return ownerRing;
    }

    @Override
    public void close() throws Exception {
        ownerRing.recycleBuffer(bufferId, bufRingId);
    }
}
