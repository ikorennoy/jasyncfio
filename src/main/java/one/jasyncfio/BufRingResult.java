package one.jasyncfio;

import java.nio.ByteBuffer;

public class BufRingResult implements AutoCloseable {
    private ByteBuffer buffer;
    private int readBytes;
    private int bufferId;
    private Ring ownerRing;

    BufRingResult(ByteBuffer buffer, int readBytes, int bufferId, Ring ownerRing) {
        this.buffer = buffer;
        this.readBytes = readBytes;
        this.bufferId = bufferId;
        this.ownerRing = ownerRing;
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
        ownerRing.recycleBuffer(bufferId);
    }
}
