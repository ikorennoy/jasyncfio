package one.jasyncfio;

import cn.danielw.fop.ObjectFactory;

class AsyncResultProviderAllocator implements ObjectFactory<AsyncResultProvider> {
    @Override
    public AsyncResultProvider create() {
        return new AsyncResultProvider();
    }

    @Override
    public void destroy(AsyncResultProvider asyncResultProvider) {

    }

    @Override
    public boolean validate(AsyncResultProvider asyncResultProvider) {
        return true;
    }
}
