package one.jasyncfio;

import cn.danielw.fop.ObjectFactory;

public class BufRingAsyncResultProviderAllocator implements ObjectFactory<BufRingAsyncResultProvider> {

    @Override
    public BufRingAsyncResultProvider create() {
        return new BufRingAsyncResultProvider();
    }

    @Override
    public void destroy(BufRingAsyncResultProvider bufRingAsyncResultProvider) {

    }

    @Override
    public boolean validate(BufRingAsyncResultProvider bufRingAsyncResultProvider) {
        return true;
    }
}
