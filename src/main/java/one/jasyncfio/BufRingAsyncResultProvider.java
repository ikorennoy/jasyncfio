package one.jasyncfio;

import cn.danielw.fop.DisruptorObjectPool;
import cn.danielw.fop.ObjectPool;
import cn.danielw.fop.PoolConfig;
import cn.danielw.fop.Poolable;

import java.util.concurrent.CompletableFuture;

class BufRingAsyncResultProvider implements ResultProvider<CompletableFuture<BufRingResult>> {

    private static final BufRingAsyncResultProviderAllocator allocator = new BufRingAsyncResultProviderAllocator();

    private static final PoolConfig poolConfig = new PoolConfig() {
        {
            setPartitionSize(100);
            setMaxSize(50);
        }
    };

    private static final ObjectPool<BufRingAsyncResultProvider> pool = new DisruptorObjectPool<>(poolConfig, allocator);

    private Poolable<BufRingAsyncResultProvider> handle;
    private CompletableFuture<BufRingResult> res = new CompletableFuture<>();

    @Override
    public void onSuccess(int result) {
        throw new IllegalArgumentException();
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
        res = null;
        handle.close();
    }


    static BufRingAsyncResultProvider newInstance() {
        Poolable<BufRingAsyncResultProvider> handle = pool.borrowObject();
        BufRingAsyncResultProvider obj = handle.getObject();
        obj.handle = handle;
        obj.res = new CompletableFuture<>();
        return obj;
    }
}
