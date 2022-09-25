package one.jasyncfio;

public class Main {
    public static void main(String[] args) {
        EventExecutor eventExecutor = EventExecutor.initDefault();

        IoUringBufRing ioUringBufRing = new IoUringBufRing(eventExecutor, 1024, 10);


    }
}
