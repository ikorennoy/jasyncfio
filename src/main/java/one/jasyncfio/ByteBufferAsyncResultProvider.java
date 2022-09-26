package one.jasyncfio;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class ByteBufferAsyncResultProvider implements ResultProvider<CompletableFuture<ByteBuffer>> {
    @Override
    public void onSuccess(Object result) {

    }

    @Override
    public void onError(Throwable ex) {

    }

    @Override
    public CompletableFuture<ByteBuffer> getInner() {
        return null;
    }

    @Override
    public void release() {

    }
}
