package one.jasyncfio;

import cn.danielw.fop.DisruptorObjectPool;
import cn.danielw.fop.ObjectPool;
import cn.danielw.fop.PoolConfig;
import cn.danielw.fop.Poolable;

import java.util.concurrent.CompletableFuture;

public class CompletableFutureResultProvider implements ResultProvider<CompletableFuture<Integer>> {
    private static final CompletableFutureResultProviderAllocator allocator = new CompletableFutureResultProviderAllocator();
    private static final PoolConfig poolConfig = new PoolConfig() {
        {
            setPartitionSize(50);
            setMinSize(100);
            setMaxSize(150);
        }
    };
    private static final ObjectPool<CompletableFutureResultProvider> pool = new DisruptorObjectPool<>(poolConfig, allocator);

    private CompletableFuture<Integer> completableFuture = null;
    private Poolable<CompletableFutureResultProvider> handle = null;

    @Override
    public void onSuccess(int result) {
        try {
            completableFuture.complete(result);
        } finally {
            release();
        }
    }

    @Override
    public void onError(Throwable ex) {
        try {
            completableFuture.completeExceptionally(ex);
        } finally {
            release();
        }
    }

    @Override
    public CompletableFuture<Integer> getInner() {
        return completableFuture;
    }

    @Override
    public void release() {
        completableFuture = null;
        handle.close();
    }

    public static CompletableFutureResultProvider newInstance() {
        Poolable<CompletableFutureResultProvider> handle = pool.borrowObject();
        CompletableFutureResultProvider object = handle.getObject();
        object.handle = handle;
        object.completableFuture = new CompletableFuture<>();
        return object;
    }

}
