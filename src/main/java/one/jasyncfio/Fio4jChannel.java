package one.jasyncfio;

public interface Fio4jChannel {
    void handleRead(int res, int data);

    void handleWrite(int res, int data);

    void handlePollAdd(int res, int data);

    void handlePollRemove(int res, int data);

    void handleConnect(int res, int data);

    void handleError(int res, int data);
}
