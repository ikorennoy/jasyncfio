package one.jasyncfio;

import cn.danielw.fop.DisruptorObjectPool;
import cn.danielw.fop.ObjectPool;
import cn.danielw.fop.PoolConfig;
import cn.danielw.fop.Poolable;

import java.util.concurrent.CompletableFuture;

class AsyncResultProvider implements ResultProvider<CompletableFuture<Integer>> {
    private static final AsyncResultProviderAllocator allocator = new AsyncResultProviderAllocator();

    private static final PoolConfig poolConfig = new PoolConfig() {
        {
            setPartitionSize(100);
            setMaxSize(50);
        }
    };
    private static final ObjectPool<AsyncResultProvider> pool = new DisruptorObjectPool<>(poolConfig, allocator);
    private Poolable<AsyncResultProvider> handle;

    private CompletableFuture<Integer> res;

    AsyncResultProvider() {
    }


    static AsyncResultProvider newInstance() {
        Poolable<AsyncResultProvider> handle = pool.borrowObject();
        AsyncResultProvider fileCommandResultCallback = handle.getObject();
        fileCommandResultCallback.handle = handle;
        fileCommandResultCallback.res = new CompletableFuture<>();
        return fileCommandResultCallback;
    }

    @Override
    public void onSuccess(int result) {
        try {
            res.complete(result);
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
    public CompletableFuture<Integer> getInner() {
        return res;
    }

    @Override
    public void release() {
        res = null;
        handle.close();
    }
}
