package one.jasyncfio;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class BufferedFileChannel {
    private final BufferedFile file;

    public BufferedFileChannel(BufferedFile file) {
        this.file = file;
    }


    public CompletableFuture<Integer> read(ByteBuffer buffer) {
        return file.read(-1, buffer);
    }

    public CompletableFuture<Integer> read(ByteBuffer buffer, Long position) {
        return file.read(position, buffer);
    }

    public CompletableFuture<Integer> write(ByteBuffer buffer) {
        return file.write(-1, buffer);
    }

    public CompletableFuture<Integer> write(ByteBuffer buffer, Long position) {
        return file.write(position, buffer);
    }


    public CompletableFuture<Integer> close() {
        return file.close();
    }
}
