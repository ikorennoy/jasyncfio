package one.jasyncfio;


import cn.danielw.fop.ObjectFactory;

public class CompletableFutureResultProviderAllocator implements ObjectFactory<CompletableFutureResultProvider> {

    @Override
    public CompletableFutureResultProvider create() {
        return new CompletableFutureResultProvider();
    }

    @Override
    public void destroy(CompletableFutureResultProvider completableFutureResultProvider) {

    }

    @Override
    public boolean validate(CompletableFutureResultProvider completableFutureResultProvider) {
        return true;
    }
}
