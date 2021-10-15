package one.jasyncfio;

public interface CompletionCallback {

    void handle(int fd, int res, int flags, byte op, int data);
}
