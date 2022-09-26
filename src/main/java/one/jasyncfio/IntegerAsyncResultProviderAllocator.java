package one.jasyncfio;

import cn.danielw.fop.ObjectFactory;

class IntegerAsyncResultProviderAllocator implements ObjectFactory<IntegerAsyncResultProvider> {
    @Override
    public IntegerAsyncResultProvider create() {
        return new IntegerAsyncResultProvider();
    }

    @Override
    public void destroy(IntegerAsyncResultProvider integerAsyncResultProvider) {

    }

    @Override
    public boolean validate(IntegerAsyncResultProvider integerAsyncResultProvider) {
        return true;
    }
}
