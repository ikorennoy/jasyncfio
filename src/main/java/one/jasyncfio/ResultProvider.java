package one.jasyncfio;

interface ResultProvider<T> {
    void onSuccess(int result);

    void onSuccess(Object object);

    void onError(Throwable ex);

    T getInner();

    void release();
}
