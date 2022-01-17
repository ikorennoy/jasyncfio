package one.jasyncfio;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class StreamReader {
    private final BufferedFile file;

    public StreamReader(BufferedFile file) {
        this.file = file;
    }


    public CompletableFuture<Integer> read(ByteBuffer buffer) {
        return file.read(-1, buffer);
    }


    public CompletableFuture<Integer> read(ByteBuffer buffer, Long position) {
        return file.read(position, buffer);
    }

    public long position() {
        return 0;
    }

    public void position(long newPosition) {

    }
}
