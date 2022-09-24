package one.jasyncfio;

public interface CompletionCallback {
    void handle(int res, int flags, long userData);
}
