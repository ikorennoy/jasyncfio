package one.jasyncfio;

interface CompletionCallback {
    void handle(int res, int flags, long userData);
}
