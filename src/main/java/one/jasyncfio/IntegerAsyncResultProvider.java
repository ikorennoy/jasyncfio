package one.jasyncfio;

import cn.danielw.fop.DisruptorObjectPool;
import cn.danielw.fop.ObjectPool;
import cn.danielw.fop.PoolConfig;
import cn.danielw.fop.Poolable;

import java.util.concurrent.CompletableFuture;

class IntegerAsyncResultProvider implements ResultProvider<CompletableFuture<Integer>> {
    private static final IntegerAsyncResultProviderAllocator allocator = new IntegerAsyncResultProviderAllocator();

    private static final PoolConfig poolConfig = new PoolConfig() {
        {
            setPartitionSize(100);
            setMaxSize(50);
        }
    };
    private static final ObjectPool<IntegerAsyncResultProvider> pool = new DisruptorObjectPool<>(poolConfig, allocator);
    private Poolable<IntegerAsyncResultProvider> handle;

    private CompletableFuture<Integer> res;

    IntegerAsyncResultProvider() {
    }


    static IntegerAsyncResultProvider newInstance() {
        Poolable<IntegerAsyncResultProvider> handle = pool.borrowObject();
        IntegerAsyncResultProvider inst = handle.getObject();
        inst.handle = handle;
        inst.res = new CompletableFuture<>();
        return inst;
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
    public void onSuccess(Object object) {
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
