package one.jasyncfio;

public interface ResultProvider<T> {
    void onSuccess(int result);

    void onError(Throwable ex);

    T getInner();

    void release();
}
