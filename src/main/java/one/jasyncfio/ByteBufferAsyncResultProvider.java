package one.jasyncfio;

import java.util.concurrent.CompletableFuture;

class ByteBufferAsyncResultProvider implements ResultProvider<CompletableFuture<BufRingResult>> {

    private final CompletableFuture<BufRingResult> res = new CompletableFuture<>();

    @Override
    public void onSuccess(int result) {

    }

    @Override
    public void onSuccess(Object object) {
        try {
            res.complete((BufRingResult) object);
        } finally {
            release();
        }
    }

    @Override
    public void onError(Throwable ex) {
        try {
            res.completeExceptionally(ex);
        } finally {
            release();
        }
    }

    @Override
    public CompletableFuture<BufRingResult> getInner() {
        return res;
    }

    @Override
    public void release() {

    }

    static ByteBufferAsyncResultProvider newInstance() {
        return new ByteBufferAsyncResultProvider();
    }
}
